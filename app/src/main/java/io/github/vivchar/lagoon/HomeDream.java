package io.github.vivchar.lagoon;

import android.service.dreams.DreamService;

/** The system screensaver: the album slideshow of {@link SaverView}, started by the TV after its idle time. */
public class HomeDream extends DreamService {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setContentView(new SaverView(this));
    }
}
