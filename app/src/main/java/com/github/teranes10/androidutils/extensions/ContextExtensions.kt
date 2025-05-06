package com.github.teranes10.androidutils.extensions

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ContextExtensions {

    /**
     * Returns a content Uri for a file using FileProvider.
     *
     * ## Manifest Setup
     * To use `getUriForFile()`, add the following to your `AndroidManifest.xml`:
     *
     * ```xml
     * <application>
     *     <provider
     *         android:name="androidx.core.content.FileProvider"
     *         android:authorities="${applicationId}.provider"
     *         android:exported="false"
     *         android:grantUriPermissions="true">
     *         <meta-data
     *             android:name="android.support.FILE_PROVIDER_PATHS"
     *             android:resource="@xml/file_paths" />
     *     </provider>
     * </application>
     * ```
     *
     * ## XML Path Setup
     * In `res/xml/file_paths.xml`, include:
     *
     * ```xml
     * <?xml version="1.0" encoding="utf-8"?>
     * <paths xmlns:android="http://schemas.android.com/apk/res/android">
     *     <external-path name="external" path="." />
     *     <external-files-path name="external_files" path="." />
     *     <external-cache-path name="external_cache" path="." />
     *     <files-path name="files" path="." />
     *     <cache-path name="cache" path="." />
     * </paths>
     * ```
     *
     * @param file The file to get the URI for.
     * @return A content URI to use with sharing intents or permissions.
     */
    fun Context.getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(this, "${packageName}.provider", file)
    }
}