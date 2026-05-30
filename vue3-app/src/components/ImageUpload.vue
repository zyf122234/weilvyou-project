<template>
  <div class="image-upload" :class="[`image-upload--${variant}`, { 'image-upload--disabled': disabled }]">
    <button class="image-upload__surface" type="button" :disabled="disabled || uploading" @click="triggerUpload">
      <img v-if="modelValue" class="image-upload__image" :src="modelValue" :alt="title" />
      <span v-else class="image-upload__placeholder">{{ placeholderText }}</span>
      <span class="image-upload__mask">
        <el-icon><Camera /></el-icon>
        <span>{{ uploading ? '上传中' : title }}</span>
      </span>
    </button>
    <input
      ref="inputRef"
      class="image-upload__input"
      type="file"
      accept="image/jpeg,image/png,image/webp,image/gif"
      @change="handleFileChange"
    />
    <p v-if="hint" class="image-upload__hint">{{ hint }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  uploader: {
    type: Function,
    required: true
  },
  title: {
    type: String,
    default: '上传图片'
  },
  placeholderText: {
    type: String,
    default: '+'
  },
  hint: {
    type: String,
    default: '支持 JPG、PNG、WebP、GIF，大小不超过 5MB'
  },
  variant: {
    type: String,
    default: 'cover'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  maxSize: {
    type: Number,
    default: 5 * 1024 * 1024
  }
})

const emit = defineEmits(['update:modelValue', 'uploaded'])
const inputRef = ref()
const uploading = ref(false)
const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

function triggerUpload() {
  if (props.disabled || uploading.value) return
  inputRef.value?.click()
}

async function handleFileChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return

  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('仅支持 JPG、PNG、WebP、GIF 格式图片')
    return
  }
  if (file.size > props.maxSize) {
    ElMessage.error('图片大小不能超过 5MB')
    return
  }

  uploading.value = true
  try {
    const res = await props.uploader(file)
    const imageUrl = res?.data || res
    emit('update:modelValue', imageUrl)
    emit('uploaded', imageUrl)
    ElMessage.success('图片上传成功')
  } catch (error) {
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.image-upload {
  display: inline-flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.image-upload__surface {
  position: relative;
  width: 100%;
  min-height: 142px;
  padding: 0;
  overflow: hidden;
  color: #0f766e;
  background:
    linear-gradient(135deg, rgba(240, 249, 255, 0.94), rgba(240, 253, 250, 0.9)),
    #fff;
  border: 1px dashed rgba(14, 165, 233, 0.48);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.image-upload__surface:hover {
  border-color: #14b8a6;
  box-shadow: 0 12px 28px rgba(14, 165, 233, 0.12);
  transform: translateY(-1px);
}

.image-upload__surface:disabled {
  cursor: not-allowed;
  opacity: 0.68;
  transform: none;
}

.image-upload__image {
  width: 100%;
  height: 100%;
  min-height: inherit;
  object-fit: cover;
  display: block;
}

.image-upload__placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: 700;
}

.image-upload__mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  background: rgba(15, 23, 42, 0.52);
  opacity: 0;
  transition: opacity 0.18s ease;
}

.image-upload__surface:hover .image-upload__mask {
  opacity: 1;
}

.image-upload__input {
  display: none;
}

.image-upload__hint {
  margin: 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.image-upload--avatar {
  width: 104px;
}

.image-upload--avatar .image-upload__surface {
  width: 104px;
  height: 104px;
  min-height: 104px;
  border: 5px solid rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  background: linear-gradient(135deg, #0284c7, #14b8a6 62%, #f59e0b);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.16);
}

.image-upload--avatar .image-upload__placeholder {
  color: #fff;
  font-size: 36px;
}

.image-upload--avatar .image-upload__hint {
  display: none;
}

@media (max-width: 768px) {
  .image-upload--avatar,
  .image-upload--avatar .image-upload__surface {
    width: 86px;
    height: 86px;
    min-height: 86px;
  }

  .image-upload--avatar .image-upload__placeholder {
    font-size: 30px;
  }
}
</style>
