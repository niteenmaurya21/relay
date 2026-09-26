package com.niteen.relay.service;


import com.niteen.relay.entity.Run;
import com.niteen.relay.entity.RunStatus;
import com.niteen.relay.entity.Workflow;
import com.niteen.relay.repository.RunRepository;
import com.niteen.relay.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Service
public class RunService {

    private final RunRepository runRepository;
    private final WorkflowRepository workflowRepository;
    private final JsonMapper jsonMapper;
    private final QueueJobService queueJobService;

    public RunService(RunRepository runRepository, WorkflowRepository workflowRepository, JsonMapper jsonMapper, QueueJobService queueJobService) {
        this.runRepository = runRepository;
        this.workflowRepository = workflowRepository;
        this.jsonMapper = jsonMapper;
        this.queueJobService = queueJobService;
    }

    public Run trigger(String workflowId, JsonNode input) {

        Workflow workflow = workflowRepository.findById(workflowId).orElseThrow(
                () -> new RuntimeException("Workflow not found for "+workflowId)
        );

        if(!"PUBLISHED".equals(workflow.getStatus().name())) {
            throw new RuntimeException("Workflow status is not PUBLISHED: "+ workflowId );
        }

        Run run = new Run();
        run.setRunId(UUID.randomUUID().toString());
        run.setWorkflowId(workflowId);
        run.setStatus(RunStatus.QUEUED);
        run.setTriggerType("manual");
        run.setTriggerInput(
                jsonMapper.writeValueAsString(input)
        );

        run.setDefinitionSnapshot(workflow.getDefinition());

        run.setStepsExecuted(0);
        run.setAiTokensUsed(0L);

        runRepository.save(run);

        queueJobService.enqueue(run.getRunId());

        return run;

    }
}
