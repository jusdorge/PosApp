package com.example.posapp;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

public class SelectCustomerLocationActivity extends AppCompatActivity {
    private MapView mapView;
    private Marker selectedMarker;
    private GeoPoint selectedGeoPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تهيئة osmdroid
        Configuration.getInstance().load(getApplicationContext(), 
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        
        setContentView(R.layout.activity_select_customer_location);

        boolean viewOnly = getIntent().getBooleanExtra("view_only", false);
        double lat = getIntent().getDoubleExtra("latitude", 0);
        double lng = getIntent().getDoubleExtra("longitude", 0);

        // إعداد الخريطة
        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        IMapController mapController = mapView.getController();
        GeoPoint defaultGeoPoint = (lat != 0 && lng != 0) ? new GeoPoint(lat, lng) : new GeoPoint(36.7525, 3.0420);
        mapController.setCenter(defaultGeoPoint);
        mapController.setZoom(14.0);

        // إذا كان هناك موقع محدد مسبقاً، أضف ماركر
        if (lat != 0 && lng != 0) {
            selectedMarker = new Marker(mapView);
            selectedMarker.setPosition(defaultGeoPoint);
            selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            selectedMarker.setTitle("موقع العميل");
            mapView.getOverlays().add(selectedMarker);
            selectedGeoPoint = defaultGeoPoint;
        }

        // إضافة مستمع للنقر على الخريطة (إذا لم يكن في وضع العرض فقط)
        if (!viewOnly) {
            MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
                @Override
                public boolean singleTapConfirmedHelper(GeoPoint p) {
                    // إزالة الماركر السابق إن وجد
                    if (selectedMarker != null) {
                        mapView.getOverlays().remove(selectedMarker);
                    }
                    
                    // إضافة ماركر جديد
                    selectedMarker = new Marker(mapView);
                    selectedMarker.setPosition(p);
                    selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    selectedMarker.setTitle("موقع العميل");
                    mapView.getOverlays().add(selectedMarker);
                    mapView.invalidate();
                    
                    selectedGeoPoint = p;
                    return true;
                }

                @Override
                public boolean longPressHelper(GeoPoint p) {
                    return false;
                }
            };
            
            MapEventsOverlay eventsOverlay = new MapEventsOverlay(mapEventsReceiver);
            mapView.getOverlays().add(0, eventsOverlay);
        }

        Button btnSave = findViewById(R.id.btn_save_location);
        if (viewOnly) {
            btnSave.setVisibility(View.GONE);
        } else {
            btnSave.setOnClickListener(v -> {
                if (selectedGeoPoint != null) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("latitude", selectedGeoPoint.getLatitude());
                    resultIntent.putExtra("longitude", selectedGeoPoint.getLongitude());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(SelectCustomerLocationActivity.this, "يرجى تحديد الموقع على الخريطة", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
}