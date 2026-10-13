# VoxelScan AR PWA - Deployment Guide

## Quick Start

The PWA is ready to deploy! All files are in the `webapp/` directory.

## Deploy to Vercel (Recommended)

1. Go to [vercel.com](https://vercel.com) and sign in
2. Import this repository
3. Configure:
   - Framework Preset: Other
   - Root Directory: `/` (or keep as is)
   - Build Command: (leave empty)
   - Output Directory: `webapp`
4. Deploy!

Or using CLI:
```bash
npx vercel --cwd /workspace/app --output webapp
```

## Deploy to GitHub Pages

1. Push this repository to GitHub
2. Go to Settings > Pages
3. Source: Deploy from a branch
4. Branch: main (or your branch)
5. Folder: /webapp
6. Save

Your app will be available at: `https://username.github.io/repository-name/`

## Deploy to Netlify

1. Go to [netlify.com](https://netlify.com) and drag/drop the `webapp` folder
2. Or connect to Git with build command empty and publish directory `webapp`

## Testing Locally

```bash
cd /workspace/app/webapp
npx serve .
# or
python3 -m http.server 8000
```

Then open http://localhost:8000 in your browser or on your mobile device.

## Installation on Mobile

1. Open the deployed URL in Chrome/Safari on your phone
2. Look for "Installieren als App" button or use browser menu "Add to Home Screen"
3. The app will install like a native app with its own icon
