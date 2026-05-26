package com.configgen.controller;

import com.configgen.model.ApiResponse;
import com.configgen.model.ParseResult;
import com.configgen.service.ExcelParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    private final ExcelParserService excelParserService;

    public ExcelController(ExcelParserService excelParserService) {
        this.excelParserService = excelParserService;
    }

    @PostMapping("/parse")
    public ResponseEntity<ApiResponse<ParseResult>> parseExcel(@RequestParam("file") MultipartFile file) {
        try {
            ExcelParserService.ParseResult parseResult = excelParserService.parse(file);
            if (parseResult.isSuccess()) {
                ParseResult result = new ParseResult(
                    parseResult.getHeaders(),
                    parseResult.getRows(),
                    parseResult.getRows().size(),
                    file.getOriginalFilename()
                );
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                String errorCode = "MISSING_FILENAME_COLUMN".equals(parseResult.getError()) ? "MISSING_FILENAME_COLUMN" : "PARSE_ERROR";
                return ResponseEntity.ok(ApiResponse.error(errorCode, parseResult.getError()));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error("MISSING_FILENAME_COLUMN", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("PARSE_ERROR", e.getMessage()));
        }
    }
}