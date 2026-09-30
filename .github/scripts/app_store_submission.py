# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

"""
Submits an uploaded build for App Review, which is everything a release used to need App Store Connect open for.

    app_store_submission.py <bundle identifier> <IOS | MAC_OS> <version> <build number> <release notes file> [--prepare]

It waits for App Store Connect to finish processing the build (processing is also where a build is refused, with the
reason mailed; this fails with the state instead of waiting forever), takes the platform's version for the release -
the one that already carries that version string, or else the editable one, renamed, or else a new one, set to be
released as soon as it is approved - attaches the build, writes the release notes as its "What's New" and submits it.
With --prepare it stops before the submission, leaving the version ready in App Store Connect for somebody to add the
release's screenshots to and submit it there; a later run with the same build submits it as it is.

A draft that is already there - a version prepared in App Store Connect for this release, with its new screenshots,
whether or not it has been added to a review submission, or one that was rejected - is used as it is: the build is
attached to it and its "What's New" replaced with the notes, everything else in it is kept exactly as it is, the release
option included, and it is submitted, in the review submission it is already in where there is one.

A version that is already waiting for review or further along with this very build is left as it is, so a run that is
repeated does not fail on its own success. Where another build is waiting for Apple - under review, or approved
and not on the store yet - this one is not submitted at all, since a platform takes one version at a time: it stays in
TestFlight, the run says so with a warning rather than failing, and it is submitted by hand once the other one has been
decided, or replaced by the next release's build. That is decided before waiting for processing, which a build that is
not going to be submitted has no reason to wait for.

Uses the App Store Connect API the way app_store_signing.py does, and the same environment.
"""

import sys
import time

from app_store_signing import awaiting_versions, fail, request

# The states in which a version still takes a build, notes and a submission. READY_FOR_REVIEW is a version added to a
# review submission that has not been submitted yet, which is what "Add for Review" in App Store Connect leaves behind.
EDITABLE_STATES = {
    "PREPARE_FOR_SUBMISSION", "READY_FOR_REVIEW", "DEVELOPER_REJECTED", "REJECTED", "METADATA_REJECTED", "INVALID_BINARY",
}
# The states of a review submission that has not been handed to App Review, or has been handed back with a rejection:
# the platform has at most one of them, and a new one cannot be started next to it.
OPEN_SUBMISSION_STATES = "READY_FOR_REVIEW,UNRESOLVED_ISSUES"
PROCESSING_TIMEOUT_SECONDS = 90 * 60
POLL_INTERVAL_SECONDS = 60
# The most App Store Connect takes for "What's New".
WHATS_NEW_LIMIT = 4000


def find_app(bundle_identifier):
    apps = request("GET", f"/apps?filter[bundleId]={bundle_identifier}")["data"]
    app = next((item for item in apps if item["attributes"]["bundleId"] == bundle_identifier), None)
    if app is None:
        fail(f"There is no app with the bundle ID {bundle_identifier} in App Store Connect.")
    return app


def wait_for_build(app_id, platform, version, build_number):
    """The build once it has been processed; a build App Store Connect does not list yet is still being received."""
    path = (
        f"/builds?filter[app]={app_id}&filter[version]={build_number}"
        f"&filter[preReleaseVersion.version]={version}&filter[preReleaseVersion.platform]={platform}"
        "&fields[builds]=version,processingState"
    )
    deadline = time.time() + PROCESSING_TIMEOUT_SECONDS
    while True:
        builds = request("GET", path)["data"]
        state = builds[0]["attributes"]["processingState"] if builds else "NOT_LISTED_YET"
        if state == "VALID":
            print(f"Build {version} ({build_number}) has been processed.")
            return builds[0]
        if state in {"FAILED", "INVALID"}:
            fail(f"App Store Connect refused build {version} ({build_number}): {state}. The reason is in the mail it sent.")
        if time.time() > deadline:
            fail(f"Build {version} ({build_number}) was still {state} after {PROCESSING_TIMEOUT_SECONDS // 60} minutes.")
        print(f"Build {version} ({build_number}) is {state}, asking again in a minute.")
        sys.stdout.flush()
        time.sleep(POLL_INTERVAL_SECONDS)


def version_for_release(app_id, platform, version):
    """The platform's version to submit, and whether an earlier one was ever released, which decides "What's New"."""
    versions = request(
        "GET", f"/apps/{app_id}/appStoreVersions?filter[platform]={platform}&limit=50&include=build"
        "&fields[appStoreVersions]=versionString,appStoreState,releaseType,build",
    )["data"]
    has_earlier = any(
        item["attributes"]["versionString"] != version and item["attributes"]["appStoreState"] not in EDITABLE_STATES
        for item in versions
    )
    existing = next((item for item in versions if item["attributes"]["versionString"] == version), None)
    if existing is None:
        existing = next((item for item in versions if item["attributes"]["appStoreState"] in EDITABLE_STATES), None)
    if existing is None:
        created = request("POST", "/appStoreVersions", {"data": {
            "type": "appStoreVersions",
            "attributes": {"platform": platform, "versionString": version, "releaseType": "AFTER_APPROVAL"},
            "relationships": {"app": {"data": {"type": "apps", "id": app_id}}},
        }})["data"]
        print(f"Created the {platform} version {version}.")
        return created, has_earlier
    # A draft keeps its release option: whoever prepared it in App Store Connect may have chosen to release it by hand.
    if existing["attributes"]["appStoreState"] in EDITABLE_STATES and existing["attributes"]["versionString"] != version:
        print(f"Renaming the editable {platform} version {existing['attributes']['versionString']} to {version}.")
        request("PATCH", f"/appStoreVersions/{existing['id']}", {"data": {
            "type": "appStoreVersions", "id": existing["id"], "attributes": {"versionString": version},
        }})
    return existing, has_earlier


