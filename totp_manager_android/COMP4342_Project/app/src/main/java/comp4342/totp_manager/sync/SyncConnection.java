package comp4342.totp_manager.sync;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.cert.X509Certificate;

import android.util.Log;
import android.util.Base64;

import org.json.JSONObject;

class TrustAllCertificates {
    public static SSLSocketFactory getTrustAllSocketFactory() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // 允许所有主机名
        HostnameVerifier allHostsValid = (hostname, session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        return sc.getSocketFactory();
    }
}


public class SyncConnection {

    private static final String BASE_URL = "https://attr0.eu.org:8443";

    public static void upload(String username, String encryptedData) throws ConnectionException {
        String endpoint = "/upload";
        try {
            URL url = new URL(BASE_URL + endpoint);
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);
            connection.setSSLSocketFactory(TrustAllCertificates.getTrustAllSocketFactory());

            JSONObject jsonParam = new JSONObject();
            jsonParam.put("username", username);
            jsonParam.put("encrypted_data", encryptedData);

            OutputStream os = connection.getOutputStream();
            os.write(jsonParam.toString().getBytes("UTF-8"));
            os.close();


            Log.i("Upload Data", jsonParam.toString());
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                InputStream in = new BufferedInputStream(connection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                Log.i("UPLOAD", "Upload success: " + result.toString());
            } else {
                Log.i("UPLOAD", "Upload error: " + responseCode);
            }

        } catch (Exception e) {
            Log.e("Upload", "Upload error", e);
            throw new ConnectionException("upload connection error");
        }
    }

    // Sync Method (username)
    public static String sync(String username) throws ConnectionException {
        String endpoint = "/sync";
        try {
            URL url = new URL(BASE_URL + endpoint);
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setSSLSocketFactory(TrustAllCertificates.getTrustAllSocketFactory());

            JSONObject requestJson = new JSONObject();
            requestJson.put("username", username);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestJson.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    JSONObject responseJson = new JSONObject(response.toString());

                    if (responseJson.has("encrypted_data")) {
                        String encryptedData = responseJson.getString("encrypted_data");

                        try {
                            android.util.Base64.decode(encryptedData, android.util.Base64.DEFAULT);
                        } catch (IllegalArgumentException e) {
                            throw new ConnectionException("Invalid Base64 in encrypted_data");
                        }

                        return encryptedData;
                    } else {
                        throw new ConnectionException("Username does not exist");
                    }
                }
            } else {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                    throw new ConnectionException("Sync failed with response: " + errorResponse);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "SYNC", e);
            throw new ConnectionException("sync connection error");
        }
    }

    // Create User Method
    public static String [] createUser() throws ConnectionException {
        String endpoint = "/create_user";
        try {
            URL url = new URL(BASE_URL + endpoint);
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setSSLSocketFactory(TrustAllCertificates.getTrustAllSocketFactory());

            String jsonInputString = "{}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                String credentials = response.toString().split("\"credentials\":\\s*\"")[1].split("\"")[0];
                String[] parts = new String(Base64.decode(credentials, Base64.DEFAULT)).split(":");
                String username = parts[0];
                String skk = parts[1];
                return new String[]{username, skk};
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot Create User: " + e.getMessage(), e);
           throw new ConnectionException("cannot create user: " + e.getMessage());
        }
    }
    private static final String TAG = "======::=====SyncConnection";
}

