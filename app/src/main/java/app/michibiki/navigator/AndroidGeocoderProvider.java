package app.michibiki.navigator;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class AndroidGeocoderProvider implements GeocoderProvider {
    private final Context context;
    private final Geocoder geocoder;

    public AndroidGeocoderProvider(Context context) {
        this.context = context.getApplicationContext();
        geocoder = new Geocoder(context, Locale.getDefault());
    }

    @Override public List<Address> search(String query, int limit) throws Exception {
        if (!Geocoder.isPresent()) return Collections.emptyList();
        String effective = query;
        if (NavStore.option(context, "city_bias", true) && NavStore.hasLocation(context)) {
            List<Address> here = geocoder.getFromLocation(NavStore.locationLat(context), NavStore.locationLon(context), 1);
            if (here != null && !here.isEmpty()) {
                String city = here.get(0).getLocality();
                if (city == null) city = here.get(0).getSubAdminArea();
                if (city != null && !city.isBlank()) effective = query + ", " + city;
            }
        }
        List<Address> result = geocoder.getFromLocationName(effective, limit);
        return result == null ? Collections.emptyList() : result;
    }
}
