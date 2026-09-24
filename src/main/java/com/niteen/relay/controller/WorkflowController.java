package com.niteen.relay.controller;


import com.niteen.relay.dto.WorkflowDefinitionRequest;
import com.niteen.relay.entity.Workflow;
import com.niteen.relay.service.WorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;


@RestController
@RequestMapping("/workflows")
public class WorkflowController {

    public final WorkflowService workflowService;
    public final JsonMapper jsonMapper;

    public WorkflowController(WorkflowService workflowService, JsonMapper jsonMapper) {
        this.workflowService = workflowService;
        this.jsonMapper = jsonMapper;
    }

    @PostMapping
    public ResponseEntity<Workflow> createWorkflow(@RequestBody WorkflowDefinitionRequest request){

        try {
            String definitionJson = jsonMapper.writeValueAsString(request);

            Workflow workflow = new Workflow();

            workflow.setId(request.getId());
            workflow.setName(request.getName());
            workflow.setDescription(request.getDescription());
            workflow.setDefinition(definitionJson);

            Workflow savedWorkflow = workflowService.createWorkflow(workflow);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(savedWorkflow);
        }
        catch (JacksonException e) {
            throw new RuntimeException("Could not create workflow definition", e);
        }
    }

    @PostMapping("/{workflowId}/publish")
    public ResponseEntity<Workflow> publishWorkflow(@PathVariable String workflowId){

        Workflow publishedWorkflow =
                workflowService.publishWorkflow(workflowId);


        return ResponseEntity.ok(publishedWorkflow);
    }

    @GetMapping
    public ResponseEntity<List<Workflow>> getAllWorkflow(){
        List<Workflow> workflows = workflowService.getAllWorkflows();
        return ResponseEntity.ok(workflows);
    }

}
