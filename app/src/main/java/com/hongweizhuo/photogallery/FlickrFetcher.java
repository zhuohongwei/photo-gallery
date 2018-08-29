package com.hongweizhuo.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetcher {

    private static final String TAG = "FlickrFetcher";
    private static final String API_KEY = "03c8039e2101d9b6324f9bd02cc87c82";

    public List<Photo> fetchPhotos() {

        List<Photo> photos = new ArrayList<>();

        try {
            String url =
                    Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback","1")
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();

            String jsonString = getUrlString(url);
            parsePhotos(photos, jsonString);

            Log.i(TAG, "Fetched contents of URL: " + photos);

        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch URL: ", ioe);

        }

        return photos;

    }

    private void parsePhotos(List<Photo> photos, String jsonString) {

        Gson gson = new Gson();

        PhotoResult result = gson.fromJson(jsonString, PhotoResult.class);

        PhotoList photoList = result.getPhotosList();

        if (photoList != null && photoList.getPhotos() != null) {

            Photo[] allPhotos = photoList.getPhotos();

            for (Photo p: allPhotos) {
                if (p.getUrl() == null) { continue; }
                photos.add(p);
            }

        }

    }


    public byte[] getUrlBytes(String urlSpec) throws IOException {

        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try {

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " +
                        urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();


        } finally {
            connection.disconnect();
        }

    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

}
