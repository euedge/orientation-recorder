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
package com.euedge.glass.orientationrecorder;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import com.euedge.glass.orientation.RecordingOrientationManager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class RecordedFileListActivity extends Activity implements OnItemClickListener, OnItemSelectedListener {

    private String selectedFilename;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recorded_file_list);

        ArrayAdapter<String> items =
                new ArrayAdapter<String>(this,
                                         R.layout.recorded_file_list_item,
                                         R.id.file_list_label);
        
        File omDir = new File(getApplicationContext().getExternalFilesDir(null),
                              OrientationRecorderService.ORIENTATIONS_DIR);
        if (omDir.exists() && omDir.isDirectory()) {
            File files[] = omDir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isFile() && file.getName().endsWith(
                            RecordingOrientationManager.EXTENSION);
                }
            });
            
            String fileNames[] = new String[files.length];
            for (int i = 0; i < files.length; ++i) {
                fileNames[i] = files[i].getName();
            }
            
            Arrays.sort(fileNames);
            
            for (int i = fileNames.length; i > 0; ) {
                items.add(fileNames[--i]);
            }
        }
        
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(items);
        
        listView.setOnItemClickListener(this);
        listView.setOnItemSelectedListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onItemClick(AdapterView<?> adapter, View view, int ix, long l) {
        selectedFilename = (String) adapter.getItemAtPosition(ix);
        bindService(new Intent(this, OrientationRecorderService.class), mConnection, 0);
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof OrientationRecorderService.OrientationRecorderBinder) {
                OrientationRecorderService.OrientationRecorderBinder binder
                            = (OrientationRecorderService.OrientationRecorderBinder) service;
                OrientationRecorderService ors =
                                binder.getOrientationRecorderService();

                ors.startReplaying(selectedFilename);

                unbindService(mConnection);
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Do nothing.
        }
    };

    @Override
    public void onItemSelected(AdapterView<?> adapter, View view, int ix, long l) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapter) {
    }

}
