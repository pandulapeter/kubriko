# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

# This doubles as the reference for Kubriko consumers (see the main README). The Compose Gradle
# plugin always prepends its own default rules, which already cover Skiko, the Compose runtime,
# kotlinx.coroutines (including the Swing dispatcher) and kotlinx.serialization; the rules here only
# add what those do not. If you obfuscate a Compose Desktop app that uses Kubriko, copy the two
# "Kubriko" blocks below (the entry-point keep above them is app-specific — swap in your own).

# App-specific: keep your application's entry point. Replace with your own main class.
-keepclasseswithmembers public class com.pandulapeter.kubrikoShowcase.KubrikoShowcaseAppKt {
    public static void main(java.lang.String[]);
}

# Kubriko (engine): needed if you serialize game state (plugin-serialization, plugin-persistence,
# the Scene Editor). Generic attributes for reflection-adjacent code, plus enum values()/valueOf().
-keepattributes Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kubriko (plugin-audio-playback only): JLayer (MP3 playback) instantiates its audio device factory
# reflectively through javazoom.jl.player.FactoryRegistry, and the audio file readers / format
# converters are resolved through META-INF/services, so all of these must keep their original names.
-keep class javazoom.** { *; }
-dontwarn javazoom.**
-keepnames class * implements javax.sound.sampled.spi.AudioFileReader
-keepnames class * implements javax.sound.sampled.spi.FormatConversionProvider
-keepnames class * implements javax.sound.sampled.spi.MixerProvider
