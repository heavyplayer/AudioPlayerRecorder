package com.heavyplayer.audioplayerrecorder.util;

import android.media.MediaPlayer;

public class SafeMediaPlayer extends MediaPlayer
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
                   MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener {
    private final static int CURRENT_POSITION_MIN_PROGRESS = 128;

    private OnPreparedListener mOnPreparedListener;
    private OnStartListener mOnStartListener;
    private OnCompletionListener mOnCompletionListener;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private OnErrorListener mOnErrorListener;

    private State mState;
    private boolean mIsGoingToPlay;

    private Integer mFixedCurrentPosition;
    private CurrentPositionManager mCurrentPositionManager;
    private Integer mDuration;

    private enum State {
        CREATED, PREPARING, PREPARED, STARTED
    }

    public SafeMediaPlayer() {
        mState = State.CREATED;
        mIsGoingToPlay = false;

        mFixedCurrentPosition = 0;
        mCurrentPositionManager = new CurrentPositionManager();
        mDuration = 100;

        super.setOnPreparedListener(this);
        super.setOnCompletionListener(this);
        super.setOnBufferingUpdateListener(this);
        super.setOnErrorListener(this);
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    public void setOnStartListener(OnStartListener listener) {
        mOnStartListener = listener;
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    @Override
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public boolean isGoingToPlay() {
        return mIsGoingToPlay;
    }

    public boolean isPrepared() {
        return mState == State.PREPARED || mState == State.STARTED;
    }

    protected boolean isPreparing() {
        return mState == State.PREPARING;
    }

    @Override
    public void prepare() throws IllegalStateException {
        prepareAsync();
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        mIsGoingToPlay = false;

        super.prepareAsync();
        mState = State.PREPARING;
    }

    @Override
    public void start() throws IllegalStateException {
        mIsGoingToPlay = true;

        if (isPrepared()) {
            final boolean isStarting = !isPlaying();
            super.start();
            mState = State.STARTED;
            mFixedCurrentPosition = null;
            mCurrentPositionManager.clear();

            if (isStarting && mOnStartListener != null) {
                mOnStartListener.onStart(this);
            }
        }
    }

    @Override
    public void pause() throws IllegalStateException {
        mIsGoingToPlay = false;

        if (mState == State.STARTED) {
            super.pause();
        }
    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {
        if (isPrepared()) {
            super.seekTo(msec);
            mFixedCurrentPosition = null;
            mCurrentPositionManager.set(ensureValidPosition(msec));
        } else {
            mFixedCurrentPosition = ensureValidPosition(msec);
            mCurrentPositionManager.clear();
        }
    }

    private int ensureValidPosition(int msec) {
        return msec < 0 ? 0 : (msec > mDuration ? mDuration : msec);
    }

    @Override
    public void stop() throws IllegalStateException {
        mIsGoingToPlay = false;

        if (isPrepared()) {
            super.stop();
            mState = State.PREPARED;
        }
    }

    @Override
    public void reset() {
        super.reset();
        mIsGoingToPlay = false;
        mFixedCurrentPosition = 0;
        mCurrentPositionManager.clear();
        mDuration = 100;
        mState = State.CREATED;

        if (mOnBufferingUpdateListener != null) {
            mOnBufferingUpdateListener.onBufferingUpdate(this, 0);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mState = State.PREPARED;

        adjustCurrentPositionAndDuration();

        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared(mp);
        }

        if (mIsGoingToPlay) {
            start();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (isPrepared()) {
            // onCompletion may be called even after there was an error.
            // We check if the player is prepared, because we only want
            // to update the player state if the playback was successful.
            // Otherwise, we should not interfere and let the error handling
            // update these values.

            mIsGoingToPlay = false;

            mFixedCurrentPosition = getDuration();
            mCurrentPositionManager.clear();
        }

        if (mOnCompletionListener != null) {
            mOnCompletionListener.onCompletion(mp);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (mOnBufferingUpdateListener != null) {
            mOnBufferingUpdateListener.onBufferingUpdate(mp, percent);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // After an error, we'll need to prepare the media player again.
        reset();

        return mOnErrorListener != null && mOnErrorListener.onError(mp, what, extra);
    }

    @Override
    public int getCurrentPosition() {
        if (mFixedCurrentPosition != null) {
            return mFixedCurrentPosition;
        } else {
            return mCurrentPositionManager != null ? mCurrentPositionManager.get() : super.getCurrentPosition();
        }
    }

    @Override
    public int getDuration() {
        return mDuration != null ? mDuration : super.getDuration();
    }

    private void adjustCurrentPositionAndDuration() {
        final float percent = getCurrentPosition() / (float) getDuration();
        final int duration = super.getDuration();
        if (mDuration != duration) {
            mDuration = duration;
            seekTo((int) (mDuration * percent));
        }
    }

    public interface OnStartListener {
        void onStart(MediaPlayer mp);
    }

    /**
     * Tries to improve MediaPlayer behavior with issue https://code.google.com/p/android/issues/detail?id=38627
     *
     * With some codecs, the MediaPlayer has a bug where getCurrentPosition()
     * goes back in the progress near the end of the playback.
     *
     * We must distinguish between this issue and the times where the MediaPlayer
     * goes back in the playback to fix time sync.
     */
    private class CurrentPositionManager {
        private Integer mLastCurrentPosition;
        private Integer mStepBackPosition;

        public void clear() {
            mLastCurrentPosition = null;
            mStepBackPosition = null;
        }

        public void set(int msec) {
            mLastCurrentPosition = msec;
            mStepBackPosition = null;
        }

        public int get() {
            final int currentPosition = SafeMediaPlayer.super.getCurrentPosition();
            final int result;
            if ((mLastCurrentPosition == null || mLastCurrentPosition <= currentPosition) ||
                    (mStepBackPosition != null && mStepBackPosition < currentPosition)) {
                result = currentPosition;
                mLastCurrentPosition = currentPosition;
                mStepBackPosition = null;
            } else {
                result = Math.min(mLastCurrentPosition + CURRENT_POSITION_MIN_PROGRESS, getDuration());
                mStepBackPosition = currentPosition;
            }

            return result;
        }
    }
}
