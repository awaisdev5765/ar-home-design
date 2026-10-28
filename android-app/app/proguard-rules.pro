# Keep default rules for MVP starter.

# TensorFlow Lite / GPU delegate
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-dontwarn org.tensorflow.lite.**
