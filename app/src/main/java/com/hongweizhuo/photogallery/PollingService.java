package com.hongweizhuo.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollingService extends IntentService {

    private static final String TAG = "PollingService";
    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);

    public static Intent newIntent(Context context) {
        return new Intent(context, PollingService.class);
    }

    public PollingService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if (!isNetworkAvailableAndConnected()) {
            return;
        }

        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);

        FlickrFetcher fetcher = new FlickrFetcher();
        List<Photo> photos = null;

        if (query == null) {
            photos = fetcher.fetchRecentPhotos(1);
        } else {
            photos = fetcher.search(query,1);
        }

        if (photos.isEmpty()) {
            return;
        }

        Photo photo = photos.get(0);
        String id = photo.getId();

        QueryPreferences.setLastResultId(this, id);

        if (lastResultId == null) {
            Log.i(TAG, "Last result id not found");
            return;
        }

        if (!id.equals(lastResultId)) {
            Log.i(TAG, "New photos available.");

            Resources resources = getResources();

            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(this);
            notificationManager.notify(0, notification);

        } else {
            Log.i(TAG, "No new photos available.");
        }

    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();

        return  isNetworkConnected;
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent i = PollingService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        return  pi != null;
    }

    public static void setServiceAlarm(Context context, boolean isOn) {

        Intent i = PollingService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);

        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        if (isOn) {
            am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pi);

        } else {
            am.cancel(pi);
            pi.cancel();
        }

    }

}