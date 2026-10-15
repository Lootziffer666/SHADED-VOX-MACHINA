# SHADED VOX MACHINA — AI Studio build brief

## Goal

Use Google AI Studio to create a **clean native Android app shell** that AI Studio may convert/generate as Kotlin. The purpose of this first build is not to repair or port the old scanner implementation. The purpose is to establish a shell that installs, launches, navigates, requests camera permission, and shows a real live camera preview reliably.

The existing HTML files in this repository are **visual/reference material only**. Do not translate their JavaScript behaviour one-to-one and do not assume their counters, scan state, export state, or voxel behaviour is real.

The decompiled tester source ZIP is **reference material only**. Do not use it as the application foundation.

## Phase 0 — known-good shell

Build these screens:

1. Splash / launch screen
2. Live Scan
3. Gallery / Projects
4. Mesh / Snap
5. Parameters / Export
6. Settings / Info

Use the existing `voxelscan_ar_*.html` files to reproduce the layout, labels, interaction hierarchy, and general appearance where useful.

### Live Scan requirements for the first build

- Request Android camera permission correctly.
- Show a real rear-camera preview.
- Use a native Android camera path such as CameraX for this shell build.
- The preview must survive pause/resume and screen rotation without crashing.
- Show a small diagnostics panel with only truthful state:
  - CAMERA PERMISSION
  - CAMERA PREVIEW
  - APP STATE
  - LAST ERROR
- Scan/record controls may exist visually, but until the scanner engine is connected they must not pretend to scan.
- Do not generate fake voxels.
- Do not randomize counters.
- Do not report a successful export unless an actual file was written.

## Architecture seam for the real scanner

Create a small scanner abstraction so the known-good app shell is independent from ARCore/depth work.

Conceptually:

```text
LiveScanScreen
    |
    v
ScannerEngine interface
    |
    +-- NoopScannerEngine        <- Phase 0, shell only
    +-- ArCoreDepthScannerEngine <- Phase 1, added later
```

The shell must remain functional if `ScannerEngine` is unavailable or fails.

Suggested state exposed by the interface later:

```text
scannerState
trackingState
depthSupported
depthEnabled
depthFrameCount
validDepthSamples
acceptedSamples
voxelCount
lastError
```

Do not implement fake values for these fields in Phase 0.

## Explicitly out of scope for Phase 0

Do not port yet:

- ARCore depth fusion
- voxel confirmation / contradiction removal
- TSDF reconstruction
- floor or wall alignment
- chunk meshing
- MagicaVoxel export
- JSON scan export
- activation / tester licensing logic
- voxel caps or tester limits

Those belong after the native shell is proven on-device.

## Definition of done

Phase 0 is done only when an installable build can:

1. launch without crashing,
2. navigate all shell screens,
3. request camera permission,
4. display the real rear-camera feed on Live Scan,
5. recover from background/foreground transitions,
6. show errors visibly instead of swallowing them,
7. contain no fake scan result.

Once that build is proven on the target phone, add ARCore depth as Phase 1 without replacing the shell.