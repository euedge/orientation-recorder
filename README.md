Orientation Recorder
====================

This is a developer library & utility app, to enable recording & replay
of orientation and location sensor data on Google Glass.

This framework is useful for recording Google Glass orientation & location,
so as to replay it later for development purposes. Real-world orientation
sensing and replaying are done via the very same notification callback
interface, thus it is transparent for the class that uses this feature.

The following data is recorded:

 * heading
 * pitch
 * roll
 * latitude
 * longitude
 * altitude
 * bearing
 * speed


Screenshots
===========

![Record](https://raw.github.com/euedge/orientation-recorder/master/visuals/device-2014-01-05-171258.png)

![Playback](https://raw.github.com/euedge/orientation-recorder/master/visuals/device-2014-01-05-171312.png)


To use as an app
================

Start the app by saying: OK, Glass. Rec orientation!

Then tap on the app, and select the 'Record' menu. A blinking red dot will
indicate that the app is recording. You can change to other apps on your Glass,
the recording will continue.

To stop recording, select 'Stop' from the menu.

To replay recording, select 'Replay' from the menu, and then select the
recording you'd like to replay. Recordings are names after the number of
milliseconds after epoch, and are sorted in a 'most recent on top' order.

Data files will be recorded in:
```
/mnt/sdcard/Android/data/com.euedge.glass.orientationrecorder/files/orientations
```



To use in your own code
=======================

Look at the OrientationManager interface.

To use a generic sensor orientation
-----------------------------------

```
SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

OrientationManager orientationManager = new SensorsOrientationManager(sensorManager, locationManager);

orientationManager.addOnChangedListener(new OnChangedListener() {

    @Override
    public void onOrientationChanged(OrientationManager orientationManager) {
        // TODO: use new heading, pitch and roll values
    }

    @Override
    public void onLocationChanged(OrientationManager orientationManager) {
        // TODO: use new location
    }

    @Override
    public void onAccuracyChanged(OrientationManager orientationManager) {
        // TODO: act on interference
    }
});


// stop, typically in onResume()
orientationManager.start();

...


// stop, typically in onPause()
orientationManager.stop();

```

the registered OnChangedListener will receive orientation & location updates, on which your application can act.


To record orientation & location data
-------------------------------------

```
File omDir = new File(getApplicationContext().getExternalFilesDir(null), "orientations");
if (!omDir.exists()) {
    omDir.mkdirs();
}
RecordingOrientationManager recordingOrientationManager = new RecordingOrientationManager(orientationManager, omDir, false);
// false means: don't start recording automatically on a call to start()

...

// stop, typically in onResume()
orientationManager.start();

...

// start recording, a new save file will be crated in omDir
recordingOrientationManager.startRecording();

...

// stop recording
recordingOrientationManager.stopRecording();

...

// stop, typically in onPause()
orientationManager.stop();

```

on each startRecording() / stopRecording() pair, a new file will be created in omDir, named after the milliseconds passed since epoch, and will contain all the orientation & location updates that were sent by the Glass' sensors. This data can be used to re-play it for development & other purposes - see below.


To replay recorded orientation
------------------------------

To replay a recorded orientation / location session, as if it was
'happening right now', do the following:

```
ReplayingOrientationManager replayingOrientationManager = new ReplayingOrientationManager();

replayingOrientationManager.addReplayListener(new ReplayListener() {
    @Override
    public void onReplayFinsihed() {
        // TODO: handle replay finished
    }
});
// add the same orientation change listener as above
orientationManager.addOnChangedListener(...);

replayingOrientationManager.setFile(new File(omDir, fileName));
replayingOrientationManager.start();
```

and then use the replayingOrientationManager as a 'real' orientation manager in your app.
