package com.github.teranes10.androidutils.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

public class VersionUpdateUtil {
    private static final String TAG = "VersionUpdateUtil";

    public static void installAPK(Activity context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Thread thread = new Thread(() -> {
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "installAPK: ", e);
            }
        });

        thread.start();
        context.finish();
    }
}
