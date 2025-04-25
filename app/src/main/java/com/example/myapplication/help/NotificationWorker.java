package com.example.myapplication.help;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;

public class NotificationWorker extends BroadcastReceiver {
    private static final String TAG = "notification_receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Notification alarm triggered!");

        NotificationHelper.showNewsNotification(context, "Check out the latest articles!");
        NotificationScheduler.rescheduleNextDay(context);
    }
}



