package io.github.vivchar.lagoon;

/**
 * How the cursor moves, shared by every control. There used to be a white frame gliding between the dock's
 * tiles here; like tvOS, the focused tile now only grows, lifts and casts a shadow.
 */
final class FocusRing {
    /** A focused dock tile, about tvOS's 1.1. */
    static final float TILE_SCALE = 1.1f;
    /** Material's "emphasized decelerate": quick start, long soft landing, no bounce. */
    static final android.view.animation.Interpolator EASE = new android.view.animation.PathInterpolator(0.05f, 0.7f, 0.1f, 1f);
    static final int MS = 280;

    private FocusRing() {
    }
}
