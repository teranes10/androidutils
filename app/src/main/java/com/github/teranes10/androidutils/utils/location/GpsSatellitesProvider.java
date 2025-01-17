package com.github.teranes10.androidutils.utils.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class GpsSatellitesProvider {
    private final Context _context;
    private final LocationManager _locationManger;
    private final GnssStatus.Callback _gnssStatusCallback;

    public interface GpsListener {
        void onStatusChanged(GnssStatus status);
    }

    public GpsSatellitesProvider(Context context, GpsListener listener) {
        this._context = context;
        this._locationManger = (LocationManager) _context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        this._gnssStatusCallback = new GnssStatus.Callback() {

            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                super.onSatelliteStatusChanged(status);
                if (listener != null) {
                    listener.onStatusChanged(status);
                }
            }
        };
    }

    public void startUpdates() {
        if (ActivityCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (_locationManger == null) {
            return;
        }

        _locationManger.registerGnssStatusCallback(_gnssStatusCallback);
    }

    public void stopUpdates() {
        if (_locationManger == null) {
            return;
        }

        _locationManger.unregisterGnssStatusCallback(_gnssStatusCallback);
    }
}
