<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# logger

Global, singleton logging utility. No separate dependency needed — already part of the engine's public API. Package: `com.pandulapeter.kubriko.logger`.

## API

```kotlin
Logger.log(
    message: String,
    details: String? = null,
    source: String? = null,
    importance: Importance = Importance.HIGH,
)
Logger.clearLogs()
Logger.entryLimit: Int  // default 1000; settable; trims oldest entries immediately on set
```

## Importance levels

`Logger.Importance` enum with three values:
- `LOW` — verbose debugging (equivalent to debug-level)
- `MEDIUM` — standard informational (equivalent to info-level)
- `HIGH` — important events or errors (default; equivalent to warn/error-level)

## Two observable surfaces

- **`Logger.logs: StateFlow<List<Entry>>`** — full in-memory buffer, newest entry first. Capped at `entryLimit` (default 1000). Safe to collect on any thread; updates are applied via `MutableStateFlow.update {}` which is thread-safe. Used by the Debug Menu log viewer.
- **`Logger.latestEntry: SharedFlow<Entry>`** — hot shared flow (`extraBufferCapacity = 64`); emits each new entry as it arrives via `tryEmit`. Does not replay history. Use for real-time streaming (e.g. forwarding to an external log sink).

## `Entry` data class

Fields: `id: String` (random UUID), `message`, `details: String?`, `source: String?`, `timestamp: Long` (epoch ms from `kotlin.time.Clock.System`), `importance: Importance`.

## Thread safety

Both `_logs` (`MutableStateFlow`) and `_latestEntry` (`MutableSharedFlow`) are safe to update from any thread. `log()` uses `tryEmit` for the shared flow (non-blocking; drops if buffer full) and `update {}` for the state flow (atomic).

## Integration with `Manager.log()`

`Manager.log(message, details, importance)` is a guarded wrapper defined in the engine that calls `Logger.log()` only when `isLoggingEnabled = true` was passed to `Kubriko.newInstance()`. The `source` parameter is automatically set to the Manager's class name. Direct calls to `Logger.log()` bypass this guard and always record.

## `entryLimit` behaviour

Setting `entryLimit` to a smaller value immediately trims the existing `logs` list via `take(value)`. Setting it to 0 effectively clears all future logs (entries are still emitted on `latestEntry`). Negative values are silently ignored.
