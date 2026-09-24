package com.niteen.relay.validation;

import com.niteen.relay.dto.WorkflowDefinitionRequest;
import com.niteen.relay.exception.WorkflowValidationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowValidator {

    private final JsonMapper jsonMapper;
    private final JsonNode nodeCatalog;

    public void validateNodeTypes(List<Map<String, Object>> nodes) {
        for(Map<String, Object> node :nodes ){
            String nodeId = String.valueOf(node.get("id"));
            Object typeValue = node.get("type");

            if(typeValue == null){
                throw new WorkflowValidationException(
                        "Node '"+ nodeId + "' is missing a type",
                        "invalid_node_type"
                );
            }

            String nodeType =  String.valueOf(typeValue);
            if(!isKnownNodeType(nodeType)) {
                throw new WorkflowValidationException(
                        "Node '" + nodeId + "' has unknown type '" +nodeType+"'",
                        "invalid_node_type"
                );

            }

        }
    }

    public void validateRequiredParameters(List<Map<String, Object>> nodes) {
        for(Map<String, Object> node :nodes ){
            String nodeId = String.valueOf(node.get("id"));
            Object typeValue = node.get("type");
            String nodeType =  String.valueOf(typeValue);

            JsonNode catalogNodes = nodeCatalog.path("nodes");
            JsonNode catalogNode = findCatalogNode(catalogNodes, nodeType);

            if(catalogNode == null){
                continue;
            }
            JsonNode catalogParams = catalogNode.path("params");

            Object paramsValue = node.get("params");
            Map<?, ?> nodeParams;
            if(paramsValue instanceof Map<?,?> paramsMap){
                nodeParams = paramsMap;
            }
            else{
                nodeParams = Map.of();
            }
            catalogParams.properties().forEach(field -> {
                String paramName = field.getKey();
                JsonNode paramDefinition = field.getValue();

                boolean required = paramDefinition.path("required").asBoolean(false);

                if(required && (!nodeParams.containsKey(paramName) || nodeParams.get(paramName) == null)){
                    throw new WorkflowValidationException(
                            "Node '" +nodeId + "' is missing required parameter '" +paramName+"'",
                            "missing_required_param"
                    );
                }
            });
        }
    }

    private JsonNode findCatalogNode(JsonNode catalogNodes, String nodeType) {

        for (JsonNode catalogNode : catalogNodes) {

            if (catalogNode.path("type").asText().equals(nodeType)) {
                return catalogNode;
            }
        }

        return null;
    }

    public void validateNodeReferences(String entry, List<Map<String, Object>> nodes) {
        java.util.Set<String> nodeIds = new java.util.HashSet<>();

        // Collecting of all the valid nodes
        for(Map<String, Object> node : nodes ){
            Object idValue = node.get("id");

            if(idValue != null){
                nodeIds.add(String.valueOf(idValue));
            }

        }

        //check entry node
        if(entry == null || !nodeIds.contains(entry)){
            throw new WorkflowValidationException(
                    "Entry node '" + entry +"' does not exist", "invalid_entry"
            );
        }

        //checking outgoing refrences
        for(Map<String, Object> node : nodes ){
            String nodeId = String.valueOf(node.get("id"));

            validateReference(
                    nodeId,
                    "next",
                    node.get("next"),
                    nodeIds
            );

            validateReference(
                    nodeId,
                    "on_true",
                    node.get("on_true"),
                    nodeIds
            );

            validateReference(
                    nodeId,
                    "on_false",
                    node.get("on_false"),
                    nodeIds
            );

        }

    }

    public void validateReference (
            String nodeId,
            String referenceName,
            Object referenceValue,
            java.util.Set<String> nodeIds
    ) {
        // null is allowed beacuse terminal nodes use "next" : null

        if(referenceValue == null){
            return;
        }

        String targetNodeId = String.valueOf(referenceValue);
        if(!nodeIds.contains(targetNodeId)){
            throw new WorkflowValidationException(
                    "Node '" + nodeId+ "' has " +referenceName+ " pointing to nonexistent node '"+ targetNodeId +"'",
                    "invalid_node_reference"
            );
        }
    }

    public boolean isKnownNodeType(String nodeType) {
        JsonNode catalogNodes = nodeCatalog.path("nodes");
        for(JsonNode catalogNode : catalogNodes) {
            String catalogType = catalogNode.path("type").asText();
            if(catalogType.equals(nodeType)){
                return true;
            }
        }
        return false;
    }

    public WorkflowValidator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        this.nodeCatalog = loadNodeCatalog();
    }
    private JsonNode loadNodeCatalog() {
        try {
            ClassPathResource resource =
                    new ClassPathResource("data/node_catalog.json");
            try (InputStream inputStream = resource.getInputStream()) {
                return jsonMapper.readTree(inputStream);
            }
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not load node_catalog.json",e);
        }
    }

    public void validateWorkflow(WorkflowDefinitionRequest definition) {
        validateNodeTypes(definition.getNodes());
        validateRequiredParameters(definition.getNodes());
        validateNodeReferences(
                definition.getEntry(),
                definition.getNodes()
        );
    }

}
