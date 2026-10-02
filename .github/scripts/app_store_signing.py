# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

"""
The signing identities of the Apple builds: one certificate of each type for a signing key, which every run finds again,
and renews before it expires, through the App Store Connect API.

Apple's distribution certificates expire after a year, and a certificate kept in a repository secret is a release that
fails on the day it does. The private key it was made for does not expire, and the App Store Connect API key may create
certificates and provisioning profiles - so the APPLE_SIGNING_KEY secret holds the key alone, and a run asks the API for
the certificates of the type it needs, takes the one made for that key that expires last (`certificate`), and creates a
new one for the same key where there is none, or where that one has less than RENEWAL_DAYS left. A certificate is never
revoked: a build has to stay signed by a valid certificate until App Review has approved it, and one whose certificate
is revoked in the meantime is refused as an invalid binary (ITMS-90238, CSSMERR_TP_CERT_REVOKED), even after it was
attached and submitted. The old one is left to expire instead, months after the last build it signed was decided. The
renewal is early enough that no build is ever signed with a certificate that could expire while it is in review.

Campfire's pipeline signs with the same key and so with the same certificate, which matters because Apple allows a team
only two Apple Distribution certificates: the two pipelines hold one between them, and the other place is the renewal's.
A certificate somebody made by hand in Xcode takes a place too, which is what a creation refused with 409 means. Its
copy of this script also finds the Mac App Store certificates and profiles, which the Showcase does not need.

The Developer ID Application certificate the Steam build is signed with works the same way, for a key of its own in the
DEVELOPER_ID_SIGNING_KEY secret: whoever holds that key can sign software every Mac trusts as the team's, and the only
remedy, revoking the certificate, stops every copy already shipped from launching - so it is kept apart from the store
key, whose certificates only ever sign builds Apple re-signs. Never revoking is not a choice there but a requirement.
A Developer ID certificate lasts five years, and Apple may keep creating one to the Account Holder rather than to an API
key, so it is renewed DEVELOPER_ID_RENEWAL_DAYS ahead: a refusal leaves months for the renewal by hand, which takes a
certificate request made from the same key and leaves the secret as it is. An expired one stops new builds only:
notarized copies keep launching.

It only needs the Python standard library and the system's openssl.

    app_store_signing.py certificate <certificateType>[,<certificateType>...] <keychain>

The certificate is looked for among every type listed, and a new one is of the first. Reads ASC_KEY_ID, ASC_ISSUER_ID
and ASC_KEY_PATH (the .p8 file), and APPLE_SIGNING_KEY_PATH, the signing key as PEM.
"""

import base64
import json
import os
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone

API = "https://api.appstoreconnect.apple.com/v1"
# LibreSSL, which every macOS has: it signs the token the same way everywhere, and it is not whatever a runner image
# happens to put first on the PATH.
OPENSSL = "/usr/bin/openssl"
# Two months: far longer than any review takes, so a build is never signed with a certificate that could expire before
# App Review has decided it, and long enough that a release in the last weeks is not the only chance to renew.
RENEWAL_DAYS = 60
DEVELOPER_ID_RENEWAL_DAYS = 180
# The states of a version that has been submitted and is not on the store yet: the build attached to one is still checked
# against its certificate.
AWAITING_STATES = {
    "READY_FOR_REVIEW", "WAITING_FOR_REVIEW", "IN_REVIEW", "WAITING_FOR_EXPORT_COMPLIANCE", "PENDING_CONTRACT",
    "ACCEPTED", "PROCESSING_FOR_APP_STORE", "PENDING_APPLE_RELEASE", "PENDING_DEVELOPER_RELEASE",
}


def fail(message):
    print(f"::error::{message}", file=sys.stderr)
    sys.exit(1)


