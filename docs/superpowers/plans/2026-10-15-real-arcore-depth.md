# Real ARCore Depth Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the converted app's simulated Live Scan input with a real ARCore camera + Raw Depth pipeline that produces visible voxels and truthful diagnostics.

**Architecture:** Keep the current Compose shell and `VoxelWorld`. Add a focused ARCore scanner backend plus an OpenGL camera background surface. A new Phase-1 live screen consumes scanner diagnostics and real world-space depth points; the old simulated screen remains in the tree only as reference and is no longer routed in production.

**Tech Stack:** Kotlin, Jetpack Compose, Android GLSurfaceView/OpenGL ES 2, Google ARCore Android SDK 1.56.0, StateFlow/SharedFlow, JUnit 4.

**Spec:** `AI_STUDIO_PHASE1_REAL_ARCORE.md`

## Global Constraints

- Do not redesign unrelated screens or data/export code.
- No synthetic depth fallback in the production live path.
- Raw confidence is unsigned 8-bit and must be converted with `and 0xFF`.
- Respect depth/confidence `rowStride` and `pixelStride`.
- Raw depth is unsigned little-endian 16-bit millimetres.
- Phase 1 uses sparse samples and a permissive confidence threshold of 32.
- Real RGB registration, TSDF, floor/wall inference and production fusion are non-goals.

## Review Focus

- Camera permission granted after first composition must recover without restart.
- ARCore install/unavailable states must be visible instead of replaced with fake data.
- Raw depth buffers with padding/non-default strides must decode correctly.
- Signed Java/Kotlin bytes >= 128 must preserve confidence 128..255.
- Session pause/resume must not race `Session.update()` on the GL thread.

---

### Task 1: Depth math helpers

**Files:**
- Create: `app/src/test/java/com/voxelscan/android/scanner/DepthImageMathTest.kt`
- Create: `app/src/main/java/com/voxelscan/android/scanner/DepthImageMath.kt`

**Interfaces:**
- Produces: unsigned confidence conversion, stride offset, unsigned LE depth decode, depth-pixel-to-camera-point conversion.

- [ ] Write tests for 0x00/0x7F/0x80/0xFF confidence, padded strides, LE uint16, and scaled intrinsics.
- [ ] Verify the test is red before implementation.
- [ ] Implement the minimal pure Kotlin helpers.
- [ ] Verify unit tests pass.

### Task 2: ARCore scanner engine and camera renderer

**Files:**
- Create: `app/src/main/java/com/voxelscan/android/scanner/ScannerModels.kt`
- Create: `app/src/main/java/com/voxelscan/android/scanner/ArCoreScannerEngine.kt`
- Create: `app/src/main/java/com/voxelscan/android/scanner/ArCoreCameraSurface.kt`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: real camera frames, diagnostics `StateFlow`, real point batches `SharedFlow`.

- [ ] Add ARCore 1.56.0 and optional ARCore manifest metadata.
- [ ] Configure `DepthMode.AUTOMATIC` only when supported.
- [ ] Render the ARCore camera texture on the GL thread.
- [ ] Acquire raw depth + confidence, count only new timestamps, and close images reliably.
- [ ] Convert sparse accepted samples to camera space, then ARCore world space.
- [ ] Publish real diagnostics and batches; never simulate fallback samples.

### Task 3: Real Phase-1 Live Scan screen

**Files:**
- Create: `app/src/main/java/com/voxelscan/android/ui/screens/ArCoreLiveScanScreen.kt`
- Modify: `app/src/main/java/com/voxelscan/android/ui/VoxelScanApp.kt`

**Interfaces:**
- Consumes scanner diagnostics/point batches.
- Produces real points in existing `VoxelWorld` and visible diagnostic/voxel UI.

- [ ] Request/re-check camera permission.
- [ ] Tie surface + scanner to lifecycle.
- [ ] Feed accepted batches into existing `VoxelWorld` only while scanning is enabled.
- [ ] Display truthful ARCore/depth/confidence counters and current voxel count.
- [ ] Preserve Mesh/Export hooks, grid selection, save/reset and basic scan controls.
- [ ] Route `LIVE_SCAN` to this screen so `sampleArDepthHit()` is absent from production live flow.

### Task 4: Build verification

**Files:**
- Create: `.github/workflows/android-ci.yml`

- [ ] Run `./gradlew testDebugUnitTest assembleDebug` on the branch.
- [ ] Fix compile/test failures by root cause.
- [ ] Confirm debug APK artifact is produced.
- [ ] Open a PR to `main`; do not merge automatically.
