package com.niteen.relay.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class OrderActionNodeExecutor {

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public OrderActionNodeExecutor(
            RestClient restClient,
            JsonMapper jsonMapper
    ) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
    }

    public JsonNode execute(
            String action,
            String orderId,
            Double amountUsd,
            String idempotencyKey
    ) {

        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException(
                    "Order action is required"
            );
        }

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException(
                    "Order ID is required"
            );
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency key is required"
            );
        }

        String endpoint;

        switch (action) {
            case "refund":
                endpoint = "/orders/" + orderId + "/refund";
                break;

            case "replacement":
                endpoint = "/orders/" + orderId + "/replacement";
                break;

            default:
                throw new IllegalArgumentException(
                        "Unsupported order action: " + action
                );
        }

        ObjectNode requestBody = jsonMapper.createObjectNode();

        if ("refund".equals(action) && amountUsd != null) {
            requestBody.put("amount_usd", amountUsd);
        }

        return restClient
                .post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(jsonMapper.writeValueAsString(requestBody))
                .retrieve()
                .body(JsonNode.class);
    }
}