def token():
    """An ES256 JSON Web Token for the API key, valid for ten minutes, which is the most the API accepts."""
    encode = lambda data: base64.urlsafe_b64encode(data).rstrip(b"=")
    now = int(time.time())
    header = encode(json.dumps({"alg": "ES256", "kid": os.environ["ASC_KEY_ID"], "typ": "JWT"}).encode())
    claims = encode(json.dumps({
        "iss": os.environ["ASC_ISSUER_ID"],
        "iat": now,
        "exp": now + 600,
        "aud": "appstoreconnect-v1",
    }).encode())
    message = header + b"." + claims
    der = subprocess.run(
        [OPENSSL, "dgst", "-sha256", "-sign", os.environ["ASC_KEY_PATH"]],
        input=message, capture_output=True, check=True,
    ).stdout
    # openssl writes the signature as an ASN.1 sequence of two integers, and a JWT wants them as 32 bytes each.
    parsed = subprocess.run([OPENSSL, "asn1parse", "-inform", "DER"], input=der, capture_output=True, check=True).stdout
    integers = [line.split(":")[-1] for line in parsed.decode().splitlines() if "INTEGER" in line]
    signature = b"".join(bytes.fromhex(value.rjust(64, "0"))[-32:] for value in integers)
    return (message + b"." + encode(signature)).decode()


def request(method, path, body=None, missing_ok=False, refused_ok=False):
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Authorization": f"Bearer {token()}", "Content-Type": "application/json"}
    try:
        with urllib.request.urlopen(urllib.request.Request(API + path, data=data, headers=headers, method=method)) as response:
            content = response.read()
            return json.loads(content) if content else None
    except urllib.error.HTTPError as error:
        details = error.read().decode()
        try:
            details = "; ".join(item.get("detail") or item.get("title", "") for item in json.loads(details)["errors"])
        except (ValueError, KeyError):
            pass
        if missing_ok and error.code == 404:
            return None
        if refused_ok and error.code in (403, 409):
            print(f"{method} {path} answered {error.code}: {details}", file=sys.stderr)
            return None
        fail(f"{method} {path} answered {error.code}: {details}")


def run(*command, input=None):
    return subprocess.run(command, input=input, check=True, capture_output=True).stdout


def parse_expiration(text):
    """The API's expirationDate, such as 2027-09-29T19:26:01.000+00:00, as an aware datetime."""
    return datetime.fromisoformat(text.replace("Z", "+00:00"))


def newest_valid(certificates, now):
    """The certificate of the list that expires last, of those that have not expired yet, or None."""
    valid = [item for item in certificates if item["expiration"] > now]
    return max(valid, key=lambda item: item["expiration"], default=None)


def needs_renewal(certificate_type, certificate, now):
    days = DEVELOPER_ID_RENEWAL_DAYS if certificate_type.startswith("DEVELOPER_ID") else RENEWAL_DAYS
    return certificate["expiration"] - now < timedelta(days=days)


def key_modulus():
    return run(OPENSSL, "rsa", "-in", os.environ["APPLE_SIGNING_KEY_PATH"], "-noout", "-modulus").strip()


def certificates_for_key(certificate_types):
    """Every certificate of the types that was made for the signing key, expired ones included."""
    listed = [item for certificate_type in certificate_types for item in request(
        "GET", f"/certificates?filter[certificateType]={certificate_type}&limit=200"
        "&fields[certificates]=name,certificateContent,expirationDate",
    )["data"]]
    modulus = key_modulus()
    found = []
    for item in listed:
        content = base64.b64decode(item["attributes"]["certificateContent"])
        # The public key is all that ties a certificate to the key: the API says nothing else about which key it was for.
        if run(OPENSSL, "x509", "-inform", "DER", "-noout", "-modulus", input=content).strip() == modulus:
            found.append({
                "id": item["id"],
                "name": item["attributes"].get("name"),
                "content": content,
                "expiration": parse_expiration(item["attributes"]["expirationDate"]),
            })
    return found


