<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue";
import { Html5Qrcode } from "html5-qrcode";

const props = defineProps({
  active: { type: Boolean, default: false },
  autoStart: { type: Boolean, default: false }
});

const emit = defineEmits(["scanned", "error"]);

const scannerRef = ref(null);
let scanner = null;
const scanning = ref(false);
const cameras = ref([]);
const selectedCamera = ref("");

onMounted(async () => {
  try {
    const devices = await Html5Qrcode.getCameras();
    cameras.value = devices;
    if (devices.length > 0) {
      // Prefer back camera on mobile
      const back = devices.find(
        (d) => d.label.toLowerCase().includes("back") || d.label.includes("后")
      );
      selectedCamera.value = back ? back.id : devices[devices.length - 1].id;
      // 自动开始扫码
      if (props.autoStart) {
        setTimeout(() => startScanning(), 100);
      }
    }
  } catch (err) {
    emit("error", "无法获取摄像头列表，请确认已授权摄像头权限");
  }
});

onBeforeUnmount(() => {
  stopScanning();
});

async function startScanning() {
  if (!selectedCamera.value) {
    emit("error", "未检测到可用摄像头");
    return;
  }

  scanner = new Html5Qrcode("barcode-scanner-region");

  try {
    await scanner.start(
      selectedCamera.value,
      {
        fps: 10,
        qrbox: { width: 280, height: 120 },
        aspectRatio: 1.5
      },
      (decodedText) => {
        emit("scanned", decodedText);
        stopScanning();
      },
      () => {
        // Ignore scan failures (no code found in frame)
      }
    );
    scanning.value = true;
  } catch (err) {
    emit("error", `摄像头启动失败：${err.message || err}`);
  }
}

async function stopScanning() {
  if (scanner && scanning.value) {
    try {
      await scanner.stop();
    } catch {
      // ignore
    }
    scanning.value = false;
  }
}

function switchCamera() {
  if (scanning.value) {
    stopScanning().then(() => startScanning());
  }
}

defineExpose({ startScanning, stopScanning, scanning });
</script>

<template>
  <div class="scanner-container">
    <div class="scanner-controls">
      <el-select
        v-if="cameras.length > 1"
        v-model="selectedCamera"
        placeholder="选择摄像头"
        size="small"
        class="camera-select"
        @change="switchCamera"
      >
        <el-option
          v-for="cam in cameras"
          :key="cam.id"
          :label="cam.label || `摄像头 ${cameras.indexOf(cam) + 1}`"
          :value="cam.id"
        />
      </el-select>
    </div>

    <div id="barcode-scanner-region" class="scanner-viewport"></div>

    <p v-if="!scanning && cameras.length === 0" class="scanner-hint">
      未检测到摄像头，请确认浏览器已授权摄像头权限。
    </p>
  </div>
</template>

<style scoped>
.scanner-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scanner-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.camera-select {
  min-width: 180px;
}

.scanner-viewport {
  width: 100%;
  min-height: 240px;
  border-radius: 8px;
  overflow: hidden;
  background: #1a1a1a;
}

.scanner-hint {
  margin: 0;
  color: #657a72;
  font-size: 13px;
}
</style>
