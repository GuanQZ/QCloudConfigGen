package com.configgen.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

        String[] headers = {"filename", "port", "server_name", "location_path", "proxy_pass"};
        List<Map<String, String>> exampleRows = java.util.Arrays.asList(
                createRow("nginx.conf", "80", "example.com", "/api", "http://backend1"),
                createRow("api.conf", "8080", "api.example.com", "/gateway", "http://backend2")
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
            dataRow.createCell(2).setCellValue(rowData.get("server_name"));
            dataRow.createCell(3).setCellValue(rowData.get("location_path"));
            dataRow.createCell(4).setCellValue(rowData.get("proxy_pass"));
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

        return new ResponseEntity<>(baos.toByteArray(), headersResponse, HttpStatus.OK);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> getExampleTemplate() throws IOException {
        String template = "server {\n" +
                "    listen {{port}};\n" +
                "    server_name {{server_name}};\n" +
                "\n" +
                "    location {{location_path}} {\n" +
                "        proxy_pass {{proxy_pass}};\n" +
                "        proxy_set_header Host $host;\n" +
                "        proxy_set_header X-Real-IP $remote_addr;\n" +
                "        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n" +
                "    }\n" +
                "\n" +
                "    access_log /var/log/nginx/{{server_name}}.log;\n" +
                "    error_log /var/log/nginx/{{server_name}}.error.log;\n" +
                "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/plain"));
        headers.setContentDispositionFormData("attachment", "nginx-template.conf");

        return new ResponseEntity<>(template.getBytes("UTF-8"), headers, HttpStatus.OK);
    }

    private Map<String, String> createRow(String filename, String port, String serverName, String locationPath, String proxyPass) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("filename", filename);
        row.put("port", port);
        row.put("server_name", serverName);
        row.put("location_path", locationPath);
        row.put("proxy_pass", proxyPass);
        return row;
    }
}