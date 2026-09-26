package com.niteen.relay.service;


import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class NotifyNodeExecutor {

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public NotifyNodeExecutor(RestClient restClient,
                              JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
    }

    public JsonNode execute(
            String channel,
            String to,
            String subject,
            String message,
            String idempotencyKey
    ) {

        ObjectNode requestBody = jsonMapper.createObjectNode();

        requestBody.put("to", to);
        requestBody.put("message", message);
        String endpoint;

        if ("email".equals(channel)) {
            endpoint = "/email/send";
        } else if ("chat".equals(channel)) {
            endpoint = "/chat/message";
        } else {
            throw new IllegalArgumentException(
                    "Unsupported notification channel: " + channel
            );
        }

        if("email".equals(channel) && subject != null) {
            requestBody.put("subject", subject);
        }


        return restClient
                .post()
                .uri(endpoint)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(jsonMapper.writeValueAsString(requestBody))
                .retrieve()
                .body(JsonNode.class);
    }

}
