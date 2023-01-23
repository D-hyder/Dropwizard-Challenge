package com.example.app;

import org.junit.Test;
import javax.xml.stream.Location;
import static org.junit.Assert.assertEquals;

public class GeolocationTest {
    @Test
    public void testGetGeolocation() {
        GeolocationAPI api = new GeolocationAPI();
        api.run("server");
        GeolocationClient client = new GeolocationClient("http://localhost:8080");
        Location location = client.getGeolocation("8.8.8.8");
        assertEquals("8.8.8.8", location.getIp());
        assertEquals("Mountain View", location.getCity());
        assertEquals("California", location.getRegion());
        assertEquals("United States", location.getCountry());
        assertEquals("94035", location.getPostalCode());
        assertEquals("37.3860", location.getLatitude());
        assertEquals("-122.0838", location.getLongitude());
        assertEquals("America/Los_Angeles", location.getTimezone());
        assertEquals("-07:00", location.getUtcOffset());
        assertEquals("US", location.getCountryCode());
        assertEquals("CA", location.getRegionCode());
    }
}