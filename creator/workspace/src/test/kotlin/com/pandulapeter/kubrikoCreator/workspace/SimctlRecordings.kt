/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

/**
 * What Xcode 27's `simctl` printed on a Mac with iOS 18.2, 27.1, and 27.2 installed (27.1 and one build of 27.2 made for the
 * iPhone Duo alone), cut down to a few device types of every family, and without the paths.
 */
internal object SimctlRecordings {
    const val DEVICE_TYPES = """
{
  "devicetypes" : [
    {
      "productFamily" : "iPhone",
      "maxRuntimeVersion" : 4294967295,
      "maxRuntimeVersionString" : "65535.255.255",
      "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPhone-18-Pro",
      "modelIdentifier" : "iPhone19,2",
      "minRuntimeVersionString" : "27.0.0",
      "minRuntimeVersion" : 1769472,
      "name" : "iPhone 18 Pro"
    },
    {
      "productFamily" : "iPhone",
      "maxRuntimeVersion" : 4294967295,
      "maxRuntimeVersionString" : "65535.255.255",
      "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPhone-Duo",
      "modelIdentifier" : "iPhone19,4",
      "minRuntimeVersionString" : "27.1.0",
      "minRuntimeVersion" : 1769728,
      "name" : "iPhone Duo"
    },
    {
      "productFamily" : "iPhone",
      "maxRuntimeVersion" : 4294967295,
      "maxRuntimeVersionString" : "65535.255.255",
      "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPhone-17e",
      "modelIdentifier" : "iPhone18,5",
      "minRuntimeVersionString" : "26.3.0",
      "minRuntimeVersion" : 1704704,
      "name" : "iPhone 17e"
    },
    {
      "productFamily" : "iPhone",
      "maxRuntimeVersion" : 4294967295,
      "maxRuntimeVersionString" : "65535.255.255",
      "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPhone-16-Pro",
      "modelIdentifier" : "iPhone17,1",
      "minRuntimeVersionString" : "18.0.0",
      "minRuntimeVersion" : 1179648,
      "name" : "iPhone 16 Pro"
    },
    {
      "productFamily" : "iPad",
      "maxRuntimeVersion" : 4294967295,
      "maxRuntimeVersionString" : "65535.255.255",
      "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPad-Pro-13-inch-M5-12GB",
      "modelIdentifier" : "iPad17,4",
      "minRuntimeVersionString" : "26.0.0",
      "minRuntimeVersion" : 1703936,
      "name" : "iPad Pro 13-inch (M5)"
    },
    {
      "productFamily" : "iPad",
      "maxRuntimeVersion" : 4294967295,
      "maxRuntimeVersionString" : "65535.255.255",
      "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPad-A16",
      "modelIdentifier" : "iPad15,7",
      "minRuntimeVersionString" : "18.3.0",
      "minRuntimeVersion" : 1180416,
      "name" : "iPad (A16)"
    },
    {
      "productFamily" : "iPad",
      "maxRuntimeVersion" : 4294967295,
      "maxRuntimeVersionString" : "65535.255.255",
      "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPad-mini-6th-generation",
      "modelIdentifier" : "iPad14,1",
      "minRuntimeVersionString" : "15.0.0",
      "minRuntimeVersion" : 983040,
      "name" : "iPad mini (6th generation)"
    },
    {
      "productFamily" : "Apple TV",
      "maxRuntimeVersion" : 4294967295,
      "maxRuntimeVersionString" : "65535.255.255",
      "identifier" : "com.apple.CoreSimulator.SimDeviceType.Apple-TV-4K-4th-generation-4K",
      "modelIdentifier" : "AppleTV18,1",
      "minRuntimeVersionString" : "27.2.0",
      "minRuntimeVersion" : 1769984,
      "name" : "Apple TV 4K (4th generation)"
    },
    {
      "productFamily" : "Apple Watch",
      "maxRuntimeVersion" : 4294967295,
      "maxRuntimeVersionString" : "65535.255.255",
      "identifier" : "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-12-46mm",
      "modelIdentifier" : "Watch8,3",
      "minRuntimeVersionString" : "27.0.0",
      "minRuntimeVersion" : 1769472,
      "name" : "Apple Watch Series 12 (46mm)"
    },
    {
      "productFamily" : "Apple Vision",
      "maxRuntimeVersion" : 4294967295,
      "maxRuntimeVersionString" : "65535.255.255",
      "identifier" : "com.apple.CoreSimulator.SimDeviceType.Apple-Vision-Pro-4K",
      "modelIdentifier" : "RealityDevice14,1",
      "minRuntimeVersionString" : "3.0.0",
      "minRuntimeVersion" : 196608,
      "name" : "Apple Vision Pro"
    },
    {
      "productFamily" : "iPhone",
      "maxRuntimeVersion" : 1048575,
      "maxRuntimeVersionString" : "15.255.255",
      "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPod-touch--7th-generation-",
      "modelIdentifier" : "iPod9,1",
      "minRuntimeVersionString" : "12.3.1",
      "minRuntimeVersion" : 787201,
      "name" : "iPod touch (7th generation)"
    }
  ]
}
"""

