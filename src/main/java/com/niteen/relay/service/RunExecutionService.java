package com.niteen.relay.service;

import com.niteen.relay.entity.*;
import com.niteen.relay.repository.ApprovalRepository;
import com.niteen.relay.repository.RunRepository;
import com.niteen.relay.repository.StepRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RunExecutionService {

    private final RunRepository runRepository;
    private final JsonMapper jsonMapper;
    private final StepRepository stepRepository;
    private final TemplateResolver templateResolver;
    private final NotifyNodeExecutor notifyNodeExecutor;
    private final QueueJobService queueJobService;
    private final DelayNodeExecutor delayNodeExecutor;
    private final HttpRequestNodeExecutor httpRequestNodeExecutor;
    private final ConditionNodeExecutor conditionNodeExecutor;
    private final ApprovalRepository approvalRepository;
    private final ApprovalNodeExecutor approvalNodeExecutor;
    private final OrderActionNodeExecutor orderActionNodeExecutor;

    public RunExecutionService(
            RunRepository runRepository,
            JsonMapper jsonMapper,
            StepRepository stepRepository,
            TemplateResolver templateResolver,
            NotifyNodeExecutor notifyNodeExecutor,
            QueueJobService queueJobService,
            DelayNodeExecutor delayNodeExecutor,
            HttpRequestNodeExecutor httpRequestNodeExecutor,
            ConditionNodeExecutor conditionNodeExecutor,
            ApprovalRepository approvalRepository,
            ApprovalNodeExecutor approvalNodeExecutor,
            OrderActionNodeExecutor orderActionNodeExecutor) {

        this.runRepository = runRepository;
        this.jsonMapper = jsonMapper;
        this.stepRepository = stepRepository;
        this.templateResolver = templateResolver;
        this.notifyNodeExecutor = notifyNodeExecutor;
        this.queueJobService = queueJobService;
        this.delayNodeExecutor = delayNodeExecutor;
        this.httpRequestNodeExecutor = httpRequestNodeExecutor;
        this.conditionNodeExecutor = conditionNodeExecutor;
        this.approvalRepository = approvalRepository;
        this.approvalNodeExecutor = approvalNodeExecutor;
        this.orderActionNodeExecutor = orderActionNodeExecutor;
    }

    public void execute(String runId) {

        Optional<Run> run = runRepository.findById(runId);

        if (run.isEmpty()) {
            return;
        }

        Run currentRun = run.get();

        currentRun.setStatus(RunStatus.RUNNING);

        if (currentRun.getStartedAt() == null) {
            currentRun.setStartedAt(LocalDateTime.now());
        }

        runRepository.save(currentRun);

        try {

            // Workflow definition snapshot
            JsonNode definition = jsonMapper.readTree(
                    currentRun.getDefinitionSnapshot()
            );

            // Original trigger input
            JsonNode input = jsonMapper.readTree(
                    currentRun.getTriggerInput()
            );
            Map<String, JsonNode> nodeOutputs = loadNodeOutputs(runId);

            /*
             * Resume from the persisted checkpoint.
             * If this is the first execution, start from the workflow entry.
             */
            String nodeId = currentRun.getCurrentNodeId();

            if (nodeId == null) {
                nodeId = definition.path("entry").asText();
            }

            // Find the current node
            JsonNode nodes = definition.path("nodes");
            JsonNode currentNode = null;

            for (JsonNode node : nodes) {
                if (nodeId.equals(node.path("id").asText())) {
                    currentNode = node;
                    break;
                }
            }

            if (currentNode == null) {
                throw new RuntimeException(
                        "Node not found: " + nodeId
                );
            }

            int maxSteps = definition
                    .path("limits")
                    .path("max_steps")
                    .asInt(20);

            if (currentRun.getStepsExecuted() >= maxSteps) {

                currentRun.setStatus(RunStatus.FAILED);
                currentRun.setError("Maximum step limit exceeded");
                currentRun.setFinishedAt(LocalDateTime.now());

                runRepository.save(currentRun);

                return;
            }

            int timeoutSeconds = definition
                    .path("limits")
                    .path("timeout_seconds")
                    .asInt(600);

            if (currentRun.getStartedAt() != null
                    && Duration.between(
                    currentRun.getStartedAt(),
                    LocalDateTime.now()
            ).getSeconds() >= timeoutSeconds) {

                currentRun.setStatus(RunStatus.FAILED);
                currentRun.setError("Workflow timeout exceeded");
                currentRun.setFinishedAt(LocalDateTime.now());

                runRepository.save(currentRun);

                return;
            }

            String nodeType = currentNode.path("type").asText();
            JsonNode params = currentNode.path("params");

            /*
             * ---------------------------------------------------------
             * DELAY NODE
             * ---------------------------------------------------------
             */
            if ("delay".equals(nodeType)) {

                long seconds = delayNodeExecutor.getDelaySeconds(
                        params.path("seconds").asLong()
                );

                Step step = new Step();

                step.setRunId(runId);
                step.setNodeId(nodeId);
                step.setNodeType(nodeType);
                step.setSequenceNumber(
                        currentRun.getStepsExecuted() + 1
                );
                step.setStatus(StepStatus.PENDING);
                step.setAttempt(0);
                step.setIdempotencyKey(runId + ":" + nodeId);

                // Persist step before execution
                stepRepository.save(step);

                step.setStatus(StepStatus.RUNNING);
                step.setStartedAt(LocalDateTime.now());

                stepRepository.save(step);

                // Persist resolved delay input
                ObjectNode resolvedInput =
                        jsonMapper.createObjectNode();

                resolvedInput.put("seconds", seconds);

                step.setResolvedInput(
                        jsonMapper.writeValueAsString(resolvedInput)
                );

                stepRepository.save(step);

                /*
                 * Move the run checkpoint to the next node.
                 */
                String nextNodeId = currentNode.path("next").isNull()
                        ? null
                        : currentNode.path("next").asText();

                currentRun.setStepsExecuted(
                        currentRun.getStepsExecuted() + 1
                );

                currentRun.setCurrentNodeId(nextNodeId);

                runRepository.save(currentRun);

                /*
                 * The delay itself is represented by the queue's
                 * availableAt timestamp. We do not block the worker
                 * with Thread.sleep().
                 */
                step.setStatus(StepStatus.SUCCEEDED);
                step.setDurationMs(0L);

                stepRepository.save(step);

                if (nextNodeId != null) {
                    queueJobService.enqueueAfter(runId, seconds);
                }

                return;
            }

            /*
             * ---------------------------------------------------------
             * CONDITION NODE
             * ---------------------------------------------------------
             */
            if ("condition".equals(nodeType)) {

                String leftTemplate = params.path("left").asText();
                String op = params.path("op").asText();
                String rightTemplate = params.path("right").asText();

                String left = templateResolver.resolve(
                        leftTemplate,
                        input,
                        nodeOutputs
                );

                String right = templateResolver.resolve(
                        rightTemplate,
                        input,
                        nodeOutputs
                );

                Step step = new Step();

                step.setRunId(runId);
                step.setNodeId(nodeId);
                step.setNodeType(nodeType);
                step.setSequenceNumber(
                        currentRun.getStepsExecuted() + 1
                );
                step.setStatus(StepStatus.PENDING);
                step.setAttempt(0);
                step.setIdempotencyKey(runId + ":" + nodeId);

                // Persist PENDING step
                stepRepository.save(step);

                // Transition to RUNNING
                step.setStatus(StepStatus.RUNNING);
                step.setStartedAt(LocalDateTime.now());

                stepRepository.save(step);

                // Persist resolved condition input
                ObjectNode resolvedInput =
                        jsonMapper.createObjectNode();

                resolvedInput.put("left", left);
                resolvedInput.put("op", op);
                resolvedInput.put("right", right);

                step.setResolvedInput(
                        jsonMapper.writeValueAsString(resolvedInput)
                );

                stepRepository.save(step);

                // Evaluate condition
                boolean result = conditionNodeExecutor.evaluate(
                        left,
                        op,
                        right
                );

                // Persist condition output
                ObjectNode output =
                        jsonMapper.createObjectNode();

                output.put("result", result);

                step.setOutput(
                        jsonMapper.writeValueAsString(output)
                );

                step.setStatus(StepStatus.SUCCEEDED);

                step.setDurationMs(
                        Duration.between(
                                step.getStartedAt(),
                                LocalDateTime.now()
                        ).toMillis()
                );

                stepRepository.save(step);

                // Choose branch
                String nextNodeId;

                if (result) {
                    nextNodeId = currentNode.path("on_true").asText();
                } else {
                    nextNodeId = currentNode.path("on_false").asText();
                }

                currentRun.setStepsExecuted(
                        currentRun.getStepsExecuted() + 1
                );

                currentRun.setCurrentNodeId(nextNodeId);

                runRepository.save(currentRun);

                if (nextNodeId != null && !nextNodeId.isBlank()) {
                    queueJobService.enqueue(runId);
                }

                return;
            }

            /*
             * ---------------------------------------------------------
             * APPROVAL NODE
             * ---------------------------------------------------------
             */
            if ("approval".equals(nodeType)) {

                String messageTemplate = params.path("message").asText();

                String message = templateResolver.resolve(
                        messageTemplate,
                        input,
                        nodeOutputs
                );

                message = approvalNodeExecutor.resolveMessage(message);


                Optional<Approval> existingApproval =
                        approvalRepository.findByRunIdAndNodeIdAndStatus(
                                runId,
                                nodeId,
                                ApprovalStatus.PENDING
                        );

                if (existingApproval.isPresent()) {
                    currentRun.setStatus(RunStatus.WAITING_APPROVAL);
                    currentRun.setCurrentNodeId(nodeId);
                    runRepository.save(currentRun);
                    return;
                }

                // Create approval request
                Approval approval = new Approval();
                approval.setRunId(runId);
                approval.setNodeId(nodeId);
                approval.setMessage(message);
                approval.setStatus(ApprovalStatus.PENDING);

                approvalRepository.save(approval);

                // Create step
                Step step = new Step();
                step.setRunId(runId);
                step.setNodeId(nodeId);
                step.setNodeType(nodeType);
                step.setSequenceNumber(
                        currentRun.getStepsExecuted() + 1
                );
                step.setStatus(StepStatus.WAITING_APPROVAL);
                step.setAttempt(0);
                step.setIdempotencyKey(runId + ":" + nodeId);

                ObjectNode resolvedInput =
                        jsonMapper.createObjectNode();

                resolvedInput.put("message", message);

                step.setResolvedInput(
                        jsonMapper.writeValueAsString(resolvedInput)
                );

                stepRepository.save(step);

                // Pause the run
                currentRun.setStatus(RunStatus.WAITING_APPROVAL);
                currentRun.setCurrentNodeId(nodeId);

                runRepository.save(currentRun);

                // Do NOT enqueue another job.
                return;
            }

            /*
             * ---------------------------------------------------------
             * ORDER ACTION NODE
             * ---------------------------------------------------------
             */
            if ("order_action".equals(nodeType)) {

                /*
                 * order_action is a sensitive side effect.
                 * It is only allowed when an approval was completed
                 * earlier in this same run.
                 */
                List<Approval> approvedApprovals =
                        approvalRepository.findByRunIdAndStatus(
                                runId,
                                ApprovalStatus.APPROVED
                        );

                if (approvedApprovals.isEmpty()) {
                    throw new RuntimeException(
                            "Order action requires an approved approval in the same run"
                    );
                }

                String action = params.path("action").asText();

                String orderIdTemplate =
                        params.path("order_id").asText();

                String orderId = templateResolver.resolve(
                        orderIdTemplate,
                        input,
                        nodeOutputs
                );

                Double amountUsd = null;

                if (params.has("amount_usd")) {
                    amountUsd = params.path("amount_usd").asDouble();
                }

                String idempotencyKey = runId + ":" + nodeId;

                Step step = new Step();

                step.setRunId(runId);
                step.setNodeId(nodeId);
                step.setNodeType(nodeType);
                step.setSequenceNumber(
                        currentRun.getStepsExecuted() + 1
                );
                step.setStatus(StepStatus.PENDING);
                step.setAttempt(0);
                step.setIdempotencyKey(idempotencyKey);

                /*
                 * Persist PENDING before executing the side effect.
                 */
                stepRepository.save(step);

                step.setStatus(StepStatus.RUNNING);
                step.setStartedAt(LocalDateTime.now());

                stepRepository.save(step);

                /*
                 * Persist the fully resolved order action input.
                 */
                ObjectNode resolvedInput =
                        jsonMapper.createObjectNode();

                resolvedInput.put("action", action);
                resolvedInput.put("order_id", orderId);

                if (amountUsd != null) {
                    resolvedInput.put("amount_usd", amountUsd);
                }

                step.setResolvedInput(
                        jsonMapper.writeValueAsString(resolvedInput)
                );

                stepRepository.save(step);

                /*
                 * Execute the sensitive order action.
                 */
                JsonNode output = orderActionNodeExecutor.execute(
                        action,
                        orderId,
                        amountUsd,
                        idempotencyKey
                );

                /*
                 * Persist output and successful completion.
                 */
                step.setOutput(
                        jsonMapper.writeValueAsString(output)
                );

                step.setStatus(StepStatus.SUCCEEDED);

                step.setDurationMs(
                        Duration.between(
                                step.getStartedAt(),
                                LocalDateTime.now()
                        ).toMillis()
                );

                stepRepository.save(step);

                /*
                 * Move to the next node.
                 */
                String nextNodeId = currentNode.path("next").isNull()
                        ? null
                        : currentNode.path("next").asText();

                currentRun.setStepsExecuted(
                        currentRun.getStepsExecuted() + 1
                );

                currentRun.setCurrentNodeId(nextNodeId);

                if (nextNodeId == null) {
                    currentRun.setStatus(RunStatus.SUCCEEDED);
                    currentRun.setFinishedAt(LocalDateTime.now());
                }

                runRepository.save(currentRun);

                if (nextNodeId != null) {
                    queueJobService.enqueue(runId);
                }

                return;
            }

            /*
             * ---------------------------------------------------------
             * NOTIFY NODE
             * ---------------------------------------------------------
             */
            if ("http_request".equals(nodeType)) {

                String method = params.path("method").asText();
                String url = params.path("url").asText();

                JsonNode bodyTemplate = params.path("body");

                JsonNode resolvedBody = templateResolver.resolveJson(
                        bodyTemplate,
                        input
                );

                Step step = new Step();

                step.setRunId(runId);
                step.setNodeId(nodeId);
                step.setNodeType(nodeType);
                step.setSequenceNumber(
                        currentRun.getStepsExecuted() + 1
                );
                step.setStatus(StepStatus.PENDING);
                step.setAttempt(0);

                String idempotencyKey = runId + ":" + nodeId;

                step.setIdempotencyKey(idempotencyKey);

                stepRepository.save(step);

                step.setStatus(StepStatus.RUNNING);
                step.setStartedAt(LocalDateTime.now());

                stepRepository.save(step);

                /*
                 * Persist the resolved HTTP request.
                 */
                ObjectNode resolvedInput =
                        jsonMapper.createObjectNode();

                resolvedInput.put("method", method);
                resolvedInput.put("url", url);
                resolvedInput.set("body", resolvedBody);

                step.setResolvedInput(
                        jsonMapper.writeValueAsString(resolvedInput)
                );

                stepRepository.save(step);

                /*
                 * Execute HTTP request.
                 */
                JsonNode output = httpRequestNodeExecutor.execute(
                        method,
                        url,
                        resolvedBody,
                        idempotencyKey
                );

                /*
                 * Persist HTTP response.
                 */
                step.setOutput(
                        jsonMapper.writeValueAsString(output)
                );

                step.setStatus(StepStatus.SUCCEEDED);

                step.setDurationMs(
                        Duration.between(
                                step.getStartedAt(),
                                LocalDateTime.now()
                        ).toMillis()
                );

                stepRepository.save(step);

                /*
                 * Move to the next node.
                 */
                String nextNodeId = currentNode.path("next").isNull()
                        ? null
                        : currentNode.path("next").asText();

                currentRun.setStepsExecuted(
                        currentRun.getStepsExecuted() + 1
                );

                currentRun.setCurrentNodeId(nextNodeId);

                runRepository.save(currentRun);

                if (nextNodeId != null) {
                    queueJobService.enqueue(runId);
                }

                return;
            }

            if (!"notify".equals(nodeType)) {
                throw new RuntimeException(
                        "Unsupported node type: " + nodeType
                );
            }

            String channel = params.path("channel").asText();

            String toTemplate = params.path("to").asText();

            String to = templateResolver.resolve(
                    toTemplate,
                    input
            );

            String messageTemplate = params.path("message").asText();

            String message = templateResolver.resolve(
                    messageTemplate,
                    input,
                    nodeOutputs
            );

            String subject = null;

            if (params.has("subject")) {

                String subjectTemplate =
                        params.path("subject").asText();

                subject = templateResolver.resolve(
                        subjectTemplate,
                        input
                );
            }

            Step step = new Step();

            step.setRunId(runId);
            step.setNodeId(nodeId);
            step.setNodeType(nodeType);
            step.setSequenceNumber(
                    currentRun.getStepsExecuted() + 1
            );
            step.setStatus(StepStatus.PENDING);
            step.setAttempt(0);

            /*
             * Stable idempotency key:
             *
             * runId:nodeId
             */
            String idempotencyKey = runId + ":" + nodeId;

            step.setIdempotencyKey(idempotencyKey);

            // Persist PENDING step
            stepRepository.save(step);

            // Transition to RUNNING
            step.setStatus(StepStatus.RUNNING);
            step.setStartedAt(LocalDateTime.now());

            stepRepository.save(step);

            /*
             * Persist the fully resolved node input.
             */
            ObjectNode resolvedInput =
                    jsonMapper.createObjectNode();

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

            /*
             * Execute the external notification.
             */
            JsonNode output = notifyNodeExecutor.execute(
                    channel,
                    to,
                    subject,
                    message,
                    step.getIdempotencyKey()
            );

            /*
             * Persist output and successful completion.
             */
            step.setOutput(
                    jsonMapper.writeValueAsString(output)
            );

            step.setStatus(StepStatus.SUCCEEDED);

            step.setDurationMs(
                    Duration.between(
                            step.getStartedAt(),
                            LocalDateTime.now()
                    ).toMillis()
            );

            stepRepository.save(step);

            /*
             * Move checkpoint to the next node.
             */
            String nextNodeId = currentNode.path("next").isNull()
                    ? null
                    : currentNode.path("next").asText();

            currentRun.setStepsExecuted(
                    currentRun.getStepsExecuted() + 1
            );

            currentRun.setCurrentNodeId(nextNodeId);

            if (nextNodeId == null) {
                currentRun.setStatus(RunStatus.SUCCEEDED);
                currentRun.setFinishedAt(LocalDateTime.now());
            }

            runRepository.save(currentRun);

            /*
             * Only enqueue another job when there is another node.
             */
            if (nextNodeId != null) {
                queueJobService.enqueue(runId);
            }

        } catch (Exception e) {

            currentRun.setStatus(RunStatus.FAILED);
            currentRun.setError(
                    e.getCause() != null
                            ? e.getCause().getMessage()
                            : e.getMessage()
            );
            currentRun.setFinishedAt(LocalDateTime.now());

            runRepository.save(currentRun);

            throw new RuntimeException(
                    "Workflow execution failed for run: " + runId,
                    e
            );
        }
    }

    private Map<String, JsonNode> loadNodeOutputs(String runId) {

        Map<String, JsonNode> nodeOutputs = new HashMap<>();

        for (Step step : stepRepository.findByRunIdOrderBySequenceNumberAsc(runId)) {

            if (step.getOutput() == null) {
                continue;
            }

            try {
                nodeOutputs.put(
                        step.getNodeId(),
                        jsonMapper.readTree(step.getOutput())
                );
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to read output for node: " + step.getNodeId(),
                        e
                );
            }
        }

        return nodeOutputs;
    }



}