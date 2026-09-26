package com.niteen.relay.service;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateResolver {

    private static final Pattern TRIGGER_BODY_PATTERN =
            Pattern.compile("\\{\\{trigger\\.body\\.([^}]+)}}");

    public String resolve(String template, JsonNode input) {

        if (template == null) {
            return null;
        }

        if (input == null) {
            return template;
        }

        Matcher matcher = TRIGGER_BODY_PATTERN.matcher(template);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {

            String fieldName = matcher.group(1);

            JsonNode value = input.path(fieldName);

            if (!value.isMissingNode() && !value.isNull()) {

                String replacement = value.asText();

                matcher.appendReplacement(
                        result,
                        Matcher.quoteReplacement(replacement)
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
}