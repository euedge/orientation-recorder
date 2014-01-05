/*
 * Copyright (C) 2014 EU Edge LLC, http://euedge.com/
 *
 * This code is modification of a work of The Android Open Source Project,
 * see the original license statement below.
 *
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.euedge.glass.orientation;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import com.euedge.glass.orientationrecorder.util.MathUtils;

/**
 * Collects and communicates information about the user's current orientation and location,
 * using the sensors provided by Android.
 */
public class SensorsOrientationManager implements OrientationManager {

    /**
     * The minimum distance desired between location notifications.
     */
    private static final long METERS_BETWEEN_LOCATIONS = 1;

    /**
     * The minimum elapsed time desired between location notifications.
     */
    private static final long MILLIS_BETWEEN_LOCATIONS = TimeUnit.SECONDS.toMillis(3);

    /**
     * The maximum age of a location retrieved from the passive location provider before it is
     * considered too old to use when the compass first starts up.
     */
    private static final long MAX_LOCATION_AGE_MILLIS = TimeUnit.MINUTES.toMillis(30);

    /**
     * The sensors used by the compass are mounted in the movable arm on Glass. Depending on how
     * this arm is rotated, it may produce a displacement ranging anywhere from 0 to about 12
     * degrees. Since there is no way to know exactly how far the arm is rotated, we just split the
     * difference.
     */
    private static final int ARM_DISPLACEMENT_DEGREES = 6;

    private final SensorManager mSensorManager;
    private final LocationManager mLocationManager;
    private final String mLocationProvider;
    private final Set<OnChangedListener> mListeners;
    private final float[] mRotationMatrix;
    private final float[] mOrientation;

    private boolean mTracking;
    private float mHeading;
    private float mRoll;
    private float mPitch;
    private Location mLocation;
    private GeomagneticField mGeomagneticField;
    private boolean mHasInterference;

    /**
     * The sensor listener used by the orientation manager.
     */
    private SensorEventListener mSensorListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mHasInterference = (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
                notifyAccuracyChanged();
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                // Get the current heading from the sensor, then notify the listeners of the
                // change.
                SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
                SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X,
                        SensorManager.AXIS_Z, mRotationMatrix);
                SensorManager.getOrientation(mRotationMatrix, mOrientation);

                // Store the pitch (used to display a message indicating that the user's head
                // angle is too steep to produce reliable results.
                mPitch = (float) Math.toDegrees(mOrientation[1]);

                // Convert the heading (which is relative to magnetic north) to one that is
                // relative to true north, using the user's current location to compute this.
                float magneticHeading = (float) Math.toDegrees(mOrientation[0]);
                mHeading = MathUtils.mod(computeTrueNorth(magneticHeading), 360.0f)
                        - ARM_DISPLACEMENT_DEGREES;

