package com.pandulapeter.kubrikoPong.implementation.helpers

import platform.UIKit.UIDevice

internal actual val platformName = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion