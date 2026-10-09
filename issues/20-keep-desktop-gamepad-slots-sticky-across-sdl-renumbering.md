# Keep desktop gamepad slots sticky across SDL renumbering

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** Desktop (JVM)
**Challenged:** amended — negative instance IDs (SDL's error value) are skipped, and the poll's JNI work runs under `JamepadRuntime`'s lock so a second instance ticking on another thread can neither read handles another `update()` is closing and reopening nor have SDL quit under it.
**Files:** `plugins/gamepad-input/src/desktopMain/kotlin/com/pandulapeter/kubriko/gamepadInput/implementation/GamepadEventHandler.desktop.kt`, new `plugins/gamepad-input/src/desktopMain/kotlin/com/pandulapeter/kubriko/gamepadInput/implementation/GamepadSlotAssignment.kt`, new `plugins/gamepad-input/src/desktopTest/kotlin/com/pandulapeter/kubriko/gamepadInput/implementation/GamepadSlotAssignmentTest.kt`, `plugins/gamepad-input/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:plugin-gamepad-input`. Internal only; it makes the backend do what
`CLAUDE.md` already promises ("Slots are sticky in every backend").

## Problem

The desktop backend reads Kubriko slot *n* straight from Jamepad's `ControllerIndex` *n*:

```kotlin
// GamepadEventHandler.desktop.kt
override fun poll() {
    val controllerManager = controllerManager ?: return
    val gamepads = gamepads ?: return
    controllerManager.update()
    for (slot in gamepads.indices) {
        gamepads[slot].read(controllerManager.getControllerIndex(slot))
    }
}
```

A Jamepad `ControllerIndex` is an **SDL device index**, not a controller identity. Jamepad 2.30.0.0's `update()`
(verified in its sources) reopens every index whenever any device is added or removed:

```java
public boolean update() {
    if (nativeControllerConnectedOrDisconnected()) {          // SDL_JOYDEVICEADDED / SDL_JOYDEVICEREMOVED
        for (int i = 0; i < controllers.length; i++) controllers[i].reconnectController();  // close + SDL_GameControllerOpen(i)
        ...
```

SDL renumbers device indices when a device is removed. With two pads connected (player one = index 0, player two =
index 1), unplugging player one's pad makes player two's pad index 0. On the next poll:

- slot 0 is still `isConnected`, so `read()` skips the connect branch and silently starts reporting **player two's**
  sticks and buttons as player one (the name is not even refreshed);
- slot 1's index is now empty, so player two is reported **disconnected**.

`handleGamepadState` and `onGamepadDisconnected` therefore tell the game the wrong player left and hand player
two's pad to player one. Android (`deviceIds`) and iOS (`controllers` array) already key their slots by identity.

A second, smaller effect of the same design: `Configuration().apply { maxNumControllers = MAX_GAMEPAD_COUNT }` only
ever looks at device indices 0..3, and SDL counts every joystick (a flight stick, a racing wheel, a 3D mouse that
enumerates as a joystick) in that range even though `SDL_GameControllerOpen` returns null for it — one such device
plugged in before the pads leaves a pad unreachable.

## Fix

1. Add `internal class GamepadSlotAssignment(slotCount: Int)` in desktopMain (pure Kotlin, no Jamepad types) that
   maps SDL **joystick instance IDs** (stable for the life of a connection, never reused within a process) to slots:
   - `val instanceIds = IntArray(slotCount) { NO_INSTANCE }` — the owner of each slot;
   - `fun reconcile(connectedIds: IntArray, connectedCount: Int)` — first releases every slot whose ID is not among
     the first `connectedCount` entries of `connectedIds`, then gives each connected ID that owns no slot the
     lowest free slot (in `connectedIds` order); IDs beyond the free slots are ignored. Release-before-claim mirrors
     Android's `refreshDevices`, so a pad plugged in during the same poll another one left can take the freed slot.
   - `fun slotOf(instanceId: Int): Int` (`NO_SLOT` when unassigned).
   No allocation: both arrays are allocated once; the linear searches are over ≤ 4 and ≤ `DEVICE_INDEX_COUNT` entries.
2. In the handler:
   - Raise the Jamepad configuration to `maxNumControllers = DEVICE_INDEX_COUNT` (private const, e.g. `16`) so
     non-gamepad joysticks occupying low device indices can't hide pads. Keep `JamepadRuntime` otherwise as is.
   - Keep `private val connectedIds = IntArray(DEVICE_INDEX_COUNT)`, `private val deviceIndexOfSlot =
     IntArray(MAX_GAMEPAD_COUNT)` and `private val readInstanceIds = IntArray(MAX_GAMEPAD_COUNT) { NO_INSTANCE }`.
   - `poll()`: call `controllerManager.update()`; then for each device index `i` in `0 until DEVICE_INDEX_COUNT`
     whose `ControllerIndex.isConnected`, read `getDeviceInstanceID()` (catch `ControllerUnpluggedException` → skip;
     also skip a negative ID, which is what `SDL_JoystickInstanceID` returns on error and which must not collide with
     `NO_INSTANCE`) into `connectedIds`, remembering `i` per entry; `reconcile(...)`; then for each slot: if it has no owner, reset
     the raw state if it was connected; otherwise read from the owner's device index, and when
     `readInstanceIds[slot] != instanceIds[slot]` (a different pad now owns the slot, or it was just claimed) reset the
     raw state, set `isConnected`/`name` and update `readInstanceIds[slot]`. Rework `RawGamepadState.read(controller)`
     accordingly: it no longer decides connection state itself, it only fills values (still catching
     `ControllerUnpluggedException` → `reset()`).
   - Reconcile on **every** poll, not only when `update()` returns true: `JamepadRuntime` shares one SDL instance
     between Kubriko instances, and only the first handler to call `update()` after a hot-plug sees `true` (it drains
     the SDL event queue). The extra cost is up to `DEVICE_INDEX_COUNT` cheap JNI calls per tick, and `isConnected`
     is already called per slot today.
   - `stopListening()` clears `instanceIds`/`readInstanceIds` back to `NO_INSTANCE`.
   - Run the body of `poll()` (from `update()` to the last read) inside `synchronized(JamepadRuntime)` — the monitor
     `acquire()`/`release()` already use — and re-check inside it that this handler still holds a manager. Every
     handler shares one `ControllerManager`, whose `update()` closes and reopens every `ControllerIndex` handle on a
     hot-plug, and `release()` may call `quitSDLGamepad()`; a Kubriko instance driven by a background `TickSource`
     (`fixedRate`/`fixedFrequency` tick on `Dispatchers.Default`) would otherwise read a handle another instance is
     closing, or poll after SDL quit — a native crash, not an exception. The race exists today, but this plan
     multiplies the JNI calls per poll. An uncontended monitor costs nothing measurable and allocates nothing; under
     the default `viewportFrames()` every instance ticks on the UI thread anyway.
3. `plugins/gamepad-input/CLAUDE.md`: in the Desktop row of "Platform Differences", note that slots are keyed by the
   SDL instance ID because SDL renumbers device indices on removal, that Jamepad's `maxNumControllers` is a
   device-index window, not a pad count, and that each `GamepadInputManager` assigns its own slots (two instances
   started at different times can give the same pad different slots, as on Android).

## Tests

`GamepadSlotAssignmentTest` (desktopTest, plain JVM, no fixtures needed):
- `secondPadKeepsItsSlotWhenTheFirstIsUnplugged`: reconcile `[10, 11]` → slots `[10, 11]`; reconcile `[11]` →
  slot 0 empty, slot 1 still `11`.
- `newPadTakesTheLowestFreeSlot`: then reconcile `[11, 12]` → slot 0 = `12`, slot 1 = `11`.
- `padReplacedInOnePollTakesTheFreedSlot`: `[10]` → `[12]` gives slot 0 = `12`.
- `idsBeyondTheSlotCountAreIgnored` and `reconcilingTheSameIdsTwiceChangesNothing`.
- `onlyTheFirstConnectedCountEntriesAreRead`: reconcile `[10, 11, 12]` with `connectedCount = 1` → only slot 0 is
  assigned (the scratch array is reused, so stale entries past the count must be ignored).

The JNI side cannot be unit-tested (no SDL on the test classpath).

## Manual check

Desktop, two gamepads: connect both, start a game that shows each player's input (the `test-input` example, with
`showcase.areTestExamplesEnabled=true`). Unplug the pad in slot 0: slot 1 must keep reporting its own pad and slot 0 must report disconnected.
Plug it back: it must take slot 0 again. Repeat with a non-gamepad joystick connected first.
