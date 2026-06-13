# This file is part of Kubriko.
# Copyright (c) Pandula Péter 2025-2026.
# https://github.com/pandulapeter/kubriko
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://mozilla.org/MPL/2.0/.

# Kubriko needs no app-side ProGuard/R8 rules on Android: R8 automatically applies the consumer
# rules that the engine's dependencies ship inside their artifacts. This file is intentionally
# empty apart from any rules your own game code requires.