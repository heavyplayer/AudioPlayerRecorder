package com.heavyplayer.audioplayerrecorder.fragment;

import com.heavyplayer.audioplayerrecorder.R;
import com.heavyplayer.audioplayerrecorder.service.AudioRecorderService;
import com.heavyplayer.audioplayerrecorder.service.manager.AudioRecorderServiceManager;
import com.heavyplayer.audioplayerrecorder.service.manager.ServiceManager;
import com.heavyplayer.audioplayerrecorder.widget.AudioRecorderMicrophone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

public class AudioRecorderFragment extends DialogFragment
        implements View.OnClickListener, ServiceManager.StateListener, AudioRecorderService.AudioRecorderStateListener {
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
        if (mFileUri == null) {
            mFileUri = getArguments().getParcelable(ARG_FILE_URI);
        }
        return mFileUri;
    }

    @Override
    final public void onClick(View v) {
        final AudioRecorderService.LocalBinder binder = mAudioRecorderServiceManager.getBinder();
        final boolean isRecording = binder != null && binder.isRecording();
        onMicrophoneClick(v, isRecording);
    }

    protected void onMicrophoneClick(View v, boolean isRecording) {
        final AudioRecorderService.LocalBinder binder = mAudioRecorderServiceManager.getBinder();
        if (binder != null) {
            if (binder.isRecording()) {
                binder.stopRecorder();
            } else {
                binder.startRecorder(getFileUri());
            }
        }
    }

    @Override
    public void onServiceBind(IBinder binder) {
        registerOnAudioRecorderService((AudioRecorderService.LocalBinder) binder);

        if (mStartRecorderOnBind) {
            mStartRecorderOnBind = false;
            ((AudioRecorderService.LocalBinder) binder).startRecorder(getFileUri());
        }
    }

    @Override
    public void onServiceUnbind(IBinder binder) {
    }

    protected void registerOnAudioRecorderService(AudioRecorderService.LocalBinder binder) {
        final AudioRecorderMicrophone microphone =
                (AudioRecorderMicrophone) getDialog().findViewById(android.R.id.input);
        if (microphone != null) {
            microphone.setOnClickListener(this);
        }

        binder.register(microphone, this);
    }

    @Override
    public void onServiceStop() {
        // Purposely empty.
    }

    @Override
    public void onStartRecorder() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void onStartRecorderFailed(Exception e) {
        // Purposely empty.
    }

    @Override
    public void onStopRecorder() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void onTimeLimitExceeded() {
        // Purposely empty.
    }
}
