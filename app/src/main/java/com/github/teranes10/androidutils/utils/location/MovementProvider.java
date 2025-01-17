package com.github.teranes10.androidutils.utils.location;

import static android.content.Context.SENSOR_SERVICE;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventCallback;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MovementProvider {
    private final SensorManager _sensorManager;
    private final Sensor _accelerometer;
    private final SensorEventListener _listener;

    public MovementProvider(Context ctx, MovementEvent listener) {
        this._sensorManager = (SensorManager) ctx.getApplicationContext().getSystemService(SENSOR_SERVICE);
        this._accelerometer = _sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        this._listener = new SensorEventCallback() {
            float[] _values = new float[3];

            @Override
            public void onSensorChanged(SensorEvent event) {
                super.onSensorChanged(event);
                _values = event.values;

                listener.onMovementChanged(new MovementValues(_values[0], _values[1], _values[2]));
            }
        };
    }

    public void startUpdates() {
        _sensorManager.registerListener(_listener, _accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
    }

    public void stopUpdates() {
        _sensorManager.unregisterListener(_listener);
    }

    public interface MovementEvent {
        void onMovementChanged(MovementValues values);
    }

    public static class MovementValues {
        public float x;
        public float y;
        public float z;

        public boolean isMoving;

        public MovementValues(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.isMoving = isMovingCalculation();
        }

        float thresh = 1.0f;
        float threshN = -thresh;

        private boolean isMovingCalculation() {
            boolean movingX = x > thresh || x < threshN;
            boolean movingY = y > thresh || y < threshN;
            boolean movingZ = x > thresh || z < threshN;
            return movingX || movingY || movingZ;
        }
    }
}
