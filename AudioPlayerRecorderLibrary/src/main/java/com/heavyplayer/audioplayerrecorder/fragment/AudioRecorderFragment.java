package com.heavyplayer.audioplayerrecorder.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import com.heavyplayer.audioplayerrecorder.R;
import com.heavyplayer.audioplayerrecorder.service.AudioRecorderService;
import com.heavyplayer.audioplayerrecorder.service.manager.AudioRecorderServiceManager;
import com.heavyplayer.audioplayerrecorder.service.manager.ServiceManager;
import com.heavyplayer.audioplayerrecorder.widget.AudioRecorderMicrophone;

public class AudioRecorderFragment extends DialogFragment
		implements View.OnClickListener, ServiceManager.StateListener {
	public static final String TAG = AudioRecorderFragment.class.getSimpleName();

	private static final String ARG_FILE_URI = "arg_file_uri";

	private AudioRecorderServiceManager mAudioRecorderServiceManager;

	private Uri mFileUri;

	private boolean mStartRecorderOnBind;

	public static AudioRecorderFragment newInstance(Uri fileUri) {
		return newInstance(new AudioRecorderFragment(), fileUri);
	}
	protected static <T extends AudioRecorderFragment> T newInstance(T fragment, Uri fileUri) {
		final Bundle args = new Bundle();
		args.putParcelable(ARG_FILE_URI, fileUri);

		fragment.setArguments(args);

		return fragment;
	}

	public static AudioRecorderFragment createInstance(Uri fileUri) {
		final Bundle args = new Bundle();
		args.putParcelable(ARG_FILE_URI, fileUri);

		final AudioRecorderFragment fragment = new AudioRecorderFragment();
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		mAudioRecorderServiceManager = new AudioRecorderServiceManager(activity);
		mAudioRecorderServiceManager.setServiceStateListener(this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mStartRecorderOnBind = savedInstanceState == null;
	}

	@Override
	public void onResume() {
		super.onResume();
		mAudioRecorderServiceManager.onFragmentResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mAudioRecorderServiceManager.onFragmentPause();
	}

	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Activity activity = getActivity();

		final View view = LayoutInflater.from(activity).inflate(R.layout.audio_recorder, null);

		return new AlertDialog.Builder(activity)
				.setTitle(R.string.audio_recorder_recording)
				.setView(view)
				.setPositiveButton(R.string.audio_recorder_stop_recording, null)
				.create();
	}

	public Uri getFileUri() {
		if(mFileUri == null)
			mFileUri = getArguments().getParcelable(ARG_FILE_URI);
		return mFileUri;
	}

	@Override
	public void onClick(View v) {
		final AudioRecorderService.LocalBinder binder = mAudioRecorderServiceManager.getBinder();
		if(binder != null) {
			if(binder.isRecording())
				binder.stopRecorder();
			else
				binder.startRecorder(getFileUri());
		}
	}

	@Override
	final public void onServiceBind(IBinder binder) {
		registerMicrophone();

		if(mStartRecorderOnBind) {
			mStartRecorderOnBind = false;
			((AudioRecorderService.LocalBinder)binder).startRecorder(getFileUri());
		}
	}

	@Override
	final public void onServiceUnbind(IBinder binder) { }

	protected void registerMicrophone() {
		final AudioRecorderService.LocalBinder binder = mAudioRecorderServiceManager.getBinder();
		if(binder != null) {
			final AudioRecorderMicrophone microphone =
					(AudioRecorderMicrophone)getDialog().findViewById(android.R.id.input);
			if(microphone != null) {
				binder.registerAudioRecorderMicrophone(microphone);
				microphone.setOnClickListener(this);
			}
		}
	}

	@Override
	final public void onServiceStop() {
		onStopRecording();
	}

	protected void onStopRecording() {
		// Purposely empty. Sub-classes may use this.
	}
}
