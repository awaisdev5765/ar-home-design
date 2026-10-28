package com.arhomedesign.paintpreview.ml

/**
 * Class indices depend on the training label map. Defaults match common **ADE20K** (150 classes)
 * ordering used by many public DeepLab exports (verify against your .tflite metadata).
 *
 * Tune these when you swap models — check the model’s `associatedFiles` / labelmap or paper.
 */
object WallSegmentationLabels {

    /** Pixels with these argmax classes contribute to the wall paint mask. */
    val WALL_CLASS_IDS: IntArray = intArrayOf(
        1, // wall
        2, // building (ADE20K — often exterior; helps some exports)
        25, // curtain (often on wall)
        92, // shelf (wall-mounted in indoor scenes)
    )

    /** Remove floor, ground, person, ceiling from wall mask even if model confuses them. */
    val EXCLUDED_CLASS_IDS: IntArray = intArrayOf(
        3, // floor
        6, // road (outdoor leak)
        12, // person
        22, // ceiling
        96, // rug (floor)
        120, // sand
        128, // sea
        130, // sky
    )

    fun isWallClass(classId: Int): Boolean {
        if (classId <= 0) return false
        if (EXCLUDED_CLASS_IDS.contains(classId)) return false
        return WALL_CLASS_IDS.contains(classId)
    }
}
