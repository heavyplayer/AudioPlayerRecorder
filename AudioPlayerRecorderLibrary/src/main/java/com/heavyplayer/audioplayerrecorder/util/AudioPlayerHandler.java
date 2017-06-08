package com.heavyplayer.audioplayerrecorder.util;

import com.heavyplayer.audioplayerrecorder.widget.AudioPlayerLayout;
import com.heavyplayer.audioplayerrecorder.widget.PlayPauseImageButton;
import com.heavyplayer.audioplayerrecorder.widget.interface_.OnDetachListener;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import java.io.IOException;

public class AudioPlayerHandler
        implements MediaPlayer.OnPreparedListener, SafeMediaPlayer.OnStartListener, MediaPlayer.OnCompletionListener,
                   MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener {
    public static final String LOG_TAG = AudioPlayerHandler.class.getSimpleName();

    private final static long PROGRESS_UPDATE_INTERVAL_MS = 200;

    private AudioManager mAudioManager;
    private AudioFocusChangeListener mAudioFocusChangeListener;

    private Uri mFileUri;

    private boolean mShowBufferIfPossible;

    private Handler mHandler;
    private ProgressUpdater mProgressUpdater;

    private SafeMediaPlayer mMediaPlayer;
    private Integer mBufferingCurrentPosition;

    private AudioPlayerLayout mView;
    private PlayPauseImageButton mButton;
    private SeekBar mSeekBar;

    public AudioPlayerHandler(Context context, Uri fileUri, boolean showBufferIfPossible, Handler handler) {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        mFileUri = fileUri;

        mShowBufferIfPossible = showBufferIfPossible;

        mHandler = handler;
        mProgressUpdater = new ProgressUpdater();

        create();
    }

    public void create() {
        mMediaPlayer = new SafeMediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnStartListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnErrorListener(this);

        mBufferingCurrentPosition = null;

        configureRegisteredViews();
    }

    public void destroy() {
        clearRegisteredViews();

        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.setOnPreparedListener(null);
                mMediaPlayer.setOnStartListener(null);
                mMediaPlayer.setOnCompletionListener(null);
                mMediaPlayer.setOnBufferingUpdateListener(null);
                mMediaPlayer.setOnErrorListener(null);
                mMediaPlayer.stop();
                mMediaPlayer.reset();
                mMediaPlayer.release();
                mMediaPlayer = null;
            } catch (Exception e) {
                Log.w(LOG_TAG, e);
            }
        }

        mBufferingCurrentPosition = null;

        abandonAudioFocus();
    }

    public void setFileUri(Uri fileUri) {
        mFileUri = fileUri;
    }

    protected void start(boolean gainAudioFocus, boolean updateButton) {
        if (gainAudioFocus) {
            gainAudioFocus();
        }

        if (!mMediaPlayer.isPreparing() && !mMediaPlayer.isPrepared()) {
            try {
                mMediaPlayer.setDataSource(mFileUri.toString());
                mMediaPlayer.prepare();
            } catch (IOException e) {
                Log.w(LOG_TAG, e);
            }
        }

        mMediaPlayer.start();

        updatePlayingState(true, updateButton);
    }

    protected void pause(boolean abandonAudioFocus, boolean updateButton) {
        mMediaPlayer.pause();

        updatePlayingState(false, updateButton);

        if (abandonAudioFocus) {
            abandonAudioFocus();
        }
    }

    protected void seekTo(int msec) {
        mMediaPlayer.seekTo(msec);
    }

    protected void updatePlayingState(boolean isPlaying, boolean updateButton) {
        if (mView != null) {
            mView.setIsPlaying(isPlaying);
        }

        if (mButton != null && updateButton) {
            mButton.setIsPlaying(isPlaying);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mView != null) {
            mView.setTimeDuration(mp.getDuration());
        }

        if (mSeekBar != null) {
            if (mSeekBar.getMax() != mp.getDuration()) {
                mSeekBar.setMax(mp.getDuration());
                mSeekBar.setProgress(mp.getCurrentPosition());
            } else if (mSeekBar.getProgress() != mp.getCurrentPosition()) {
                mSeekBar.setProgress(mp.getCurrentPosition());
            }
        }
    }

    @Override
    public void onStart(MediaPlayer mp) {
        // Update seek bar.
        startSeekBarUpdate();
    }

    public void startSeekBarUpdate() {
        // Update seek bar.
        mHandler.removeCallbacks(mProgressUpdater);
        mHandler.post(mProgressUpdater);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // Updates seek bar.
        if (mSeekBar != null) {
            mSeekBar.setProgress(mp.getCurrentPosition());
        }

        updatePlayingState(false, true);

        abandonAudioFocus();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (mShowBufferIfPossible) {
            mBufferingCurrentPosition = (int) (mp.getDuration() * (percent / 100f));

            if (mSeekBar != null) {
                mSeekBar.setSecondaryProgress(mBufferingCurrentPosition);
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            // Recreate media player.
            destroy();
            create();
        } else {
            abandonAudioFocus();
        }

        return false;
    }

    public void registerView(AudioPlayerLayout view) {
        mView = view;
        mView.setOnDetachListener(new OnDetachListener() {
            @Override
            public void onStartTemporaryDetach(View v) {
                clearRegisteredViews();
            }

            @Override
            public void onDetachedFromWindow(View v) {
                clearRegisteredViews();
            }
        });

        mButton = view.getButton();
        mSeekBar = view.getSeekBar();

        configureRegisteredViews();

        // Resume updater.
        startSeekBarUpdate();
    }

    protected void configureRegisteredViews() {
        configureView();
        configureButton();
        configureSeekBar();
    }

    protected void configureView() {
        if (mView != null && mMediaPlayer != null) {
            // Resume duration.
            // Don't worry about current position as it will
            // always be correlated with the seek bar position.
            mView.setTimeDuration(mMediaPlayer.getDuration());

            // Resume playing state.
            mView.setIsPlaying(mMediaPlayer.isGoingToPlay());
        }
    }

    protected void configureButton() {
        if (mButton != null && mMediaPlayer != null) {
            mButton.setOnPlayPauseListener(new PlayPauseImageButton.OnPlayPauseListener() {
                @Override
                public void onPlay(View v) {
                    start(true, false);
                }

                @Override
                public void onPause(View v) {
                    pause(true, false);
                }
            });

            // Resume playing state.
            mButton.setIsPlaying(mMediaPlayer.isGoingToPlay());
        }
    }

    protected void configureSeekBar() {
        if (mSeekBar != null && mMediaPlayer != null) {
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mHandler.removeCallbacks(mProgressUpdater);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    seekTo(seekBar.getProgress());
                    mHandler.post(mProgressUpdater);
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mView.setTimeCurrentPosition(progress);
                }
            });

            // Resume progress.
            mSeekBar.setMax(mMediaPlayer.getDuration());
            mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());
            mSeekBar.setSecondaryProgress(mBufferingCurrentPosition != null ? mBufferingCurrentPosition : 0);
        }
    }

    protected void clearRegisteredViews() {
        if (mView != null) {
            mView.setOnDetachListener(null);
            mView = null;
        }

        if (mButton != null) {
            mButton.setOnPlayPauseListener(null);
            mButton = null;
        }

        if (mSeekBar != null) {
            mSeekBar.setOnSeekBarChangeListener(null);
            mSeekBar = null;
        }
    }

    protected void gainAudioFocus() {
        if (mAudioFocusChangeListener == null) {
            mAudioFocusChangeListener = new AudioFocusChangeListener();
        }

        // Request audio focus for playback
        mAudioManager.requestAudioFocus(
                mAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    }

    protected void abandonAudioFocus() {
        // Abandon audio focus when playback complete.
        if (mAudioFocusChangeListener != null) {
            mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
        }
    }

    private class AudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    pause(true, true);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    pause(false, true);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    start(false, true);
                    break;
            }
        }
    }

    protected class ProgressUpdater implements Runnable {
        @Override
        public void run() {
            if (mSeekBar != null && mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());
                mHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS);
            }
        }
    }
}
