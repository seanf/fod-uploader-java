package com.fortify.fod.fodapi;

import com.fortify.fod.fodapi.controllers.ReleaseController;
import com.fortify.fod.fodapi.controllers.StaticScanController;
import com.fortify.fod.parser.Proxy;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import okhttp3.Credentials;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;


public class FodApi {
    public final String GRANT_TYPE_PASSWORD = "password";
    public final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    private String baseUrl;
    private OkHttpClient client;
    private String token;

    // Used for automatically re-authenticating if need be
    private String tenantCode = "";
    private String username = "";
    private String password = "";
    private String grantType = "";

    private final int CONNECTION_TIMEOUT = 10;
    private final int WRITE_TIMEOUT = 30;
    private final int READ_TIMEOUT = 30;

    private StaticScanController staticScanController;
    public StaticScanController getStaticScanController() { return staticScanController; }
    private ReleaseController releaseController;
    public ReleaseController getReleaseController() { return releaseController; }

    public FodApi(String url, Proxy clProxy) {
        baseUrl = url;
        client = Create(clProxy);

        staticScanController = new StaticScanController(this);
        releaseController = new ReleaseController(this);
    }

    public void authenticate(String tenantCode, String username, String password, String grantType) {
        try {
            // Build the form body
            FormBody.Builder formBodyBuilder = new FormBody.Builder().add("scope", "https://hpfod.com/tenant");
            // Has username/password stuff
            if (grantType.equals(GRANT_TYPE_PASSWORD)) {
                formBodyBuilder.add("grant_type", GRANT_TYPE_PASSWORD)
                        .add("username", tenantCode + "\\" + username)
                        .add("password", password);
            // Has api key/secret
            } else {
                formBodyBuilder.add("grant_type", GRANT_TYPE_CLIENT_CREDENTIALS)
                        .add("client_id", username)
                        .add("client_secret", password);
            }
            RequestBody formBody = formBodyBuilder.build();

            // Create the request
            Request request = new Request.Builder()
                    .url(baseUrl + "/oauth/token")
                    .post(formBody)
                    .build();


            // Get the response
            Response response = client.newCall(request).execute();

            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response);

            // Read the results and close the response
            String content = IOUtils.toString(response.body().byteStream(), "utf-8");
            response.body().close();

            // Parse the Response
            JsonParser parser = new JsonParser();
            JsonObject obj = parser.parse(content).getAsJsonObject();
            token = obj.get("access_token").getAsString();
            // You've authenticated once, so we can use this again during polling if need be
            this.tenantCode = tenantCode;
            this.username = username;
            this.password = password;
            this.grantType = grantType;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void authenticate() {
        this.authenticate(tenantCode, username, password, grantType);
    }

    public void retireToken() {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/oauth/retireToken")
                    .addHeader("Authorization","Bearer " + token)
                    .get()
                    .build();
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                // Read the results and close the response
                String content = IOUtils.toString(response.body().byteStream(), "utf-8");
                response.body().close();

                JsonParser parser = new JsonParser();
                JsonObject obj = parser.parse(content).getAsJsonObject();
                String messageResponse = obj.get("message").getAsString();

                System.out.println("Retiring Token : " + messageResponse);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private OkHttpClient Create(Proxy clProxy) {
        OkHttpClient.Builder c = new OkHttpClient().newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS);

        // If there's no proxy just create a normal client
        if(clProxy == null)
            return c.build();

        // Build out the proxy
        OkHttpClient.Builder builder = c
            .proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(clProxy.getProxyUri().getHost(), clProxy.getProxyUri().getPort())));

        if (clProxy.hasUsername() && clProxy.hasPassword()) {
            // Include NTDomain and NTWorkstation in auth
            Authenticator proxyAuthenticator;
            String credentials;

            if (clProxy.hasNTDomain() && clProxy.hasNTWorkstation()) {
                credentials = new NTCredentials(clProxy.getUsername(), clProxy.getPassword(),
                        clProxy.getNTWorkstation(), clProxy.getNTDomain()).toString();
            // Just use username and password
            } else {
                credentials = Credentials.basic(clProxy.getUsername(), clProxy.getPassword());
            }
            proxyAuthenticator = (route, response) -> response.request().newBuilder()
                    .header("Proxy-Authorization", credentials)
                    .build();
            builder.proxyAuthenticator(proxyAuthenticator);
        }
        return builder.build();
    }

    public String getToken() { return token; }
    public OkHttpClient getClient() { return client; }
    public String getBaseUrl() { return baseUrl; }
}
