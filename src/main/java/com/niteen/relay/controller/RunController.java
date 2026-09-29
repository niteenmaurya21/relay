package com.niteen.relay.controller;

import com.niteen.relay.entity.Run;
import com.niteen.relay.service.RunService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
public class RunController {

    private final RunService runService;
    public final JsonMapper jsonMapper;

    @Value("${relay.webhook.secret}")
    private String webhookSecret;

    public RunController(RunService runService
    , JsonMapper jsonMapper) {
        this.runService = runService;
        this.jsonMapper = jsonMapper;
    }

    @PostMapping("/workflows/{workflowId}/trigger")
    public Run trigger(
            @PathVariable String workflowId,
            @RequestBody JsonNode body
    ) {
        JsonNode input = body.path("input");

        return runService.trigger(workflowId, input, "manual");
    }

    @GetMapping("/runs/{runId}")
    public Run getRun(@PathVariable String runId) {
        return runService.getRun(runId);
    }

    @PostMapping("/hooks/{workflowId}")
    public ResponseEntity<?> webhook(
            @PathVariable String workflowId,
            @RequestHeader(value = "X-Relay-Secret", required = false) String secret,
            @RequestBody String body
    ) {


        if (secret == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("X-Relay-Secret header is missing");
        }

        if (!webhookSecret.equals(secret)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid webhook secret");
        }
        JsonNode payload;

        try {
            payload = jsonMapper.readTree(body);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body("Invalid JSON payload");
        }

        Run run = runService.trigger(
                workflowId,
                payload,
                "webhook"
        );

        return ResponseEntity.ok(run);
    }
}