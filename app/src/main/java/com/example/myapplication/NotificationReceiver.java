package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Directly call the showNotification method or create a helper
        NotificationWorker worker = new NotificationWorker(context, null);
        worker.showNotification("News Update", "Check out the latest articles!", context);
    }
}
