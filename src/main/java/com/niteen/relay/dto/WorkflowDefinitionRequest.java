package com.niteen.relay.dto;

import java.util.List;
import java.util.Map;

public class WorkflowDefinitionRequest {

    private String id;
    private String name;
    private String description;
    private Map<String, Object> trigger;
    private String entry;
    private Map<String, Object> limits;
    private List<Map<String, Object>> nodes;

    public WorkflowDefinitionRequest() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getTrigger() {
        return trigger;
    }

    public void setTrigger(Map<String, Object> trigger) {
        this.trigger = trigger;
    }

    public String getEntry() {
        return entry;
    }

    public void setEntry(String entry) {
        this.entry = entry;
    }

    public Map<String, Object> getLimits() {
        return limits;
    }

    public void setLimits(Map<String, Object> limits) {
        this.limits = limits;
    }

    public List<Map<String, Object>> getNodes() {
        return nodes;
    }

    public void setNodes(List<Map<String, Object>> nodes) {
        this.nodes = nodes;
    }
}
