package com.github.teranes10.androidutils.utils.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

public class LocationManagerProvider extends LocationProvider {
    private static final String TAG = "LocationManagerProvider";
    private final Context _ctx;
    private final LocationManager mLocationManager;
    private final LocationListener mLocationListener;

    public LocationManagerProvider(Context ctx, ILocationListener listener) {
        Log.i(TAG, "LocationManagerProvider: ");
        this._ctx = ctx;
        mLocationManager = (LocationManager) _ctx.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = location -> {
            if (listener != null) {
                listener.onLocationChanged(location);
            }
        };
    }

    @Override
    public void startUpdates(int intervalMillis, int minIntervalMills, int minDistance) {
        if (mLocationManager == null) {
            return;
        }

        final boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(_ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(_ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        mLocationManager.getBestProvider(criteria, true);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minIntervalMills, minDistance, mLocationListener);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minIntervalMills, minDistance, mLocationListener);
    }

    @Override
    public void stopUpdates() {
        if (mLocationManager == null) {
            return;
        }

        mLocationManager.removeUpdates(mLocationListener);
    }
}
