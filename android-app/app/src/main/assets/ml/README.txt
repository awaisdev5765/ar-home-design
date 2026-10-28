Wall paint — TensorFlow Lite model (optional)
==============================================

Place a semantic segmentation model here named:

  wall_deeplab.tflite

Recommended: ADE20K-trained DeepLab (MobileNetV2) with ~150 classes so class "wall"
can be isolated. Export or download a .tflite with:

  INPUT:  [1, H, W, 3] uint8 or float32, RGB, values typically [0,255] or [0,1]
  OUTPUT: either
    - [1, H, W, C] float32 logits (argmax taken in app), or
    - [1, H, W] uint8 / int32 class id per pixel

The app maps ADE20K-style class ids in WallSegmentationLabels.kt (tune WALL_CLASS_IDS
and EXCLUDED_CLASS_IDS for your model’s label order).

Without this file, the preview automatically falls back to ARCore plane painting.

Example search: "deeplabv3 ade20k tflite mobilenet" or convert from TensorFlow Hub.

Download (public, ADE20K TFLite):
  Kaggle Models — search for "deeplabv3-mobilenetv2" by spsayakpaul, TensorFlow Lite + ADE20K
  variant → download .tflite → rename to wall_deeplab.tflite in this folder.

Colab export (custom size / quant):
  github.com/sayakpaul/Adventures-in-TensorFlow-Lite — DeepLabV3/DeepLab_TFLite_ADE20k.ipynb

GPU: Interpreter uses GpuDelegate when the device supports it (falls back to CPU).
