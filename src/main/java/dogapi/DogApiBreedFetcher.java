package dogapi;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * BreedFetcher implementation that relies on the dog.ceo API.
 * Note that all failures get reported as BreedNotFoundException
 * exceptions to align with the requirements of the BreedFetcher interface.
 */
public class DogApiBreedFetcher implements BreedFetcher {
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Fetch the list of sub breeds for the given breed from the dog.ceo API.
     *
     * @param breed the breed to fetch sub breeds for
     * @return list of sub breeds for the given breed
     * @throws BreedNotFoundException if the breed does not exist (or if the API call fails for any reason)
     */
    @Override
    public List<String> getSubBreeds(String breed) throws BreedNotFoundException {
        String url = "https://dog.ceo/api/breed/" + breed.toLowerCase() + "/list";
        try {
            java.net.URL u = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() == 404) {
                throw new BreedNotFoundException(breed);
            }

            java.io.InputStreamReader reader = new java.io.InputStreamReader(conn.getInputStream());
            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) sb.append((char) c);
            reader.close();
            String result = sb.toString();

            if (result.contains("\"status\":\"error\"")) {
                throw new BreedNotFoundException(breed);
            }

            int start = result.indexOf('[');
            int end = result.indexOf(']');
            if (start == -1 || end == -1) return List.of();
            String inside = result.substring(start + 1, end).replace("\"", "");
            if (inside.isBlank()) return List.of();
            return List.of(inside.split(","));
        } catch (java.io.IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Server returned HTTP response code: 404")) {
                throw new BreedNotFoundException(breed);
            }
            throw new RuntimeException("Network error", e);
        }
    }
}