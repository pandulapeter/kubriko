# Logger Tool

The `logger` tool provides a simple, reactive logging system for Kubriko. It allows both the engine and individual plugins to record events that can then be viewed in real-time, typically through the Debug Menu.

## Features

- **Reactive Updates**: Uses `StateFlow` to emit log changes instantly.
- **Structured Entries**: Each log entry includes a timestamp, importance level, and optional source and details.
- **Memory Management**: Configurable `entryLimit` to prevent logs from consuming too much memory.
- **In-App Visualization**: Designed to be easily integrated with the `debug-menu` for on-device debugging.

## Usage

### 1. Log a Message

You can log messages from anywhere in your project:

```kotlin
Logger.log(
    message = "Something happened",
    details = "More specific information about the event",
    source = "MyManager",
    importance = Logger.Importance.MEDIUM
)
```

### 2. Configure Limits

Adjust how many log entries are kept in memory:

```kotlin
Logger.entryLimit = 500 // Keeps the 500 most recent entries
```

### 3. Observe Logs

To build a custom log viewer, observe the `logs` flow:

```kotlin
scope.launch {
    Logger.logs.collect { entries ->
        // Update your UI with the new entries
    }
}
```

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:tool-logger`
