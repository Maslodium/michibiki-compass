package app.michibiki.navigator;

import android.location.Address;
import java.util.List;

public interface GeocoderProvider {
    List<Address> search(String query, int limit) throws Exception;
}
