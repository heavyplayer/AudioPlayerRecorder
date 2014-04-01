package com.heavyplayer.audioplayerrecorder.sample.activity;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import com.heavyplayer.audioplayerrecorder.sample.R;
import com.heavyplayer.audioplayerrecorder.sample.obj.Item;
import com.heavyplayer.audioplayerrecorder.service.AudioPlayerServiceHandler;
import com.heavyplayer.audioplayerrecorder.widget.AudioPlayerLayout;

import java.io.File;

public class PlayerActivity extends RecorderActivity {
	protected AudioPlayerServiceHandler mAudioPlayerServiceHandler;

	@Override
	protected ListAdapter onCreateAdapter(Context context, Item[] objects) {
		return new AudioPlayerItemAdapter(context, objects);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAudioPlayerServiceHandler = new AudioPlayerServiceHandler(this);
		mAudioPlayerServiceHandler.setListView(mListView);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mAudioPlayerServiceHandler.onActivityResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mAudioPlayerServiceHandler.onActivityPause();
	}

	protected class AudioPlayerItemAdapter extends ItemAdapter {
		public AudioPlayerItemAdapter(Context context, Item[] objects) {
			super(context, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final View view = super.getView(position, convertView, parent);

			if(mAudioPlayerServiceHandler.getAudioPlayerBinder() != null) {
				final AudioPlayerLayout audioPlayerLayout =
						(AudioPlayerLayout)view.findViewById(R.id.item_audio_player);
				if(audioPlayerLayout != null) {
					final Item item = getItem(position);
					mAudioPlayerServiceHandler.getAudioPlayerBinder().register(
							item.getId(),
							generateExternalStorageFileUri(item.getFileName()),
							audioPlayerLayout);
				}
			}

			return view;
		}
	}

	protected Uri generateExternalStorageFileUri(String fileName) {
		final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), fileName);
		return Uri.fromFile(file);
	}
}
