# AI Studio Phase 1 — Replace simulated scan input with real ARCore Depth

## Context

The current `main` branch is a successful Kotlin/Jetpack Compose conversion of the previous UI and data model, but **the live scanner is still simulated**.

Current fake path to remove from live scanning:

- `LiveScanScreen.kt` runs a timed loop and calls `telemetry.sampleArDepthHit(...)`.
- `SensorTelemetry.sampleArDepthHit(...)` synthesizes x/y/z points using `sin`, `cos`, and random colors.
- Status values such as LiDAR active / 60 FPS are currently optimistic placeholders rather than ARCore truth.

Do **not** redesign the app. Keep the current visual shell, navigation, voxel world, exporters, gallery, settings, theme, and controls unless a tiny integration seam is required.

## Goal

Make the Live Scan screen use a **real ARCore session and real ARCore Depth API data**.

Phase 1 is deliberately narrow:

1. Show the real ARCore camera feed behind the existing Compose HUD.
2. Establish ARCore tracking.
3. Enable Depth when supported.
4. Acquire real raw depth frames and raw confidence images.
5. Display truthful live diagnostics.
6. Convert a small subset of real depth samples into world-space points.
7. Insert those points into the existing `VoxelWorld` so real voxels become visible.

Do not implement advanced reconstruction, floor/wall classification, TSDF, color projection, chunk optimization, free-space carving, or filtering beyond the minimum needed to prove the real pipeline.

## Dependency

Use the current stable Google ARCore Android SDK dependency:

```kotlin
implementation("com.google.ar:core:1.56.0")
```

Keep repositories compatible with Google Maven.

## Architecture

Create a dedicated scanner backend, for example:

```text
ArCoreScannerEngine
  ├─ session lifecycle
  ├─ camera/background rendering
  ├─ tracking state
  ├─ depth support + depth mode
  ├─ raw depth acquisition
  ├─ raw confidence acquisition
  ├─ depth -> camera-space points
  ├─ camera pose -> world-space points
  └─ diagnostic StateFlow
```

The UI should consume state from this engine. Do not put ARCore session management inside random Compose callbacks.

A small interface is preferred so the scanner source remains replaceable later:

```kotlin
interface ScannerEngine {
    val diagnostics: StateFlow<ScannerDiagnostics>
    val points: Flow<ScannerPoint>
    fun resume()
    fun pause()
    fun destroy()
}
```

Names may differ, but preserve the separation.

## ARCore lifecycle

Implement normal ARCore installation/availability and camera-permission handling.

Create a `Session` only when ARCore and camera permission are available. Tie session pause/resume to the host lifecycle. Never run a second camera pipeline such as CameraX simultaneously with the ARCore session.

Configure the ARCore session before use:

```kotlin
val config = session.config
val depthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
if (depthSupported) {
    config.depthMode = Config.DepthMode.AUTOMATIC
}
session.configure(config)
```

If Depth is unsupported, show that truthfully in diagnostics. Do not generate fallback fake depth.

## Camera background

The Live Scan screen must show the real camera image driven by the same ARCore session.

Use an ARCore-compatible OpenGL camera-background renderer hosted in the existing Compose screen, e.g. via `AndroidView` + `GLSurfaceView`, while preserving the current Compose HUD as an overlay.

The renderer must:

- create an external OES camera texture,
- call `session.setCameraTextureName(...)`,
- call `session.update()` on the GL/render thread,
- draw the frame camera background,
- publish the latest frame/tracking/depth diagnostics to the scanner engine/UI.

Do not substitute a fake black viewport or a second CameraX preview.

## Depth acquisition

For each current ARCore frame, only process depth when camera tracking is `TrackingState.TRACKING`.

Acquire raw depth and confidence with:

```kotlin
frame.acquireRawDepthImage16Bits()
frame.acquireRawDepthConfidenceImage()
```

Always close both `Image` objects using `use {}` / `try-finally`.

`NotYetAvailableException` is normal while ARCore is still building depth. Count/report it but do not treat it as a fatal crash.

Also surface `NotTrackingException`, `IllegalStateException`, and resource/deadline errors in `lastError` rather than swallowing them.

Only increment the new-depth-frame counter when the raw depth timestamp changes.

## Critical confidence handling

ARCore raw confidence is **unsigned 8-bit, 0..255**.

Kotlin/Java `Byte` is signed. Never do this:

```kotlin
val confidence = buffer.get(index).toInt()
```

Do this:

```kotlin
val confidence = buffer.get(index).toInt() and 0xFF
```

For Phase 1, expose min / average / max confidence in the HUD.

Start with a permissive acceptance threshold so the pipeline can be proven. Recommended initial threshold:

