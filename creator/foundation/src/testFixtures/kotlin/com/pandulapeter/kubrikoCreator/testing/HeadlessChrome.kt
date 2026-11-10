/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import com.pandulapeter.kubrikoCreator.foundation.findProgram
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.OutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO

/**
 * Headless Chrome (or Chromium), driven through its DevTools protocol: one page, navigated and captured, which opens no
 * window. Found where [find] looks.
 */
public class HeadlessChrome private constructor(
    private val process: Process,
    private val socket: WebSocket,
    private val answers: ConcurrentHashMap<Int, CompletableFuture<JsonObject>>,
    private val events: MutableList<String>,
) : AutoCloseable {
    private val ids = AtomicInteger()

    init {
        call("Runtime.enable", JsonObject(emptyMap()))
        // Kubriko starts a game only once its page has the focus, which a headless page never gets by itself.
        call("Emulation.setFocusEmulationEnabled", buildJsonObject { put("enabled", true) })
    }

    /**
     * What the page logged to its console and the exceptions it didn't catch, oldest first.
     */
    public val console: List<String> get() = synchronized(events) { events.toList() }

    /**
     * Loads [address] in the page.
     */
    public fun navigate(address: URI) {
        call("Page.navigate", buildJsonObject { put("url", address.toString()) })
    }

    /**
     * Makes the pages loaded from now on report [languages] as the browser's, as a browser started under the POSIX locale
     * reports `en-US@posix`, which a headless Chrome on a Mac never does by itself.
     */
    public fun reportLanguages(vararg languages: String) {
        val list = languages.joinToString(", ") { "'$it'" }
        val script = "Object.defineProperty(navigator, 'languages', { get: () => [$list], configurable: true });" +
            "Object.defineProperty(navigator, 'language', { get: () => '${languages.first()}', configurable: true });"
        call("Page.enable", JsonObject(emptyMap()))
        call("Page.addScriptToEvaluateOnNewDocument", buildJsonObject { put("source", script) })
    }

    /**
     * What the page shows now.
     */
    public fun screenshot(): BufferedImage {
        val data = call("Page.captureScreenshot", buildJsonObject { put("format", "png") })["data"]!!.jsonPrimitive.content
        return ImageIO.read(ByteArrayInputStream(Base64.getDecoder().decode(data)))
    }

    /**
     * What the page shows once [isReady] accepts it, or the last it showed after [timeoutInMilliseconds], or null if it
     * never had anything to capture: a page that is loading has nothing for a moment.
     */
    public fun awaitScreenshot(timeoutInMilliseconds: Long, isReady: (BufferedImage) -> Boolean): BufferedImage? {
        val end = System.currentTimeMillis() + timeoutInMilliseconds
        var image: BufferedImage? = null
        while (true) {
            image = runCatching { screenshot() }.getOrNull() ?: image
            if ((image != null && isReady(image)) || System.currentTimeMillis() >= end) return image
            Thread.sleep(POLLING_INTERVAL_IN_MILLISECONDS)
        }
    }

    private fun call(method: String, parameters: JsonObject): JsonObject {
        val id = ids.incrementAndGet()
        val answer = CompletableFuture<JsonObject>().also { answers[id] = it }
        socket.sendText(buildJsonObject {
            put("id", id)
            put("method", method)
            put("params", parameters)
        }.toString(), true).get(10, TimeUnit.SECONDS)
        val message = answer.get(30, TimeUnit.SECONDS)
        message["error"]?.let { error("$method failed: $it") }
        return message["result"]!!.jsonObject
    }

    override fun close() {
        socket.abort()
        process.destroy()
        if (!process.waitFor(10, TimeUnit.SECONDS)) process.destroyForcibly()
    }

    public companion object {
        private const val POLLING_INTERVAL_IN_MILLISECONDS = 250L
        private const val LOOPBACK_ONLY = "MAP * ~NOTFOUND, EXCLUDE 127.0.0.1, EXCLUDE localhost"

        /**
         * The Chrome `KUBRIKO_CREATOR_CHROME` names, else the one in the Mac's applications folder or the first Chrome or
         * Chromium on the `PATH`, or null if there is none.
         */
        public fun find(): String? = (
            listOfNotNull(System.getenv("KUBRIKO_CREATOR_CHROME"), "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome") +
                listOf("google-chrome", "chromium", "chromium-browser").mapNotNull { name -> findProgram(name)?.path }
            ).firstOrNull { File(it).canExecute() }

        /**
         * Starts [chrome] with a profile of its own in [directory] and a page [width] by [height] pixels, reaching no host but
         * this computer, so a page passes only if it needs nothing from elsewhere (Compose for Web downloads a font a text
         * lacks from Google Fonts, which a player offline doesn't get).
         */
        public fun start(chrome: String, directory: File, width: Int, height: Int): HeadlessChrome {
            val process = ProcessBuilder(
                chrome, "--headless=new", "--no-sandbox", "--no-first-run", "--enable-unsafe-swiftshader", "--use-angle=swiftshader",
                "--window-size=$width,$height", "--host-resolver-rules=$LOOPBACK_ONLY", "--remote-debugging-port=0", "--user-data-dir=${File(directory, "chrome-profile")}", "about:blank",
            ).redirectErrorStream(true).start()
            val browserAddress = process.inputStream.bufferedReader().lineSequence()
                .firstNotNullOfOrNull { line -> line.substringAfter("DevTools listening on ", "").takeIf { it.isNotEmpty() } }
                ?: error("Chrome didn't start its DevTools.")
            Thread { process.inputStream.transferTo(OutputStream.nullOutputStream()) }.apply { isDaemon = true }.start()
            val devTools = URI(browserAddress)
            val http = HttpClient.newHttpClient()
            val pages = http.send(HttpRequest.newBuilder(URI("http://${devTools.authority}/json/list")).build(), HttpResponse.BodyHandlers.ofString()).body()
            val page = Json.parseToJsonElement(pages).jsonArray
                .map { it.jsonObject }.first { it["type"]?.jsonPrimitive?.content == "page" }
            val answers = ConcurrentHashMap<Int, CompletableFuture<JsonObject>>()
            val events = mutableListOf<String>()
            val socket = http.newWebSocketBuilder().buildAsync(URI(page["webSocketDebuggerUrl"]!!.jsonPrimitive.content), Listener(answers, events)).get(10, TimeUnit.SECONDS)
            return HeadlessChrome(process, socket, answers, events)
        }
    }

    /**
     * Hands each answer to the call waiting for it, joining the frames a long one (a screenshot) comes in, and keeps the
     * console's messages and uncaught exceptions in [events].
     */
    private class Listener(
        private val answers: ConcurrentHashMap<Int, CompletableFuture<JsonObject>>,
        private val events: MutableList<String>,
    ) : WebSocket.Listener {
        private val text = StringBuilder()

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            text.append(data)
            if (last) {
                val message = Json.parseToJsonElement(text.toString()).jsonObject
                text.clear()
                message["id"]?.jsonPrimitive?.int?.let { id -> answers.remove(id)?.complete(message) }
                when (message["method"]?.jsonPrimitive?.content) {
                    "Runtime.consoleAPICalled", "Runtime.exceptionThrown" -> synchronized(events) { events += message["params"].toString() }
                }
            }
            webSocket.request(1)
            return null
        }
    }
}
