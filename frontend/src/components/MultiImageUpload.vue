<script setup>
import { ref } from "vue";
import { uploadImage, getImageUrl } from "../api";
import { ElMessage } from "element-plus";
import { Plus, Delete } from "@element-plus/icons-vue";

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  max: { type: Number, default: 9 },
  disabled: { type: Boolean, default: false }
});

const emit = defineEmits(["update:modelValue"]);

const uploading = ref(false);

async function handleUpload(options) {
  uploading.value = true;
  try {
    const result = await uploadImage(options.file);
    const newList = [...props.modelValue, result.url];
    emit("update:modelValue", newList);
    ElMessage.success("上传成功");
  } catch (e) {
    ElMessage.error(e.message || "上传失败");
  } finally {
    uploading.value = false;
  }
}

function removeImage(index) {
  const newList = [...props.modelValue];
  newList.splice(index, 1);
  emit("update:modelValue", newList);
}
</script>

<template>
  <div class="multi-image-upload">
    <div class="image-list">
      <div v-for="(url, index) in modelValue" :key="index" class="image-item">
        <img :src="getImageUrl(url)" alt="图片" class="thumb" />
        <button v-if="!disabled" class="remove-btn" @click.prevent="removeImage(index)">
          <el-icon><Delete /></el-icon>
        </button>
      </div>
      <el-upload
        v-if="modelValue.length < max && !disabled"
        :http-request="handleUpload"
        :show-file-list="false"
        accept="image/*"
        capture="environment"
        :disabled="uploading"
        class="upload-trigger"
      >
        <div class="add-btn">
          <el-icon><Plus /></el-icon>
          <span>{{ uploading ? "上传中" : "拍照/选图" }}</span>
        </div>
      </el-upload>
    </div>
  </div>
</template>

<style scoped>
.multi-image-upload {
  width: 100%;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.image-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

.image-item .thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  padding: 0;
  border: 0;
  cursor: pointer;
}

.upload-trigger {
  width: 80px;
  height: 80px;
}

.add-btn {
  width: 80px;
  height: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  color: #909399;
  font-size: 11px;
  cursor: pointer;
}

.add-btn:hover {
  border-color: #1c6b55;
  color: #1c6b55;
}
</style>
