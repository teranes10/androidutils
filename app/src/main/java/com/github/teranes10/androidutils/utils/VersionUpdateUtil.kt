package com.github.teranes10.androidutils.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.github.teranes10.androidutils.extensions.ContextExtensions.getUriForFile
import java.io.File

object VersionUpdateUtil {
    private const val TAG = "VersionUpdateUtil"

    fun Context.openUnknownSourcesSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    fun Context.installAPK(apkFile: File) {
        if (!packageManager.canRequestPackageInstalls()) {
            openUnknownSourcesSettings()
            return
        }

        val uri = getUriForFile(apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No Activity found to handle APK installation.", e)
            Toast.makeText(this, "Unable to install APK", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error installing APK", e)
        }
    }
}
