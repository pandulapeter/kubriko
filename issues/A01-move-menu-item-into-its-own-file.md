# Move the internal `MenuItem` Composable out of `Menu.kt` into `MenuItem.kt`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/Menu.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/MenuItem.kt` (new)
- `app/shared/CLAUDE.md`

## Problem
`code-style` → Composable structure: every non-private top-level UI Composable lives in a file named after it.
`Menu.kt` (at 2480325f) holds `internal fun MenuItem(...)` (lines 92-129), which a second file also uses
(`ShowcaseContent.kt`, the Welcome row of the side menu: `MenuItem(isSelected = selectedShowcaseEntry == null, title = Res.string.welcome, …)`),
next to `LazyListScope.menu()`:

```kotlin
@Composable
internal fun MenuItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    title: StringResource,
    subtitle: StringResource,
    onSelected: () -> Unit,
) = Column(
```

A re-scan of every top-level `@Composable` in `app/` confirmed that this and the two in `app/desktop/.../TitleBar.kt` (plan
A02) are the only non-private UI Composables that do not have their own file.

## Fix
Move `MenuItem` verbatim (signature, body, no comment attached) into the new
`app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/MenuItem.kt`, same package, with the
MPL-2.0 header copied from `Menu.kt`. It stays `internal`.

`Menu.kt` keeps `LazyListScope.menu()`, `menuItemIndex()`, `private groupedForMenu()` and `private MenuCategoryLabel`.

Imports: `MenuItem.kt` takes what it uses (`background`, `Arrangement`, `Column`, `WindowInsets`, `WindowInsetsSides`,
`asPaddingValues`, `fillMaxWidth`, `only`, `padding`, `safeDrawing`, `selectable`, `LocalContentColor`, `MaterialTheme`,
`Text`, `contentColorFor`, `Composable`, `Modifier`, `Color`, `dp`, `StringResource`, `stringResource`). Remove from
`Menu.kt` the ones nothing there uses any more (`background`, `Column`, `selectable`, `LocalContentColor`,
`contentColorFor`, `Color`); keep the ones `MenuCategoryLabel` still needs (`fillMaxWidth`, `padding`, the insets
imports, `Arrangement`, `MaterialTheme`, `Text`, `stringResource`, `StringResource`).

`app/shared/CLAUDE.md` → Key files: change the `Menu.kt` line to
"`implementation/ui/Menu.kt` — `LazyListScope.menu()` extension, `menuItemIndex()`, `MenuCategoryLabel`." and add
"`implementation/ui/MenuItem.kt` — one selectable menu row (title + subtitle)."

Then grep the whole repo (docs, every `CLAUDE.md`, `.claude/skills`, build files, workflows) for `Menu.kt` / `MenuItem` and
fix any other reference in the same commit.

## Behaviour
Verbatim move within one package; callers resolve the same function. Nothing renders differently.

## Public API
None (`app/shared` is not published; the function stays `internal`).

## Tests
The existing ones (`MenuItemIndexTest` stays where it is — it tests `menuItemIndex`, which does not move).

## Verify
`./gradlew :app:shared:compileKotlinDesktop :app:shared:desktopTest`

## Manual check
None.
