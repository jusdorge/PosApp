package com.example.posapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.google.android.gms.tasks.OnSuccessListener;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class SelectCustomerLocationActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private MapView mapView;
    private IMapController mapController;
    private Marker selectedMarker;
    private GeoPoint selectedPoint;
    private Button btnSaveLocation;
    private Button btnCurrentLocation;
    private ProgressBar progressBar;

    private FusedLocationProviderClient fusedLocationClient;
    private MyLocationNewOverlay myLocationOverlay;
    private boolean isViewOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // تهيئة إعدادات osmdroid
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        // تعيين وكيل المستخدم
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_select_customer_location_osm);

        // تهيئة موفر الموقع
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // قراءة المعاملات
        isViewOnly = getIntent().getBooleanExtra("view_only", false);
        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lng = getIntent().getDoubleExtra("longitude", 0);

        // تهيئة العناصر
        initializeViews();

        // تهيئة الخريطة
        setupMap();

        // إذا كان هناك موقع محدد مسبقاً
        if (lat != 0 && lng != 0) {
            selectedPoint = new GeoPoint(lat, lng);
            addMarker(selectedPoint);
            mapController.setCenter(selectedPoint);
        }
    }

    private void initializeViews() {
        mapView = findViewById(R.id.map);
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

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        mapController = mapView.getController();
        mapController.setZoom(15.0);

        // تعيين نقطة البداية للخريطة
        GeoPoint startPoint = new GeoPoint(35.40367, 0.38471);
        mapController.setCenter(startPoint);

        // إضافة طبقة موقعي
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        // السماح بالنقر على الخريطة لتحديد موقع
        if (!isViewOnly) {
            mapView.setOnTouchListener((v, event) -> {
                // السماح للخريطة بمعالجة اللمس أولاً
                return false;
            });

            // استخدام MapEventsOverlay للنقر على الخريطة
            MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(
                    new MapEventsReceiver() {
                        @Override
                        public boolean singleTapConfirmedHelper(GeoPoint p) {
                            // عند النقر على الخريطة
                            selectedPoint = p;
                            addMarker(p);
                            btnSaveLocation.setEnabled(true);
                            return true;
                        }

                        @Override
                        public boolean longPressHelper(GeoPoint p) {
                            return false;
                        }
                    }
            );
            mapView.getOverlays().add(0, mapEventsOverlay);
        }
    }

    private void addMarker(GeoPoint point) {
        // إزالة العلامة السابقة إن وجدت
        if (selectedMarker != null) {
            mapView.getOverlays().remove(selectedMarker);
        }

        // إضافة علامة جديدة
        selectedMarker = new Marker(mapView);
        selectedMarker.setPosition(point);
        selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        selectedMarker.setTitle("موقع العميل");
        selectedMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_map_marker));
        mapView.getOverlays().add(selectedMarker);

        // تحديث الخريطة
        mapView.invalidate();
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
                            GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            selectedPoint = currentPoint;
                            addMarker(currentPoint);
                            mapController.setZoom(17.0);
                            mapController.setCenter(currentPoint);
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
        if (selectedPoint != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitude", selectedPoint.getLatitude());
            resultIntent.putExtra("longitude", selectedPoint.getLongitude());
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
                getCurrentLocation();
            } else {
                Toast.makeText(this, "تم رفض صلاحية الموقع", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDetach();
    }
}