package com.configgen.controller;

import com.configgen.model.ApiResponse;
import com.configgen.service.TemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getBuiltInTemplates() {
        List<Map<String, String>> templates = templateService.getBuiltInTemplates();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @GetMapping("/{name}")
    public ResponseEntity<ApiResponse<Map<String, String>>> getBuiltInTemplate(@PathVariable String name) {
        Map<String, String> template = templateService.getBuiltInTemplate(name);
        if (template == null) {
            return ResponseEntity.ok(ApiResponse.error("TEMPLATE_NOT_FOUND", "模板不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    @PostMapping("/parse")
    public ResponseEntity<ApiResponse<Map<String, Object>>> parseTemplate(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("TEMPLATE_PARSE_ERROR", "模板内容不能为空"));
        }
        List<String> placeholders = templateService.parsePlaceholders(content);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("placeholders", placeholders);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}