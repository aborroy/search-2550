package org.alfresco.rest.client.rest;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class RestClient {

    static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Value("${alfresco.repository.user}")
    String alfrescoUser;
    @Value("${alfresco.repository.pass}")
    String alfrescoPass;
    @Value("${alfresco.repository.url}/s/bulkobj/mapobjects")
    String baseUrl;

    OkHttpClient client;
    String authHeader;

    @PostConstruct
    public void init() {

        client = new OkHttpClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();

        String auth = alfrescoUser + ":" + alfrescoPass;
        byte[] encodedAuth = Base64.getEncoder().encode(
                auth.getBytes(StandardCharsets.ISO_8859_1));
        authHeader = "Basic " + new String(encodedAuth);

    }

    public String createDocuments(String folderId, File jsonFile) {

        RequestBody jsonBody = RequestBody.create(JSON, jsonFile);
        Request request = new Request.Builder()
                .url(baseUrl + "/" + folderId + "?autoCreate=y")
                .post(jsonBody)
                .addHeader("Authorization", authHeader)
                .build();
        Call call = client.newCall(request);
        ResponseBody responseBody = null;
        try {
            responseBody = call.execute().body();
            return responseBody.string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
