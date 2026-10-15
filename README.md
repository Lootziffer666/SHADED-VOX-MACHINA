# VoxelScan AR (SHADED-VOX-MACHINA)

VoxelScan AR is a 3D volumetric AR scanning, LiDAR mesh reconstruction, and MagicaVoxel VOX export engine built for Android with Kotlin and Jetpack Compose.

## Key Features

- **Live AR Voxel Scanner:** Real-time 3D voxelization engine using spatial hash mapping, raycast free-space carving, hit evidence accumulation, and confidence scoring.
- **Interactive 3D Voxel Viewport:** Orbit (yaw & pitch), pinch-to-zoom, pan, isometric 3D shaded solid cubes, point clouds with depth glow, wireframe bounding mesh, and elevation layer cutaway slider.
- **Mesh & Snap Processing:**
  - Axis Yaw orientation alignment (-180° to +180°) with auto room edge snapping.
  - Multi-resolution grid snapping: 0.01m (1cm), 0.02m (2cm), 0.05m (5cm), and 0.10m (10cm).
  - Outlier filtering & noise reduction with minimum evidence thresholds.
  - Live mesh metrics (raw hits, confirmed voxels, estimated triangles).
- **Multi-Format 3D Exporter:**
  - **MagicaVoxel (.vox):** Full binary RIFF chunk encoder (`MAIN`, `SIZE`, `XYZI`, `RGBA`) supporting 256-color palettes.
  - **Wavefront OBJ (.obj):** 3D polygonal quad mesh with embedded RGB vertex colors.
  - **Stanford PLY (.ply):** Volumetric point cloud format with coordinates, RGB, and confidence metrics.
  - **Native Android Sharesheet:** Direct export and file sharing via `FileProvider`.
- **Project Gallery:**
  - Pre-loaded showcase projects (*Wohnzimmer Ostwand*, *Cyberpunk Mech Core*, *Antike Stele*, *Espresso Machine Studio*, *Neo-Tokyo Street Corner*).
  - Local storage monitoring with quota telemetry.
  - Fullscreen 3D model inspector with orbit controls and layer slicing.
- **Hardware Sensor Telemetry:**
  - ARCore Depth API & 6DoF gyroscope tracking (Pitch, Roll, Yaw).
  - Performance presets (Performance, Balanced, Ultra).
  - Cyberpunk Emerald/Amber theme.

## Architecture

- **Platform:** Android SDK 36, Kotlin 2.2.10, Gradle 9.3.1, AGP 9.1.1.
- **UI Framework:** 100% Jetpack Compose with Material Design 3.
- **Graphics Pipeline:** Custom 2.5D/3D projection engine with depth-sorted Painter's algorithm in Compose Canvas.
