// Dependency-free, deterministic checks for the JavaScript embedded in WasmTriangleBridge.kt:
// exact byte transfer, counts, untextured batches, memory growth, allocation failure and fallback.
// Run with: node engine/src/webMain/checkTriangleBridge.mjs
// See README.md beside this file for when to run it and what it cannot catch.
// This is not a substitute for visual testing on real GPU drivers.
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import vm from 'node:vm';

const bridgeSource = await readFile(new URL('./kotlin/com/pandulapeter/kubriko/implementation/WasmTriangleBridge.kt', import.meta.url), 'utf8');
const functions = [...bridgeSource.matchAll(/@JsFun\("""([\s\S]*?)"""\)/g)].map(m => m[1]);
assert.equal(functions.length, 2);
const sourceMemory = new WebAssembly.Memory({initial: 32});
const targetMemory = new WebAssembly.Memory({initial: 32});
let hide, drawArgs, frees = 0, allocations = 0, failAllocation = false;
const context = vm.createContext({wasmExports: {memory: sourceMemory}, globalThis: {
  addEventListener: (name, callback) => { assert.equal(name, 'pagehide'); hide = callback; },
}});
const create = vm.runInContext(`(${functions[0]})`, context);
const draw = vm.runInContext(`(${functions[1]})`, context);
assert.equal(create({}), null);
const native = {
  memory: targetMemory,
  malloc: () => { allocations++; return failAllocation ? 0 : 64; },
  free: () => { frees++; },
  org_jetbrains_skia_Canvas__1nDrawVertices: (canvas, mode, vertices, positions, colors, texture, indices, indexOffset, blendMode, paint) => {
    drawArgs = [canvas, mode, vertices, positions, colors, texture, indices, indexOffset, blendMode, paint];
  },
};
assert.equal(create({wasmExports: {...native, org_jetbrains_skia_Canvas__1nDrawVertices: () => {}}}), null);
context.globalThis.__kubrikoLegacyVertices = true;
assert.equal(create({wasmExports: native}), null);
delete context.globalThis.__kubrikoLegacyVertices;
const bridge = create({wasmExports: native});
for (const [vertices, indices, textured] of [[3,3,false],[37,69,true],[65535,65535,true],[3,3,false]]) {
  const colors = vertices * 8, texture = colors + vertices * 4;
  const indexOffset = texture + (textured ? vertices * 8 : 0);
  const bytes = indexOffset + indices * 2;
  const expected = new Uint8Array(bytes);
  for (let i = 0; i < bytes; i++) expected[i] = (i * 131 + 193) & 255;
  new Uint8Array(sourceMemory.buffer).set(expected, 128);
  assert.equal(draw(bridge,128,bytes,11,22,vertices,colors,textured?texture:-1,indices,indexOffset,13), true);
  assert.deepEqual(drawArgs, [11,0,vertices,64,64+colors,textured?64+texture:0,indices,64+indexOffset,13,22]);
  assert.deepEqual(new Uint8Array(targetMemory.buffer,64,bytes), expected);
  // Both memories can grow between draws; old typed-array views must not survive.
  sourceMemory.grow(1); targetMemory.grow(1);
}
assert.equal(allocations, 2);
hide();
assert.equal(bridge.pointer, 0);
failAllocation = true;
assert.equal(draw(bridge,128,42,11,22,3,24,-1,3,36,13), false);
failAllocation = false;
assert.equal(draw(bridge,128,42,11,22,3,24,-1,3,36,13), true);
assert.equal(frees, 2);

console.log('PASS: WasmTriangleBridge byte/count/lifetime/fallback checks. Not full GPU equivalence.');
