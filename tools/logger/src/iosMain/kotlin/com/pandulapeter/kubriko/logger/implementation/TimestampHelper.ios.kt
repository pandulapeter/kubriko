package com.pandulapeter.kubriko.logger.implementation

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun getCurrentTimestamp() = (NSDate().timeIntervalSince1970 * 1000).toLong()