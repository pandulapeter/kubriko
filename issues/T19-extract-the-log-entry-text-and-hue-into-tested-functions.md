# Extract the log entry text and source hue out of the LogEntry Composable into tested internal functions

**Kind:** test  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** tool-debug-menu (internal code only)
**Files:** tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/ui/LogEntry.kt, tools/debug-menu/src/commonTest/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/ui/LogEntryTextTest.kt (new)
**Challenged:** amended — the hue range assertion is spelled as a real Kotlin range (`0f..<360f`); there is no `Float` `until`.

## Problem
`ui/LogEntry.kt:38-45` formats the line inside the Composable body:
```kotlin
    text = entry.source.let { source ->
        val timestamp = Instant.fromEpochMilliseconds(entry.timestamp).toLocalDateTime(TimeZone.currentSystemDefault()).time.let {
            "${it.hour}:${it.minute}:${it.second}.${it.nanosecond / 1_000_000}"
        }
        val message = if (source == null) entry.message else "${entry.source}: ${entry.message}"
        val suffix = if (entry.details.isNullOrBlank()) "" else "*"
        "[$timestamp] $message$suffix"
    }
```
and the colour hash is a private `String.toHue()` (`substringAfterLast('@').hashCode()`, made non-negative, `% 360`). Neither can
be unit-tested; the code style asks for formatting to be a pure function next to its Composable.

## Fix
In `LogEntry.kt` (same file, below the Composable):
- `@OptIn(ExperimentalTime::class) internal fun logEntryText(entry: Logger.Entry, timeZone: TimeZone): String` — the body above
  verbatim, with `timeZone` in place of `TimeZone.currentSystemDefault()`. The Composable passes
  `text = logEntryText(entry, TimeZone.currentSystemDefault())`.
- Rename `private fun String.toHue()` to `internal fun sourceHue(source: String): Float` with the same body (reading `source`
  instead of `this`), and call `sourceHue(source)` from `getColor`.
- Move the `@OptIn(ExperimentalTime::class)` from the Composable to `logEntryText` if the Composable no longer needs it.
Keep the current output exactly, including the unpadded time (`9:5:3.7`); padding is a separate decision plan.

## Behaviour
Same strings and colours.

## Public API
None (internal).

## Tests
`LogEntryTextTest` (commonTest runs on the desktop JVM), building `Logger.Entry(id = "1", message = "m", details = null, source = null, timestamp = ..., importance = Logger.Importance.LOW)`:
- `timestamp = 3_723_004L`, `TimeZone.UTC`, no source, no details → `"[1:2:3.4] m"`.
- with `source = "Src"` → `"[1:2:3.4] Src: m"`; with `details = "d"` → ends with `"*"`; with `details = " "` → no `"*"`.
- `sourceHue("A@1") == sourceHue("B@1")` (only the part after the last `@` counts) and `sourceHue(x) in 0f..<360f` (there is no `until` for
  `Float`) for a few strings, including one whose `hashCode()` is negative.

## Verify
`./gradlew :tools:debug-menu:compileKotlinDesktop :tools:debug-menu:compileKotlinWasmJs :tools:debug-menu:desktopTest`

## Manual check
None.
