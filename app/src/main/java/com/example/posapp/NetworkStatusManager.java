package com.example.posapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * مدير حالة الشبكة - يراقب الاتصال بالإنترنت ويخبر التطبيق عن التغييرات
 */
public class NetworkStatusManager {
    private static final String TAG = "NetworkStatusManager";
    private static NetworkStatusManager instance;
    
    private ConnectivityManager connectivityManager;
    private List<NetworkStatusListener> listeners;
    private boolean isOnline = false;
    private ConnectivityManager.NetworkCallback networkCallback;
    
    public interface NetworkStatusListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }
    
    private NetworkStatusManager(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        listeners = new ArrayList<>();
        
        // فحص الحالة الحالية للشبكة
        checkCurrentNetworkStatus();
        
        // بدء مراقبة تغييرات الشبكة
        startNetworkMonitoring();
    }
    
    public static synchronized NetworkStatusManager getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkStatusManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * إضافة مستمع لتغييرات حالة الشبكة
     */
    public void addNetworkStatusListener(NetworkStatusListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "Network status listener added. Total listeners: " + listeners.size());
        }
    }
    
    /**
     * إزالة مستمع تغييرات حالة الشبكة
     */
    public void removeNetworkStatusListener(NetworkStatusListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            Log.d(TAG, "Network status listener removed. Total listeners: " + listeners.size());
        }
    }
    
    /**
     * التحقق من حالة الشبكة الحالية
     */
    private void checkCurrentNetworkStatus() {
        boolean wasOnline = isOnline;
        isOnline = isNetworkAvailable();
        
        Log.d(TAG, "Current network status: " + (isOnline ? "ONLINE" : "OFFLINE"));
        
        // إذا تغيرت الحالة، أخبر المستمعين
        if (wasOnline != isOnline) {
            notifyNetworkStatusChange();
        }
    }
    
    /**
     * فحص ما إذا كانت الشبكة متوفرة
     */
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
    
    /**
     * بدء مراقبة تغييرات الشبكة
     */
    private void startNetworkMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    Log.d(TAG, "🌐 Network became available");
                    boolean wasOnline = isOnline;
                    isOnline = true;
                    
                    if (!wasOnline) {
                        notifyNetworkAvailable();
                    }
                }
                
                @Override
                public void onLost(@NonNull Network network) {
                    Log.d(TAG, "🚫 Network lost");
                    boolean wasOnline = isOnline;
                    isOnline = false;
                    
                    if (wasOnline) {
                        notifyNetworkLost();
                    }
                }
                
                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                    boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                         networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    
                    boolean wasOnline = isOnline;
                    isOnline = hasInternet;
                    
                    if (wasOnline != isOnline) {
                        Log.d(TAG, "📶 Network capabilities changed. Internet: " + hasInternet);
                        notifyNetworkStatusChange();
                    }
                }
            };
            
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
            Log.d(TAG, "✅ Network monitoring started (API >= 24)");
        } else {
            Log.d(TAG, "⚠️ Network monitoring not available for API < 24");
        }
    }
    
    /**
     * إيقاف مراقبة الشبكة
     */
    public void stopNetworkMonitoring() {
        if (networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                Log.d(TAG, "✅ Network monitoring stopped");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error stopping network monitoring", e);
            }
        }
    }
    
    /**
     * إخبار المستمعين بتغيير حالة الشبكة
     */
    private void notifyNetworkStatusChange() {
        if (isOnline) {
            notifyNetworkAvailable();
        } else {
            notifyNetworkLost();
        }
    }
    
    /**
     * إخبار المستمعين بتوفر الشبكة
     */
    private void notifyNetworkAvailable() {
        Log.d(TAG, "🔄 Notifying " + listeners.size() + " listeners: Network AVAILABLE");
        for (NetworkStatusListener listener : listeners) {
            try {
                listener.onNetworkAvailable();
            } catch (Exception e) {
                Log.e(TAG, "❌ Error notifying network available", e);
            }
        }
    }
    
    /**
     * إخبار المستمعين بفقدان الشبكة
     */
    private void notifyNetworkLost() {
        Log.d(TAG, "🔄 Notifying " + listeners.size() + " listeners: Network LOST");
        for (NetworkStatusListener listener : listeners) {
            try {
                listener.onNetworkLost();
            } catch (Exception e) {
                Log.e(TAG, "❌ Error notifying network lost", e);
            }
        }
    }
    
    /**
     * الحصول على حالة الشبكة الحالية
     */
    public boolean isOnline() {
        return isOnline;
    }
    
    /**
     * الحصول على وصف حالة الشبكة
     */
    public String getNetworkStatusDescription() {
        if (isOnline) {
            return "متصل بالإنترنت - البيانات تتم مزامنتها";
        } else {
            return "غير متصل - يعمل في الوضع المحلي";
        }
    }
}