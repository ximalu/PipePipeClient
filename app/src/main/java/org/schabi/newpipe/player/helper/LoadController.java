package org.schabi.newpipe.player.helper;

import androidx.media3.common.C;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.LoadControl;

/**
 * Wrapper around DefaultLoadControl that adds preloading control and
 * SABR-aware buffer defaults.
 * Call {@link #getLoadControl()} to get the actual LoadControl for ExoPlayer.
 *
 * SABR defaults (used when no user override): keep the player's buffer target
 * well BELOW the SABR pump's read-ahead cushion (50s). 20s target → ~30s real
 * read-ahead, comfortably inside the 50s cushion. Fine for DASH/HLS too.
 */
public class LoadController {

    public static final String TAG = "LoadController";
    private static final int DEFAULT_MIN_BUFFER_MS = 12_000;
    private static final int DEFAULT_MAX_BUFFER_MS = 20_000;
    private static final int BUFFER_FOR_PLAYBACK_MS = 2_000;
    private static final int BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 3_000;

    private final DefaultLoadControl delegate;
    private boolean preloadingEnabled = true;

    public LoadController() {
        this(DEFAULT_MIN_BUFFER_MS, DEFAULT_MAX_BUFFER_MS);
    }

    public LoadController(final int minBufferMs, final int maxBufferMs) {
        final int min = minBufferMs > 0 ? minBufferMs : DEFAULT_MIN_BUFFER_MS;
        final int max = maxBufferMs > 0 ? maxBufferMs : DEFAULT_MAX_BUFFER_MS;
        delegate = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        min, max,
                        BUFFER_FOR_PLAYBACK_MS,
                        BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
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
