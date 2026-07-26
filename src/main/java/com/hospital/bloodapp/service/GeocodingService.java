package com.hospital.bloodapp.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class GeocodingService {

    private final Map<String, double[]> cityCoordinates = new HashMap<>();
    private final HttpClient httpClient;

    public GeocodingService() {
        // Pre-populate coordinate mapping for Tamil Nadu major cities (offline fallback)
        cityCoordinates.put("chennai", new double[]{13.0827, 80.2707});
        cityCoordinates.put("madurai", new double[]{9.9252, 78.1198});
        cityCoordinates.put("salem", new double[]{11.6643, 78.1460});
        cityCoordinates.put("coimbatore", new double[]{11.0168, 76.9558});
        cityCoordinates.put("trichy", new double[]{10.7905, 78.7047});
        cityCoordinates.put("tiruchirappalli", new double[]{10.7905, 78.7047});
        cityCoordinates.put("tirunelveli", new double[]{8.7139, 77.7567});
        cityCoordinates.put("vellore", new double[]{12.9165, 79.1325});
        cityCoordinates.put("thanjavur", new double[]{10.7870, 79.1378});
        cityCoordinates.put("erode", new double[]{11.3410, 77.7172});
        cityCoordinates.put("thoothukudi", new double[]{8.7642, 78.1348});
        cityCoordinates.put("tuticorin", new double[]{8.7642, 78.1348});
        cityCoordinates.put("kanyakumari", new double[]{8.0883, 77.5385});
        cityCoordinates.put("dindigul", new double[]{10.3673, 77.9803});
        cityCoordinates.put("namakkal", new double[]{11.2189, 78.1674});
        cityCoordinates.put("anna nagar", new double[]{13.0856, 80.2137});
        cityCoordinates.put("t nagar", new double[]{13.0418, 80.2341});
        cityCoordinates.put("velachery", new double[]{12.9815, 80.2180});
        cityCoordinates.put("porur", new double[]{13.0359, 80.1568});

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Geocode a free-text city/address string.
     * First tries Nominatim (OpenStreetMap), then falls back to hardcoded map.
     */
    public double[] geocodeCity(String address) {
        if (address == null || address.trim().isEmpty()) {
            return cityCoordinates.get("chennai");
        }

        // 1. Try Nominatim live geocoding
        double[] nominatimResult = geocodeViaNominatim(address);
        if (nominatimResult != null) {
            return nominatimResult;
        }

        // 2. Fallback: search the hardcoded map
        String key = address.trim().toLowerCase();
        if (cityCoordinates.containsKey(key)) {
            return cityCoordinates.get(key);
        }
        // partial match fallback
        for (Map.Entry<String, double[]> entry : cityCoordinates.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return entry.getValue();
            }
        }

        // 3. Default to Chennai with slight random offset so pins don't stack
        double baseLat = 13.0827;
        double baseLng = 80.2707;
        double randomOffsetLat = (Math.random() - 0.5) * 0.1;
        double randomOffsetLng = (Math.random() - 0.5) * 0.1;
        return new double[]{baseLat + randomOffsetLat, baseLng + randomOffsetLng};
    }

    /**
     * Call Nominatim API and parse the first result's lat/lon.
     * Returns null if request fails or no results found.
     */
    private double[] geocodeViaNominatim(String address) {
        try {
            String encoded = URLEncoder.encode(address + ", Tamil Nadu, India", StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + encoded
                    + "&format=json&limit=1&addressdetails=0";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "BloodDonorApp/1.0 (bloodapp@hospital.local)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body().trim();
                if (body.startsWith("[") && body.length() > 5 && !body.equals("[]")) {
                    // Simple JSON parse without Jackson dependency
                    String latStr = extractJsonField(body, "lat");
                    String lonStr = extractJsonField(body, "lon");
                    if (latStr != null && lonStr != null) {
                        double lat = Double.parseDouble(latStr);
                        double lon = Double.parseDouble(lonStr);
                        System.out.println("[Nominatim] Geocoded '" + address + "' -> " + lat + ", " + lon);
                        return new double[]{lat, lon};
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Nominatim] Geocoding failed for '" + address + "': " + e.getMessage());
        }
        return null;
    }

    /**
     * Simple JSON field extractor (avoids adding heavy JSON library dependency).
     * Finds the first occurrence of "fieldName":"value" or "fieldName":value in a JSON string.
     */
    private String extractJsonField(String json, String fieldName) {
        String key = "\"" + fieldName + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colonIdx = json.indexOf(":", idx + key.length());
        if (colonIdx == -1) return null;
        int startIdx = colonIdx + 1;
        while (startIdx < json.length() && json.charAt(startIdx) == ' ') startIdx++;
        if (startIdx >= json.length()) return null;
        char firstChar = json.charAt(startIdx);
        if (firstChar == '"') {
            // String value
            int endIdx = json.indexOf("\"", startIdx + 1);
            return endIdx == -1 ? null : json.substring(startIdx + 1, endIdx);
        } else {
            // Numeric value
            int endIdx = startIdx;
            while (endIdx < json.length() && (Character.isDigit(json.charAt(endIdx))
                    || json.charAt(endIdx) == '.' || json.charAt(endIdx) == '-')) {
                endIdx++;
            }
            return json.substring(startIdx, endIdx);
        }
    }

    /**
     * Distance calculation using Haversine formula.
     * Returns distance in km.
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
