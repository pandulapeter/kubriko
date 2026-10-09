# Decide whether proper names (the Licenses screen's dependency names) are exempt from the string resource rule

**Kind:** docs  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Planned
**Artifact:** unpublished (app); the rule lives in `.claude/skills/code-style/SKILL.md` (cross-lane)
**Files:**
- `.claude/skills/code-style/SKILL.md` (option (a))
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/licenses/Dependency.kt` and
  `app/shared/src/commonMain/composeResources/values/strings.xml` (option (b))

**Rebased:** on 70de96c6 after the Now plans landed.

## Problem
`code-style` → User-facing strings: every on-screen string lives in `strings.xml`; the exemptions (`SKILL.md:122`) are persistence keys,
file names, log/serialization identifiers and numeric readouts. The Licenses screen renders 23 inline literals as
chip text (`Dependency.dependencyName`, e.g. `dependencyName = "AndroidX Activity"`, `"Kotlin"`, `"freesound.org"`;
in `Dependency.kt`; rendered at `LicensesScreen.kt:92` `text = dependency.dependencyName` at 70de96c6). They are proper names (product,
project and site names) that no translation would change, so the rule as written flags them while its intent does not.

## Decision
(a) Exempt proper names — product, library and site names shown as-is — in the code-style skill's exemption list; no code
change. (b) Move the 23 names into `strings.xml` (`other_licenses_dependency_<id>`), store `StringResource` in the enum
and resolve at the call site, and preload them in the Licenses gate (`LicensesScreenStateHolder.kt`'s `areResourcesLoaded()`). **Recommended: (a)** — 23 resources that can never
differ per locale add load-gate work and noise without benefit.

## Fix
Under (a): add "proper names shown verbatim (product, library, site names)" to the Exempt line of `code-style` →
User-facing strings. Under (b): as described above, one commit.

## Behaviour
None under (a); under (b) the same text, now loaded through resources.

## Public API
None.

## Tests
None.

## Verify
(b) only: `./gradlew :app:shared:compileKotlinDesktop`

## Manual check
None.