```text
confidence >= 32
```

Also report how many pixels would pass `>= 128` for comparison, but do not begin with an aggressive threshold that can reduce the scan to zero.

## Strides are mandatory

Do not assume tightly packed images.

For both depth and confidence images, respect:

- `plane.rowStride`
- `plane.pixelStride`

Create/test helper functions that calculate the byte offset for `(x, y)` correctly.

Depth is 16-bit little-endian. Treat depth as an unsigned 16-bit millimetre value. A depth value of 0 is invalid.

## Real point generation

For this phase, sample only a sparse grid from the raw depth image (for example every 8–16 pixels) to keep the diagnostic path cheap.

Convert a valid depth pixel into camera-space coordinates using camera intrinsics scaled to the actual depth image dimensions. ARCore camera coordinates look forward along negative Z.

Then transform the camera-space point into ARCore world coordinates using the current camera pose.

Do not synthesize positions with trigonometric functions or randomness.

For Phase 1 the voxel color may be a fixed high-visibility debug color. Real RGB projection belongs to Phase 2.

Feed accepted real world-space points into the existing `VoxelWorld.addPoint(...)` path.

Temporarily disable or bypass any old confirmation/evidence threshold that can hide the first real point. The purpose of Phase 1 is to prove:

```text
ARCore frame -> raw depth -> confidence -> world point -> VoxelWorld -> visible voxel
```

## Remove simulation from the live path

`SensorTelemetry` may continue to provide real accelerometer/gyroscope information if useful, but `sampleArDepthHit(...)` must no longer be called by Live Scan.

Prefer deleting the fake depth generator once nothing depends on it. If kept for a demo/test mode, it must be explicitly named `Simulated...` and must never silently run as a fallback when ARCore fails.

No UI label may say `LiDAR`, `Depth`, `Tracking`, or `FPS` unless it is backed by the real current runtime state.

## Diagnostic HUD

Add a compact diagnostic block to Live Scan. It can be visually modest but must remain readable while testing.

Show at least:

```text
ARCore:          READY / INSTALLING / ERROR
Tracking:        TRACKING / PAUSED / STOPPED
Depth supported: YES / NO
Depth mode:      AUTOMATIC / DISABLED
Depth frames:    <count>
New depth FPS:   <measured rate>
Depth size:      <width>x<height>
Valid depth:     <count in sampled pixels>
Confidence:      min <n> / avg <n> / max <n>
Conf >=32:       <count>
Conf >=128:      <count>
Accepted points: <count>
Voxels:          <existing world count>
Last error:      <latest meaningful error or —>
```

These values must be real. Never hard-code `TRACKING`, `60 FPS`, `LiDAR active`, or fake counts.

## Keep the current app shell

Do not rewrite unrelated files.

Preserve:

- current Jetpack Compose visual design,
- current navigation,
- `VoxelWorld`,
- voxel renderer unless a tiny integration fix is required,
- gallery/project storage,
- existing VOX/OBJ/PLY exporters,
- settings and controls.

The Live Scan viewport may change internally to host the ARCore background surface, but the existing HUD should remain layered above it.

## Tests

Add focused unit tests for pure helpers:

1. unsigned confidence conversion:
   - byte `0x00` -> 0
   - byte `0x7F` -> 127
   - byte `0x80` -> 128
   - byte `0xFF` -> 255
2. rowStride/pixelStride index calculation.
3. little-endian unsigned depth decoding.
4. depth pixel + scaled intrinsics -> camera-space point math.

Do not mock an entire ARCore session just to create meaningless coverage.

## Acceptance criteria

Phase 1 is complete only when all of the following are true:

- App builds and launches.
- Existing UI/navigation still works.
- Live Scan shows the real camera feed.
- Tracking status comes from ARCore.
- Depth support/mode are real values.
- Moving the phone around a textured room eventually produces `Depth frames > 0`.
- Confidence min/avg/max are real and not hard-coded.
- At least some valid real depth samples are accepted.
- `VoxelWorld` receives real world-space points.
- At least one visible voxel comes from real ARCore depth.
- No `sampleArDepthHit()` or equivalent synthetic generator is used in the production Live Scan path.
- `./gradlew test assembleDebug` succeeds.

If any criterion cannot be achieved, stop and report exactly which boundary failed using the diagnostic counters. Do not hide the failure with simulated data.

## Non-goals for this pass

Do not add yet:

- RGB texture/color registration,
- floor/wall inference,
- mesh reconstruction,
- TSDF,
- free-space carving,
- multi-frame confidence fusion,
- optimized chunk streaming,
- final production filtering,
- new UI redesign,
- Gemini/Firebase features.

Those come only after the raw real pipeline is proven.