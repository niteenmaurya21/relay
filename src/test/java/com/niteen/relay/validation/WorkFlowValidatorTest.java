package com.niteen.relay.validation;

import com.niteen.relay.exception.WorkflowValidationException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowValidatorTest {

    private final WorkflowValidator validator =
            new WorkflowValidator(new JsonMapper());

    @Test
    void shouldRejectUnknownNodeType() {

        List<Map<String, Object>> nodes = List.of(
                Map.of(
                        "id", "test_node",
                        "type", "unknown_node_type"
                )
        );

        assertThrows(
                WorkflowValidationException.class,
                () -> validator.validateNodeTypes(nodes)
        );
    }

    @Test
    void shouldRejectNodeWithoutType() {

        List<Map<String, Object>> nodes = List.of(
                Map.of(
                        "id", "test_node"
                )
        );

        assertThrows(
                WorkflowValidationException.class,
                () -> validator.validateNodeTypes(nodes)
        );
    }

    @Test
    void shouldRejectMissingRequiredParameter() {

        List<Map<String, Object>> nodes = List.of(
                Map.of(
                        "id", "condition_node",
                        "type", "condition",
                        "params", Map.of(
                                "op", "equals",
                                "right", "approved"
                        )
                )
        );

        assertThrows(
                WorkflowValidationException.class,
                () -> validator.validateRequiredParameters(nodes)
        );
    }

    @Test
    void shouldRejectInvalidEntryNode() {

        List<Map<String, Object>> nodes = List.of(
                Map.of(
                        "id", "start",
                        "type", "condition"
                )
        );

        assertThrows(
                WorkflowValidationException.class,
                () -> validator.validateNodeReferences("does_not_exist", nodes)
        );
    }

    @Test
    void shouldRejectInvalidNextReference() {

        List<Map<String, Object>> nodes = List.of(
                Map.of(
                        "id", "start",
                        "type", "delay",
                        "next", "does_not_exist"
                )
        );

        assertThrows(
                WorkflowValidationException.class,
                () -> validator.validateNodeReferences("start", nodes)
        );
    }

    @Test
    void shouldRejectInvalidOnTrueReference() {

        List<Map<String, Object>> nodes = List.of(
                Map.of(
                        "id", "condition_node",
                        "type", "condition",
                        "on_true", "does_not_exist"
                )
        );

        assertThrows(
                WorkflowValidationException.class,
                () -> validator.validateNodeReferences("condition_node", nodes)
        );
    }

    @Test
    void shouldRejectInvalidOnFalseReference() {

        List<Map<String, Object>> nodes = List.of(
                Map.of(
                        "id", "condition_node",
                        "type", "condition",
                        "on_false", "does_not_exist"
                )
        );

        assertThrows(
                WorkflowValidationException.class,
                () -> validator.validateNodeReferences("condition_node", nodes)
        );
    }

}