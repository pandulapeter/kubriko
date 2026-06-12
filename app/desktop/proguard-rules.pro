# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

# Note: the Compose Gradle plugin always prepends its own default rules file, which already covers
# Skiko, the Compose runtime, kotlinx.coroutines (including the Swing dispatcher) and
# kotlinx.serialization. The rules below only cover what that file does not.

-keepclasseswithmembers public class com.pandulapeter.kubrikoShowcase.KubrikoShowcaseAppKt {
    public static void main(java.lang.String[]);
}

# Generic attributes needed for reflection-adjacent code (serialization of generic types, stack traces).
-keepattributes Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Kotlin enums accessed through values()/valueOf() (e.g. by kotlinx.serialization).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# JLayer (MP3 playback) instantiates its audio device factory reflectively through
# javazoom.jl.player.FactoryRegistry, so its classes must keep their original names.
-keep class javazoom.** { *; }
-dontwarn javazoom.**

# Service provider implementations registered in META-INF/services must keep their names.
-keepnames class * implements javax.sound.sampled.spi.AudioFileReader
-keepnames class * implements javax.sound.sampled.spi.FormatConversionProvider
-keepnames class * implements javax.sound.sampled.spi.MixerProvider
