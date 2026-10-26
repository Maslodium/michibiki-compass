package app.michibiki.navigator;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

public final class SystemLocationProvider implements LocationProvider, LocationListener {
    private final Context context;
    private final LocationManager manager;
    private Listener listener;

    public SystemLocationProvider(Context context) {
        this.context = context.getApplicationContext();
        manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override public void start(Listener listener) {
        this.listener = listener;
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listener.onProviderError("Location permission is required");
            return;
        }
        boolean requested = false;
        if (NavStore.option(context, "use_gps", true) && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, this, Looper.getMainLooper());
            requested = true;
            Location last = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) listener.onLocation(last);
        }
        if (NavStore.option(context, "use_network", false) && manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 3f, this, Looper.getMainLooper());
            requested = true;
            Location last = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (last != null) listener.onLocation(last);
        }
        if (!requested) listener.onProviderError("Enable GPS or network location in Settings");
    }

    @Override public void stop() {
        try { manager.removeUpdates(this); } catch (SecurityException ignored) {}
        listener = null;
    }

    @Override public void onLocationChanged(Location location) {
        if (listener != null) listener.onLocation(location);
    }

    @Override public void onProviderDisabled(String provider) {
        if (listener != null) listener.onProviderError(provider + " is disabled");
    }

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
}
