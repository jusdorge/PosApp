package com.example.posapp;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.posapp.model.Customer;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.util.ArrayList;
import java.util.List;

public class CustomersMapActivity extends AppCompatActivity {
    private MapView mapView;
    private FirebaseFirestore db;
    private List<Customer> customersWithLocation = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تهيئة osmdroid
        Configuration.getInstance().load(getApplicationContext(), 
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        
        setContentView(R.layout.activity_customers_map);

        // إعداد شريط العنوان
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("مواقع العملاء");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();

        // إعداد الخريطة
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);

        // تحميل العملاء
        loadCustomersWithLocation();
    }

    private void loadCustomersWithLocation() {
        db.collection("customers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    customersWithLocation.clear();
                    
                    int totalCustomers = queryDocumentSnapshots.size();
                    int customersWithLocationCount = 0;
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Customer customer = document.toObject(Customer.class);
                        customer.setId(document.getId());
                        
                        // تسجيل بيانات العميل للتشخيص
                        double lat = customer.getLatitude();
                        double lng = customer.getLongitude();
                        
                        // طباعة تفاصيل العميل للتشخيص
                        System.out.println("العميل: " + customer.getName() + 
                                ", الموقع: " + lat + ", " + lng);
                        
                        // فقط العملاء الذين لديهم موقع محدد (تحسين الشرط)
                        if (lat != 0.0 && lng != 0.0 && !Double.isNaN(lat) && !Double.isNaN(lng)) {
                            customersWithLocation.add(customer);
                            customersWithLocationCount++;
                            System.out.println("تمت إضافة العميل: " + customer.getName());
                        }
                    }
                    
                    // عرض رسالة تشخيص
                    String message = String.format("تم تحميل %d عميل، منهم %d لديهم مواقع محددة", 
                            totalCustomers, customersWithLocationCount);
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    
                    // إضافة الماركرات على الخريطة
                    addMarkersToMap();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل في تحميل العملاء: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addMarkersToMap() {
        IMapController mapController = mapView.getController();
        
        System.out.println("بدء إضافة الماركرات، عدد العملاء: " + customersWithLocation.size());
        
        if (customersWithLocation.isEmpty()) {
            Toast.makeText(this, "لا يوجد عملاء بمواقع محددة", Toast.LENGTH_SHORT).show();
            // توسيط الخريطة على موقع افتراضي
            GeoPoint defaultLocation = new GeoPoint(36.7525, 3.0420); // الجزائر العاصمة
            mapController.setCenter(defaultLocation);
            mapController.setZoom(10.0);
            return;
        }

        // حساب مركز جميع النقاط
        double avgLat = 0, avgLon = 0;
        int markerCount = 0;
        
        for (Customer customer : customersWithLocation) {
            GeoPoint position = new GeoPoint(customer.getLatitude(), customer.getLongitude());
            
            System.out.println("إضافة ماركر للعميل: " + customer.getName() + 
                    " في الموقع: " + position.getLatitude() + ", " + position.getLongitude());
            
            Marker marker = new Marker(mapView);
            marker.setPosition(position);
            marker.setTitle(customer.getName());
            marker.setSnippet("الهاتف: " + customer.getPhone() + "\nالدين: " + String.format("%.2f دج", customer.getTotalDebt()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            
            // إضافة مستمع للنقر على الماركر
            marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
                if (!clickedMarker.isInfoWindowShown()) {
                    clickedMarker.showInfoWindow();
                } else {
                    // عند النقر على نافذة المعلومات، افتح تفاصيل العميل
                    Customer clickedCustomer = findCustomerByPosition(clickedMarker.getPosition());
                    if (clickedCustomer != null) {
                        CustomerDetailsDialog dialog = CustomerDetailsDialog.newInstance(clickedCustomer.getId());
                        dialog.show(getSupportFragmentManager(), "CustomerDetailsDialog");
                    }
                }
                return true;
            });
            
            mapView.getOverlays().add(marker);
            markerCount++;
            
            avgLat += customer.getLatitude();
            avgLon += customer.getLongitude();
        }
        
        // تحديث الخريطة لإظهار التغييرات
        mapView.invalidate();
        
        // توسيط الخريطة على متوسط المواقع
        avgLat /= customersWithLocation.size();
        avgLon /= customersWithLocation.size();
        GeoPoint centerPoint = new GeoPoint(avgLat, avgLon);
        mapController.setCenter(centerPoint);
        mapController.setZoom(12.0);
        
        System.out.println("تم إضافة " + markerCount + " ماركر بنجاح");
        Toast.makeText(this, "تم العثور على " + customersWithLocation.size() + " عميل بموقع محدد", Toast.LENGTH_SHORT).show();
    }

    private Customer findCustomerByPosition(GeoPoint position) {
        for (Customer customer : customersWithLocation) {
            if (Math.abs(customer.getLatitude() - position.getLatitude()) < 0.0001 &&
                Math.abs(customer.getLongitude() - position.getLongitude()) < 0.0001) {
                return customer;
            }
        }
        return null;
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 