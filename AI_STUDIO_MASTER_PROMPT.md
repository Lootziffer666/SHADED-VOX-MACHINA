# AI Studio master prompt — Phase 0 Android shell

Build a native Android version of **SHADED VOX MACHINA** from the material in this repository.

Important: AI Studio may convert/generate the application as Kotlin. That is desired. Do **not** preserve the old HTML/JavaScript runtime architecture. Treat the HTML files as UI/interaction references and rebuild the app as a clean native Android application.

Read `AI_STUDIO_START_HERE.md` first and obey it as the phase contract.

## Primary objective

Create a boring, reliable, installable Android shell before attempting voxel scanning.

The first build must prove only this chain:

```text
APP LAUNCH
  -> NAVIGATION
  -> CAMERA PERMISSION
  -> REAL REAR CAMERA PREVIEW
  -> VISIBLE DIAGNOSTICS
```

Do not implement ARCore depth or voxel generation in this first pass.

## UI source material

Use the repository's `voxelscan_ar_*.html` pages to infer the desired visual shell and screen hierarchy. Preserve useful labels, grouping, controls, and general visual identity, but simplify anything that makes the native shell fragile.

The following existing pages are reference screens:

- splash screen
- immersive/fullscreen live scan
- live scan controls
- gallery/projects
- mesh/snap
- parameters/export
- settings/info

Do not port mock data or fake behaviour from those demos.

## Native implementation constraints

- Kotlin Android application.
- Use standard Android project structure.
- Use a current stable Android SDK/toolchain available to AI Studio.
- Prefer Jetpack Compose for the shell UI unless AI Studio has a stronger native default.
- Use a native rear-camera preview path such as CameraX.
- Handle runtime camera permission.
- Handle lifecycle correctly: pause/resume/background/foreground.
- Errors must be visible in the app diagnostics instead of silently ignored.
- No login, activation server, tester licensing, vendor gate, API key, cloud backend, or account requirement.
- No network dependency is required for Phase 0.

## Screen behaviour

### Splash

Show the app identity briefly, then enter the application. Do not block launch on any network or scanner initialization.

### Live Scan

This is the important screen.

It must show:

- full camera preview,
- shell controls inspired by the reference UI,
- a compact diagnostics overlay,
- a visually present but clearly inactive scanner state.

Diagnostics must show truthful values only:

```text
CAMERA PERMISSION  GRANTED / DENIED
CAMERA PREVIEW     ACTIVE / INACTIVE
SCANNER ENGINE     NOT CONNECTED
APP STATE          ACTIVE / PAUSED
LAST ERROR         — / message
```

A Scan/Record button may be visible, but if pressed during Phase 0 it should explain that the scanner engine is not connected. Do not fabricate scan progress or voxels.

### Other screens

Gallery, Mesh/Snap, Parameters/Export, and Settings/Info need working navigation and native layouts, but their scanner-dependent actions can be inert/disabled for Phase 0.

## Architecture

Keep camera/UI lifecycle independent of scanner implementation.

Create an interface or equivalent abstraction named `ScannerEngine` with a `NoopScannerEngine` implementation for Phase 0. The UI must not know ARCore details.

Later we will add `ArCoreDepthScannerEngine` behind the same interface.

Do not build a giant framework. Keep this seam small and obvious.

## Build acceptance test

Before considering the task complete, verify that the generated project can build and that the app can logically satisfy these tests:

1. cold launch works,
2. camera permission prompt works,
3. rear camera preview becomes visible after permission,
4. denying permission does not crash the app,
5. reopening after denial gives a usable recovery path,
6. moving app to background and returning does not crash,
7. navigation between all screens works,
8. no scanner result is faked,
9. scanner absence cannot break the camera shell.

If there is a choice between preserving an old feature and making the shell reliable, choose reliability.

When the Phase 0 build is complete, stop. Do not continue into ARCore depth automatically.