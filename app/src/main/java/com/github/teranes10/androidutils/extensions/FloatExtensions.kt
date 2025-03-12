package com.github.teranes10.androidutils.extensions

import kotlin.math.round

object FloatExtensions {

    fun Float.toFixed2(): Float = round(this * 100) / 100
}