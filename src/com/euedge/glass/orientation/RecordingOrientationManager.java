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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.location.Location;

/**
 * An orientation manager, that encloses another OrientationManager instance,
 * and records each update into a file. 
 */
public class RecordingOrientationManager implements OrientationManager {

    public static final String EXTENSION = ".om";
    
	private OrientationManager orientationManager;
	
	private File baseDir;
	
	private boolean autoRecord;
	
	private boolean isRecording;
	
	private BufferedWriter writer;
	
	private long startTime;
	
	private OnChangedListener changeListener = new OnChangedListener() {

        @Override
        public void onOrientationChanged(OrientationManager orientationManager) {
            writeOrientation(orientationManager);
        }

        @Override
        public void onLocationChanged(OrientationManager orientationManager) {
            writeLocation(orientationManager);
        }

        @Override
        public void onAccuracyChanged(OrientationManager orientationManager) {
            writeAccuracy(orientationManager);
        }
	
	};
	
	public RecordingOrientationManager(OrientationManager orientationManager,
			                           File baseDir) {
		this.orientationManager = orientationManager;
		this.baseDir = baseDir;
		
		isRecording = false;
		writer = null;
		autoRecord = true;
		
		
		orientationManager.addOnChangedListener(new OnChangedListener() {

            @Override
            public void onOrientationChanged(
                    OrientationManager orientationManager) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onLocationChanged(OrientationManager orientationManager) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onAccuracyChanged(OrientationManager orientationManager) {
                // TODO Auto-generated method stub
                
            }
		});

	}

	   public RecordingOrientationManager(OrientationManager orientationManager,
               File baseDir, boolean autoRecord) {
        this.orientationManager = orientationManager;
        this.baseDir = baseDir;
        this.autoRecord = autoRecord;
        
        isRecording = false;
        writer = null;
    }

	@Override
	public void addOnChangedListener(OnChangedListener listener) {
		orientationManager.addOnChangedListener(listener);
	}

	@Override
	public void removeOnChangedListener(OnChangedListener listener) {
		orientationManager.removeOnChangedListener(listener);
	}
	
	@Override
	public boolean isRecording() {
	    return isRecording;
	}

	@Override
	public void start() {
	    if (autoRecord) {
	        startRecording();
	    }
	    
        orientationManager.addOnChangedListener(changeListener);

		orientationManager.start();
		
		// trigger a location statement if we have a 'last known location' at start
		if (orientationManager.hasLocation()) {
		    writeLocation(orientationManager);
		}
	}
	
	public void startRecording() {
	    if (isRecording) {
	        return;
	    }
	    
        try {
            startTime = System.currentTimeMillis();
            
            writer = new BufferedWriter(new FileWriter(new File(baseDir,
                                        (startTime + EXTENSION))));
            isRecording = true;
        } catch (IOException e) {
        }
	}

	@Override
	public void stop() {
	    stopRecording();
	    
        orientationManager.removeOnChangedListener(changeListener);

		orientationManager.stop();
	}

	public void stopRecording() {
        if (isRecording && writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
            }
            writer = null;
        }
        isRecording = false;
	}
	
	@Override
	public boolean hasInterference() {
		return orientationManager.hasInterference();
	}

	@Override
	public boolean hasLocation() {
		return orientationManager.hasLocation();
	}

	@Override
	public float getHeading() {
		return orientationManager.getHeading();
	}

	@Override
	public float getPitch() {
		return orientationManager.getPitch();
	}

	@Override
	public float getRoll() {
		return orientationManager.getRoll();
	}

	@Override
	public Location getLocation() {
		return orientationManager.getLocation();
	}

    private void writeOrientation(OrientationManager orientationManager) {
        if (isRecording && writer != null) {
            long elapsed = System.currentTimeMillis() - startTime;
            
            try {
                writer.write(Long.toString(elapsed));
                writer.write(",O,");
                writer.write(Float.toString(orientationManager.getHeading()));
                writer.write(',');
                writer.write(Float.toString(orientationManager.getPitch()));
                writer.write(',');
                writer.write(Float.toString(orientationManager.getRoll()));
                writer.newLine();
            } catch (IOException e) {
            }
        }
    }

    private void writeLocation(OrientationManager orientationManager) {
        if (isRecording && writer != null) {
            long elapsed = System.currentTimeMillis() - startTime;
            Location l = orientationManager.getLocation();
            
            try {
                writer.write(Long.toString(elapsed));
                writer.write(",L,");
                writer.write(Double.toString(l.getLatitude()));
                writer.write(',');
                writer.write(Double.toString(l.getLongitude()));
                writer.write(',');
                writer.write(Double.toString(l.getAltitude()));
                writer.write(',');
                writer.write(Float.toString(l.getBearing()));
                writer.write(',');
                writer.write(Float.toString(l.getSpeed()));
                writer.write(',');
                writer.write(Float.toString(l.getAccuracy()));
                writer.newLine();
            } catch (IOException e) {
            }
        }
    }

    private void writeAccuracy(OrientationManager orientationManager) {
        if (isRecording && writer != null) {
            long elapsed = System.currentTimeMillis() - startTime;
            
            try {
                writer.write(Long.toString(elapsed));
                writer.write(",A,");
                writer.write(Boolean.toString(orientationManager.hasInterference()));
                writer.newLine();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public boolean isReplaying() {
        return false;
    }
}
