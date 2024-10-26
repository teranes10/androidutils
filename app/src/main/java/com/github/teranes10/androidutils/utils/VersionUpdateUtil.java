package com.github.teranes10.androidutils.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

public class VersionUpdateUtil {
    private static final String TAG = "VersionUpdateUtil";

    public static void installAPK(Activity context, String filePath)  {
        File file = new File(filePath);

        if (file.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uriFromFile(context, new File(filePath)), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);


            // startActivity(intent);
            Thread thread = new Thread(() -> {
                try {
                    context.startActivity(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
            thread.start();
            context.finish();

        } else {
            Toast.makeText(context, "Installing another version of myDevice.", Toast.LENGTH_LONG).show();
        }
    }

    private static Uri uriFromFile(Context context, File file) {
        return FileProvider.getUriForFile(context, "au.com.softclient.mydevices.provider", file);
    }
}
