import * as THREE from 'https://unpkg.com/three@0.158.0/build/three.module.js';
import { ARButton } from 'https://unpkg.com/three@0.158.0/examples/jsm/webxr/ARButton.js';

let camera, scene, renderer;
let controller;

const voxelWorld = new VoxelWorld();
let isScanning = false;

export async function initAR() {
  scene = new THREE.Scene();
  
  camera = new THREE.PerspectiveCamera(70, window.innerWidth / window.innerHeight, 0.01, 20);
  
  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setPixelRatio(window.devicePixelRatio);
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.xr.enabled = true;
  
  const container = document.querySelector('.ar-fullscreen-canvas');
  if (container) {
    // Clear placeholder SVG and add canvas
    const svg = container.querySelector('.voxel-3d-scene');
    if (svg) svg.remove();
    container.appendChild(renderer.domElement);
  }
  
  controller = renderer.xr.getController(0);
  scene.add(controller);
  
  const light = new THREE.HemisphereLight(0xffffff, 0xbbbbff, 1);
  light.position.set(0, 1, 0);
  scene.add(light);
  
  window.addEventListener('resize', onWindowResize);
}

function onWindowResize() {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
}

function animate() {
  renderer.setAnimationLoop(render);
}

function render(time, frame) {
  if (frame) {
    // AR frame processing would go here
  }
  renderer.render(scene, camera);
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    initAR().then(animate);
  });
} else {
  initAR().then(animate);
}
