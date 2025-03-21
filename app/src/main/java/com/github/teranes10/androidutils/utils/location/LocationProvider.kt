package com.github.teranes10.androidutils.utils.location;

import android.location.Location;

public abstract class LocationProvider {
    public abstract void startUpdates(int intervalMillis, int minIntervalMills, int minDistance);

    public abstract void stopUpdates();

    public interface ILocationListener {
        void onLocationChanged(Location location);
    }
}