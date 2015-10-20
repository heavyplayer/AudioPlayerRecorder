package com.heavyplayer.audioplayerrecorder.sample.activity;

import com.heavyplayer.audioplayerrecorder.sample.R;
import com.heavyplayer.audioplayerrecorder.sample.obj.Item;
import com.heavyplayer.audioplayerrecorder.service.AudioPlayerService;
import com.heavyplayer.audioplayerrecorder.service.manager.AudioPlayerServiceManager;
import com.heavyplayer.audioplayerrecorder.service.manager.ServiceManager;
import com.heavyplayer.audioplayerrecorder.widget.AudioPlayerLayout;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.Toast;

import java.io.File;

public class PlayerActivity extends RecorderActivity {
    protected AudioPlayerServiceManager mAudioPlayerServiceManager;

    @Override
    protected ListAdapter onCreateAdapter(Context context, Item[] objects) {
        return new AudioPlayerItemAdapter(context, objects);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioPlayerServiceManager = new AudioPlayerServiceManager(this);
        mAudioPlayerServiceManager.setServiceStateListener(new ServiceManager.StateListener() {
            @Override
            public void onServiceBind(IBinder binder) {
                if (mListView != null) {
                    mListView.invalidateViews();
                }
            }

            @Override
            public void onServiceUnbind(IBinder binder) {
            }

            @Override
            public void onServiceStop() {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAudioPlayerServiceManager.onActivityResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAudioPlayerServiceManager.onActivityPause();
    }

    protected class AudioPlayerItemAdapter extends ItemAdapter {
        public AudioPlayerItemAdapter(Context context, Item[] objects) {
            super(context, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view = super.getView(position, convertView, parent);

            final AudioPlayerService.LocalBinder binder = mAudioPlayerServiceManager.getBinder();
            if (binder != null) {
                final AudioPlayerLayout audioPlayerLayout =
                        (AudioPlayerLayout) view.findViewById(R.id.item_audio_player);
                if (audioPlayerLayout != null) {
                    final Item item = getItem(position);
                    Uri uri = generateExternalStorageFileUri(item.getFileName());
                    if (uri != null) {
                        binder.register(
                                item.getId(),
                                generateExternalStorageFileUri(item.getFileName()),
                                true,
                                audioPlayerLayout);
                    } else {
                        Toast.makeText(getContext(), R.string.error_storage_not_available, Toast.LENGTH_LONG).show();
                    }
                }
            }

            return view;
        }
    }

    @Nullable
    protected Uri generateExternalStorageFileUri(String fileName) {
        final File filesDir = getExternalFilesDir(null);
        if (filesDir != null) {
            final File file = new File(filesDir.getAbsolutePath(), fileName);
            return Uri.fromFile(file);
        } else {
            return null;
        }
    }
}
