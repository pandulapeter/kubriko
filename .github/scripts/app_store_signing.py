# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

"""
Signing identities that last one workflow run, and until the next one where that run uploaded a build.

Apple's distribution certificates expire after a year, and a certificate kept in a repository secret is a release that
fails on the day it does. The App Store Connect API key does not expire, and an Admin key may create certificates and
provisioning profiles - so a run makes its own: a private key that never leaves the runner, a certificate for it,
the profiles that name that certificate. A build has to stay signed by a valid certificate until App Review has
approved it, not only until it is uploaded or processed: one whose certificate is revoked in the meantime is refused as
an invalid binary (ITMS-90238, CSSMERR_TP_CERT_REVOKED), even after it has been attached and submitted. So a run that
uploaded a build notes which build it was in the state file (`record-build`) and keeps its identities, and the workflow
hands the state file on as an artifact, which the next run of the same workflow revokes before it makes its own
(`cleanup-earlier`) - unless App Store Connect says the build is attached to a version that is still waiting for review,
in review or not on the store yet, in which case it is left alone and the artifact kept, to be asked about again by the
run after. A state file from before builds were recorded is kept for as long as any version of the platform is waiting
for Apple. A build that was uploaded and not submitted is revoked by the next run, whose own build replaces it. A run
that uploaded nothing revokes its own at the end. Revoking a distribution certificate does not touch what Apple has
already released, which it signs again. Apple tells certificates made here and certificates made by hand apart in no way the API
shows, so what a run made is known only from its state file: an artifact that expires before the next run leaves its
certificates to expire on their own after a year. Never use this for a Developer ID certificate: an app signed outside
the store is checked against it on every Mac that opens it, and revoking it breaks every copy already downloaded.

Everything this creates is written into a state file first, and `cleanup` removes exactly that and nothing else, so an
identity somebody made by hand is never touched. It only needs the Python standard library and the system's openssl.

    app_store_signing.py certificate <certificateType> <keychain>
    app_store_signing.py profile <profileType> <bundle identifier> <certificateType> <output file>
    app_store_signing.py record-build <IOS | MAC_OS> <version> <build number>
    app_store_signing.py cleanup
    app_store_signing.py cleanup-earlier <artifact name> <bundle identifier> <IOS | MAC_OS>

Reads ASC_KEY_ID, ASC_ISSUER_ID and ASC_KEY_PATH (the .p8 file), and APP_STORE_SIGNING_STATE, the state file;
`cleanup-earlier` also GITHUB_REPOSITORY and GH_TOKEN, for the gh command line tool, with the actions: write permission.
"""

import base64
import glob
import json
import os
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

API = "https://api.appstoreconnect.apple.com/v1"
# LibreSSL, which every macOS has: it signs the token the same way everywhere, and it is not whatever a runner image
# happens to put first on the PATH.
OPENSSL = "/usr/bin/openssl"
# The states of a version that has been submitted and is not on the store yet: the build attached to one is still checked
# against its certificate. An approved one counts until it is on the store: keeping a certificate one run longer costs
# nothing, and revoking it early gets the build refused.
AWAITING_STATES = {
    "READY_FOR_REVIEW", "WAITING_FOR_REVIEW", "IN_REVIEW", "WAITING_FOR_EXPORT_COMPLIANCE", "PENDING_CONTRACT",
    "ACCEPTED", "PROCESSING_FOR_APP_STORE", "PENDING_APPLE_RELEASE", "PENDING_DEVELOPER_RELEASE",
}
# The states of a version that has not been submitted but may still be with the build attached to it: one a run prepared
# and left for screenshots, or one rejected and sent again as it is. Its build is submitted later, by hand, and is
# refused if its certificate was revoked in between.
UNSUBMITTED_STATES = {"PREPARE_FOR_SUBMISSION", "DEVELOPER_REJECTED", "REJECTED", "METADATA_REJECTED"}


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


def request(method, path, body=None, missing_ok=False):
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Authorization": f"Bearer {token()}", "Content-Type": "application/json"}
    try:
        with urllib.request.urlopen(urllib.request.Request(API + path, data=data, headers=headers, method=method)) as response:
            content = response.read()
            return json.loads(content) if content else None
    except urllib.error.HTTPError as error:
        if missing_ok and error.code == 404:
            return None
        details = error.read().decode()
        try:
            details = "; ".join(item.get("detail") or item.get("title", "") for item in json.loads(details)["errors"])
        except (ValueError, KeyError):
            pass
        fail(f"{method} {path} answered {error.code}: {details}")


