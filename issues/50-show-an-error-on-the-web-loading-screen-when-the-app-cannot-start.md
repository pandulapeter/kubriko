# Show an error on the web loading screen when the browser cannot run the app or its start fails

**Kind:** bug (platform edge case)  ·  **Severity:** medium  ·  **Platforms:** Web
**Challenged:** amended — the listeners now also ignore `ErrorEvent`s without an `error` object (ResizeObserver loop notices, sanitized "Script error.") and rejections whose stack points into a browser extension, the load and listeners must sit before the IIFE's dev-server early return, and `hideLoadingScreen` must still hide an error-state screen.
**Files:** `app/web/src/webMain/resources/index.html`, `app/web/CLAUDE.md`

Ships in no published artifact (`app/web` is the Showcase).

## Problem

The loading screen in `index.html` sits above the canvas (`z-index: 20`) and only `window.hideLoadingScreen()`,
called from Kotlin through `onFirstFrameDrawn = ::hideLoadingScreen` in `KubrikoShowcaseApp.kt`, can dismiss it:

```js
window.hideLoadingScreen = () => {
    stopTracking();
    loadingScreen.classList.add('hidden');
    ...
};
```

Nothing else ever changes it: there is no feature check, no `error` / `unhandledrejection` listener, and the page
loads the bundle with a plain `<script src="web.js"></script>`. The generated loader inside `web.js` only logs when
compilation fails, then rethrows:

```js
catch(e){if(e instanceof WebAssembly.CompileError){let e="Please make sure that your runtime environment supports the latest version of Wasm GC and Exception-Handling proposals.\n...";...console.error(e)...}throw e}
```

So on a browser without Wasm GC (Safari before 18.2, every iOS 17 browser, older Chrome/Firefox), on a failed or
interrupted download of the ~13 MB of wasm, or when Kotlin's `main` throws before the first frame, the user sees the
logo and a progress bar that fills and then pulses (`.loadingProgress.complete`) forever, with no hint that anything
went wrong.

Verified on the built distribution (`app/web/build/dist/wasmJs/productionExecutable`, Kotlin 2.4.20): both wasm
binaries use the **legacy** exception-handling opcodes (`try`/`catch_all`) — Node 24 validates them by default and
rejects them with `--no-experimental-wasm-legacy-eh`; they do not need `exnref`. The `wasm:js-string` builtins are
polyfilled by the loader (`"wasm:js-string": n`), so they need no probe.

## Fix

All in `index.html`; the recommended change is (a) + (b), with (c) falling out of (a) for free.

**(a) Feature probe before loading the app.** Replace the static `<script src="web.js"></script>` with a dynamic load
that only happens when the browser validates a minimal module using a GC struct type and a legacy-EH tag + `try` /
`catch_all` — the same two proposals the binaries need:

```js
// A struct type (Wasm GC) and a tag with try / catch_all (legacy exception handling): the two proposals the
// Kotlin/Wasm binaries are compiled against.
const isWasmSupported = (() => {
    try {
        return typeof WebAssembly === 'object' && WebAssembly.validate(new Uint8Array([
            0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00,
            0x01, 0x08, 0x02, 0x5f, 0x01, 0x7f, 0x00, 0x60, 0x00, 0x00,
            0x03, 0x02, 0x01, 0x01,
            0x0d, 0x03, 0x01, 0x00, 0x01,
            0x0a, 0x0e, 0x01, 0x0c, 0x00, 0x06, 0x40, 0x41, 0x00, 0xfb, 0x00, 0x00, 0x1a, 0x19, 0x0b, 0x0b,
        ]));
    } catch (e) {
        return false;
    }
})();
```

Module layout, for whoever has to change it: type section `[struct (field i32 immutable), func () -> ()]`, one
function of type 1, tag section (id 13) with one tag of type 1, and a body
`try (empty) i32.const 0; struct.new 0; drop; catch_all; end; end`. Verified with Node 24.10:
`WebAssembly.validate` → `true`, `new WebAssembly.Module(...)` compiles; with `--no-experimental-wasm-legacy-eh` it
returns `false`, exactly as the real binaries do. (Node cannot switch GC off, so the GC half was checked by a
GC-only control module validating on its own; an engine without GC fails to decode type form `0x5f`.) The check
script is `scratchpad/writer-S/probe.mjs` in the review scratchpad — not needed for execution.

When `isWasmSupported` is false, put the screen into its error state with the "unsupported browser" message and do
not load `web.js`. Otherwise:

```js
const appScript = document.createElement('script');
appScript.src = 'web.js';
appScript.onerror = () => showError(<start-failed message>);
document.body.appendChild(appScript);
```

This is option (c) — a `web.js` 404 or network error — at no extra cost.

