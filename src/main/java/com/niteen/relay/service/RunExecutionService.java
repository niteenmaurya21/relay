package com.niteen.relay.service;


import com.niteen.relay.entity.Run;
import com.niteen.relay.entity.RunStatus;
import com.niteen.relay.entity.Step;
import com.niteen.relay.entity.StepStatus;
import com.niteen.relay.repository.RunRepository;
import com.niteen.relay.repository.StepRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RunExecutionService {

    private final RunRepository runRepository;
    private final JsonMapper jsonMapper;
    private final StepRepository stepRepository;
    private final TemplateResolver templateResolver;
    private final NotifyNodeExecutor notifyNodeExecutor;


    public RunExecutionService(RunRepository runRepository,
                               JsonMapper jsonMapper,
                               StepRepository stepRepository,
                               TemplateResolver templateResolver,
                               NotifyNodeExecutor notifyNodeExecutor) {
        this.runRepository = runRepository;
        this.jsonMapper = jsonMapper;
        this.stepRepository = stepRepository;
        this.templateResolver = templateResolver;
        this.notifyNodeExecutor = notifyNodeExecutor;
    }

    public void execute(String runId){

        Optional<Run> run = runRepository.findById(runId);

        if(run.isEmpty()) {
            return;
        }

        Run currentRun = run.get();

        currentRun.setStatus(RunStatus.RUNNING);
        currentRun.setStartedAt(LocalDateTime.now());

        runRepository.save(currentRun);



       try {

           //definition
           JsonNode definition = jsonMapper.readTree(
                   currentRun.getDefinitionSnapshot()
           );

           //input
           JsonNode input = jsonMapper.readTree(
                   currentRun.getTriggerInput()
           );

           String entryNodeId = definition.path("entry").asText();

           JsonNode nodes = definition.path("nodes");
           JsonNode entryNode= null;

           for(JsonNode node : nodes) {
               if (entryNodeId.equals(node.path("id").asText())) {

                   entryNode = node;
                   break;
               }
           }
               if(entryNode == null) {
                   throw new RuntimeException(
                           "Entry node not found: " +entryNodeId
                   );
               }

           String nodeType = entryNode.path("type").asText();
               if(!"notify".equals(nodeType)) {
                   throw new RuntimeException(
                           "Unsupported node type: " +nodeType
                   );
               }
               JsonNode params = entryNode.path("params");

               String channel = params.path("channel").asText();

           String toTemplate = params.path("to").asText();
               String to = templateResolver.resolve(toTemplate, input);

           String messageTemplate = params.path("message").asText();
           String message = templateResolver.resolve(messageTemplate, input);

           String subject = null;

           if (params.has("subject")) {
               String subjectTemplate = params.path("subject").asText();
               subject = templateResolver.resolve(subjectTemplate, input);
           }

           Step step = new Step();

           step.setRunId(runId);
           step.setNodeId(entryNodeId);
           step.setNodeType(nodeType);
           step.setSequenceNumber(currentRun.getStepsExecuted() + 1);
           step.setStatus(StepStatus.PENDING);
           step.setAttempt(0);

           String idempotencyKey = runId + ":" + entryNodeId;
           step.setIdempotencyKey(idempotencyKey);

           stepRepository.save(step);

           step.setStatus(StepStatus.RUNNING);
           step.setStartedAt(LocalDateTime.now());

           stepRepository.save(step);
           //persist STEP as PENDING, then transition it to RUNNING

           ObjectNode resolvedInput = jsonMapper.createObjectNode();

           resolvedInput.put("channel", channel);
           resolvedInput.put("to", to);
           resolvedInput.put("message", message);

           if (subject != null) {
               resolvedInput.put("subject", subject);
           }

           step.setResolvedInput(
                   jsonMapper.writeValueAsString(resolvedInput)
           );

           stepRepository.save(step);

           JsonNode output = notifyNodeExecutor.execute(
                   channel,
                   to,
                   subject,
                   message,
                   step.getIdempotencyKey()
           );

           step.setOutput(
                   jsonMapper.writeValueAsString(output)
           );

           step.setStatus(StepStatus.SUCCEEDED);
           step.setDurationMs(
                   java.time.Duration.between(
                           step.getStartedAt(),
                           LocalDateTime.now()
                   ).toMillis()
           );
           stepRepository.save(step);

           String nextNodeId = entryNode.path("next").isNull()
                   ? null
                   : entryNode.path("next").asText();

           currentRun.setStepsExecuted(
                   currentRun.getStepsExecuted() + 1
           );

           currentRun.setCurrentNodeId(nextNodeId);

           runRepository.save(currentRun);

       }
       catch (Exception e) {
           throw new RuntimeException("Workflow execution failed for run: "+runId,e);
       }

    }

}
