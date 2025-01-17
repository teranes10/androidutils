package com.github.teranes10.androidutils.utils.location;

import android.content.Context;
import android.location.Geocoder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.teranes10.androidutils.utils.Utils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class AddressProvider {
    private static final String TAG = "AddressProvider";

    public static CompletableFuture<Address> getAddressAsync(Context context, double latitude, double longitude) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<android.location.Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses == null || addresses.isEmpty()) {
                    return null;
                }

                return addresses.stream().map(x -> new Address(
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
            } catch (Exception e) {
                Log.e(TAG, "getAddress: " + e.getLocalizedMessage());
                return null;
            }
        });
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
}
