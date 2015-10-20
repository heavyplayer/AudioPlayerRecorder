package com.heavyplayer.audioplayerrecorder.sample.activity;

import com.heavyplayer.audioplayerrecorder.fragment.AudioRecorderFragment;
import com.heavyplayer.audioplayerrecorder.sample.R;
import com.heavyplayer.audioplayerrecorder.sample.obj.Item;
import com.heavyplayer.audioplayerrecorder.utils.AudioUtils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class RecorderActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private final static String PERMISSION = Manifest.permission.RECORD_AUDIO;
    private final static int REQUEST_CODE_PERMISSION = 0;

    private static Item[] sItems = {
            new Item(0),
            new Item(1),
            new Item(2),
            new Item(3),
            new Item(4),
            new Item(5),
            new Item(6),
            new Item(7),
            new Item(8),
            new Item(9),
            new Item(11),
            new Item(12),
            new Item(13),
            new Item(14),
    };

    protected TextView mTitleView;
    protected ListView mListView;

    // Tracks if permission was just granted,
    // workaround for https://code.google.com/p/android-developer-preview/issues/detail?id=2823.
    private boolean mStartRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitleView = (TextView) findViewById(android.R.id.title);

        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setAdapter(onCreateAdapter(this, sItems));
        mListView.setOnItemClickListener(this);
        // Select first item.
        mListView.performItemClick(mListView.getChildAt(0), 0, mListView.getItemIdAtPosition(0));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mStartRecord) {
            mStartRecord = false;
            onRecord(null);
        }
    }

    protected ListAdapter onCreateAdapter(Context context, Item[] objects) {
        return new ItemAdapter(context, objects);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final String fileName = ((Item) parent.getItemAtPosition(position)).getFileName();
        mTitleView.setText(fileName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onRecord(View v) {
        if (!AudioUtils.isMicrophoneAvailable(this)) {
            Toast.makeText(this, R.string.error_microphone_not_available, Toast.LENGTH_LONG).show();
            return;
        }

        if (hasPermission()) {
            final String fileName = getSelectedItem().getFileName();
            if (fileName == null) {
                Toast.makeText(this, R.string.error_filename_invalid, Toast.LENGTH_LONG).show();
                return;
            }

            Uri uri = generateExternalStorageFileUri(fileName);
            if (uri != null) {
                AudioRecorderFragment.newInstance(uri)
                                     .show(getSupportFragmentManager(), AudioRecorderFragment.TAG);
            } else {
                Toast.makeText(this, R.string.error_storage_not_available, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Nullable
    protected Uri generateExternalStorageFileUri(String fileName) {
        File filesDir = getExternalFilesDir(null);
        if (filesDir != null) {
            final File file = new File(filesDir.getAbsolutePath(), fileName);
            return Uri.fromFile(file);
        } else {
            return null;
        }
    }

    protected Item getSelectedItem() {
        return (Item) mListView.getItemAtPosition(mListView.getCheckedItemPosition());
    }

    private boolean hasPermission() {
        if (ContextCompat.checkSelfPermission(this, PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION)) {
                Snackbar.make(mListView, R.string.permission_rationale, Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.permission_action_allow), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                requestPermission();
                            }
                        }).show();
            } else {
                requestPermission();
            }
            return false;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{PERMISSION}, REQUEST_CODE_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mStartRecord = true;
                    // Just call {@link #onRecord(View)} when bug fux goes live.
                } else {
                    Snackbar.make(mListView, R.string.permission_rationale, Snackbar.LENGTH_LONG)
                            .setAction(
                                    getString(R.string.permission_action_settings), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent intent = new Intent();
                                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            intent.setData(
                                                    Uri.parse("package:" + getApplicationContext().getPackageName()));
                                            if (intent.resolveActivity(getPackageManager()) != null) {
                                                startActivity(intent);
                                            }
                                        }
                                    }).show();
                }
                break;
        }
    }

    protected class ItemAdapter extends ArrayAdapter<Item> {
        public ItemAdapter(Context context, Item[] objects) {
            super(context, R.layout.audio_player_list_item, R.id.item_title, objects);
        }
    }
}
