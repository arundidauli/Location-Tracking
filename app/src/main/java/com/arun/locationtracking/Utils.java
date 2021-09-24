package com.arun.locationtracking;

import static android.content.Context.NOTIFICATION_SERVICE;


import static com.arun.locationtracking.BaseActivity.getDeviceId;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.arun.locationtracking.model.Coordinate;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


/**
 * Created by Freaky Jolly on 01-04-2018.
 */

public class Utils {
    public static final long UPDATE_INTERVAL = 10 * 1000;
    public static final float SMALLEST_DISPLACEMENT = 1.0F;
    public static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;
    public static final long MAX_WAIT_TIME = UPDATE_INTERVAL * 2;
    final static String KEY_LOCATION_UPDATES_RESULT = "location-update-result";
    final static String DB_REFERENCE = "locationTracking";
    final static String Coordinate = "coordinate";
    private static final String TAG = "UtilsClass";
    public static float accuracy;
    static String addressFragments = "";
    static List<Address> addresses = null;

    static void setLocationUpdatesResult(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_LOCATION_UPDATES_RESULT, value)
                .apply();
    }

    @SuppressLint("MissingPermission")
    public static void getLocationUpdates(final Context context, final Intent intent, String latLng) {

        LocationResult result = LocationResult.extractResult(intent);
        if (result != null) {


            List<Location> locations = result.getLocations();
            Location firstLocation = locations.get(0);

            LocationRequestHelper.getInstance(context).setValue("lat", firstLocation.getLatitude());
            LocationRequestHelper.getInstance(context).setValue("long", firstLocation.getLongitude());

            updateTextField(context, firstLocation);

          //  showNotificationOngoing(context, latLng);
        }
    }



    public static void showNotificationOngoing(Context context, String latLng) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT);

        String CHANNEL_ID = "channel_location";
        String CHANNEL_NAME = "channel_location";

        NotificationCompat.Builder builder = null;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
            builder = new NotificationCompat.Builder(context, CHANNEL_ID);
            builder.setChannelId(CHANNEL_ID);
            builder.setBadgeIconType(NotificationCompat.BADGE_ICON_NONE);
        } else {
            builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        }
        builder.setContentTitle("" + DateFormat.getDateTimeInstance().format(new Date()) + ":" + accuracy);
        builder.setContentText(addressFragments);
        Uri notificationSound = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
       // builder.setSound(notificationSound);
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.ic_baseline_gps_fixed_24);
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();
        notificationManager.notify(3, notification);

    }
    public static void updateTextField(Context context, Location location) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(Coordinate);
        Coordinate coordinate = new Coordinate();
        coordinate.set_lat(location.getLatitude());
        coordinate.set_long(location.getLongitude());
        coordinate.setTime_stamp(System.currentTimeMillis());
        databaseReference.child(getDeviceId(context)).setValue(coordinate).addOnCompleteListener(task -> {

            // Toast.makeText(MainActivity.this,"data saved...",Toast.LENGTH_LONG).show();

        });
    }

    public static BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public static void removeNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }
}

