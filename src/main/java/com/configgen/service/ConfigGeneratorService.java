package com.configgen.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ConfigGeneratorService {

    private final TemplateService templateService;

    public ConfigGeneratorService(TemplateService templateService) {
        this.templateService = templateService;
    }

    public byte[] generateZip(String templateContent, List<Map<String, String>> rows) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map<String, String> row : rows) {
                String filename = row.get("filename");
                if (filename == null || filename.trim().isEmpty()) {
                    throw new IllegalArgumentException("filename 列不能为空");
                }
                String content = templateService.fillTemplate(templateContent, row);
                ZipEntry entry = new ZipEntry(filename);
                zos.putNextEntry(entry);
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException("生成 ZIP 失败", e);
        }
        return baos.toByteArray();
    }

    public String previewOne(String templateContent, Map<String, String> rowData) {
        return templateService.fillTemplate(templateContent, rowData);
    }
}