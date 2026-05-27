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
        return generateZip(templateContent, rows, false, null);
    }

    public byte[] generateZip(String templateContent, List<Map<String, String>> rows,
                             boolean mergeOutput, String mergedFilename) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map<String, String> row : rows) {
                String filename = row.get("filename");
                validateFilename(filename, "filename");
                String content = templateService.fillTemplate(templateContent, row);
                ZipEntry entry;
                if (mergeOutput) {
                    entry = new ZipEntry(filename + "/" + mergedFilename);
                } else {
                    entry = new ZipEntry(filename);
                }
                zos.putNextEntry(entry);
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException("生成 ZIP 失败", e);
        }
        return baos.toByteArray();
    }

    private void validateFilename(String filename, String errorCode) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException(errorCode + ": 文件名不能为空");
        }
        if (filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException(errorCode + ": 文件名不能包含路径分隔符");
        }
        if (filename.contains("<") || filename.contains(">") || filename.contains(":") ||
            filename.contains("\"") || filename.contains("|") || filename.contains("?") || filename.contains("*")) {
            throw new IllegalArgumentException(errorCode + ": 文件名包含非法字符");
        }
        if (filename.length() > 255) {
            throw new IllegalArgumentException(errorCode + ": 文件名长度不能超过255");
        }
    }

    public String previewOne(String templateContent, Map<String, String> rowData) {
        return templateService.fillTemplate(templateContent, rowData);
    }
}