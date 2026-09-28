# Remove the iOS ContentView's window size notifications that nothing observes

**Challenged:** sound

**Kind:** cleanup  ·  **Severity:** low  ·  **Platforms:** iOS
**Files:** `app/ios/iosApp/iosApp/ContentView.swift`

## Problem

`ContentView.swift` posts a notification on every size class change:

```swift
extension Notification.Name {
    static let windowSizeChanged = Notification.Name("windowSizeChanged")
}
...
struct ContentView: View {
    @Environment(\.horizontalSizeClass) var horizontalSizeClass
    @Environment(\.verticalSizeClass) var verticalSizeClass

    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .onChange(of: horizontalSizeClass) { newValue in
                NotificationCenter.default.post(
                    name: .windowSizeChanged,
                    object: nil
                )
            }
            .onChange(of: verticalSizeClass) { newValue in
                ...
```

Nothing subscribes to `windowSizeChanged`: a search of every `.kt`, `.swift` and `.m` file in the repository finds only
these three references. The posts do nothing except make every size-class change re-evaluate `ContentView` (reading
the two environment values subscribes it to them), and the one-parameter `onChange(of:perform:)` form is deprecated
since iOS 17. Compose sizes itself from the view's bounds, which `ComposeView` already fills.

## Fix

Delete the `Notification.Name` extension, the two `@Environment` properties and both `.onChange` modifiers, leaving

```swift
struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
```

Also trim the trailing blank lines at the end of the file. Plan 87 then adds its state and `.statusBarHidden` to this
body.

## Tests

None: Swift UI shell code, no test target.

## Manual check

iPad (or iPad simulator) on iOS 17 or 18, run the `[iOS] Showcase` scheme from Xcode:

1. Rotate between portrait and landscape, and on iPad enter and leave Split View / Stage Manager at a few widths.
2. Expected: the Showcase relayouts exactly as before (compact list below 640 dp, side menu above), with no blank or
   stale frame.
