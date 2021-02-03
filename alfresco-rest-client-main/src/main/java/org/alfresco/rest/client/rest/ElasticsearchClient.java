package org.alfresco.rest.client.rest;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.apache.chemistry.opencmis.commons.impl.json.JSONObject;
import org.apache.chemistry.opencmis.commons.impl.json.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class ElasticsearchClient {

    @Value("${elasticsearch.server.url}")
    String baseUrl;

    OkHttpClient client;
    String authHeader;

    @PostConstruct
    public void init() {
        client = new OkHttpClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public int getDocumentCount() {

        Request request = new Request.Builder()
                .url(baseUrl + "/alfresco/_count")
                .build();

        Call call = client.newCall(request);
        ResponseBody responseBody = null;
        try {
            responseBody = call.execute().body();
            JSONObject json = (JSONObject) new JSONParser().parse(responseBody.string());
            return ((BigInteger) json.get("count")).intValue();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
