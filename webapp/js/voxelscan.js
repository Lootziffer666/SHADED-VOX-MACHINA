// VoxelScan AR - Core Web Implementation
// Ported from native Android implementation

class Voxel {
  constructor(x, y, z, samples, timestamp) {
    this.r = x;
    this.g = y;
    this.b = z;
    this.samples = samples;
    this.lastSeen = timestamp;
    this.lastHitFrame = -Number.MAX_VALUE;
    this.lastFreeFrame = -Number.MAX_VALUE;
    this.freeSamples = 0;
  }
  
  add(r, g, b, weight, timestamp, frame) {
    const wMax = Math.max(0.05, weight);
    const total = this.samples + wMax;
    this.r = ((this.r * this.samples) + (r * wMax)) / total;
    this.g = ((this.g * this.samples) + (g * wMax)) / total;
    this.b = ((this.b * this.samples) + (b * wMax)) / total;
    if (this.lastHitFrame !== frame) {
      this.samples = Math.min(30.0, this.samples + weight);
      this.freeSamples *= 0.94;
      this.lastHitFrame = frame;
    }
    this.lastSeen = timestamp;
  }
  
  isConfirmed(threshold = 5.0) {
    return this.samples >= threshold && this.freeSamples < (this.samples * 0.72) + 3.0;
  }
}

class VoxelWorld {
  constructor(voxelSize = 0.05) {
    this.voxelSize = voxelSize;
    this.chunks = new Map(); // chunkKey -> Map<localKey, Voxel>
    this.occupied = new Set();
    this.MAX_VOXELS = 3000;
    this.insertionsThisFrame = 0;
    this.insertionFrame = 0;
  }
  
  key(x, y, z) {
    return x + ',' + y + ',' + z;
  }
  
  worldToGrid(worldPos) {
    return Math.floor(worldPos / this.voxelSize + 1e-12);
  }
  
  gridToWorld(gridPos) {
    return gridPos * this.voxelSize;
  }
  
  getChunkKey(gridX, gridY, gridZ) {
    const cx = Math.floor(gridX / 16);
    const cy = Math.floor(gridY / 16);
    const cz = Math.floor(gridZ / 16);
    return cx + ',' + cy + ',' + cz;
  }
  
  getLocalKey(gridX, gridY, gridZ) {
    const lx = ((gridX % 16) + 16) % 16;
    const ly = ((gridY % 16) + 16) % 16;
    const lz = ((gridZ % 16) + 16) % 16;
    return lx + ',' + ly + ',' + lz;
  }
}

class VoxelScanCore {
  constructor() {
    this.voxelWorld = new VoxelWorld();
    this.isScanning = false;
    this.isPaused = false;
    this.voxelCount = 0;
    this.confirmedCount = 0;
  }
  
  startScan() {
    this.isScanning = true;
    this.isPaused = false;
  }
  
  pauseScan() {
    this.isPaused = !this.isPaused;
  }
  
  stopScan() {
    this.isScanning = false;
    this.isPaused = false;
  }
}

window.VoxelScanCore = VoxelScanCore;
window.VoxelWorld = VoxelWorld;
window.Voxel = Voxel;
