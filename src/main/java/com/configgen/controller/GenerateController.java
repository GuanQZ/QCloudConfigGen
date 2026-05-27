package com.configgen.controller;

import com.configgen.model.ApiResponse;
import com.configgen.service.ConfigGeneratorService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/generate")
public class GenerateController {

    private final ConfigGeneratorService configGeneratorService;

    public GenerateController(ConfigGeneratorService configGeneratorService) {
        this.configGeneratorService = configGeneratorService;
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<Map<String, String>>> preview(@RequestBody Map<String, Object> request) {
        String templateContent = (String) request.get("templateContent");
        @SuppressWarnings("unchecked")
        Map<String, String> rowData = (Map<String, String>) request.get("rowData");

        if (templateContent == null || templateContent.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("TEMPLATE_CONTENT_ERROR", "模板内容不能为空"));
        }
        if (rowData == null || rowData.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("ROW_DATA_ERROR", "行数据不能为空"));
        }

        String content = configGeneratorService.previewOne(templateContent, rowData);
        return ResponseEntity.ok(ApiResponse.success(Map.of("content", content)));
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@RequestBody Map<String, Object> request) {
        String templateContent = (String) request.get("templateContent");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> rows = (List<Map<String, String>>) request.get("rows");

        if (templateContent == null || templateContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (rows == null || rows.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] zipBytes = configGeneratorService.generateZip(templateContent, rows);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "configs.zip");

        return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
    }
}