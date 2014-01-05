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
package com.euedge.glass.orientationrecorder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;

/**
 * This activity manages the options menu that appears when the user taps on the
 * speed hud's live card.
 */
public class OrientationRecorderMenuActivity extends Activity {

    private OrientationRecorderService.OrientationRecorderBinder mOrientationRecorderService;
    private boolean mResumed;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof OrientationRecorderService.OrientationRecorderBinder) {
                mOrientationRecorderService = (OrientationRecorderService.OrientationRecorderBinder) service;
                openOptionsMenu();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResumed = true;
        bindService(new Intent(this, OrientationRecorderService.class), mConnection, 0);
        openOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
    }

    @Override
    public void openOptionsMenu() {
        if (mResumed && mOrientationRecorderService != null) {
            super.openOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.orientation_recorder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        OrientationRecorderService ors =
                mOrientationRecorderService.getOrientationRecorderService();
        
        switch (item.getItemId()) {
        case R.id.record:
            ors.startRecording();
            invalidateOptionsMenu();
            return true;
        case R.id.replay:
            displayReplayList();
            invalidateOptionsMenu();
            return true;
        case R.id.stop:
            if (ors.isRecording()) {
                ors.stopRecording();
            }
            if (ors.isReplaying()) {
                ors.stopReplaying();
            }
            invalidateOptionsMenu();
            return true;
        case R.id.exit:
            stopService(new Intent(this, OrientationRecorderService.class));
            return true;
        case R.id.uom_kmh:
            ors.setUom(OrientationRecorderView.UOM_KMH);
            return true;
        case R.id.uom_mph:
            ors.setUom(OrientationRecorderView.UOM_MPH);
            return true;
        case R.id.uom_kt:
            ors.setUom(OrientationRecorderView.UOM_KT);
            return true;
        case R.id.uom_mps:
            ors.setUom(OrientationRecorderView.UOM_MPS);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        OrientationRecorderService ors =
                mOrientationRecorderService.getOrientationRecorderService();
        
         menu.findItem(R.id.record).setEnabled(!(ors.isRecording() || ors.isReplaying()));
         menu.findItem(R.id.stop).setEnabled(ors.isRecording() || ors.isReplaying());
         menu.findItem(R.id.replay).setEnabled(!(ors.isRecording() || ors.isReplaying()));
         
         return true;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);

        unbindService(mConnection);

        // We must call finish() from this method to ensure that the activity ends either when an
        // item is selected from the menu or when the menu is dismissed by swiping down.
        finish();
    }
    
    private void displayReplayList() {
        Intent intent = new Intent(this, RecordedFileListActivity.class);
        startActivity(intent);
    }
}
