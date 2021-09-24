package com.arun.locationtracking;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.arun.locationtracking.model.Coordinate;
import com.arun.locationtracking.model.User;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.arun.locationtracking.databinding.ActivityMapsBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class MapsActivity extends BaseActivity {

    private GoogleMap googleMap;
    private ActivityMapsBinding binding;
    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.onCreate(savedInstanceState);
        mapFragment.onResume();
        // needed to get the map to display immediately
        DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReference(Utils.Coordinate);

        if (getIntent()!=null){
            databaseReference.child(getIntent().getStringExtra("deviceId")).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()){
                        Coordinate coordinate=dataSnapshot.getValue(Coordinate.class);
                        if (coordinate != null) {
                            LatLng userLocation=new LatLng(coordinate.get_lat(),coordinate.get_long()); new Handler(Looper.getMainLooper()).post(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            initMap(userLocation);

                                        }
                                    });

                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

    private void initMap(LatLng latLng) {
        // Gets to GoogleMap from the MapView and does initialization stuff
        mapFragment.getMapAsync(map -> {
            googleMap = map;
            // For showing a move to my location button
            googleMap.setIndoorEnabled(false);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            googleMap.getUiSettings().setAllGesturesEnabled(true);
            //googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setScrollGesturesEnabled(true);
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            if (latLng==null){
                LatLng sydney = new LatLng(-34, 151);
                googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
            }else {
                googleMap.addMarker(new MarkerOptions().position(latLng).title("He is here")
                        .draggable(false)
                        .icon(Utils.bitmapDescriptorFromVector(this, R.drawable.ic_pin_circle_24)));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));

            }
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));


        });
    }

}