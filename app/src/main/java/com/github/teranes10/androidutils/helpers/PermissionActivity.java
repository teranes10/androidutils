package com.github.teranes10.androidutils.helpers;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CALL_PHONE;
import static android.Manifest.permission.MODIFY_AUDIO_SETTINGS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.SEND_SMS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.teranes10.androidutils.ui.ClickListener;
import com.github.teranes10.androidutils.utils.AppUtil;
import com.github.teranes10.androidutils.utils.PermissionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class PermissionActivity extends AppCompatActivity {
    public abstract List<PermissionDetail> getPermissions();

    public abstract void onPermissionGranted();

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        handlePermissions();
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        handlePermissions();
    }

    private AlertDialog _alertDialog = null;

    private void handlePermissions() {
        List<PermissionDetail> permissionsNotGranted = new ArrayList<>();

        for (PermissionDetail detail : getPermissions()) {
            if (detail.hasPermission != null) {
                if (!detail.hasPermission.apply(this)) {
                    permissionsNotGranted.add(detail);
                }
            } else if (detail.permissions != null) {
                if (!PermissionUtil.hasPermission(this, detail.permissions)) {
                    permissionsNotGranted.add(detail);
                }
            }
        }

        if (!permissionsNotGranted.isEmpty()) {
            String permissionNames = permissionsNotGranted.stream().map(x -> x.name).collect(Collectors.joining(", "));

            if (_alertDialog != null) {
                _alertDialog.cancel();
            }

            _alertDialog = new AlertDialog.Builder(this)
                    .setTitle("Permissions Required")
                    .setMessage("This app requires " + permissionNames + " permissions to function properly. Please grant the necessary permissions.")
                    .setPositiveButton("Grant Permissions", (dialog, which) -> {
                        if (ClickListener.isDoubleClick()) {
                            return;
                        }

                        dialog.cancel();

                        if (AppUtil.isFirstBoot(this)) {
                            String[] permissions = permissionsNotGranted.stream()
                                    .map(x -> x.permissions)
                                    .filter(Objects::nonNull)
                                    .flatMap(Arrays::stream)
                                    .toArray(String[]::new);

                            PermissionUtil.checkMultiplePermissions(this, permissions, 10000);
                        } else {
                            for (PermissionDetail detail : permissionsNotGranted) {
                                if (detail.getPermission != null) {
                                    detail.getPermission.accept(this);
                                } else if (detail.permissions != null) {
                                    PermissionUtil.showPermissionRationaleDialog(this,
                                            detail.title,
                                            detail.desc,
                                            detail.permissions,
                                            detail.requestCode
                                    );
                                }
                            }
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        if (ClickListener.isDoubleClick()) {
                            return;
                        }

                        dialog.cancel();

                        Toast.makeText(this, "Permissions are required to proceed", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .create();

            _alertDialog.show();
        } else {
            onPermissionGranted();
        }
    }

    public static class PermissionDetail {
        private final String name;
        private String title;
        private String desc;
        private String[] permissions;
        private int requestCode;
        private Function<Activity, Boolean> hasPermission;
        private Consumer<Activity> getPermission;

        public PermissionDetail(String name, String title, String desc, String[] permissions, int requestCode) {
            this.name = name;
            this.title = title;
            this.desc = desc;
            this.permissions = permissions;
            this.requestCode = requestCode;
        }

        public PermissionDetail(String name, Function<Activity, Boolean> hasPermission, Consumer<Activity> getPermission) {
            this.name = name;
            this.getPermission = getPermission;
            this.hasPermission = hasPermission;
        }

        public static PermissionDetail getStorage() {
            return new PermissionDetail(
                    "Storage",
                    "Storage Permission Required",
                    "This app needs storage permissions to function properly. Please allow access.",
                    new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE},
                    10001
            );
        }

        public static PermissionDetail getMic() {
            return new PermissionDetail(
                    "Record Audio",
                    "Audio Permission Required",
                    "This app needs audio permissions to stream and listen to audio. Please allow access.",
                    new String[]{RECORD_AUDIO, MODIFY_AUDIO_SETTINGS},
                    10002
            );
        }

        public static PermissionDetail getLocation() {
            return new PermissionDetail(
                    "Location",
                    "Location Permission Required",
                    "This app needs location permissions to function properly. Please allow access.",
                    new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION},
                    10003
            );
        }

        public static PermissionDetail getBackgroundLocation() {
            return new PermissionDetail(
                    "Background Location",
                    activity -> PermissionUtil.hasPermission(activity, ACCESS_BACKGROUND_LOCATION),
                    activity -> {
                        if (PermissionUtil.hasPermission(activity, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION})) {
                            PermissionUtil.showPermissionRationaleDialog(
                                    activity, "Background Location Permission Required.",
                                    "This app needs background location permissions to function properly. Please allow access.",
                                    new String[]{ACCESS_BACKGROUND_LOCATION},
                                    10004);
                        }
                    }
            );
        }

        public static PermissionDetail getPhone() {
            return new PermissionDetail(
                    "Phone",
                    "Phone Permission Required.",
                    "This app needs phone permissions to function properly. Please allow access.",
                    new String[]{CALL_PHONE, SEND_SMS},
                    10005
            );
        }

        public static PermissionDetail getOverlay() {
            return new PermissionDetail(
                    "Overlay",
                    PermissionUtil::hasOverlayPermission,
                    PermissionUtil::getOverlayPermission
            );
        }

        public static PermissionDetail getExternalStorage() {
            return new PermissionDetail(
                    "External Storage Manager",
                    activity -> PermissionUtil.hasExternalStorageManagerPermission(),
                    PermissionUtil::getExternalStorageManagerPermission
            );
        }

        public static PermissionDetail getUsageStats() {
            return new PermissionDetail(
                    "Usage Stats",
                    PermissionUtil::hasUsageStatsPermission,
                    PermissionUtil::getUsageStatsPermission
            );
        }
    }
}
