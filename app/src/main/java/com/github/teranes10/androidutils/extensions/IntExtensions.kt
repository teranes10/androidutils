@file:OptIn(ExperimentalContracts::class)

package com.github.teranes10.androidutils.extensions

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object IntExtensions {

    fun Int?.isNullOrZero(): Boolean {
        contract { returns(false) implies (this@isNullOrZero != null) }
        return this == null || this == 0
    }

    fun Int?.isNullOrNonPositive(): Boolean {
        contract { returns(false) implies (this@isNullOrNonPositive != null) }
        return this == null || this <= 0
    }

}