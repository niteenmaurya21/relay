package com.niteen.relay.service;

import com.niteen.relay.dto.WorkflowDefinitionRequest;
import com.niteen.relay.exception.WorkflowValidationException;
import com.niteen.relay.entity.Workflow;
import com.niteen.relay.entity.WorkflowStatus;
import com.niteen.relay.repository.WorkflowRepository;
import com.niteen.relay.validation.WorkflowValidator;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowValidator workflowValidator;
    private final JsonMapper jsonMapper;

    public WorkflowService(WorkflowRepository workflowRepository,
                           WorkflowValidator workflowValidator,
                           JsonMapper jsonMapper) {
        this.workflowRepository = workflowRepository;
        this.workflowValidator = workflowValidator;
        this.jsonMapper = jsonMapper;
    }

    public Workflow createWorkflow(
            Workflow workflow
    ) {

        workflow.setStatus(WorkflowStatus.DRAFT);

        LocalDateTime now = LocalDateTime.now();
        workflow.setCreatedAt(now);
        workflow.setUpdatedAt(now);

        return workflowRepository.save(workflow);
    }

    public List<Workflow> findAllByStatus(WorkflowStatus status) {
        return workflowRepository.findAll();
    }

    public Workflow getWorkflowById(String workflowid) {
        return workflowRepository.findById(workflowid)
                .orElseThrow(()->
                        new RuntimeException("Workflow with id: "+workflowid+" not found"));
    }

    public List<Workflow> getAllWorkflows() {
        return workflowRepository.findAll();
    }

    public Workflow publishWorkflow(String workflowId) {

        Workflow workflow = getWorkflowById(workflowId);

        try {
            WorkflowDefinitionRequest definition =
                    jsonMapper.readValue(
                            workflow.getDefinition(),
                            WorkflowDefinitionRequest.class
                    );

            workflowValidator.validateWorkflow(definition);

            workflow.setStatus(WorkflowStatus.PUBLISHED);

            workflow.setUpdatedAt(LocalDateTime.now());

            return workflowRepository.save(workflow);

        } catch (WorkflowValidationException e) {
            throw e;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not read workflow definition",
                    e
            );
        }
    }

}
