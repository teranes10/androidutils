package com.github.teranes10.androidutils.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.GnssStatus;
import android.location.LocationManager;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class LocationUtil {
    public static final int MIN_SATELLITE_COUNT = 4;
    private static final MutableLiveData<Location> _location = new MutableLiveData<>();
    private static final String TAG = "LocationUtil";

    private static Location getLocation() {
        return _location.getValue();
    }

    private static LiveData<Location> getLiveLocation() {
        return _location;
    }

    private static LocationCallback _locationCallback;

    public static void startLocationUpdates(Context context) {
        LocationRequest locationRequest = new LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .setMinUpdateDistanceMeters(10)
                .setWaitForAccurateLocation(true)
                .build();

        _locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                int lastIndex = locationResult.getLocations().size() - 1;
                android.location.Location currentLocation = locationResult.getLocations().get(lastIndex);

                double latitude = currentLocation.getLatitude();
                double longitude = currentLocation.getLongitude();
                double speed = currentLocation.getSpeed();
                float accuracy = currentLocation.getAccuracy();

                getAddressAsync(context, latitude, longitude).thenAccept(address -> {
                    Location location = new Location(
                            latitude, longitude,
                            address,
                            speed,
                            accuracy);

                    _location.postValue(location);
                });
            }
        };

        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            HandlerThread handlerThread = new HandlerThread("LocationHandler");
            handlerThread.start();
            LocationServices.getFusedLocationProviderClient(context)
                    .requestLocationUpdates(locationRequest, _locationCallback, handlerThread.getLooper());
        }
    }

    public static void stopLocationUpdates(Context context) {
        LocationServices.getFusedLocationProviderClient(context)
                .removeLocationUpdates(_locationCallback);
    }

    public static CompletableFuture<Location> getCurrentLocation(Context context) {
        CompletableFuture<Location> future = new CompletableFuture<>();
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            future.complete(null);
        }

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnCompleteListener(task -> {
                    android.location.Location locationResult = task.getResult();
                    if (locationResult == null) {
                        future.complete(null);
                    } else {
                        future.complete(new Location(
                                locationResult.getLatitude(),
                                locationResult.getLongitude(),
                                getAddress(context, locationResult.getLatitude(), locationResult.getLongitude()),
                                locationResult.getSpeed(),
                                locationResult.getAccuracy()));
                    }
                });

        return future;
    }

    public static CompletableFuture<Location> getLastKnownLocationAsync(Context context) {
        return CompletableFuture.supplyAsync(() -> {
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }

            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
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

            return getAddressAsync(context, location.getLatitude(), location.getLongitude())
                    .thenApply(address ->
                            new Location(
                                    location.getLatitude(),
                                    location.getLongitude(),
                                    address,
                                    location.getSpeed(),
                                    location.getAccuracy()))
                    .exceptionally(e -> null)
                    .join();
        });
    }

    private static final LinkedHashMap<LatLng, Address> _addresses = new LinkedHashMap<>();

    private static void _addAddress(double latitude, double longitude, Address address) {
        if (address == null) {
            return;
        }

        _addresses.put(new LatLng(latitude, longitude), address);
        if (_addresses.size() > 10) {
            _addresses.keySet().stream().findFirst().ifPresent(_addresses::remove);
        }
    }

    public static Address getAddress(Context context, double latitude, double longitude) {
        try {
            Address address = _addresses.get(new LatLng(latitude, longitude));
            if (address != null) {
                return address;
            }

            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            address = geocoder.getFromLocation(latitude, longitude, 1).stream()
                    .map(x -> new Address(
                            Utils.getOrDefault(x.getAddressLine(x.getMaxAddressLineIndex())),
                            Utils.getOrDefault(x.getFeatureName()),
                            Utils.getOrDefault(x.getThoroughfare()),
                            Utils.getOrDefault(x.getSubAdminArea()),
                            Utils.getOrDefault(x.getLocality()),
                            Utils.getOrDefault(x.getAdminArea()),
                            Utils.getOrDefault(x.getCountryName()),
                            Utils.getOrDefault(x.getCountryCode()),
                            Utils.getOrDefault(x.getPostalCode())
                    )).findFirst().orElse(null);

            _addAddress(latitude, longitude, address);
            return address;
        } catch (Exception e) {
            Log.e(TAG, "getAddress: " + e.getLocalizedMessage());
            return null;
        }
    }

    public static CompletableFuture<Address> getAddressAsync(Context context, double latitude, double longitude) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Address address = _addresses.get(new LatLng(latitude, longitude));
                if (address != null) {
                    return address;
                }

                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                address = geocoder.getFromLocation(latitude, longitude, 1).stream()
                        .map(x -> new Address(
                                Utils.getOrDefault(x.getAddressLine(x.getMaxAddressLineIndex())),
                                Utils.getOrDefault(x.getFeatureName()),
                                Utils.getOrDefault(x.getThoroughfare()),
                                Utils.getOrDefault(x.getSubAdminArea()),
                                Utils.getOrDefault(x.getLocality()),
                                Utils.getOrDefault(x.getAdminArea()),
                                Utils.getOrDefault(x.getCountryName()),
                                Utils.getOrDefault(x.getCountryCode()),
                                Utils.getOrDefault(x.getPostalCode())
                        )).findFirst().orElse(null);

                _addAddress(latitude, longitude, address);
                return address;
            } catch (Exception e) {
                Log.e(TAG, "getAddress: " + e.getLocalizedMessage());
                return null;
            }
        });
    }

    private static LocationManager _locationManger;
    private static GnssStatus.Callback _gnssStatusCallback;

    private static void addGpsListener(Context context, GpsListener listener) {
        if (listener == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        _locationManger = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        _gnssStatusCallback = new GnssStatus.Callback() {
            final boolean _status = false;

            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                super.onSatelliteStatusChanged(status);
                boolean currentStatus = status.getSatelliteCount() >= MIN_SATELLITE_COUNT;
                listener.onStatusChanged(status.getSatelliteCount(), currentStatus);
            }
        };

        _locationManger.registerGnssStatusCallback(_gnssStatusCallback);
    }

    private static void removeGpsListener() {
        if (_locationManger == null) {
            return;
        }
        if (_gnssStatusCallback == null) {
            return;
        }

        _locationManger.unregisterGnssStatusCallback(_gnssStatusCallback);
    }

    public static class Location {
        public double latitude;
        public double longitude;
        public Address address;
        public double speed;
        public float accuracy;

        public Location(double latitude, double longitude, Address address, double speed, float accuracy) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
            this.speed = speed;
            this.accuracy = accuracy;
        }

        @NonNull
        @Override
        public String toString() {
            return "Location{" +
                    "latitude=" + latitude +
                    ", longitude=" + longitude +
                    ", address=" + address +
                    ", speed=" + speed +
                    '}';
        }
    }

    public static class Address {
        public String addressLine;
        public String streetNo;
        public String streetName;
        public String city;
        public String suburb;
        public String state;
        public String country;
        public String countryCode;
        public String postCode;

        public Address(String addressLine, String streetNo, String streetName, String city, String suburb, String state, String country, String countryCode, String postCode) {
            this.addressLine = addressLine;
            this.streetNo = streetNo;
            this.streetName = streetName;
            this.city = city;
            this.suburb = suburb;
            this.state = state;
            this.country = country;
            this.countryCode = countryCode;
            this.postCode = postCode;
        }

        @NonNull
        @Override
        public String toString() {
            return "Address{" +
                    "streetLine='" + addressLine + '\'' +
                    ", streetNo='" + streetNo + '\'' +
                    ", streetName='" + streetName + '\'' +
                    ", suburb='" + suburb + '\'' +
                    ", state='" + state + '\'' +
                    ", country='" + country + '\'' +
                    ", postCode='" + postCode + '\'' +
                    '}';
        }
    }

    public interface GpsListener {
        void onStatusChanged(int count, boolean isGpsSignalAvailable);
    }

    public static List<LatLng> removeParallelLines(List<LatLng> points) {
        if (!(points != null && points.size() > 0)) {
            return new ArrayList<>();
        }

        List<Double> slopes = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            slopes.add((points.get(i + 1).latitude - points.get(i).latitude) /
                    (points.get(i + 1).longitude - points.get(i).longitude));
        }

        // If the slope of two line segments is equal, then the two lines are parallel.
        List<LatLng> parallelLines = new ArrayList<>();
        for (int i = 0; i < slopes.size() - 1; i++) {
            if (slopes.get(i).equals(slopes.get(i + 1))) {
                parallelLines.add(points.get(i));
                parallelLines.add(points.get(i + 1));
            }
        }

        // Remove the parallel lines from the list of points.
        List<LatLng> nonParallelPoints = new ArrayList<>();
        for (LatLng point : points) {
            if (!parallelLines.contains(point)) {
                nonParallelPoints.add(point);
            }
        }

        return nonParallelPoints;
    }

    private static boolean isBetterLocation(android.location.Location location, android.location.Location currentBestLocation, int interval) {
        if (currentBestLocation == null) {
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > interval;
        boolean isSignificantlyOlder = timeDelta < -interval;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else return isNewer && !isSignificantlyLessAccurate && isFromSameProvider;
    }

    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private static final long TIMEOUT = 10 * 1000;
    private static final float ACCURACY_THRESHOLD = 100;
    private static final float SPEED_THRESHOLD = 90;  //changed from 100 Shanthan

    public static boolean isReliable(android.location.Location location) {
        long timestamp = location.getTime();
        if (timestamp < System.currentTimeMillis() - TIMEOUT) {
            return false;
        }

        float accuracy = location.getAccuracy();
        if (accuracy > ACCURACY_THRESHOLD) {
            return false;
        }

        float speed = location.getSpeed();
        return !(speed > SPEED_THRESHOLD);
    }
}
