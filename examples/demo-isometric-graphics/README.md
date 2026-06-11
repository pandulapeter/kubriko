<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Isometric Graphics Demo

A real-time isometric 3D world built entirely from **cuboids**, rendered on top of Kubriko's
Cartesian coordinate system. Every visible object — the characters and the trees — is a box with
individually colored or textured faces, projected into an isometric view and depth-sorted every
frame. Nothing is a pre-rendered sprite.

Move the main character with the on-screen joystick or the keyboard (WASD / arrows). Drag anywhere
to rotate and tilt the camera, and use the scroll wheel or a pinch gesture to zoom. A circular
minimap in the top-right corner tracks the character and the surrounding scenery.

This module is a flattened, editor-free embedding of the standalone Tesselar project: the model and
region editors are omitted, and the engine, renderer and gameplay code all live in this single module.
