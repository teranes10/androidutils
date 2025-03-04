package com.github.teranes10.androidutils.utils.location;

import static com.github.teranes10.androidutils.utils.location.AddressProvider.getAddressAsync;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;

import java.util.concurrent.CompletableFuture;

public class LastKnownLocationProvider {

    public static CompletableFuture<Location> getLastKnownLocationAsync(Context context) {
        return CompletableFuture.supplyAsync(() -> {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }

            LocationManager lm = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                return null;
            }

            android.location.Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                return null;
            }

            if (location.getLatitude() == 0.0 && location.getLongitude() == 0.0) {
                return null;
            }

            return location;
        });
    }
}
