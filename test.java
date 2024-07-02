package com.test;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final int requestLimit;
    private final TimeUnit timeUnit;
    private final OkHttpClient httpClient;
    private static final Gson gson = new Gson();
    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final RateLimiter rateLimiter;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.rateLimiter = new RateLimiter(timeUnit, requestLimit);
        this.httpClient = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public synchronized Response createDocument(Object document, String signature) throws IOException {
        // Checking the request limit
        if (!rateLimiter.acquire()) {
            return new Response.Builder()
                    .request(new Request.Builder().url(URL).build())
                    .protocol(Protocol.HTTP_1_1)
                    .code(429) // HTTP 429 Too Many Requests
                    .message("Too Many Requests - please try again later.")
                    .build();
        }

        // Converting the document object to JSON format
        String jsonDocument = gson.toJson(document);

        // Building HTTP request
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(jsonDocument, JSON);
        Request request = new Request.Builder()
                .url(URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + signature)
                .build();

        // Executing HTTP request and handle response
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to create document: " + response);
            }
            return response;
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Document {
        Description description;
        String doc_id;
        String doc_status;
        String doc_type;
        // I ignored "109" value, assuming it is a typo in the test description
        boolean importRequest;
        String owner_inn;
        String participant_inn;
        String producer_inn;
        String production_date;
        String production_type;
        List<Product> products;
        String reg_date;
        String reg_number;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Description {
        String participantInn;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Product {
        String certificate_document;
        String certificate_document_date;
        String certificate_document_number;
        String owner_inn;
        String producer_inn;
        String production_date;
        String tnved_code;
        String uit_code;
        String uitu_code;
    }

    private static class RateLimiter {
        private final long rateLimitNanos;
        private long lastRequestTimeNanos = 0;

        public RateLimiter(TimeUnit unit, int requests) {
            this.rateLimitNanos = unit.toNanos(requests);
        }

        public synchronized boolean acquire() {
            long currentTimeNanos = System.nanoTime();
            long elapsedNanos = currentTimeNanos - lastRequestTimeNanos;
            if (elapsedNanos >= rateLimitNanos) {
                lastRequestTimeNanos = currentTimeNanos;
                return true;
            }
            return false;
        }
    }

    // the following code is just to show how the class would work with the given document
    public static void main(String[] args) {
        String signature = "signature";
        TimeUnit timeUnit = TimeUnit.MINUTES;
        int requestLimit = 60;

        CrptApi crptApi = new CrptApi(timeUnit, requestLimit);

        Description description = new Description();
        description.setParticipantInn("string");

        Product product = new Product();
        product.setCertificate_document("string");
        product.setCertificate_document_date("2020-01-23");
        product.setCertificate_document_number("string");
        product.setOwner_inn("string");
        product.setProducer_inn("string");
        product.setProduction_date("2020-01-23");
        product.setTnved_code("string");
        product.setUit_code("string");
        product.setUitu_code("string");

        List<Product> products = new ArrayList<>();
        products.add(product);

        Document document = new Document();
        document.setDescription(description);
        document.setDoc_id("string");
        document.setDoc_status("string");
        document.setDoc_type("LP_INTRODUCE_GOODS");
        document.setImportRequest(true);
        document.setOwner_inn("string");
        document.setParticipant_inn("string");
        document.setProducer_inn("string");
        document.setProduction_date("2020-01-23");
        document.setProduction_type("string");
        document.setProducts(products);
        document.setReg_date("2020-01-23");
        document.setReg_number("string");

        try (Response response = crptApi.createDocument(document, signature)) {
            // TODO Process the response here (parse JSON body of the response)
        } catch (IOException e) {
            // TODO implement logging here
            e.printStackTrace();
        }
    }
}
