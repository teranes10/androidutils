package com.github.teranes10.androidutils.utils.location

import kotlin.math.sqrt

data class MovementValues(
    val x: Float,
    val y: Float,
    val z: Float,
    val magnitude: Float = sqrt(x * x + y * y + z * z),
    val isMoving: Boolean = checkIsMoving(x, y, z)
) {

    companion object {
        const val thresh: Float = 1.0f
        const val threshN: Float = -thresh

        private fun checkIsMoving(x: Float, y: Float, z: Float): Boolean {
            val movingX = x > thresh || x < threshN
            val movingY = y > thresh || y < threshN
            val movingZ = z > thresh || z < threshN
            return movingX || movingY || movingZ
        }
    }
}