package com.blackfalcon.nearestmasjid;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/** Downloads mosques around a point from OpenStreetMap (Overpass API). */
public class Net {
    static final String[] SERVERS = {
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    };

    static String get(String server, String query) {
        HttpURLConnection c = null;
        try {
            String u = server + "?data=" + URLEncoder.encode(query, "UTF-8");
            c = (HttpURLConnection) new URL(u).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(40000);
            c.setRequestProperty("User-Agent", "BlackFalconNearestMasjid/1.0");
            if (c.getResponseCode() != 200) return null;
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
            r.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /** Returns null if the network failed, otherwise the (possibly empty) list. */
    public static ArrayList<Place> mosques(double lat, double lon, int radius) {
        String ar = "(around:" + radius + "," + lat + "," + lon + ")";
        String q = "[out:json][timeout:30];("
            + "nwr[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"]" + ar + ";"
            + "nwr[\"building\"=\"mosque\"]" + ar + ";"
            + ");out center 300;";
        String body = null;
        for (String s : SERVERS) {
            body = get(s, q);
            if (body != null) break;
        }
        if (body == null) return null;
        ArrayList<Place> out = new ArrayList<Place>();
        try {
            JSONArray el = new JSONObject(body).getJSONArray("elements");
            for (int i = 0; i < el.length(); i++) {
                JSONObject o = el.getJSONObject(i);
                double la, lo;
                if (o.has("lat") && o.has("lon")) {
                    la = o.getDouble("lat");
                    lo = o.getDouble("lon");
                } else {
                    JSONObject c = o.optJSONObject("center");
                    if (c == null) continue;
                    la = c.getDouble("lat");
                    lo = c.getDouble("lon");
                }
                JSONObject t = o.optJSONObject("tags");
                Place p = new Place();
                if (t != null) {
                    String n = t.optString("name", "");
                    p.en = t.optString("name:en", "");
                    if (p.en.length() == 0) p.en = n;
                    p.ur = t.optString("name:ur", "");
                    p.ar = t.optString("name:ar", "");
                }
                p.lat = la;
                p.lon = lo;
                p.id = "osm" + o.optString("type", "n") + o.optLong("id", 0);
                p.osm = true;
                out.add(p);
            }
        } catch (Exception e) {
            return null;
        }
        return out;
    }
}