    const val RUNTIMES = """
{
  "runtimes" : [
    {
      "isAvailable" : true,
      "version" : "18.2",
      "isInternal" : false,
      "buildversion" : "22C150",
      "supportedDeviceTypes" : [
        {
          "name" : "iPhone 16 Pro",
          "productFamily" : "iPhone",
          "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPhone-16-Pro"
        },
        {
          "name" : "iPad mini (6th generation)",
          "productFamily" : "iPad",
          "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPad-mini-6th-generation"
        }
      ],
      "identifier" : "com.apple.CoreSimulator.SimRuntime.iOS-18-2",
      "platform" : "iOS",
      "name" : "iOS 18.2"
    },
    {
      "isAvailable" : true,
      "version" : "27.1",
      "isInternal" : false,
      "buildversion" : "24A94232",
      "supportedDeviceTypes" : [
        {
          "name" : "iPhone Duo",
          "productFamily" : "iPhone",
          "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPhone-Duo"
        }
      ],
      "identifier" : "com.apple.CoreSimulator.SimRuntime.iOS-27-1",
      "platform" : "iOS",
      "name" : "iOS 27.1"
    },
    {
      "isAvailable" : true,
      "version" : "27.2",
      "isInternal" : false,
      "buildversion" : "24B8117",
      "supportedDeviceTypes" : [
        {
          "name" : "iPhone 18 Pro",
          "productFamily" : "iPhone",
          "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPhone-18-Pro"
        },
        {
          "name" : "iPhone 17e",
          "productFamily" : "iPhone",
          "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPhone-17e"
        },
        {
          "name" : "iPhone 16 Pro",
          "productFamily" : "iPhone",
          "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPhone-16-Pro"
        },
        {
          "name" : "iPad Pro 13-inch (M5)",
          "productFamily" : "iPad",
          "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPad-Pro-13-inch-M5-12GB"
        },
        {
          "name" : "iPad (A16)",
          "productFamily" : "iPad",
          "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPad-A16"
        },
        {
          "name" : "iPad mini (6th generation)",
          "productFamily" : "iPad",
          "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPad-mini-6th-generation"
        }
      ],
      "identifier" : "com.apple.CoreSimulator.SimRuntime.iOS-27-2",
      "platform" : "iOS",
      "name" : "iOS 27.2"
    },
    {
      "isAvailable" : true,
      "version" : "27.2",
      "isInternal" : false,
      "buildversion" : "24B91016s",
      "supportedDeviceTypes" : [
        {
          "name" : "iPhone Duo",
          "productFamily" : "iPhone",
          "identifier" : "com.apple.CoreSimulator.SimDeviceType.iPhone-Duo"
        }
      ],
      "identifier" : "com.apple.CoreSimulator.SimRuntime.iOS-27-2-iPhone19-4",
      "platform" : "iOS",
      "name" : "iOS 27.2"
    }
  ]
}
"""

    /**
     * What `simctl create` prints once it made a simulator: its UDID.
     */
    const val CREATED = "6F1E2C3A-9B8D-4E5F-A1B2-C3D4E5F60718\n"

    /**
     * What `simctl create` prints for a device type the runtime doesn't run.
     */
    const val INCOMPATIBLE = "An error was encountered processing the command (domain=com.apple.CoreSimulator.SimError, code=403):\nIncompatible device\n"
}
