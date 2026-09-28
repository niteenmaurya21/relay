package com.niteen.relay.controller;

import com.niteen.relay.entity.Run;
import com.niteen.relay.service.RunService;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
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
    public Run webhook(
            @PathVariable String workflowId,
            @RequestHeader("X-Relay-Secret") String secret,
            @RequestBody JsonNode body
    ) {
        return runService.trigger(workflowId, body, "webhook");
    }
}