                notifyOrientationChanged();
                
            } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                mRoll = (float) -Math.atan(event.values[0]
                        / Math.sqrt(event.values[1] * event.values[1] + event.values[2] * event.values[2]));

                notifyOrientationChanged();
            }
        }
    };

    /**
     * The location listener used by the orientation manager.
     */
    protected LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;
            updateGeomagneticField();
            notifyLocationChanged();
        }

        @Override
        public void onProviderDisabled(String provider) {
            // Don't need to do anything here.
        }

        @Override
        public void onProviderEnabled(String provider) {
            // Don't need to do anything here.
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Don't need to do anything here.
        }
    };

    /**
     * Initializes a new instance of {@code OrientationManager}, using the specified context to
     * access system services.
     */
    public SensorsOrientationManager(SensorManager sensorManager, LocationManager locationManager) {
        mRotationMatrix = new float[16];
        mOrientation = new float[9];
        mSensorManager = sensorManager;
        mLocationManager = locationManager;
        mListeners = new LinkedHashSet<OnChangedListener>();

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setBearingRequired(true);
        criteria.setSpeedRequired(true);
        criteria.setAltitudeRequired(true);

        mLocationProvider = mLocationManager.getBestProvider(criteria, false);
    }

    /* (non-Javadoc)
	 * @see com.euedge.glass.orientationrecorder.OrientationManagerI#addOnChangedListener(com.euedge.glass.orientationrecorder.OrientationManager.OnChangedListener)
	 */
    @Override
	public void addOnChangedListener(OnChangedListener listener) {
        mListeners.add(listener);
    }

    /* (non-Javadoc)
	 * @see com.euedge.glass.orientationrecorder.OrientationManagerI#removeOnChangedListener(com.euedge.glass.orientationrecorder.OrientationManager.OnChangedListener)
	 */
    @Override
	public void removeOnChangedListener(OnChangedListener listener) {
        mListeners.remove(listener);
    }

    /* (non-Javadoc)
	 * @see com.euedge.glass.orientationrecorder.OrientationManagerI#start()
	 */
    @Override
	public void start() {
        if (!mTracking) {
            mSensorManager.registerListener(mSensorListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                    SensorManager.SENSOR_DELAY_UI);

            mSensorManager.registerListener(mSensorListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                    SensorManager.SENSOR_DELAY_UI);

            // The rotation vector sensor doesn't give us accuracy updates, so we observe the
            // magnetic field sensor solely for those.
            mSensorManager.registerListener(mSensorListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    SensorManager.SENSOR_DELAY_UI);

            Location lastLocation = mLocationManager
                    .getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (lastLocation != null) {
                long locationAge = lastLocation.getTime() - System.currentTimeMillis();
                if (locationAge < MAX_LOCATION_AGE_MILLIS) {
                    mLocation = lastLocation;
                    updateGeomagneticField();
                }
            }

            if (mLocationProvider != null) {
                mLocationManager.requestLocationUpdates(mLocationProvider,
                        MILLIS_BETWEEN_LOCATIONS, METERS_BETWEEN_LOCATIONS, mLocationListener,
                        Looper.getMainLooper());
            }

            mTracking = true;
        }
    }

    /* (non-Javadoc)
	 * @see com.euedge.glass.orientationrecorder.OrientationManagerI#stop()
	 */
    @Override
	public void stop() {
        if (mTracking) {
            mSensorManager.unregisterListener(mSensorListener);
            mLocationManager.removeUpdates(mLocationListener);
            mTracking = false;
        }
    }

    /* (non-Javadoc)
	 * @see com.euedge.glass.orientationrecorder.OrientationManagerI#hasInterference()
	 */
    @Override
	public boolean hasInterference() {
        return mHasInterference;
    }

    /* (non-Javadoc)
	 * @see com.euedge.glass.orientationrecorder.OrientationManagerI#hasLocation()
	 */
    @Override
	public boolean hasLocation() {
        return mLocation != null;
    }

    /* (non-Javadoc)
	 * @see com.euedge.glass.orientationrecorder.OrientationManagerI#getHeading()
	 */
    @Override
	public float getHeading() {
        return mHeading;
    }

    /* (non-Javadoc)
	 * @see com.euedge.glass.orientationrecorder.OrientationManagerI#getPitch()
	 */
    @Override
	public float getPitch() {
        return mPitch;
    }

    /* (non-Javadoc)
	 * @see com.euedge.glass.orientationrecorder.OrientationManagerI#getRoll()
	 */
    @Override
	public float getRoll() {
        return mRoll;
    }

    /* (non-Javadoc)
	 * @see com.euedge.glass.orientationrecorder.OrientationManagerI#getLocation()
	 */
    @Override
	public Location getLocation() {
        return mLocation;
    }

    /**
     * Notifies all listeners that the user's orientation has changed.
     */
    private void notifyOrientationChanged() {
        for (OnChangedListener listener : mListeners) {
            listener.onOrientationChanged(this);
        }
    }

    /**
     * Notifies all listeners that the user's location has changed.
     */
    private void notifyLocationChanged() {
        for (OnChangedListener listener : mListeners) {
            listener.onLocationChanged(this);
        }
    }

    /**
     * Notifies all listeners that the compass's accuracy has changed.
     */
    private void notifyAccuracyChanged() {
        for (OnChangedListener listener : mListeners) {
            listener.onAccuracyChanged(this);
        }
    }

    /**
     * Updates the cached instance of the geomagnetic field after a location change.
     */
    private void updateGeomagneticField() {
        mGeomagneticField = new GeomagneticField((float) mLocation.getLatitude(),
                (float) mLocation.getLongitude(), (float) mLocation.getAltitude(),
                mLocation.getTime());
    }

    /**
     * Use the magnetic field to compute true (geographic) north from the specified heading
     * relative to magnetic north.
     *
     * @param heading the heading (in degrees) relative to magnetic north
     * @return the heading (in degrees) relative to true north
     */
    private float computeTrueNorth(float heading) {
        if (mGeomagneticField != null) {
            return heading + mGeomagneticField.getDeclination();
        } else {
            return heading;
        }
    }

    @Override
    public boolean isRecording() {
        return false;
    }

    @Override
    public boolean isReplaying() {
        return false;
    }
}
