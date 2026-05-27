package com.configgen.service;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TemplateServiceTest {

    @Test
    void testParsePlaceholders() {
        TemplateService service = new TemplateService();
        String content = "server {\n    listen {{port}};\n    server_name {{domain}};\n}";
        List<String> placeholders = service.parsePlaceholders(content);
        assertEquals(Arrays.asList("port", "domain"), placeholders);
    }

    @Test
    void testParsePlaceholders_noDuplicates() {
        TemplateService service = new TemplateService();
        String content = "{{a}} {{a}} {{b}}";
        List<String> placeholders = service.parsePlaceholders(content);
        assertEquals(Arrays.asList("a", "b"), placeholders);
    }

    @Test
    void testFillTemplate() {
        TemplateService service = new TemplateService();
        String content = "listen {{port}};";
        Map<String, String> row = new java.util.LinkedHashMap<>();
        row.put("port", "8080");
        String result = service.fillTemplate(content, row);
        assertEquals("listen 8080;", result);
    }

    @Test
    void testFillTemplate_missingKey() {
        TemplateService service = new TemplateService();
        String content = "listen {{port}};";
        Map<String, String> row = new java.util.LinkedHashMap<>();
        row.put("other", "8080");
        String result = service.fillTemplate(content, row);
        assertEquals("listen {{port}};", result);
    }

    @Test
    void testGetBuiltInTemplates() {
        TemplateService service = new TemplateService();
        List<Map<String, String>> templates = service.getBuiltInTemplates();
        assertFalse(templates.isEmpty());
        assertTrue(templates.stream().anyMatch(t -> "nginx".equals(t.get("name"))));
        assertTrue(templates.stream().anyMatch(t -> "spring-cloud-gateway".equals(t.get("name"))));
    }
}