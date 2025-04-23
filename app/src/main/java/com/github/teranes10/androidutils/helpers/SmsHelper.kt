package com.github.teranes10.androidutils.helpers

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.github.teranes10.androidutils.utils.AndroidUtil
import com.github.teranes10.androidutils.utils.Utils

object SmsHelper {
    private const val TAG = "SmsHelper"

    fun sentSMS(
        context: Context,
        numbers: List<String>,
        vararg messages: String,
    ): Boolean {
        try {
            val smsManager = SmsManager.getDefault()
            for (phoneNumber in numbers) {
                val validatedNumber = Utils.validatePhoneNumber(phoneNumber, "+61")
                if (validatedNumber.isNullOrBlank()) {
                    continue
                }

                for (message in messages) {
                    smsManager.sendTextMessage(validatedNumber, null, message, null, null)
                }

                AndroidUtil.toast(context, "Sent SMS to: $phoneNumber")
            }

            return true
        } catch (ex: Exception) {
            Log.e(TAG, "Send SMS: ", ex)
            return false
        }
    }
}
