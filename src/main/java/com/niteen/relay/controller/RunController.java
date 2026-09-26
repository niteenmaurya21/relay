package com.niteen.relay.controller;

import com.niteen.relay.entity.Run;
import com.niteen.relay.service.RunService;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/workflows")
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @PostMapping("/{workflowId}/trigger")
    public Run trigger(
            @PathVariable String workflowId,
            @RequestBody JsonNode body
    ) {
        JsonNode input = body.path("input");

        return runService.trigger(workflowId, input);
    }
}