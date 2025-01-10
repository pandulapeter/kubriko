package com.pandulapeter.kubriko.logger.implementation

@JsFun("() => Date.now()")
private external fun getUnixTimestampMillis(): Double

internal actual fun getCurrentTimestamp() = getUnixTimestampMillis().toLong()