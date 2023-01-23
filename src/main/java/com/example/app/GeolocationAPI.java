package com.example.app;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.stream.Location;

public class GeolocationAPI extends Application<GeolocationAPIConfig> {
    private Connection conn;
    private final ConcurrentHashMap<String, Location> cache = new ConcurrentHashMap<>();
    private final long CACHE_EXPIRATION_TIME = TimeUnit.MINUTES.toMillis(1);

    public static void main(String[] args) throws Exception {
        new GeolocationAPI().run(args);
    }

    @Override
    public void run(GeolocationAPIConfig config, Environment env) {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:geolocation.db");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS location (ip TEXT PRIMARY KEY, city TEXT NOT NULL, region TEXT NOT NULL, country TEXT NOT NULL, postal_code TEXT, latitude REAL NOT NULL, longitude REAL NOT NULL, timezone TEXT NOT NULL, utc_offset TEXT NOT NULL, country_code TEXT NOT NULL, region_code TEXT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        final Client client = ClientBuilder.newClient();
        env.jersey().register(new GeolocationResource(client, conn));
    }

    @Path("/geolocation/{ip}")
    @Produces(MediaType.APPLICATION_JSON)
    public class GeolocationResource {
        private final Client client;
        private final Connection conn;

        public GeolocationResource(Client client, Connection conn) {
            this.client = client;
            this.conn = conn;
        }

        @GET
        public Response getGeolocation(@PathParam("ip") String ip) {
            try {
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM location WHERE ip = ?");
                stmt.setString(1, ip);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return Response.ok(rs.getString("ip")+" "+rs.getString("city")+" "+rs.getString("region")+" "+rs.getString("country")+" "+rs.getString("postal_code")+" "+rs.getDouble("latitude")+" "+rs.getDouble("longitude")+" "+rs.getString("timezone")+" "+rs.getString("utc_offset")+" "+rs.getString("country_code")+" "+rs.getString("region_code")).build();
                }
                else {
                    Response externalApiResponse = client.target("http://ip-api.com/json/" + ip)
                            .request(MediaType.APPLICATION_JSON)
                            .get();
                    String response = externalApiResponse.readEntity(String.class);

                    PreparedStatement insert = conn.prepareStatement("INSERT INTO location (ip, city, region, country, postal_code, latitude, longitude, timezone, utc_offset, country_code, region_code) VALUES (?,?,?,?,?,?,?,?,?,?,?);");
                    insert.setString(1,ip);
                    insert.setString(2,city);
                    insert.setString(3,region);
                    insert.setString(4,country);
                    insert.setString(5,postal_code);
                    insert.setDouble(6,latitude);
                    insert.setDouble(7,longitude);
                    insert.setString(8,timezone);
                    insert.setString(9,utc_offset);
                    insert.setString(10,country_code);
                    insert.setString(11,region_code);
                    insert.execute();
                    return externalApiResponse;
                }
            } catch (SQLException e) {
                System.err.println(e.getMessage());
                return Response.serverError().build();
            }
        }
    }
}
