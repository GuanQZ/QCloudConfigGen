package com.configgen.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.util.*;

@Service
public class ExcelParserService {

    public static class ParseResult {
        private final List<String> headers;
        private final List<Map<String, String>> rows;
        private final String error;

        public ParseResult(List<String> headers, List<Map<String, String>> rows, String error) {
            this.headers = headers;
            this.rows = rows;
            this.error = error;
        }

        public List<String> getHeaders() { return headers; }
        public List<Map<String, String>> getRows() { return rows; }
        public String getError() { return error; }
        public boolean isSuccess() { return error == null; }
    }

    public ParseResult parse(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return new ParseResult(null, null, "文件名不能为空");
        }

        if (filename.endsWith(".xlsx")) {
            return parseXlsx(file.getInputStream());
        } else if (filename.endsWith(".xls")) {
            return parseXls(file.getInputStream());
        } else if (filename.endsWith(".csv")) {
            return parseCsv(file.getInputStream());
        } else {
            return new ParseResult(null, null, "不支持的文件格式，仅支持 .xlsx、.xls 和 .csv");
        }
    }

    private ParseResult parseXlsx(InputStream inputStream) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            return parseWorkbook(workbook);
        }
    }

    private ParseResult parseXls(InputStream inputStream) throws Exception {
        try (Workbook workbook = new HSSFWorkbook(inputStream)) {
            return parseWorkbook(workbook);
        }
    }

    private ParseResult parseWorkbook(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.iterator();

        if (!rowIterator.hasNext()) {
            return new ParseResult(null, null, "Excel 文件为空");
        }

        Row headerRow = rowIterator.next();
        List<String> headers = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();

        for (Cell cell : headerRow) {
            String headerValue = dataFormatter.formatCellValue(cell);
            if (headerValue == null || headerValue.trim().isEmpty()) {
                return new ParseResult(null, null, "Excel 文件包含空列名");
            }
            headers.add(headerValue.trim());
        }

        if (!headers.contains("filename")) {
            return new ParseResult(null, null, "缺少必需的 'filename' 列");
        }

        List<Map<String, String>> rows = new ArrayList<>();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Map<String, String> rowData = new LinkedHashMap<>();
            boolean isEmpty = true;

            for (int i = 0; i < headers.size(); i++) {
                Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                String value = getCellValueAsString(cell, dataFormatter);
                rowData.put(headers.get(i), value);
                if (value != null && !value.trim().isEmpty()) {
                    isEmpty = false;
                }
            }

            if (!isEmpty) {
                rows.add(rowData);
            }
        }

        return new ParseResult(headers, rows, null);
    }

    private ParseResult parseCsv(InputStream inputStream) throws Exception {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                return new ParseResult(null, null, "CSV 文件为空");
            }

            headers = parseCsvLine(headerLine);
            if (!headers.contains("filename")) {
                return new ParseResult(null, null, "缺少必需的 'filename' 列");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                Map<String, String> rowData = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String value = i < values.size() ? values.get(i) : "";
                    rowData.put(headers.get(i), value);
                }
                rows.add(rowData);
            }
        }

        return new ParseResult(headers, rows, null);
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(sb.toString().trim());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        values.add(sb.toString().trim());
        return values;
    }

    private String getCellValueAsString(Cell cell, DataFormatter dataFormatter) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell);
    }
}