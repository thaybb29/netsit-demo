package com.proyecto.netsit_demo.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class PythonNetworkClientService {

    @Value("${netsit.python.url:http://127.0.0.1:5000}")
    private String pythonUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PythonNetworkClientService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode escanearRed(String subnet) throws Exception {
        if (subnet == null || subnet.isBlank()) {
            throw new IllegalArgumentException("La subred no puede estar vacía.");
        }

        // Construcción explícita del objeto JSON {"subnet": "..."}
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("subnet", subnet.trim());

        String json = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(pythonUrl + "/api/network/scan"))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofSeconds(130))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Python respondió HTTP " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readTree(response.body());
    }
}