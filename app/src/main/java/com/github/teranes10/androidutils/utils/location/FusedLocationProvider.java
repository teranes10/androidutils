package com.github.teranes10.androidutils.utils.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class FusedLocationProvider extends LocationProvider {
    private static final String TAG = "FusedLocationProvider";
    private final Context _ctx;
    private final LocationCallback _locationCallback;

    public FusedLocationProvider(Context ctx, ILocationListener listener) {
        Log.i(TAG, "FusedLocationProvider: ");
        this._ctx = ctx;
        this._locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                int lastIndex = locationResult.getLocations().size() - 1;
                Location location = locationResult.getLocations().get(lastIndex);
                if (listener != null) {
                    listener.onLocationChanged(location);
                }
            }
        };
    }

    @Override
    public void startUpdates(int intervalMillis, int minIntervalMills, int minDistance) {
        LocationRequest locationRequest = new LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
                .setMinUpdateIntervalMillis(minIntervalMills)
                .setMinUpdateDistanceMeters(minDistance)
                .setWaitForAccurateLocation(true)
                .build();

        if (ActivityCompat.checkSelfPermission(_ctx,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(_ctx,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            HandlerThread handlerThread = new HandlerThread("LocationHandler");
            handlerThread.start();
            LocationServices.getFusedLocationProviderClient(_ctx)
                    .requestLocationUpdates(locationRequest, _locationCallback, handlerThread.getLooper());
        }
    }

    @Override
    public void stopUpdates() {
        if(_locationCallback != null) {
            LocationServices.getFusedLocationProviderClient(_ctx).removeLocationUpdates(_locationCallback);
        }
    }
}
