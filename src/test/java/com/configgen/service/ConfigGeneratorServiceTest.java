package com.configgen.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.ZipEntry;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ConfigGeneratorServiceTest {

    @Test
    void testGenerateZip() throws Exception {
        TemplateService templateService = new TemplateService();
        ConfigGeneratorService service = new ConfigGeneratorService(templateService);

        String template = "server {\n    listen {{port}};\n}";
        List<Map<String, String>> rows = java.util.Arrays.asList(
                createMap("filename", "beijing.conf", "port", "8080"),
                createMap("filename", "shanghai.conf", "port", "8081")
        );

        byte[] zip = service.generateZip(template, rows);
        assertNotNull(zip);
        assertTrue(zip.length > 0);

        // Verify ZIP content
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry1 = zis.getNextEntry();
            assertEquals("beijing.conf", entry1.getName());

            byte[] content1 = readAllBytes(zis);
            assertTrue(new String(content1).contains("8080"));

            ZipEntry entry2 = zis.getNextEntry();
            assertEquals("shanghai.conf", entry2.getName());

            byte[] content2 = readAllBytes(zis);
            assertTrue(new String(content2).contains("8081"));
        }
    }

    @Test
    void testPreviewOne() {
        TemplateService templateService = new TemplateService();
        ConfigGeneratorService service = new ConfigGeneratorService(templateService);

        String template = "listen {{port}};";
        Map<String, String> row = createMap("port", "8080");
        String result = service.previewOne(template, row);
        assertEquals("listen 8080;", result);
    }

    @Test
    void testGenerateZipWithMergeOutput() throws Exception {
        TemplateService templateService = new TemplateService();
        ConfigGeneratorService service = new ConfigGeneratorService(templateService);

        String template = "server {\n    listen {{port}};\n}";
        List<Map<String, String>> rows = java.util.Arrays.asList(
                createMap("filename", "beijing", "port", "8080"),
                createMap("filename", "shanghai", "port", "8081")
        );

        byte[] zip = service.generateZip(template, rows, true, "nginx.conf");
        assertNotNull(zip);
        assertTrue(zip.length > 0);

        // Verify ZIP content with folder structure
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry1 = zis.getNextEntry();
            assertEquals("beijing/nginx.conf", entry1.getName());

            byte[] content1 = readAllBytes(zis);
            assertTrue(new String(content1).contains("8080"));

            ZipEntry entry2 = zis.getNextEntry();
            assertEquals("shanghai/nginx.conf", entry2.getName());

            byte[] content2 = readAllBytes(zis);
            assertTrue(new String(content2).contains("8081"));
        }
    }

    @Test
    void testGenerateZipWithInvalidMergedFilename() {
        TemplateService templateService = new TemplateService();
        ConfigGeneratorService service = new ConfigGeneratorService(templateService);

        String template = "server {\n    listen {{port}};\n}";
        List<Map<String, String>> rows = java.util.Arrays.asList(
                createMap("filename", "beijing", "port", "8080")
        );

        // Test invalid character in mergedFilename
        assertThrows(IllegalArgumentException.class, () -> {
            service.generateZip(template, rows, true, "ng:in.conf");
        });
    }

    private Map<String, String> createMap(String... keysAndValues) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    private byte[] readAllBytes(ZipInputStream zis) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zis.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }
}