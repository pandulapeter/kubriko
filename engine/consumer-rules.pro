# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

# A Kubriko instance registers at most one Manager per class, so every Manager subclass must stay a class of its own.
# Class merging would otherwise give two Managers one class, and the second one registered would be dropped.
# Unused Managers can still be removed, and all of them renamed.
-keep,allowobfuscation,allowshrinking class * extends com.pandulapeter.kubriko.manager.Manager
