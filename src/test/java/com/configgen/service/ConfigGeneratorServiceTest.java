package com.configgen.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.*;

class ConfigGeneratorServiceTest {

    @Test
    void testGenerateZip() throws Exception {
        TemplateService templateService = new TemplateService();
        ConfigGeneratorService service = new ConfigGeneratorService(templateService);

        String template = "server {\n    listen {{port}};\n}";
        List<Map<String, String>> rows = List.of(
                Map.of("filename", "beijing.conf", "port", "8080"),
                Map.of("filename", "shanghai.conf", "port", "8081")
        );

        byte[] zip = service.generateZip(template, rows);
        assertNotNull(zip);
        assertTrue(zip.length > 0);

        // Verify ZIP content
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry1 = zis.getNextEntry();
            assertEquals("beijing.conf", entry1.getName());

            byte[] content1 = zis.readAllBytes();
            assertTrue(new String(content1).contains("8080"));

            ZipEntry entry2 = zis.getNextEntry();
            assertEquals("shanghai.conf", entry2.getName());

            byte[] content2 = zis.readAllBytes();
            assertTrue(new String(content2).contains("8081"));
        }
    }

    @Test
    void testPreviewOne() {
        TemplateService templateService = new TemplateService();
        ConfigGeneratorService service = new ConfigGeneratorService(templateService);

        String template = "listen {{port}};";
        Map<String, String> row = Map.of("port", "8080");
        String result = service.previewOne(template, row);
        assertEquals("listen 8080;", result);
    }
}