<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# One-time setup for `[Showcase] Publish Desktop`

Everything that [`showcase-desktop-publish.yml`](workflows/showcase-desktop-publish.yml) needs before its first run. The
workflow builds the Windows, macOS and Linux versions of the Showcase app, uploads them to Steam as a single build and sets
that build live on a beta branch. Promoting it to the `default` branch stays a manual click, because
[Steam does not let a build script do that](https://partner.steamgames.com/doc/sdk/uploading).

At the end, the repository has to hold the following. The first job of the workflow checks all of it in about a minute and
names whatever is missing or malformed, so it is safe to run the workflow to see how far along the setup is.

| Kind | Name | Status |
|---|---|---|
| Secret | `STEAM_USERNAME` | new, [step 5](#5-save-the-login-token-of-the-build-account) |
| Secret | `STEAM_CONFIG_VDF_BASE64` | new, [step 5](#5-save-the-login-token-of-the-build-account) |
| Secret | `MACOS_DEVELOPER_ID_CERTIFICATE_BASE64` | new, [step 7](#7-export-the-developer-id-certificate) |
| Secret | `MACOS_DEVELOPER_ID_CERTIFICATE_PASSWORD` | new, [step 7](#7-export-the-developer-id-certificate) |
| Secret | `APP_STORE_CONNECT_KEY_ID`, `APP_STORE_CONNECT_ISSUER_ID`, `APP_STORE_CONNECT_PRIVATE_KEY` | already set for the iOS workflow, reused for notarization |
| Variable | `STEAM_DEPOT_ID_WINDOWS`, `STEAM_DEPOT_ID_MACOS`, `STEAM_DEPOT_ID_LINUX` | new, [step 6](#6-store-the-depot-ids) |

The commands below use the [GitHub CLI](https://cli.github.com/) from the root of the repository. The same values can be
entered on the web instead, under
[Settings → Secrets and variables → Actions](https://github.com/pandulapeter/kubriko/settings/secrets/actions)
(secrets and [variables](https://github.com/pandulapeter/kubriko/settings/variables/actions) are separate tabs there).

## Finding your way around Steamworks

The pages used below are not part of the store page editor. They live under the *technical* settings of the app:

1. Open the [app landing page](https://partner.steamgames.com/apps/landing/3585120) of Kubriko Showcase.
2. In the **Technical Tools** box, click **Edit Steamworks Settings**.
3. The page that opens has its own row of tabs: **Application**, **SteamPipe**, **Installation**, **Security**, … , **Publish**.
   Hovering **SteamPipe** reveals **Builds** and **Depots**, hovering **Installation** reveals **General Installation**.

Direct links, which skip the navigation:

| Page | Link |
|---|---|
| SteamPipe → Builds | https://partner.steamgames.com/apps/builds/3585120 |
| SteamPipe → Depots | https://partner.steamgames.com/apps/depots/3585120 |
| Installation → General Installation (launch options) | https://partner.steamgames.com/apps/config/3585120 |
| Publish | https://partner.steamgames.com/apps/publishing/3585120 |

If one of these answers with an access error, the Steam account you are signed in with is missing a permission for the
app, see the [Managing Users](https://partner.steamgames.com/doc/gettingstarted/managing_users) documentation.

## 1. Create a build account

Uploading with your own Steam account works, but its login token would then sit in a GitHub secret. A separate account that
can do nothing but upload builds of this one app is the safer choice, and it is what
[Valve recommends](https://partner.steamgames.com/doc/sdk/uploading#Build_Account).

1. [Create a new Steam account](https://store.steampowered.com/join/), with an email address you control. It does not need
   to own anything.
2. Attach the [Steam Guard Mobile Authenticator](https://help.steampowered.com/en/faqs/view/6891-E071-C9D9-0134) or at least a
   phone number to it. Steam requires one of the two from any account that pushes builds of a released app.
3. With your own account, open [Steamworks](https://partner.steamgames.com/) → **Users & Permissions** → **Manage Users** →
   **Add User**, and invite the email address of the build account.
4. Accept the invitation from the build account's mailbox.
5. Back in **Manage Users**, open the build account and grant it exactly two permissions, for Kubriko Showcase only:
   - **Edit App Metadata**
   - **Publish App Changes To Steam**

> Steam locks a partner account out of publishing for **3 days** after a security change such as adding the authenticator or
> a phone number. If the first upload fails with a permission error right after this step, that is the likely cause.

## 2. Create the beta branch

The workflow sets each build live on a branch called `prerelease`, unless another name is typed into its `steam_branch`
input. The branch has to exist first.

1. Open [SteamPipe → Builds](https://partner.steamgames.com/apps/builds/3585120).
2. Find the list of app branches on that page (next to the `default` branch) and click **Create new app branch**.
3. Name it `prerelease`. A password is optional: without one the branch is visible to every owner of the app under
   *Properties → Betas* in the Steam client, with one only to people who know it.

See [Application Branches](https://partner.steamgames.com/doc/store/application/branches) if the page looks different.

## 3. Look up the depot IDs

1. Open [SteamPipe → Depots](https://partner.steamgames.com/apps/depots/3585120).
2. Note the ID of each depot together with the operating system it is configured for. They are most likely `3585121`,
   `3585122` and `3585123`, but which one belongs to which system depends on the order they were created in.
3. While there, check that each depot is restricted to a single operating system (Windows, macOS, Linux + SteamOS). A depot
   without an OS restriction would be downloaded on every platform.

## 4. Check the launch options

The workflow uploads the bare output of `createReleaseDistributable`, so the root of each depot looks like this:

| Depot | Content of its root | Executable the launch option has to name |
|---|---|---|
| Windows | `Kubriko Showcase.exe`, `app/`, `runtime/` | `Kubriko Showcase.exe` |
| macOS | `Kubriko Showcase.app/` | `Kubriko Showcase.app` |
| Linux | `bin/`, `lib/` | `bin/Kubriko Showcase` |

1. Open [Installation → General Installation](https://partner.steamgames.com/apps/config/3585120).
2. Compare the **Executable** of each launch option with the table. There should be one launch option per operating
   system, each restricted to that system.
3. If the builds that are live today were uploaded with a different layout (for example with everything inside an extra
   folder), change the launch options to match the table, then push the change through the
   [Publish](https://partner.steamgames.com/apps/publishing/3585120) tab. Launch options are not part of a build, so they
   only take effect once published.

A mismatch cannot reach players by accident: the new build is only live on `prerelease` until you promote it, which leaves
room to try it first.

## 5. Save the login token of the build account

`steamcmd` asks for the password and the Steam Guard code once, then keeps a token in its `config.vdf` file. The workflow
restores that file instead of knowing the password.

```bash
mkdir ~/steamcmd && cd ~/steamcmd
curl -sqL https://steamcdn-a.akamaihd.net/client/installer/steamcmd_osx.tar.gz | tar zxf -
./steamcmd.sh +login <build_account_name> +quit
```

Enter the password and the Steam Guard code when asked. A second run of the last command should now sign in without asking
anything, which confirms that the token was saved. Then, from the root of the repository:

```bash
base64 -i ~/Library/Application\ Support/Steam/config/config.vdf | gh secret set STEAM_CONFIG_VDF_BASE64
gh secret set STEAM_USERNAME    # prompts for the value: the account name, not the email address
```

The token expires after a few months, and changing the password of the account revokes it. When that happens, the first
job of the workflow fails within a minute and prints these same commands. Treat `config.vdf` like a password.

## 6. Store the depot IDs

With the IDs from [step 3](#3-look-up-the-depot-ids):

```bash
gh variable set STEAM_DEPOT_ID_WINDOWS --body <id>
gh variable set STEAM_DEPOT_ID_MACOS --body <id>
gh variable set STEAM_DEPOT_ID_LINUX --body <id>
```

These are variables rather than secrets, as depot IDs are public.

## 7. Export the Developer ID certificate

The macOS build is signed with the same **Developer ID Application: PETER PANDULA (N45A6ZHDGY)** certificate that local
builds use, which is already in the keychain of your Mac.

1. Open **Keychain Access**, select the **login** keychain and the **My Certificates** tab. The tab matters: exporting from
   *Certificates* leaves the private key behind, and the workflow will report that no signing identity was found.
2. Right-click **Developer ID Application: PETER PANDULA (N45A6ZHDGY)** → **Export…**, choose the `.p12` format and set a
   password.
3. Store both, then delete the exported file:

```bash
base64 -i ~/Desktop/DeveloperID.p12 | gh secret set MACOS_DEVELOPER_ID_CERTIFICATE_BASE64
gh secret set MACOS_DEVELOPER_ID_CERTIFICATE_PASSWORD    # prompts for the password chosen above
rm ~/Desktop/DeveloperID.p12
```

If the certificate ever has to be replaced, new ones are created under
[Certificates, Identifiers & Profiles](https://developer.apple.com/account/resources/certificates/list) with the
*Developer ID Application* type.

## 8. Confirm that the App Store Connect key can notarize

Notarization reuses the three `APP_STORE_CONNECT_*` secrets of the iOS workflow, so there is nothing to add. It only works
with a **Team Key** (not an Individual Key) that has at least the Developer role, which can be checked under
[Users and Access → Integrations → App Store Connect API](https://appstoreconnect.apple.com/access/integrations/api).

## Releasing

1. Bump `showcase.versionName` in `gradle.properties` and push it.
2. Run [`[Showcase] Publish Desktop`](https://github.com/pandulapeter/kubriko/actions/workflows/showcase-desktop-publish.yml)
   with **Run workflow**.
3. Optionally try the result: in the Steam client, right-click the app → **Properties** → **Betas** → `prerelease`.
4. Open [SteamPipe → Builds](https://partner.steamgames.com/apps/builds/3585120) (the summary of the workflow run links to it),
   pick `default` in the *Set build live on branch* dropdown of the new build, click **Preview Change**, then
   **Set Build Live Now**, and confirm it in the Steam mobile app.

## When a run fails

| Symptom | Likely cause |
|---|---|
| *Verify the configuration* lists problems | A secret or variable is missing or malformed, the message says which. |
| *Verify the Steam login* fails | The token expired or was revoked, redo [step 5](#5-save-the-login-token-of-the-build-account). |
| *Set up code signing* finds no identity | The `.p12` was exported without its private key, redo [step 7](#7-export-the-developer-id-certificate). |
| *Notarize the app* is rejected | The log of Apple is printed in the step. An authentication error instead points at [step 8](#8-confirm-that-the-app-store-connect-key-can-notarize). |
| *Launch the app* fails on one platform | Either a real startup crash (the output of the app is printed), or the runner could not open a window. Re-run with `run_smoke_test` unchecked to tell the two apart. |
| *Upload the build to Steam* fails with a permission error | The build account lacks one of the two permissions, or is inside the 3 day lockout of [step 1](#1-create-a-build-account). |
| *Upload the build to Steam* fails on `SetLive` | The branch of [step 2](#2-create-the-beta-branch) does not exist under that exact name. |
