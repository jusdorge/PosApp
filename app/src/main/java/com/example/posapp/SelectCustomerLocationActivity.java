package com.example.posapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class SelectCustomerLocationActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Marker selectedMarker;
    private LatLng selectedLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_customer_location);

        boolean viewOnly = getIntent().getBooleanExtra("view_only", false);
        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lng = getIntent().getDoubleExtra("longitude", 0);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                mMap = googleMap;
                LatLng defaultLatLng = (lat != 0 && lng != 0) ? new LatLng(lat, lng) : new LatLng(36.7525, 3.0420);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 14f));
                if (lat != 0 && lng != 0) {
                    selectedMarker = mMap.addMarker(new MarkerOptions().position(defaultLatLng).title("موقع العميل"));
                    selectedLatLng = defaultLatLng;
                }
                if (!viewOnly) {
                    mMap.setOnMapClickListener(point -> {
                        if (selectedMarker != null) selectedMarker.remove();
                        selectedMarker = mMap.addMarker(new MarkerOptions().position(point).title("موقع العميل"));
                        selectedLatLng = point;
                    });
                }
            });
        }

        Button btnSave = findViewById(R.id.btn_save_location);
        if (viewOnly) {
            btnSave.setVisibility(View.GONE);
        } else {
            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selectedLatLng != null) {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("latitude", selectedLatLng.latitude);
                        resultIntent.putExtra("longitude", selectedLatLng.longitude);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(SelectCustomerLocationActivity.this, "يرجى تحديد الموقع على الخريطة", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng defaultLatLng = new LatLng(36.7525, 3.0420); // الجزائر العاصمة كموقع افتراضي
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 10f));

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (selectedMarker != null) selectedMarker.remove();
                selectedMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("موقع العميل"));
                selectedLatLng = latLng;
            }
        });
    }
} 