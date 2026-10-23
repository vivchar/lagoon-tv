package io.github.vivchar.lagoon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Starts the watcher after a reboot and after an update of this app. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        KeyWatchService.start(context);
    }
}
