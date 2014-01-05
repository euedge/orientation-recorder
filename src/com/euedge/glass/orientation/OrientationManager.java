/*
 * Copyright (C) 2014 EU Edge LLC, http://euedge.com/
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

import android.location.Location;

public interface OrientationManager {

    /**
     * Classes should implement this interface if they want to be notified of changes in the user's
     * location, orientation, or the accuracy of the compass.
     */
    public interface OnChangedListener {
        /**
         * Called when the user's orientation changes.
         *
         * @param orientationManager the orientation manager that detected the change
         */
        void onOrientationChanged(OrientationManager orientationManager);

        /**
         * Called when the user's location changes.
         *
         * @param orientationManager the orientation manager that detected the change
         */
        void onLocationChanged(OrientationManager orientationManager);

        /**
         * Called when the accuracy of the compass changes.
         *
         * @param orientationManager the orientation manager that detected the change
         */
        void onAccuracyChanged(OrientationManager orientationManager);
    }


	/**
	 * Adds a listener that will be notified when the user's location or orientation changes.
	 */
	public void addOnChangedListener(OnChangedListener listener);

	/**
	 * Removes a listener from the list of those that will be notified when the user's location or
	 * orientation changes.
	 */
	public void removeOnChangedListener(OnChangedListener listener);

	/**
	 * Starts tracking the user's location and orientation. After calling this method, any
	 * {@link OnChangedListener}s added to this object will be notified of these events.
	 */
	public void start();

	/**
	 * Stops tracking the user's location and orientation. Listeners will no longer be notified of
	 * these events.
	 */
	public void stop();
	
	public boolean isRecording();
	
	public boolean isReplaying();

	/**
	 * Gets a value indicating whether there is too much magnetic field interference for the
	 * compass to be reliable.
	 *
	 * @return true if there is magnetic interference, otherwise false
	 */
	public boolean hasInterference();

	/**
	 * Gets a value indicating whether the orientation manager knows the user's current location.
	 *
	 * @return true if the user's location is known, otherwise false
	 */
	public boolean hasLocation();

	/**
	 * Gets the user's current heading, in degrees. The result is guaranteed to be between 0 and
	 * 360.
	 *
	 * @return the user's current heading, in degrees
	 */
	public float getHeading();

	/**
	 * Gets the user's current pitch (head tilt angle), in degrees. The result is guaranteed to be
	 * between -90 and 90.
	 *
	 * @return the user's current pitch angle, in degrees
	 */
	public float getPitch();

	/**
	 * Gets the user's current roll angle (head tilt angle), in degrees.
	 *
	 * @return the user's current roll angle, in degrees
	 */
	public float getRoll();

	/**
	 * Gets the user's current location.
	 *
	 * @return the user's current location
	 */
	public Location getLocation();

}