def state_path():
    return os.environ.get("APP_STORE_SIGNING_STATE") or os.path.join(os.environ.get("RUNNER_TEMP", "."), "app-store-signing.json")


def read_state():
    try:
        with open(state_path()) as file:
            return json.load(file)
    except FileNotFoundError:
        return {"certificates": {}, "profiles": []}


def write_state(state):
    with open(state_path(), "w") as file:
        json.dump(state, file)


def run(*command):
    subprocess.run(command, check=True, capture_output=True)


def create_certificate(certificate_type, keychain):
    """Creates a key and a certificate of the given type for it, and imports both into the keychain."""
    with tempfile.TemporaryDirectory() as directory:
        key = os.path.join(directory, "key.pem")
        csr = os.path.join(directory, "request.csr")
        certificate = os.path.join(directory, "certificate.cer")
        run(OPENSSL, "genrsa", "-out", key, "2048")
        run(OPENSSL, "req", "-new", "-key", key, "-out", csr, "-subj", "/CN=GitHub Actions")
        with open(csr) as file:
            csr_content = file.read()
        created = request("POST", "/certificates", {"data": {
            "type": "certificates",
            "attributes": {"certificateType": certificate_type, "csrContent": csr_content},
        }})["data"]
        # Recorded before anything else can fail, so that cleanup revokes it whatever happens next.
        state = read_state()
        state["certificates"][certificate_type] = created["id"]
        write_state(state)
        with open(certificate, "wb") as file:
            file.write(base64.b64decode(created["attributes"]["certificateContent"]))
        tools = ["-T", "/usr/bin/codesign", "-T", "/usr/bin/productbuild", "-T", "/usr/bin/security"]
        run("security", "import", key, "-k", keychain, "-t", "priv", "-f", "openssl", *tools)
        run("security", "import", certificate, "-k", keychain, "-t", "cert", "-f", "x509")
    attributes = created["attributes"]
    print(f"Created {certificate_type} certificate {created['id']} ({attributes.get('name')}), expiring {attributes.get('expirationDate')}")


def create_profile(profile_type, bundle_identifier, certificate_type, output):
    """Creates a profile of the given type for the bundle ID and the certificate this run created, and saves it."""
    certificate = read_state()["certificates"].get(certificate_type)
    if not certificate:
        fail(f"This run has created no {certificate_type} certificate to make a profile for.")
    matches = request("GET", f"/bundleIds?filter[identifier]={bundle_identifier}&limit=200")["data"]
    # The filter matches prefixes as well, so com.example.app also finds com.example.app.widget.
    bundle = next((item for item in matches if item["attributes"]["identifier"] == bundle_identifier), None)
    if bundle is None:
        fail(f"There is no App ID {bundle_identifier} in the developer account.")
    # Profile names are unique in an account; the run's id keeps two runs of the same workflow apart.
    name = f"CI {bundle_identifier} {os.environ.get('GITHUB_RUN_ID', int(time.time()))}"
    created = request("POST", "/profiles", {"data": {
        "type": "profiles",
        "attributes": {"name": name, "profileType": profile_type},
        "relationships": {
            "bundleId": {"data": {"type": "bundleIds", "id": bundle["id"]}},
            "certificates": {"data": [{"type": "certificates", "id": certificate}]},
        },
    }})["data"]
    state = read_state()
    state["profiles"].append(created["id"])
    write_state(state)
    with open(output, "wb") as file:
        file.write(base64.b64decode(created["attributes"]["profileContent"]))
    print(f"Created {profile_type} profile {created['id']} ({name}) for {bundle_identifier}")


def remove(state):
    """Deletes the profiles and revokes the certificates of a state file; one that is gone already is not an error."""
    for profile in state["profiles"]:
        request("DELETE", f"/profiles/{profile}", missing_ok=True)
        print(f"Deleted profile {profile}")
    for certificate_type, certificate in state["certificates"].items():
        request("DELETE", f"/certificates/{certificate}", missing_ok=True)
        print(f"Revoked {certificate_type} certificate {certificate}")


def cleanup():
    """Deletes the profiles and revokes the certificates this run created, and nothing else."""
    remove(read_state())
    write_state({"certificates": {}, "profiles": []})


def gh(*arguments):
    return subprocess.run(["gh", *arguments], check=True, capture_output=True, text=True).stdout


