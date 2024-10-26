package com.github.teranes10.androidutils.utils;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtil {
    public static final int OVERLAY_PERMISSION_REQUEST_CODE = 10001;
    public static final int EXTERNAL_STORAGE_MANAGER_PERMISSION_REQUEST_CODE = 10002;
    public static final int USAGE_STATS_PERMISSION_REQUEST_CODE = 10003;
    public static final int ACCESSIBILITY_PERMISSION_REQUEST_CODE = 10004;
    private static final String TAG = "PermissionUtil";

    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasPermission(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }

        return true;
    }

    public static boolean shouldShowPermissionRequest(Activity context, String permission) {
        return context.shouldShowRequestPermissionRationale(permission);
    }

    public static boolean shouldShowPermissionRequest(Activity context, String[] permissions) {
        for (String permission : permissions) {
            if (!shouldShowPermissionRequest(context, permission)) {
                return false;
            }
        }

        return true;
    }

    public static Boolean shouldShowStoragePermissionRequest(Activity ctx) {
        return ctx.shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE) ||
                ctx.shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE);
    }

    public static boolean hasStoragePermission(Context context) {
        return hasPermission(context, READ_EXTERNAL_STORAGE) && hasPermission(context, WRITE_EXTERNAL_STORAGE);
    }

    public static boolean isExternalStorageManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }

        return true;
    }

    public static void getExternalStorageManagerPermission(Activity context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }

        if (isExternalStorageManager()) {
            return;
        }

        Intent intent = new Intent();
        intent.setAction(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
        context.startActivityForResult(intent, EXTERNAL_STORAGE_MANAGER_PERMISSION_REQUEST_CODE);
    }

    public static void getOverlayPermission(Activity context) {
        if (hasOverlayPermission(context)) {
            return;
        }

        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
        context.startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);

    }

    public static boolean hasOverlayPermission(Context context) {
        return Settings.canDrawOverlays(context);
    }

    public static void getAccessibilityPermission(Activity context, Class<? extends AccessibilityService> service) {
        if (hasAccessibilityPermission(context, service)) {
            return;
        }

        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            context.startActivityForResult(intent, ACCESSIBILITY_PERMISSION_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "getAccessibilityPermission: ActivityNotFoundException");
        }
    }

    public static boolean hasAccessibilityPermission(Context context, Class<? extends AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo serviceInfo = enabledService.getResolveInfo().serviceInfo;
            if (serviceInfo.packageName.equals(context.getPackageName()) && serviceInfo.name.equals(service.getName())) {
                return true;
            }
        }

        return false;
    }

    public static void getUsageStatsPermission(Activity context) {
        if (hasUsageStatsPermission(context)) {
            return;
        }

        try {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            context.startActivityForResult(intent, USAGE_STATS_PERMISSION_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "getUsageStatsPermission: ActivityNotFoundException");
        }
    }

    public static boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
        if (mode == AppOpsManager.MODE_DEFAULT) {
            return (context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            return (mode == AppOpsManager.MODE_ALLOWED);
        }
    }

    public static void checkMultiplePermissions(Activity _context, String[] permissions, int request_code) {
        List<String> permissionsList = new ArrayList<>();

        for (String permission : permissions) {
            if (!hasPermission(_context, permission)) {
                permissionsList.add(permission);
            }
        }

        if (permissionsList.size() > 0) {
            ActivityCompat.requestPermissions(_context, permissionsList.toArray(new String[0]), request_code);
        }
    }

    public static Integer getStoragePermission(Activity _context, int request_code) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getExternalStorageManagerPermission(_context);        // all files access not required anymore
        } else {
            checkMultiplePermissions(_context, new String[]{
                    READ_EXTERNAL_STORAGE,
                    WRITE_EXTERNAL_STORAGE
            }, request_code);
        }
        return request_code;
    }

    public static Boolean hasLocationPermission(Context _context) {
        return hasPermission(_context, Manifest.permission.ACCESS_FINE_LOCATION) &&
                hasPermission(_context, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public static Boolean shouldShowLocationPermissionRequest(Activity ctx) {
        return ctx.shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) ||
                ctx.shouldShowRequestPermissionRationale(ACCESS_COARSE_LOCATION);
    }

    public static Integer getLocationPermission(Activity _context, int request_code) {
        checkMultiplePermissions(_context, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, request_code);
        return request_code;
    }

    public static Boolean hasCallPermission(Context _context) {
        return hasPermission(_context, Manifest.permission.CALL_PHONE);
    }

    public static Integer getCallPermission(Activity _context, int request_code) {
        checkMultiplePermissions(_context, new String[]{
                Manifest.permission.CALL_PHONE
        }, request_code);
        return request_code;
    }

    public static void openAppSettings(Activity ctx) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", ctx.getPackageName(), null);
        intent.setData(uri);
        ctx.startActivity(intent);
    }

    public static void showPermissionRationaleDialog(Activity ctx, String title, String message, String[] permissions, int code) {
        if (PermissionUtil.hasPermission(ctx, permissions)) {
            return;
        }

        if (shouldShowPermissionRequest(ctx, permissions)) {
            checkMultiplePermissions(ctx, permissions, code);
        } else {
            new AlertDialog.Builder(ctx)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Grant Permissions", (dialog, which) -> {
                        dialog.cancel();
                        PermissionUtil.openAppSettings(ctx);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.cancel();
                        Toast.makeText(ctx, "Permissions are required to proceed", Toast.LENGTH_SHORT).show();
                    })
                    .create()
                    .show();
        }
    }

    public static boolean hasAudioPermission(Context context) {
        return hasPermission(context, Manifest.permission.RECORD_AUDIO) &&
                hasPermission(context, Manifest.permission.MODIFY_AUDIO_SETTINGS);
    }

}
