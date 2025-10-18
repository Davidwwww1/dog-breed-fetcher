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
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (response.code() == 404) {
                    throw new BreedNotFoundException(breed);
                } else {
                    throw new IOException("Unexpected HTTP response: " + response.code());
                }
            }

            String body = response.body().string();
            JSONObject json = new JSONObject(body);

            String status = json.optString("status", "error");
            if (status.equalsIgnoreCase("error")) {
                throw new BreedNotFoundException(breed);
            }

            Object message = json.get("message");
            List<String> subBreeds = new ArrayList<>();

            if (message instanceof JSONArray) {
                JSONArray arr = (JSONArray) message;
                for (int i = 0; i < arr.length(); i++) {
                    subBreeds.add(arr.getString(i));
                }
            } else if (message instanceof JSONObject) {
                JSONObject msgObj = (JSONObject) message;
                if (!msgObj.has(breed.toLowerCase())) {
                    throw new BreedNotFoundException(breed);
                }
                JSONArray arr = msgObj.getJSONArray(breed.toLowerCase());
                for (int i = 0; i < arr.length(); i++) {
                    subBreeds.add(arr.getString(i));
                }
            } else {
                throw new IOException("Unexpected JSON structure for message field");
            }
            Collections.sort(subBreeds);
            return subBreeds;
        } catch (BreedNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Network or I/O error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error parsing dog API response", e);
        }
    }
}