def record_build(platform, version, build_number):
    """Notes in the state file which build its identities signed, which is what `cleanup-earlier` asks Apple about."""
    state = read_state()
    state["build"] = {"platform": platform, "version": version, "number": build_number}
    write_state(state)


def find_app_id(bundle_identifier):
    apps = request("GET", f"/apps?filter[bundleId]={bundle_identifier}")["data"]
    app = next((item for item in apps if item["attributes"]["bundleId"] == bundle_identifier), None)
    if app is None:
        fail(f"There is no app with the bundle ID {bundle_identifier} in App Store Connect.")
    return app["id"]


def awaiting_versions(app_id, platform):
    """The platform's versions that have been submitted and are not on the store yet."""
    versions = request(
        "GET", f"/apps/{app_id}/appStoreVersions?filter[platform]={platform}&limit=50"
        "&fields[appStoreVersions]=versionString,appStoreState",
    )["data"]
    return [item for item in versions if item["attributes"]["appStoreState"] in AWAITING_STATES]


def pending_version_of_build(app_id, build):
    """The version the build is attached to, where that one is not on the store yet and may still be submitted with it."""
    builds = request(
        "GET", f"/builds?filter[app]={app_id}&filter[version]={build['number']}"
        f"&filter[preReleaseVersion.version]={build['version']}&filter[preReleaseVersion.platform]={build['platform']}",
    )["data"]
    if not builds:
        return None
    version = (request("GET", f"/builds/{builds[0]['id']}/appStoreVersion", missing_ok=True) or {}).get("data")
    return version if version and version["attributes"]["appStoreState"] in AWAITING_STATES | UNSUBMITTED_STATES else None


def still_needed(app_id, platform, state):
    """Why the identities of a state file must not be revoked yet, or None where they may be."""
    build = state.get("build")
    if build:
        version = pending_version_of_build(app_id, build)
        if version:
            return (f"build {build['version']} ({build['number']}) is attached to the {platform} version "
                    f"{version['attributes']['versionString']}, which is {version['attributes']['appStoreState']}")
        return None
    # A state file that names no build tells nothing about which one it signed, so it is kept for as long as any version
    # of the platform is waiting for Apple, since that may be the one.
    awaiting = awaiting_versions(app_id, platform)
    if awaiting:
        version = awaiting[0]["attributes"]
        return f"the {platform} version {version['versionString']} is {version['appStoreState']}"
    return None


def cleanup_earlier(artifact_name, bundle_identifier, platform):
    """
    Revokes what earlier runs kept for their builds, except for a build that is still waiting for Apple, and deletes each
    artifact once nothing in it is kept any more - so identities that are kept are asked about again by the next run.
    """
    repository = os.environ["GITHUB_REPOSITORY"]
    artifacts = json.loads(gh("api", f"repos/{repository}/actions/artifacts?name={artifact_name}&per_page=100"))["artifacts"]
    app_id = None
    for artifact in artifacts:
        if artifact["expired"]:
            continue
        run_id = artifact["workflow_run"]["id"]
        kept = False
        with tempfile.TemporaryDirectory() as directory:
            gh("run", "download", str(run_id), "--repo", repository, "--name", artifact_name, "--dir", directory)
            for path in glob.glob(os.path.join(directory, "*.json")):
                with open(path) as file:
                    state = json.load(file)
                app_id = app_id or find_app_id(bundle_identifier)
                reason = still_needed(app_id, platform, state)
                if reason:
                    print(f"Keeping what run {run_id} made, since {reason}: revoking it would get that build refused.")
                    kept = True
                else:
                    remove(state)
        if not kept:
            gh("api", "--method", "DELETE", f"repos/{repository}/actions/artifacts/{artifact['id']}")
            print(f"Removed what run {run_id} kept for its build.")


if __name__ == "__main__":
    command, arguments = (sys.argv[1], sys.argv[2:]) if len(sys.argv) > 1 else (None, [])
    if command == "certificate" and len(arguments) == 2:
        create_certificate(*arguments)
    elif command == "profile" and len(arguments) == 4:
        create_profile(*arguments)
    elif command == "cleanup" and not arguments:
        cleanup()
    elif command == "record-build" and len(arguments) == 3:
        record_build(*arguments)
    elif command == "cleanup-earlier" and len(arguments) == 3:
        cleanup_earlier(*arguments)
    else:
        fail(__doc__.strip().split("\n\n")[3])
