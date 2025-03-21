package com.github.teranes10.androidutils.utils.location

data class MovementValues(val x: Float, val y: Float, val z: Float) {
    val isMoving: Boolean = checkIsMoving()

    companion object {
        const val thresh: Float = 1.0f
        const val threshN: Float = -thresh
    }

    private fun checkIsMoving(): Boolean {
        val movingX = x > thresh || x < threshN
        val movingY = y > thresh || y < threshN
        val movingZ = z > thresh || z < threshN
        return movingX || movingY || movingZ
    }
}