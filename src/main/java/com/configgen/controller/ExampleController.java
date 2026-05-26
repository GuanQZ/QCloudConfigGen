package com.configgen.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/example")
public class ExampleController {

    @GetMapping("/excel")
    public ResponseEntity<byte[]> getExampleExcel() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("配置示例");

        String[] headers = {"filename", "port", "domain", "upstream"};
        List<Map<String, String>> exampleRows = List.of(
            createRow("nginx.conf", "80", "example.com", "backend1"),
            createRow("gateway.yaml", "8080", "api.example.com", "backend2")
        );

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        for (int rowIdx = 0; rowIdx < exampleRows.size(); rowIdx++) {
            Row dataRow = sheet.createRow(rowIdx + 1);
            Map<String, String> rowData = exampleRows.get(rowIdx);
            dataRow.createCell(0).setCellValue(rowData.get("filename"));
            dataRow.createCell(1).setCellValue(rowData.get("port"));
            dataRow.createCell(2).setCellValue(rowData.get("domain"));
            dataRow.createCell(3).setCellValue(rowData.get("upstream"));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        HttpHeaders headersResponse = new HttpHeaders();
        headersResponse.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headersResponse.setContentDispositionFormData("attachment", "example.xlsx");

        return ResponseEntity.ok(headersResponse).body(baos.toByteArray());
    }

    private Map<String, String> createRow(String filename, String port, String domain, String upstream) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("filename", filename);
        row.put("port", port);
        row.put("domain", domain);
        row.put("upstream", upstream);
        return row;
    }
}