**(b) Error listeners while loading.** Register `window` `error` and `unhandledrejection` listeners in the same IIFE
that put the screen into its error state with the "could not start" message, and remove both in
`window.hideLoadingScreen` (after the first frame an error is the app's business, not the loading screen's). The
loader's `CompileError` rethrow, a rejected `instantiateStreaming` (failed download), and a throw from `main` all
surface as an `unhandledrejection` of the webpack async module; a throw in a frame callback before the first frame
surfaces as `error`. Ignore an `ErrorEvent` whose `filename` is set and not same-origin
(`new URL(event.filename, document.baseURI).origin !== location.origin`), so a browser extension's error cannot
take the page down. Also ignore an `ErrorEvent` whose `error` is `null`/`undefined`: that is how the browser reports
a "ResizeObserver loop completed with undelivered notifications" notice (a benign warning that any extension or
embedded widget using a `ResizeObserver` can trigger, reported with the page's own URL as `filename`, so the origin
check does not catch it) and a sanitized cross-origin "Script error."; a real failure of the app always carries the
thrown object (a Kotlin/Wasm throw surfaces as a `WebAssembly.Exception` or JS `Error`). A `PromiseRejectionEvent`
has no `filename`, so for `unhandledrejection` ignore a reason whose `stack` (when it is a string) contains
`-extension://` (`chrome-extension://`, `moz-extension://`, `safari-web-extension://`) — extensions that inject
scripts into the page's own world (wallets, translators) can reject promises there. The error state is sticky: once
shown, later events change nothing — but `window.hideLoadingScreen` still hides the screen and removes the listeners
when it is called, error state or not, so a false positive can never keep a running app covered.

**Placement — required for the dev server.** The existing IIFE `return`s early when there is no
`window.kubrikoResourceSizes` table (`if (!sizes || typeof TransformStream === 'undefined') return;`), which is always
the case on `wasmJsBrowserDevelopmentRun` (the dev server gets no preloads, see `app/web/CLAUDE.md`). The probe, the
listener registration and the `web.js` append must all run regardless of that branch: put them after the
size-table block with the early return turned into an `if (sizes && typeof TransformStream !== 'undefined') { ... }`
block (or in a second inline script after the first). Keep the order "install the fetch wrapper, then append
`web.js`": the dynamically inserted script runs asynchronously, so the wrapper is in place before the loader's
`fetch` either way, but the source order documents the dependency. The dynamically inserted classic script still
sets `document.currentScript`, which webpack's automatic `publicPath` reads, so the wasm URLs resolve as before;
the streaming path is untouched (the loader still calls `WebAssembly.instantiateStreaming(fetch(...))` through the
wrapper). Losing the preload scanner's early discovery of `web.js` costs nothing measurable — the page is a few KB
of HTML and the inline script runs immediately.

**Error state.** Add an element under the progress bar:

```html
<p class="loadingMessage" id="loadingMessage" hidden></p>
```

`showError(message)` sets its text, removes `hidden`, calls `stopTracking()`, and hides the progress bar (remove
the `tracking` class). For the "could not start" case also append a `<button>` that calls `location.reload()`.
Style `.loadingMessage` (and the button) in white on the existing `#6060AA`, `font-family: system-ui, sans-serif`,
centered, `max-width: 320px`, with a 16px side margin so it fits a phone. Messages (English; the page is static
HTML outside Compose, so the `strings.xml` rule does not reach it — the existing `alt="Loading..."` is the same):

- unsupported: "Your browser can't run Kubriko Showcase. Update it, or open this page in a recent version of
  Chrome, Edge, Firefox or Safari (18.2 or newer)."
- start failed: "Kubriko Showcase couldn't start. Check your connection and reload the page." + "Reload" button.

Not recommended: a timeout for a download that stalls without failing — the progress bar already shows that the
download is stuck, and a timeout would misfire on slow connections.

Known limitation to note, not fix: in the production build `injectWebPreloads` puts `<link rel="preload">` tags for
the wasm in `<head>`, so an unsupported browser still downloads them before the probe runs. Making the preloads
conditional is not worth the complexity for browsers that cannot run the app anyway.

**Docs.** In `app/web/CLAUDE.md` → "Load time", add a paragraph: the page probes for Wasm GC + legacy exception
handling before loading `web.js`, shows an "unsupported browser" message when it fails, and shows a reload hint
when the app errors before its first frame; the probe matches the binaries' **legacy** EH, so if the build ever
moves to the new exception proposal (`exnref` / `try_table`, e.g. a Kotlin default change or
`-Xwasm-use-new-exception-proposal`), the probe's body must change with it. Also replace "nothing but
`hideLoadingScreen()` can dismiss it" accordingly.

## Tests

None can be written: `app/web` has no test source set, and the change is inline JavaScript in a static page. The
probe bytes were verified with Node (see Fix). After editing, extract the array from `index.html` and run it through
`node -e "console.log(WebAssembly.validate(new Uint8Array([...])))"` once to catch a copy error.

## Manual check

- `./gradlew :app:web:wasmJsBrowserDistribution`, serve `app/web/build/dist/wasmJs/productionExecutable`, open it in
  Chrome / Firefox / Safari 18.2+: the app starts as before and no message flashes.
- Same page in Safari on iOS 17 or macOS Safari < 18.2 (or Chrome with
  `chrome://flags` → disable WebAssembly Garbage Collection, where still available): the "unsupported browser"
  message appears immediately, the progress bar is hidden.
- With DevTools → Network → block `*.wasm` (or go offline after the HTML has loaded): the "couldn't start" message
  with a Reload button appears; Reload works once the block is removed.
- Block `web.js` the same way: the same message appears.
- `./gradlew :app:web:wasmJsBrowserDevelopmentRun`: the app still loads on the dev server (no size table, so this
  exercises the no-tracking branch) and no message flashes.
- With a few extensions enabled (an ad blocker, a password manager), reload the production page a few times: no
  message flashes during normal startup.
