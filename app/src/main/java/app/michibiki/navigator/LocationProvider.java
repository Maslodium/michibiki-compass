package app.michibiki.navigator;

import android.location.Location;

public interface LocationProvider {
    interface Listener {
        void onLocation(Location location);
        void onProviderError(String message);
    }
    void start(Listener listener);
    void stop();
}
