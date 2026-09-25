package com.niteen.relay.config;


import com.niteen.relay.entity.Workflow;
import com.niteen.relay.entity.WorkflowStatus;
import com.niteen.relay.repository.WorkflowRepository;
import com.niteen.relay.validation.WorkflowValidator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.time.LocalDateTime;

@Component
public class SeedWorkflowLoader implements CommandLineRunner {

    private final WorkflowRepository workflowRepository;
    private final JsonMapper jsonMapper;
    public final WorkflowValidator workflowValidator;

    public SeedWorkflowLoader(WorkflowRepository workflowRepository, JsonMapper jsonMapper, WorkflowValidator workflowValidator) {
        this.workflowRepository = workflowRepository;
        this.jsonMapper = jsonMapper;
        this.workflowValidator = workflowValidator;
    }

    @Override
    public void run(String... args){
        try {
            ClassPathResource resource =
                    new ClassPathResource("data/seed_workflows.json");

            try (InputStream inputStream = resource.getInputStream()) {
                JsonNode root= jsonMapper.readTree(inputStream);
                JsonNode workflows = root.get("workflows");

                for(JsonNode workflowNode : workflows){
                    String workflowId = workflowNode.path("id").asText();
                if(workflowRepository.existsById(workflowId)){
                    continue;
                }

                String definationJson = jsonMapper.writeValueAsString(workflowNode);

                Workflow workflow = new  Workflow();
                workflow.setId(workflowId);
                workflow.setName(workflowNode.path("name").asText());
                workflow.setDescription(workflowNode.path("description").asText());
                workflow.setDefinition(definationJson);
                workflow.setStatus(WorkflowStatus.PUBLISHED);

                    LocalDateTime now = LocalDateTime.now();
                    workflow.setCreatedAt(now);
                    workflow.setUpdatedAt(now);

                    workflowRepository.save(workflow);
                }
            }
        }
        catch(Exception e){
            throw new IllegalStateException("Could not load seed workflows ", e);
        }
    }
}

