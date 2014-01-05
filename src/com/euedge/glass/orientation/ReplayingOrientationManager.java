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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import android.location.Location;

public class ReplayingOrientationManager implements OrientationManager {
    
    private BufferedReader reader;
    
    private Set<OnChangedListener> listeners;
    
    private Location location;
    
    private float heading;
    
    private float pitch;
    
    private float roll;
    
    private boolean hasInterference;
    
    private long nextOffset;
    
    private char nextType;

    private Location nextLocation;
    
    private float nextHeading;
    
    private float nextPitch;
    
    private float nextRoll;
    
    private boolean nextHasInterference;

    boolean shouldRun;
    
    Set<ReplayListener> replayListeners;
    
    public interface ReplayListener {
        public void onReplayFinsihed();
    }
    
    private Thread readerThread = new Thread() {
        long startTime;
        
        long elapsedTime;
        
        String line;
        
        @Override
        public void run() {
            startTime = System.currentTimeMillis();
            elapsedTime = 0;
            
            while (shouldRun) {
                try {
                    line = reader.readLine();
                    if (line == null || line.isEmpty()) {
                        shouldRun = false;
                        break;
                    }
                    
                    readNextLine(line);
                    
                    while (elapsedTime < nextOffset) {
                        try {
                            Thread.sleep(nextOffset - elapsedTime);
                        } catch (InterruptedException e) {
                            if (!shouldRun) {
                                reader.close();
                                notifyStop();
                                return;
                            }
                        }
                        elapsedTime = System.currentTimeMillis() - startTime;
                    }
                    
                    updateNextDataAndNotifyListeners();

                } catch (IOException e) {
                    shouldRun = false;
                }
            }
            
            try {
                reader.close();
            } catch (IOException e) {
            }
            notifyStop();
        }
    };

    public ReplayingOrientationManager()  {
        listeners = new LinkedHashSet<OnChangedListener>();
        replayListeners = new LinkedHashSet<ReplayListener>();
    }
    
    public ReplayingOrientationManager(File file) throws FileNotFoundException {
        reader = new BufferedReader(new FileReader(file));
        listeners = new LinkedHashSet<OnChangedListener>();
        replayListeners = new LinkedHashSet<ReplayListener>();
    }
    
    public void setFile(File file) throws FileNotFoundException {
        reader = new BufferedReader(new FileReader(file));
    }
    
    @Override
    public void addOnChangedListener(OnChangedListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeOnChangedListener(OnChangedListener listener) {
        listeners.remove(listener);
    }

    public void addReplayListener(ReplayListener listener) {
        replayListeners.add(listener);
    }

    public void removeReplayListener(ReplayListener listener) {
        replayListeners.remove(listener);
    }

    @Override
    public void start() {
        if (!isReplaying()) {
            shouldRun = true;
            readerThread.start();
        }
    }

    @Override
    public void stop() {
        shouldRun = false;
        readerThread.interrupt();
    }
    
    @Override
    public boolean isReplaying() {
        return shouldRun && readerThread.isAlive();
    }

    @Override
    public boolean hasInterference() {
        return hasInterference;
    }

    @Override
    public boolean hasLocation() {
        return location != null;
    }

    @Override
    public float getHeading() {
        return heading;
    }

    @Override
    public float getPitch() {
        return pitch;
    }

    @Override
    public float getRoll() {
        return roll;
    }

    @Override
    public Location getLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Notifies all listeners that the user's orientation has changed.
     */
    private void notifyOrientationChanged() {
        for (OnChangedListener listener : listeners) {
            listener.onOrientationChanged(this);
        }
    }

    /**
     * Notifies all listeners that the user's location has changed.
     */
    private void notifyLocationChanged() {
        for (OnChangedListener listener : listeners) {
            listener.onLocationChanged(this);
        }
    }

    /**
     * Notifies all listeners that the compass's accuracy has changed.
     */
    private void notifyAccuracyChanged() {
        for (OnChangedListener listener : listeners) {
            listener.onAccuracyChanged(this);
        }
    }

    private void notifyStop() {
        for (ReplayListener listener : replayListeners) {
            listener.onReplayFinsihed();
        }
    }

    private void readNextLine(String line) {
        StringTokenizer tok = new StringTokenizer(line, ",");
        nextOffset = Long.parseLong(tok.nextToken());
        nextType = tok.nextToken().charAt(0);

        switch (nextType) {
        case 'O':
            nextHeading = Float.parseFloat(tok.nextToken());
            nextPitch   = Float.parseFloat(tok.nextToken());
            nextRoll    = Float.parseFloat(tok.nextToken());
            break;
        case 'A':
            nextHasInterference = Boolean.parseBoolean(tok.nextToken());
            break;
        case 'L':
            nextLocation = new Location("ReplayingOrientationManager");
            nextLocation.setLatitude(Double.parseDouble(tok.nextToken()));
            nextLocation.setLongitude(Double.parseDouble(tok.nextToken()));
            nextLocation.setAltitude(Double.parseDouble(tok.nextToken()));
            nextLocation.setBearing(Float.parseFloat(tok.nextToken()));
            nextLocation.setSpeed(Float.parseFloat(tok.nextToken()));
            nextLocation.setAccuracy(Float.parseFloat(tok.nextToken()));
            break;
        default:
        }
    }
    
    private void updateNextDataAndNotifyListeners() {
        switch (nextType) {
        case 'O':
            heading = nextHeading;
            pitch   = nextPitch;
            roll    = nextRoll;
            notifyOrientationChanged();
            break;
        case 'A':
            hasInterference = nextHasInterference;
            notifyAccuracyChanged();
            break;
        case 'L':
            location = nextLocation;
            notifyLocationChanged();
            break;
        default:
        }
    }

    @Override
    public boolean isRecording() {
        return false;
    }
}