def attached_build_id(version):
    build = request("GET", f"/appStoreVersions/{version['id']}/relationships/build")["data"]
    return build["id"] if build else None


def attached_build_number(version):
    build = request("GET", f"/appStoreVersions/{version['id']}/build?fields[builds]=version", missing_ok=True)
    return ((build or {}).get("data") or {}).get("attributes", {}).get("version")


def write_whats_new(version, notes):
    localizations = request("GET", f"/appStoreVersions/{version['id']}/appStoreVersionLocalizations")["data"]
    if not localizations:
        fail("The version has no localizations to write the release notes into.")
    # The listing is in English only; any other localization it gains gets the same text rather than an old one.
    for localization in localizations:
        request("PATCH", f"/appStoreVersionLocalizations/{localization['id']}", {"data": {
            "type": "appStoreVersionLocalizations", "id": localization["id"], "attributes": {"whatsNew": notes},
        }})
        print(f"Wrote the release notes for {localization['attributes']['locale']}.")


def submit(app_id, platform, version):
    """Adds the version to the platform's open review submission, or to a new one, and submits it."""
    open_submissions = request(
        "GET", f"/reviewSubmissions?filter[app]={app_id}&filter[platform]={platform}&filter[state]={OPEN_SUBMISSION_STATES}",
    )["data"]
    if open_submissions:
        submission = open_submissions[0]
    else:
        submission = request("POST", "/reviewSubmissions", {"data": {
            "type": "reviewSubmissions",
            "attributes": {"platform": platform},
            "relationships": {"app": {"data": {"type": "apps", "id": app_id}}},
        }})["data"]
    items = request("GET", f"/reviewSubmissions/{submission['id']}/items?include=appStoreVersion")["data"]
    listed = any(
        (item["relationships"].get("appStoreVersion", {}).get("data") or {}).get("id") == version["id"] for item in items
    )
    if not listed:
        request("POST", "/reviewSubmissionItems", {"data": {
            "type": "reviewSubmissionItems",
            "relationships": {
                "reviewSubmission": {"data": {"type": "reviewSubmissions", "id": submission["id"]}},
                "appStoreVersion": {"data": {"type": "appStoreVersions", "id": version["id"]}},
            },
        }})
    request("PATCH", f"/reviewSubmissions/{submission['id']}", {"data": {
        "type": "reviewSubmissions", "id": submission["id"], "attributes": {"submitted": True},
    }})


def main(bundle_identifier, platform, version, build_number, notes_file, prepare_only):
    with open(notes_file) as file:
        notes = file.read().strip()
    if len(notes) > WHATS_NEW_LIMIT:
        fail(f"The release notes are {len(notes)} characters long, and App Store Connect takes {WHATS_NEW_LIMIT}.")
    app = find_app(bundle_identifier)
    # A draft that was added to a review submission counts as waiting for Apple to the signing script, which keeps its
    # certificate either way, but it has not been submitted and is this release's to take.
    awaiting = [
        item for item in awaiting_versions(app["id"], platform) if item["attributes"]["appStoreState"] not in EDITABLE_STATES
    ]
    if awaiting:
        attributes = awaiting[0]["attributes"]
        awaiting_build = attached_build_number(awaiting[0])
        if attributes["versionString"] == version and awaiting_build == build_number:
            print(f"The {platform} version {version} is already {attributes['appStoreState']} with build {build_number}; nothing to do.")
        else:
            print(f"::warning::The {platform} version {attributes['versionString']} is {attributes['appStoreState']} with "
                  f"build {awaiting_build}, and a platform takes one version at a time, so build {version} ({build_number}) "
                  "stays in TestFlight without being submitted. Submit it by hand once that one has been decided, or let "
                  "the next release replace it.")
        return
    build = wait_for_build(app["id"], platform, version, build_number)
    store_version, has_earlier = version_for_release(app["id"], platform, version)
    state = store_version["attributes"]["appStoreState"]
    if state not in EDITABLE_STATES:
        if attached_build_id(store_version) == build["id"]:
            print(f"The {platform} version {version} is already {state} with build {build_number}; nothing to do.")
            return
        fail(f"The {platform} version {version} is already {state} with another build, and a released version takes no "
             "new one: raise showcase.versionName for this release.")
    request("PATCH", f"/appStoreVersions/{store_version['id']}/relationships/build", {
        "data": {"type": "builds", "id": build["id"]},
    })
    print(f"Attached build {build_number} to the {platform} version {version}.")
    # The first version of a platform has nothing to say "new" about, and App Store Connect refuses the field there.
    if has_earlier:
        if not notes:
            fail("An update needs release notes for its \"What's New\", and there are none.")
        write_whats_new(store_version, notes)
    if prepare_only:
        print(f"Prepared the {platform} version {version} ({build_number}) without submitting it: add the screenshots in "
              "App Store Connect and submit it there.")
        return
    submit(app["id"], platform, store_version)
    print(f"Submitted the {platform} version {version} ({build_number}) for review. It is released as soon as it is approved.")


if __name__ == "__main__":
    arguments = sys.argv[1:]
    prepare = arguments[5:] == ["--prepare"]
    if len(arguments) != (6 if prepare else 5) or arguments[1] not in {"IOS", "MAC_OS"}:
        fail(__doc__.strip().split("\n\n")[1].strip())
    main(*arguments[:5], prepare)
