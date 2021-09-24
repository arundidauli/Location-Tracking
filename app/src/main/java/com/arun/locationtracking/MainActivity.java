package com.arun.locationtracking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.arun.locationtracking.adapter.UsersAdapter;
import com.arun.locationtracking.databinding.ActivityMainBinding;
import com.arun.locationtracking.model.Coordinate;
import com.arun.locationtracking.model.User;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends BaseActivity implements UsersAdapter.MyClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private ActivityMainBinding binding;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private SettingsClient mSettingsClient;
    private LocationSettingsRequest mLocationSettingsRequest;
    private ActivityRecognitionClient mActivityRecognitionClient;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        userList = new ArrayList<>();
        UsersAdapter usersAdapter = new UsersAdapter(MainActivity.this, this);
        binding.progressBar.setVisibility(View.VISIBLE);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(Utils.DB_REFERENCE);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                binding.progressBar.setVisibility(View.GONE);

                if (dataSnapshot.exists()) {
                    for (DataSnapshot users : dataSnapshot.getChildren()) {
                        if (!Objects.equals(users.child("deviceId").getValue(String.class), getDeviceId(MainActivity.this))) {
                        User user = users.getValue(User.class);
                        userList.add(user);
                        }
                    }
                    usersAdapter.setUsers(userList);
                    binding.rvUsers.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                    binding.rvUsers.setAdapter(usersAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                binding.progressBar.setVisibility(View.GONE);

            }
        });

        mSettingsClient = LocationServices.getSettingsClient(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        createLocationRequest();
        buildLocationSettingsRequest();

        mActivityRecognitionClient = new ActivityRecognitionClient(this);
        OnDutyOffDuty();
    }

    @Override
    public void clicked(String id) {
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        intent.putExtra("deviceId",id);
        startActivity(intent);
    }


    private void UserLatLong(String id){
        DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReference(Utils.Coordinate);
        databaseReference.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                binding.progressBar.setVisibility(View.GONE);

                if (dataSnapshot.exists()){

                    Coordinate coordinate=dataSnapshot.getValue(Coordinate.class);

                    if (coordinate != null) {

                        Geocoder gcd = new Geocoder(MainActivity.this, Locale.getDefault());
                        List<Address> addresses = null;
                        try {
                            addresses = gcd.getFromLocation(coordinate.get_lat(), coordinate.get_long(), 1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (addresses.size() > 0) {
                           getDirection(addresses.get(0).getSubLocality()+" "+addresses.get(0).getLocality());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                binding.progressBar.setVisibility(View.GONE);

            }
        });
    }
    @Override
    public void getLatLong(String id) {
        binding.progressBar.setVisibility(View.VISIBLE);
       // Log.e(TAG, "" + " Device ID ******************** "+id);

        UserLatLong(id);

    }


    private void getDirection(String destination) {
        String my_data = "http://maps.google.com/maps?daddr=" + destination;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(my_data));
        intent.setPackage("com.google.android.apps.maps");
        startActivity(intent);
    }


    public void updateTextField(Context context) {
       // Log.e(TAG, "updateText ******************** " + LocationRequestHelper.getInstance(context).getStringValue("lat", ""));
       // Log.e(TAG, "updateText ******************** " + LocationRequestHelper.getInstance(context).getStringValue("long", ""));
    }

    private void OnDutyOffDuty() {
        binding.OnDutySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.OnDutySwitch.setChecked(isChecked);
            if (isChecked) {
                binding.OnDutySwitch.setText(R.string.on_duty);
                binding.OnDutySwitch.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.green_holo));
                requestLocationUpdates();
            } else {
                binding.OnDutySwitch.setText(R.string.off_duty);
                binding.OnDutySwitch.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.orange_holo));
                removeLocationUpdates();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Dexter.withContext(MainActivity.this)
                    .withPermissions(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                            if (report.areAllPermissionsGranted()) {
                                if (isGpsEnabled()) {
                                    PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                                            .registerOnSharedPreferenceChangeListener(MainActivity.this);

                                    if (!checkPermissions()) {
                                        requestPermissions();
                                    }
                                    updateTextField(MainActivity.this);
                                    updateButtonsState(LocationRequestHelper.getInstance(MainActivity.this).getBoolanValue("RequestingLocationUpdates", false));

                                } else {
                                    startLocationPermissionRequest();
                                }
                            }

                            if (report.isAnyPermissionPermanentlyDenied()) {

                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    }).check();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTextField(this);
        updateButtonsState(LocationRequestHelper.getInstance(this).getBoolanValue("RequestingLocationUpdates", false));
    }

    @Override
    protected void onStop() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check for the integer request code originally supplied to startResolutionForResult().
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    toast("GPS turned on");
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        toast("Please Provide Location Permission.");
                        return;
                    }
                    changeStatusAfterGetLastLocation("1", "Manual");

                    break;
                case Activity.RESULT_CANCELED:
                    if (!checkPermissions()) {
                        requestPermissions();
                    }
                    toast("GPS is required to Start Tracking");
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
                requestPermissions();
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                // Permission denied.
                Snackbar.make(
                        findViewById(R.id.activity_main),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        updateTextField(this);
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Utils.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(Utils.FASTEST_UPDATE_INTERVAL);

        //mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //mLocationRequest.setSmallestDisplacement(Utils.SMALLEST_DISPLACEMENT);
        //mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        mLocationRequest.setMaxWaitTime(Utils.MAX_WAIT_TIME);
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, LocationUpdatesBroadcastReceiver.class);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setAction(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION);

        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            startLocationPermissionRequest();
                        }
                    });

        } else {
            Log.i(TAG, "Requesting permission");
            startLocationPermissionRequest();
        }
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    public void requestLocationUpdates() {
        if (!checkPermissions()) {
            toast("Please Allow Location Permission!");
            requestPermissions();
            return;
        }
        try {
            mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                            changeStatusAfterGetLastLocation("1", "Manual");
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            int statusCode = ((ApiException) e).getStatusCode();
                            switch (statusCode) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                            "location settings ");
                                    try {
                                        ResolvableApiException rae = (ResolvableApiException) e;
                                        rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                    } catch (IntentSender.SendIntentException sie) {
                                        Log.i(TAG, "PendingIntent unable to execute request.");
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    String errorMessage = "Location settings are inadequate, and cannot be " +
                                            "fixed here. Fix in Settings.";
                                    Log.e(TAG, errorMessage);
                                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                case LocationSettingsStatusCodes.DEVELOPER_ERROR:
                                    Log.e(TAG, "DEVELOPER_ERROR");
                            }
                        }
                    });

        } catch (SecurityException e) {
            LocationRequestHelper.getInstance(getApplicationContext()).setValue("RequestingLocationUpdates", false);
            e.printStackTrace();
        }
    }

    public void removeLocationUpdates() {
        changeStatusAfterGetLastLocation("0", "Manual");
    }

    @SuppressLint("MissingPermission")
    private void changeStatusAfterGetLastLocation(final String value, final String changeby) {
        if (value.equals("1")) {
            toast("Location Updates Started!");

            mFusedLocationClient.requestLocationUpdates(mLocationRequest, getPendingIntent());
            LocationRequestHelper.getInstance(getApplicationContext()).setValue("RequestingLocationUpdates", true);

            Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                    Utils.UPDATE_INTERVAL,
                    getPendingIntent());

            task.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void result) {

                }
            });
            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.i(TAG, "addOnFailureListener mActivityRecognitionClient " + e);
                }
            });

        } else if (value.equals("0")) {

            LocationRequestHelper.getInstance(getApplicationContext()).setValue("RequestingLocationUpdates", false);
            mFusedLocationClient.removeLocationUpdates(getPendingIntent());
            // Utils.removeNotification(getApplicationContext());

            toast("Location Updates Stopped!");

            Task<Void> task = mActivityRecognitionClient.removeActivityUpdates(
                    getPendingIntent());
            task.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                }
            });
            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.i(TAG, "removeActivityUpdates addOnFailureListener " + e);

                }
            });
        }
        updateButtonsState(LocationRequestHelper.getInstance(this).getBoolanValue("RequestingLocationUpdates", false));
    }

    public void updateButtonsState(boolean requestingLocationUpdates) {
        if (requestingLocationUpdates) {
            binding.OnDutySwitch.setText(R.string.on_duty);
            binding.OnDutySwitch.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.green_holo));
            binding.OnDutySwitch.setChecked(true);
        } else {
            binding.OnDutySwitch.setText(R.string.off_duty);
            binding.OnDutySwitch.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.orange_holo));
            binding.OnDutySwitch.setChecked(false);


        }
    }


}