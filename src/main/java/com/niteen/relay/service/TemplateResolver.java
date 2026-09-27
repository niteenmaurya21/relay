package com.niteen.relay.service;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateResolver {

    private static final Pattern TRIGGER_BODY_PATTERN =
            Pattern.compile("\\{\\{trigger\\.body\\.([^}]+)}}");

    private static final Pattern NODE_OUTPUT_PATTERN =
            Pattern.compile("\\{\\{nodes\\.([^.]+)\\.output((?:\\.[^}]+)*)}}");

    private final JsonMapper jsonMapper;

    public TemplateResolver(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /*
     * Resolves templates using trigger input and previous node outputs.
     *
     * Supported examples:
     *
     * {{trigger.body.order_id}}
     * {{trigger.body.customer_email}}
     *
     * {{nodes.create_shipment.output.body.shipment_id}}
     */
    public String resolve(
            String template,
            JsonNode input,
            Map<String, JsonNode> nodeOutputs
    ) {

        if (template == null) {
            return null;
        }

        String result = template;

        // Resolve trigger.body.* templates
        if (input != null) {
            result = resolveTriggerTemplates(result, input);
        }

        // Resolve nodes.*.output.* templates
        if (nodeOutputs != null) {
            result = resolveNodeOutputTemplates(result, nodeOutputs);
        }

        return result;
    }

    /*
     * Backward-compatible resolver used by existing code.
     *
     * It resolves trigger.body.* templates only.
     */
    public String resolve(String template, JsonNode input) {
        return resolve(template, input, null);
    }

    private String resolveTriggerTemplates(
            String template,
            JsonNode input
    ) {

        Matcher matcher = TRIGGER_BODY_PATTERN.matcher(template);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {

            String fieldName = matcher.group(1);

            JsonNode value = input.path(fieldName);

            if (!value.isMissingNode() && !value.isNull()) {

                matcher.appendReplacement(
                        result,
                        Matcher.quoteReplacement(value.asText())
                );

            } else {

                matcher.appendReplacement(
                        result,
                        Matcher.quoteReplacement(matcher.group(0))
                );
            }
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private String resolveNodeOutputTemplates(
            String template,
            Map<String, JsonNode> nodeOutputs
    ) {

        Matcher matcher = NODE_OUTPUT_PATTERN.matcher(template);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {

            String nodeId = matcher.group(1);
            String path = matcher.group(2);

            JsonNode output = nodeOutputs.get(nodeId);

            /*
             * We don't have output for this node yet.
             * Leave the template unchanged.
             */
            if (output == null) {

                matcher.appendReplacement(
                        result,
                        Matcher.quoteReplacement(matcher.group(0))
                );

                continue;
            }

            JsonNode value = output;

            /*
             * Example:
             *
             * path =
             * .body.shipment_id
             *
             * We remove the first "." and traverse:
             *
             * body
             *   ↓
             * shipment_id
             */
            if (path != null && !path.isEmpty()) {

                String[] parts =
                        path.substring(1).split("\\.");

                for (String part : parts) {

                    value = value.path(part);

                    if (value.isMissingNode()) {
                        break;
                    }
                }
            }

            if (!value.isMissingNode() && !value.isNull()) {

                matcher.appendReplacement(
                        result,
                        Matcher.quoteReplacement(value.asText())
                );

            } else {

                /*
                 * If the requested output path doesn't exist,
                 * leave the original template unchanged.
                 */
                matcher.appendReplacement(
                        result,
                        Matcher.quoteReplacement(matcher.group(0))
                );
            }
        }

        matcher.appendTail(result);

        return result.toString();
    }

    /*
     * Resolves templates recursively inside JSON.
     *
     * Example:
     *
     * {
     *   "order_id": "{{trigger.body.order_id}}"
     * }
     *
     * becomes:
     *
     * {
     *   "order_id": "ord_2001"
     * }
     */
    public JsonNode resolveJson(
            JsonNode node,
            JsonNode input
    ) {

        if (node == null) {
            return null;
        }

        if (node.isTextual()) {

            return jsonMapper.getNodeFactory().textNode(
                    resolve(node.asText(), input)
            );
        }

        if (node.isObject()) {

            var objectNode = jsonMapper.createObjectNode();

            node.properties().forEach(entry -> {

                objectNode.set(
                        entry.getKey(),
                        resolveJson(
                                entry.getValue(),
                                input
                        )
                );
            });

            return objectNode;
        }

        if (node.isArray()) {

            var arrayNode = jsonMapper.createArrayNode();

            for (JsonNode element : node) {

                arrayNode.add(
                        resolveJson(
                                element,
                                input
                        )
                );
            }

            return arrayNode;
        }

        return node;
    }
}