package com.example.posapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class SelectCustomerLocationActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap mMap;
    private Marker selectedMarker;
    private LatLng selectedLatLng;
    private Button btnSaveLocation;
    private Button btnCurrentLocation;
    private ProgressBar progressBar;

    private FusedLocationProviderClient fusedLocationClient;
    private boolean isViewOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_customer_location);

        // تهيئة موفر الموقع
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // قراءة المعاملات
        isViewOnly = getIntent().getBooleanExtra("view_only", false);
        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lng = getIntent().getDoubleExtra("longitude", 0);

        // تهيئة العناصر
        initializeViews();

        // تهيئة الخريطة
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // إذا كان هناك موقع محدد مسبقاً
        if (lat != 0 && lng != 0) {
            selectedLatLng = new LatLng(lat, lng);
        }
    }

    private void initializeViews() {
        btnSaveLocation = findViewById(R.id.btn_save_location);
        btnCurrentLocation = findViewById(R.id.btn_current_location);
        progressBar = findViewById(R.id.progress_bar);

        if (isViewOnly) {
            btnSaveLocation.setVisibility(View.GONE);
            btnCurrentLocation.setVisibility(View.GONE);
        } else {
            btnSaveLocation.setOnClickListener(v -> saveLocation());
            btnCurrentLocation.setOnClickListener(v -> getCurrentLocation());
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // إعداد الخريطة
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false); // نستخدم زرنا المخصص

        // إذا كان هناك موقع محدد، عرضه
        if (selectedLatLng != null) {
            selectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(selectedLatLng)
                    .title("موقع العميل"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15f));
        } else {
            // الانتقال إلى موقع افتراضي (الجزائر العاصمة)
            LatLng defaultLocation = new LatLng(36.7525, 3.0420);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));
        }

        // السماح بالنقر على الخريطة لتحديد موقع
        if (!isViewOnly) {
            mMap.setOnMapClickListener(latLng -> {
                if (selectedMarker != null) {
                    selectedMarker.remove();
                }
                selectedMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("موقع العميل"));
                selectedLatLng = latLng;
                btnSaveLocation.setEnabled(true);
            });
        }

        // محاولة تفعيل موقع المستخدم إذا كان لديه الصلاحية
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnCurrentLocation.setEnabled(false);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        progressBar.setVisibility(View.GONE);
                        btnCurrentLocation.setEnabled(true);

                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                            if (selectedMarker != null) {
                                selectedMarker.remove();
                            }

                            selectedMarker = mMap.addMarker(new MarkerOptions()
                                    .position(currentLatLng)
                                    .title("موقعك الحالي"));
                            selectedLatLng = currentLatLng;

                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));
                            btnSaveLocation.setEnabled(true);
                        } else {
                            Toast.makeText(SelectCustomerLocationActivity.this,
                                    "لا يمكن الحصول على الموقع الحالي", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnCurrentLocation.setEnabled(true);
                    Toast.makeText(SelectCustomerLocationActivity.this,
                            "فشل في الحصول على الموقع: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveLocation() {
        if (selectedLatLng != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitude", selectedLatLng.latitude);
            resultIntent.putExtra("longitude", selectedLatLng.longitude);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "يرجى تحديد الموقع على الخريطة", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
                getCurrentLocation();
            } else {
                Toast.makeText(this, "تم رفض صلاحية الموقع", Toast.LENGTH_SHORT).show();
            }
        }
    }
}