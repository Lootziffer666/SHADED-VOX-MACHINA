# VoxelScan AR PWA

This is a Progressive Web App version of VoxelScan AR built from the decompiled app and redesigned UI mockups.

## Features
- Installable as PWA (works offline with service worker)
- Responsive UI based on the provided HTML mockups
- Can be deployed to Vercel, GitHub Pages, or Netlify

## Deployment

### Vercel
1. Connect this repository/folder to Vercel
2. Set build command: (none - static site)
3. Set output directory: webapp
4. Deploy

### GitHub Pages
1. Push to GitHub
2. Enable Pages for webapp directory
3. Access via https://username.github.io/repo-name

## Local Development
```bash
# Serve the webapp directory
npx serve webapp
# or
python3 -m http.server 8000 -d webapp
```

## Notes
The original Android app used ARCore + OpenGL for real-time TSDF voxel reconstruction.
This PWA preserves the UI/UX design and adds PWA capabilities for web deployment.