def create_certificate(certificate_type):
    """A new certificate of the type for the signing key, or None where Apple refuses to make one."""
    with tempfile.TemporaryDirectory() as directory:
        csr = os.path.join(directory, "request.csr")
        run(OPENSSL, "req", "-new", "-key", os.environ["APPLE_SIGNING_KEY_PATH"], "-out", csr, "-subj", "/CN=GitHub Actions")
        with open(csr) as file:
            csr_content = file.read()
    created = request("POST", "/certificates", {"data": {
        "type": "certificates",
        "attributes": {"certificateType": certificate_type, "csrContent": csr_content},
    }}, refused_ok=True)
    if created is None:
        return None
    attributes = created["data"]["attributes"]
    print(f"Created {certificate_type} certificate {created['data']['id']} ({attributes.get('name')}), "
          f"expiring {attributes['expirationDate']}")
    return {
        "id": created["data"]["id"],
        "name": attributes.get("name"),
        "content": base64.b64decode(attributes["certificateContent"]),
        "expiration": parse_expiration(attributes["expirationDate"]),
    }


def renewal_by_hand(certificate_type):
    """What to do where Apple refuses to make a certificate of the type."""
    if certificate_type.startswith("DEVELOPER_ID"):
        return ("Make the certificate request from the signing key - openssl req -new -key <the key> -subj '/CN=Developer "
                "ID' -out request.csr - and upload it at https://developer.apple.com/account/resources/certificates/add as a "
                "Developer ID Application certificate, from the Account Holder's account. The secret stays as it is, and "
                "the old certificate must not be revoked.")
    return ("The team already has as many of the type as it may: revoke one in the developer account that no build waiting "
            "for App Review was signed with.")


def import_certificate(certificate_types, keychain):
    """Finds or renews the certificate of the types for the signing key, and imports both into the keychain."""
    types = certificate_types.split(",")
    now = datetime.now(timezone.utc)
    current = newest_valid(certificates_for_key(types), now)
    if current is None or needs_renewal(types[0], current, now):
        created = create_certificate(types[0])
        if created is not None:
            current = created
        elif current is not None:
            print(f"::warning::The {types[0]} certificate {current['id']} expires on {current['expiration']:%Y-%m-%d} and "
                  f"Apple refused a new one. {renewal_by_hand(types[0])} The next run takes it from there.")
        else:
            fail(f"There is no valid {types[0]} certificate for the signing key, and Apple refused a new one. "
                 f"{renewal_by_hand(types[0])} Then run this again.")
    else:
        print(f"Using certificate {current['id']} ({current['name']}), expiring {current['expiration']:%Y-%m-%d}")
    with tempfile.TemporaryDirectory() as directory:
        # The format `security` imports with -f openssl, whichever one the secret was written in.
        key = os.path.join(directory, "key.pem")
        certificate = os.path.join(directory, "certificate.cer")
        run(OPENSSL, "rsa", "-in", os.environ["APPLE_SIGNING_KEY_PATH"], "-out", key)
        with open(certificate, "wb") as file:
            file.write(current["content"])
        tools = ["-T", "/usr/bin/codesign", "-T", "/usr/bin/security"]
        imported = subprocess.run(
            ["security", "import", key, "-k", keychain, "-t", "priv", "-f", "openssl", *tools], capture_output=True, text=True,
        )
        if imported.returncode != 0:
            fail(f"security could not import the signing key: {imported.stderr.strip()}")
        run("security", "import", certificate, "-k", keychain, "-t", "cert", "-f", "x509")


def awaiting_versions(app_id, platform):
    """The platform's versions that have been submitted and are not on the store yet."""
    versions = request(
        "GET", f"/apps/{app_id}/appStoreVersions?filter[platform]={platform}&limit=50"
        "&fields[appStoreVersions]=versionString,appStoreState",
    )["data"]
    return [item for item in versions if item["attributes"]["appStoreState"] in AWAITING_STATES]


if __name__ == "__main__":
    command, arguments = (sys.argv[1], sys.argv[2:]) if len(sys.argv) > 1 else (None, [])
    if command == "certificate" and len(arguments) == 2:
        import_certificate(*arguments)
    else:
        fail(next(part for part in __doc__.split("\n\n") if part.lstrip().startswith("app_store_signing.py")))
