# VoxelScan AR - PWA Conversion

This repository contains the VoxelScan AR Android app (decompiled) and its PWA web version.

## What's been done

1. **Original App Analysis**: Extracted and examined the decompiled Android app (Java, ARCore, OpenGL ES, TSDF voxel reconstruction)
2. **UI Mockups**: The 7 HTML files represent the redesigned UI for the app
3. **PWA Conversion**: Created a fully functional PWA in `/webapp/` that:
   - Preserves all UI screens and design
   - Includes PWA manifest for installability
   - Has service worker for offline support
   - Uses WebXR + Three.js for AR capabilities (Web version)
   - Ports core voxel logic to JavaScript

## PWA Structure

```
webapp/
├── index.html        # Entry redirect
├── main.html         # Navigation hub
├── immersive.html    # Main AR scanning view (with WebXR)
├── live-scan.html    # Controls view
├── mesh-snap.html    # Mesh editing view
├── parameter-export.html # Settings/view
├── einstellungen-info.html # Info/settings
├── galerie-projekte.html  # Gallery
├── manifest.json     # PWA manifest
├── sw.js            # Service worker
├── js/
│   ├── app.js       # PWA registration & install prompt
│   ├── voxelscan.js # Core voxel classes (Voxel, VoxelWorld)
│   └── voxelscan-core.js # Three.js + WebXR implementation
└── css/app.css      # Styles
```

## Deployment

See `DEPLOYMENT.md` for detailed deployment instructions to Vercel, GitHub Pages, or Netlify.

Quick deploy to Vercel: Set output directory to `webapp/`

## Development

To run locally:
```bash
cd /workspace/app/webapp
npx serve .
```

Then open http://localhost:3000 in your browser or mobile device.

## AR Support

The web version uses WebXR API for AR. AR features require:
- Android device with ARCore support
- Chrome/Edge browser with WebXR enabled
- HTTPS connection (or localhost for testing)

## Key Differences from Native

- ARCore replaced by WebXR
- OpenGL ES shaders replaced by Three.js/WebGL
- Native Java voxel logic ported to TypeScript/JavaScript
- File I/O adapted for web (File System Access API where available)
