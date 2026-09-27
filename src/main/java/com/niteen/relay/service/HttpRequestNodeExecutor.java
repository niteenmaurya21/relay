package com.niteen.relay.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class HttpRequestNodeExecutor {

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public HttpRequestNodeExecutor(
            RestClient restClient,
            JsonMapper jsonMapper) {

        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
    }

    public JsonNode execute(
            String method,
            String url,
            JsonNode body,
            String idempotencyKey) {

        if (!"POST".equalsIgnoreCase(method)) {
            throw new IllegalArgumentException(
                    "Unsupported HTTP method: " + method
            );
        }

        JsonNode response = restClient.post()
                .uri(url)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(body))
                .retrieve()
                .body(JsonNode.class);

        ObjectNode result = jsonMapper.createObjectNode();
        result.set("body", response);

        return result;
    }
}