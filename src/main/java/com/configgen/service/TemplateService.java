package com.configgen.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private static final Map<String, String> BUILT_IN_TEMPLATES = new LinkedHashMap<>();

    static {
        BUILT_IN_TEMPLATES.put("nginx", "server {\n" +
                "    listen {{port}};\n" +
                "    server_name {{domain}};\n" +
                "\n" +
                "    location / {\n" +
                "        proxy_pass http://{{upstream}};\n" +
                "        proxy_set_header Host $host;\n" +
                "        proxy_set_header X-Real-IP $remote_addr;\n" +
                "    }\n" +
                "}");
        BUILT_IN_TEMPLATES.put("spring-cloud-gateway", "spring:\n" +
                "  cloud:\n" +
                "    gateway:\n" +
                "      routes:\n" +
                "        - id: {{route_id}}\n" +
                "          uri: http://{{upstream}}\n" +
                "          predicates:\n" +
                "            - Path=/{{path_prefix}}/**\n" +
                "          filters:\n" +
                "            - StripPrefix=1");
    }

    public List<Map<String, String>> getBuiltInTemplates() {
        List<Map<String, String>> templates = new ArrayList<>();
        int id = 1;
        for (Map.Entry<String, String> entry : BUILT_IN_TEMPLATES.entrySet()) {
            Map<String, String> template = new HashMap<>();
            template.put("id", String.valueOf(id++));
            template.put("name", entry.getKey());
            template.put("content", entry.getValue());
            templates.add(template);
        }
        return templates;
    }

    public Map<String, String> getBuiltInTemplate(String name) {
        String content = BUILT_IN_TEMPLATES.get(name);
        if (content != null) {
            Map<String, String> template = new HashMap<>();
            template.put("id", name);
            template.put("name", name);
            template.put("content", content);
            return template;
        }
        return null;
    }

    public List<String> parsePlaceholders(String content) {
        List<String> placeholders = new ArrayList<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        Set<String> seen = new LinkedHashSet<>();
        while (matcher.find()) {
            String placeholder = matcher.group(1).trim();
            if (!seen.contains(placeholder)) {
                seen.add(placeholder);
                placeholders.add(placeholder);
            }
        }
        return placeholders;
    }

    public String fillTemplate(String templateContent, Map<String, String> rowData) {
        String result = templateContent;
        for (Map.Entry<String, String> entry : rowData.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}