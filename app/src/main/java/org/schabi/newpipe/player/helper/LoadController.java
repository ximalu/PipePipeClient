package org.schabi.newpipe.player.helper;

import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.LoadControl;

/**
 * Wrapper around DefaultLoadControl that adds preloading control.
 * Call {@link #getLoadControl()} to get the actual LoadControl for ExoPlayer.
 */
public class LoadController {

    public static final String TAG = "LoadController";
    private final DefaultLoadControl delegate;
    private boolean preloadingEnabled = true;

    public LoadController() {
        this(50000, 100000);
    }

    public LoadController(final int minBufferMs, final int maxBufferMs) {
        delegate = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        minBufferMs,
                        maxBufferMs,
                        2500,  // DEFAULT_BUFFER_FOR_PLAYBACK_MS
                        5000)  // DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();
    }

    /** Returns the actual LoadControl to pass to ExoPlayer. */
    public LoadControl getLoadControl() {
        return delegate;
    }

    public void onPrepared() {
        preloadingEnabled = true;
        delegate.onPrepared();
    }

    public void onStopped() {
        preloadingEnabled = true;
        delegate.onStopped();
    }

    public void onReleased() {
        preloadingEnabled = true;
        delegate.onReleased();
    }

    public boolean shouldContinueLoading(final long playbackPositionUs,
                                         final long bufferedDurationUs,
                                         final float playbackSpeed) {
        if (!preloadingEnabled) {
            return false;
        }
        return delegate.shouldContinueLoading(
                playbackPositionUs, bufferedDurationUs, playbackSpeed);
    }

    public void disablePreloadingOfCurrentTrack() {
        preloadingEnabled = false;
    }
}
