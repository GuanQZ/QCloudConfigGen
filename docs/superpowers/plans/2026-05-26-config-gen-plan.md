# ConfigGen 配置文件生成工具 - 实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个本地配置文件生成工具，通过模板 + Excel 数据批量生成配置文件

**Architecture:** Spring Boot 单 JAR 后端 + Vue.js 3 嵌入式前端，分层设计：Controller → Service → Repository

**Tech Stack:** Spring Boot 3.x, Vue.js 3, Element Plus, Monaco Editor, Maven, Apache POI (Excel), jpackage

---

## Chunk 1: 项目脚手架

### 1.1 创建 Spring Boot 项目结构

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/configgen/ConfigGenApplication.java`
- Create: `src/main/java/com/configgen/controller/TemplateController.java`
- Create: `src/main/java/com/configgen/controller/ExcelController.java`
- Create: `src/main/java/com/configgen/controller/GenerateController.java`
- Create: `src/main/java/com/configgen/controller/ExampleController.java`
- Create: `src/main/java/com/configgen/service/TemplateService.java`
- Create: `src/main/java/com/configgen/service/ExcelParserService.java`
- Create: `src/main/java/com/configgen/service/ConfigGeneratorService.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/java/com/configgen/model/ApiResponse.java`
- Create: `src/main/java/com/configgen/model/ParseResult.java`

**pom.xml 内容：**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <groupId>com.configgen</groupId>
    <artifactId>config-gen</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <vue.version>3.4.0</vue.version>
    </properties>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Excel 处理 -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.5</version>
        </dependency>

        <!-- YAML 处理 (Spring Boot 3.x 已内置，无需单独声明) -->

        <!-- JSON -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
        </plugins>
        <finalName>config-gen</finalName>
    </build>
</project>
```

**src/main/java/com/configgen/ConfigGenApplication.java:**

```java
package com.configgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class ConfigGenApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigGenApplication.class, args);
    }

    // 实现"启动后自动打开浏览器"
    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        String port = System.getProperty("server.port", "8080");
        String url = "http://localhost:" + port;
        System.out.println("========================================");
        System.out.println("ConfigGen 已启动: " + url);
        System.out.println("正在打开浏览器...");
        System.out.println("========================================");
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + url);
            } else if (os.contains("windows")) {
                Runtime.getRuntime().exec("cmd /c start " + url);
            } else {
                Runtime.getRuntime().exec("xdg-open " + url);
            }
        } catch (Exception e) {
            System.out.println("请手动打开浏览器访问: " + url);
        }
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }
        };
    }
}
```

**src/main/resources/application.yml:**

```yaml
server:
  port: 8080

spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

**src/main/java/com/configgen/model/ApiResponse.java:**

```java
package com.configgen.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String error;
    private String message;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, null, message);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, null, errorCode, message);
    }
}
```

**src/main/java/com/configgen/model/ParseResult.java:**

```java
package com.configgen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParseResult {
    private List<String> columns;
    private List<Map<String, String>> rows;
    private int totalRows;
    private String filename;
}
```

- [ ] **Step 1: 创建目录结构**
- [ ] **Step 2: 编写 pom.xml**
- [ ] **Step 3: 编写 ConfigGenApplication.java**
- [ ] **Step 4: 编写 application.yml 和 model 类**
- [ ] **Step 5: 提交代码**

---

### 1.2 创建服务层基础类

**Files:**
- Modify: `src/main/java/com/configgen/service/TemplateService.java`
- Modify: `src/main/java/com/configgen/service/ExcelParserService.java`
- Modify: `src/main/java/com/configgen/service/ConfigGeneratorService.java`

**src/main/java/com/configgen/service/TemplateService.java:**

```java
package com.configgen.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private static final Map<String, String> BUILT_IN_TEMPLATES = new LinkedHashMap<>();

    static {
        BUILT_IN_TEMPLATES.put("nginx", "server {\n" +
                "    listen {{port}};\n" +
                "    server_name {{domain}};\n" +
                "\n" +
                "    location / {\n" +
                "        proxy_pass http://{{upstream}};\n" +
                "        proxy_set_header Host $host;\n" +
                "        proxy_set_header X-Real-IP $remote_addr;\n" +
                "    }\n" +
                "}");
        BUILT_IN_TEMPLATES.put("spring-cloud-gateway", "spring:\n" +
                "  cloud:\n" +
                "    gateway:\n" +
                "      routes:\n" +
                "        - id: {{route_id}}\n" +
                "          uri: http://{{upstream}}\n" +
                "          predicates:\n" +
                "            - Path=/{{path_prefix}}/**\n" +
                "          filters:\n" +
                "            - StripPrefix=1");
    }

    public List<Map<String, String>> getBuiltInTemplates() {
        List<Map<String, String>> templates = new ArrayList<>();
        int id = 1;
        for (Map.Entry<String, String> entry : BUILT_IN_TEMPLATES.entrySet()) {
            Map<String, String> template = new HashMap<>();
            template.put("id", String.valueOf(id++));
            template.put("name", entry.getKey());
            template.put("content", entry.getValue());
            templates.add(template);
        }
        return templates;
    }

    public Map<String, String> getBuiltInTemplate(String name) {
        String content = BUILT_IN_TEMPLATES.get(name);
        if (content != null) {
            Map<String, String> template = new HashMap<>();
            template.put("id", name);
            template.put("name", name);
            template.put("content", content);
            return template;
        }
        return null;
    }

    public List<String> parsePlaceholders(String content) {
        List<String> placeholders = new ArrayList<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        Set<String> seen = new LinkedHashSet<>();
        while (matcher.find()) {
            String placeholder = matcher.group(1).trim();
            if (!seen.contains(placeholder)) {
                seen.add(placeholder);
                placeholders.add(placeholder);
            }
        }
        return placeholders;
    }

    public String fillTemplate(String templateContent, Map<String, String> rowData) {
        String result = templateContent;
        for (Map.Entry<String, String> entry : rowData.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }
}
```

**src/main/java/com/configgen/service/ExcelParserService.java:**

```java
package com.configgen.service;

import com.configgen.model.ParseResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ExcelParserService {

    public ParseResult parse(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        ParseResult result;
        if (filename.endsWith(".xlsx")) {
            result = parseXlsx(file.getInputStream());
        } else if (filename.endsWith(".xls")) {
            result = parseXls(file.getInputStream());
        } else if (filename.endsWith(".csv")) {
            result = parseCsv(file.getInputStream());
        } else {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 .xlsx、.xls、.csv");
        }

        result.setFilename(filename);

        // 验证 filename 列
        if (!result.getColumns().contains("filename")) {
            throw new IllegalArgumentException("MISSING_FILENAME_COLUMN:缺少必需列：filename");
        }

        return result;
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
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new IllegalArgumentException("Excel 文件为空或格式错误");
        }

        List<String> columns = new ArrayList<>();
        int lastCellNum = headerRow.getLastCellNum();
        for (int i = 0; i < lastCellNum; i++) {
            Cell cell = headerRow.getCell(i);
            String value = getCellValueAsString(cell);
            if (value != null && !value.trim().isEmpty()) {
                columns.add(value.trim());
            }
        }

        List<Map<String, String>> rows = new ArrayList<>();
        int dataRows = sheet.getLastRowNum();
        for (int i = 1; i <= dataRows; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Map<String, String> rowData = new LinkedHashMap<>();
            boolean isEmpty = true;
            for (int j = 0; j < columns.size(); j++) {
                Cell cell = row.getCell(j);
                String value = getCellValueAsString(cell);
                rowData.put(columns.get(j), value != null ? value : "");
                if (value != null && !value.trim().isEmpty()) {
                    isEmpty = false;
                }
            }
            if (!isEmpty) {
                rows.add(rowData);
            }
        }

        ParseResult result = new ParseResult();
        result.setColumns(columns);
        result.setRows(rows);
        result.setTotalRows(rows.size());
        return result;
    }

    private ParseResult parseCsv(InputStream inputStream) throws Exception {
        List<String> columns = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalArgumentException("CSV 文件为空");
            }

            // 解析表头
            String[] headerCells = parseCsvLine(line);
            for (String cell : headerCells) {
                columns.add(cell.trim());
            }

            // 解析数据行
            while ((line = reader.readLine()) != null) {
                String[] cells = parseCsvLine(line);
                Map<String, String> rowData = new LinkedHashMap<>();
                for (int i = 0; i < columns.size(); i++) {
                    String value = i < cells.length ? cells[i] : "";
                    rowData.put(columns.get(i), value.trim());
                }
                rows.add(rowData);
            }
        }

        ParseResult result = new ParseResult();
        result.setColumns(columns);
        result.setRows(rows);
        result.setTotalRows(rows.size());
        return result;
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
```

**src/main/java/com/configgen/service/ConfigGeneratorService.java:**

```java
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

    public byte[] generateZip(String templateContent,
                              List<Map<String, String>> rows) {
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
```

- [ ] **Step 1: 编写 TemplateService.java（内置模板 + 占位符解析 + 填充）**
- [ ] **Step 2: 编写 ExcelParserService.java（Excel/CSV 解析）**
- [ ] **Step 3: 编写 ConfigGeneratorService.java（ZIP 生成 + 预览）**
- [ ] **Step 4: 编写 ParseResult model**
- [ ] **Step 5: 提交代码**

---

### 1.3 创建 Controller 层

**Files:**
- Modify: `src/main/java/com/configgen/controller/TemplateController.java`
- Modify: `src/main/java/com/configgen/controller/ExcelController.java`
- Modify: `src/main/java/com/configgen/controller/GenerateController.java`
- Modify: `src/main/java/com/configgen/controller/ExampleController.java`

**src/main/java/com/configgen/controller/TemplateController.java:**

```java
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
        Map<String, Object> result = Map.of("placeholders", placeholders);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
```

**src/main/java/com/configgen/controller/ExcelController.java:**

```java
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
            ParseResult result = excelParserService.parse(file);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            String errorCode = "EXCEL_PARSE_ERROR";
            if (message != null && message.startsWith("MISSING_FILENAME_COLUMN")) {
                errorCode = "MISSING_FILENAME_COLUMN";
            }
            return ResponseEntity.ok(ApiResponse.error(errorCode, message));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("EXCEL_PARSE_ERROR", "Excel 文件格式错误或损坏：" + e.getMessage()));
        }
    }
}
```

**src/main/java/com/configgen/controller/GenerateController.java:**

```java
package com.configgen.controller;

import com.configgen.model.ApiResponse;
import com.configgen.service.ConfigGeneratorService;
import com.configgen.service.TemplateService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/generate")
public class GenerateController {

    private final ConfigGeneratorService configGeneratorService;
    private final TemplateService templateService;

    public GenerateController(ConfigGeneratorService configGeneratorService,
                            TemplateService templateService) {
        this.configGeneratorService = configGeneratorService;
        this.templateService = templateService;
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<Map<String, String>>> preview(
            @RequestBody Map<String, Object> request) {

        String templateContent = (String) request.get("templateContent");
        @SuppressWarnings("unchecked")
        Map<String, String> rowData = (Map<String, String>) request.get("rowData");

        if (templateContent == null || rowData == null) {
            return ResponseEntity.ok(ApiResponse.error("INVALID_REQUEST", "参数不完整"));
        }

        String content = configGeneratorService.previewOne(templateContent, rowData);
        return ResponseEntity.ok(ApiResponse.success(Map.of("content", content)));
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@RequestBody Map<String, Object> request) {
        String templateContent = (String) request.get("templateContent");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> rows = (List<Map<String, String>>) request.get("rows");

        if (templateContent == null || rows == null || rows.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] zipContent = configGeneratorService.generateZip(templateContent, rows);

        String timestamp = String.valueOf(System.currentTimeMillis());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("configs-" + timestamp + ".zip")
                .build());

        return new ResponseEntity<>(zipContent, headers, HttpStatus.OK);
    }
}
```

**src/main/java/com/configgen/controller/ExampleController.java:**

```java
package com.configgen.controller;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/api/example")
public class ExampleController {

    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadExampleExcel() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("配置数据");

            // 表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"filename", "port", "domain", "upstream"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 示例数据行 1
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("beijing-nginx.conf");
            row1.createCell(1).setCellValue("8080");
            row1.createCell(2).setCellValue("bj.test.com");
            row1.createCell(3).setCellValue("beijing-backend");

            // 示例数据行 2
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("shanghai-nginx.conf");
            row2.createCell(1).setCellValue("8080");
            row2.createCell(2).setCellValue("sh.test.com");
            row2.createCell(3).setCellValue("shanghai-backend");

            // 自适应列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("example-config-data.xlsx")
                    .build());

            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
```

- [ ] **Step 1: 编写 TemplateController**
- [ ] **Step 2: 编写 ExcelController**
- [ ] **Step 3: 编写 GenerateController**
- [ ] **Step 4: 编写 ExampleController**
- [ ] **Step 5: 提交代码**

---

### 1.4 后端 API 验证

**Files:**
- Create: `src/test/java/com/configgen/service/TemplateServiceTest.java`
- Create: `src/test/java/com/configgen/service/ExcelParserServiceTest.java`

**src/test/java/com/configgen/service/TemplateServiceTest.java:**

```java
package com.configgen.service;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TemplateServiceTest {

    @Test
    void testParsePlaceholders() {
        TemplateService service = new TemplateService();
        String content = "server {\n    listen {{port}};\n    server_name {{domain}};\n}";
        List<String> placeholders = service.parsePlaceholders(content);
        assertEquals(Arrays.asList("port", "domain"), placeholders);
    }

    @Test
    void testParsePlaceholders_noDuplicates() {
        TemplateService service = new TemplateService();
        String content = "{{a}} {{a}} {{b}}";
        List<String> placeholders = service.parsePlaceholders(content);
        assertEquals(Arrays.asList("a", "b"), placeholders);
    }

    @Test
    void testFillTemplate() {
        TemplateService service = new TemplateService();
        String content = "listen {{port}};";
        Map<String, String> row = Map.of("port", "8080");
        String result = service.fillTemplate(content, row);
        assertEquals("listen 8080;", result);
    }

    @Test
    void testFillTemplate_missingKey() {
        TemplateService service = new TemplateService();
        String content = "listen {{port}};";
        Map<String, String> row = Map.of("other", "8080");
        String result = service.fillTemplate(content, row);
        assertEquals("listen {{port}};", result);
    }

    @Test
    void testGetBuiltInTemplates() {
        TemplateService service = new TemplateService();
        var templates = service.getBuiltInTemplates();
        assertFalse(templates.isEmpty());
        assertTrue(templates.stream().anyMatch(t -> "nginx".equals(t.get("name"))));
        assertTrue(templates.stream().anyMatch(t -> "spring-cloud-gateway".equals(t.get("name"))));
    }
}
```

**src/test/java/com/configgen/service/ConfigGeneratorServiceTest.java:**

```java
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
    void testGenerateZip() {
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

        // 验证 ZIP 内容
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
```

- [ ] **Step 1: 编写 TemplateServiceTest**
- [ ] **Step 2: 编写 ConfigGeneratorServiceTest**
- [ ] **Step 3: 运行测试验证**
- [ ] **Step 4: 提交代码**

---

### 1.5 前端脚手架（Vue.js 嵌入式）

**Files:**
- Create: `src/main/resources/static/index.html`
- Create: `src/main/resources/static/js/app.js`
- Create: `src/main/resources/static/css/style.css`
- Create: `src/main/resources/static/js/monaco-editor/loader.js` (Monaco CDN)

**src/main/resources/static/index.html:**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ConfigGen - 配置文件生成工具</title>
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/element-plus@2.4.4/dist/index.css">
    <script src="https://cdn.jsdelivr.net/npm/vue@3.4.0/dist/vue.global.prod.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/element-plus@2.4.4/dist/index.full.js"></script>
    <script src="js/monaco-editor/loader.js"></script>
</head>
<body>
    <div id="app">
        <div class="header">
            <h1>ConfigGen <span class="subtitle">配置文件生成工具</span></h1>
        </div>

        <div class="steps">
            <el-steps :active="currentStep" finish-status="success" align-center>
                <el-step title="模板" description="选择或上传模板"></el-step>
                <el-step title="数据" description="上传 Excel 数据"></el-step>
                <el-step title="预览" description="预览并下载"></el-step>
            </el-steps>
        </div>

        <div class="content">
            <!-- Step 1: Template -->
            <div v-if="currentStep === 0" class="step-content">
                <div class="left-panel">
                    <h3>选择模板</h3>
                    <el-card class="template-card" v-for="t in templates" :key="t.id"
                             :class="{selected: selectedTemplate && selectedTemplate.name === t.name}"
                             @click="selectTemplate(t)">
                        <template #header>
                            <span>{{ t.name }}</span>
                        </template>
                        <div class="template-preview">{{ t.content.substring(0, 100) }}...</div>
                    </el-card>

                    <el-divider>或上传模板文件</el-divider>

                    <el-upload drag :auto-upload="false" :limit="1"
                               accept=".conf,.yaml,.yml,.properties,.txt"
                               :on-change="handleTemplateUpload">
                        <el-icon><upload-filled /></el-icon>
                        <span>点击或拖拽上传模板文件</span>
                    </el-upload>

                    <div class="template-actions">
                        <el-button type="primary" :disabled="!selectedTemplate" @click="goToStep(1)">
                            下一步
                        </el-button>
                    </div>
                </div>

                <div class="right-panel">
                    <h3>模板编辑</h3>
                    <div ref="editorContainer" class="monaco-editor-container"></div>
                    <div class="placeholder-info" v-if="placeholders.length > 0">
                        <el-tag type="info">检测到 {{ placeholders.length }} 个占位符:</el-tag>
                        <el-tag v-for="p in placeholders" :key="p" size="small" class="placeholder-tag">
                            {{ '{{' + p + '}}' }}
                        </el-tag>
                    </div>
                </div>
            </div>

            <!-- Step 2: Excel Data -->
            <div v-if="currentStep === 1" class="step-content">
                <div class="left-panel">
                    <h3>上传数据文件</h3>
                    <el-upload drag :auto-upload="false" :limit="1"
                               accept=".xlsx,.xls,.csv"
                               :on-change="handleExcelUpload">
                        <el-icon><upload-filled /></el-icon>
                        <span>点击或拖拽上传 Excel/CSV 文件</span>
                    </el-upload>

                    <div class="template-actions">
                        <el-button @click="goToStep(0)">上一步</el-button>
                        <el-button type="primary" :disabled="!excelData" @click="goToStep(2)">
                            下一步
                        </el-button>
                    </div>
                </div>

                <div class="right-panel">
                    <h3>数据预览</h3>
                    <div v-if="excelData" class="data-info">
                        <el-descriptions :column="2" border>
                            <el-descriptions-item label="文件名">{{ excelData.filename }}</el-descriptions-item>
                            <el-descriptions-item label="数据行数">{{ excelData.totalRows }} 行</el-descriptions-item>
                        </el-descriptions>

                        <h4>字段匹配状态</h4>
                        <div v-for="p in placeholders" :key="p" class="field-status">
                            <el-icon v-if="excelData.columns.includes(p)" color="#67c23a"><circle-check /></el-icon>
                            <el-icon v-else color="#f56c6c"><circle-close /></el-icon>
                            <span>{{ p }}</span>
                            <span v-if="excelData.columns.includes(p)" class="match-ok">已匹配</span>
                            <span v-else class="match-fail">Excel 中未找到此列</span>
                        </div>
                    </div>

                    <div v-if="excelData && excelData.rows.length > 0" class="data-table">
                        <h4>数据预览（前三行）</h4>
                        <el-table :data="excelData.rows.slice(0, 3)" border style="width: 100%">
                            <el-table-column v-for="col in excelData.columns" :key="col" :prop="col" :label="col" />
                        </el-table>
                    </div>
                </div>
            </div>

            <!-- Step 3: Preview & Download -->
            <div v-if="currentStep === 2" class="step-content">
                <div class="left-panel">
                    <h3>选择配置</h3>
                    <div class="config-list">
                        <div v-for="(row, index) in excelData.rows" :key="index"
                             class="config-item" :class="{selected: selectedRowIndex === index}"
                             @click="selectRow(index)">
                            <span class="config-name">{{ row.filename || '未命名' }}</span>
                            <el-tag size="small" type="info">{{ index + 1 }}</el-tag>
                        </div>
                    </div>

                    <div class="template-actions">
                        <el-button @click="goToStep(1)">上一步</el-button>
                        <el-button type="success" @click="downloadZip" :loading="downloading">
                            下载 ZIP
                        </el-button>
                    </div>
                </div>

                <div class="right-panel">
                    <h3>配置预览</h3>
                    <div v-if="selectedRowIndex !== null" class="preview-content">
                        <pre>{{ previewContent }}</pre>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="js/app.js"></script>
</body>
</html>
```

**src/main/resources/static/css/style.css:**

```css
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: #f5f7fa;
    min-height: 100vh;
}

#app {
    max-width: 1400px;
    margin: 0 auto;
    padding: 20px;
}

.header {
    text-align: center;
    margin-bottom: 30px;
}

.header h1 {
    font-size: 28px;
    color: #303133;
}

.header .subtitle {
    font-size: 16px;
    color: #909399;
    font-weight: normal;
    margin-left: 10px;
}

.steps {
    background: white;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.content {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    min-height: 600px;
}

.step-content {
    display: flex;
    padding: 20px;
    gap: 20px;
    height: 100%;
}

.left-panel {
    flex: 0 0 350px;
}

.right-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
}

.right-panel h3 {
    margin-bottom: 15px;
    color: #303133;
}

.left-panel h3 {
    margin-bottom: 15px;
    color: #303133;
}

.template-card {
    margin-bottom: 10px;
    cursor: pointer;
    transition: all 0.3s;
}

.template-card:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.template-card.selected {
    border: 2px solid #409eff;
    background: #ecf5ff;
}

.template-preview {
    font-size: 12px;
    color: #606266;
    white-space: pre-wrap;
    max-height: 60px;
    overflow: hidden;
}

.monaco-editor-container {
    height: 400px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
}

.placeholder-info {
    margin-top: 10px;
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
    align-items: center;
}

.placeholder-tag {
    font-family: monospace;
}

.template-actions {
    margin-top: 20px;
    display: flex;
    gap: 10px;
    justify-content: flex-end;
}

.data-info {
    margin-bottom: 20px;
}

.field-status {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 8px 0;
}

.match-ok {
    color: #67c23a;
    font-size: 12px;
}

.match-fail {
    color: #f56c6c;
    font-size: 12px;
}

.data-table {
    margin-top: 20px;
}

.data-table h4 {
    margin-bottom: 10px;
    color: #606266;
}

.config-list {
    max-height: 450px;
    overflow-y: auto;
    border: 1px solid #ebeef5;
    border-radius: 4px;
}

.config-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 15px;
    border-bottom: 1px solid #ebeef5;
    cursor: pointer;
    transition: background 0.2s;
}

.config-item:last-child {
    border-bottom: none;
}

.config-item:hover {
    background: #f5f7fa;
}

.config-item.selected {
    background: #ecf5ff;
    border-left: 3px solid #409eff;
}

.config-name {
    font-size: 13px;
    color: #303133;
}

.preview-content {
    flex: 1;
    overflow: auto;
    background: #1e1e1e;
    border-radius: 4px;
    padding: 15px;
}

.preview-content pre {
    color: #d4d4d4;
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 13px;
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-all;
}
```

- [ ] **Step 1: 创建目录结构和 index.html**
- [ ] **Step 2: 编写 style.css**
- [ ] **Step 3: 提交代码**

---

### 1.6 前端逻辑实现

**Files:**
- Create: `src/main/resources/static/js/app.js`
- Create: `src/main/resources/static/js/monaco-editor/loader.js` (从 CDN 下载)

**src/main/resources/static/js/app.js:**

```javascript
const { createApp, ref, computed, watch, nextTick } = Vue;
const { ElMessage } = ElementPlus;

const app = createApp({
    setup() {
        const currentStep = ref(0);
        const templates = ref([]);
        const selectedTemplate = ref(null);
        const templateContent = ref('');
        const placeholders = ref([]);
        const excelData = ref(null);
        const selectedRowIndex = ref(null);
        const previewContent = ref('');
        const downloading = ref(false);
        const editorContainer = ref(null);
        let editor = null;

        // 加载内置模板
        const loadTemplates = async () => {
            try {
                const res = await fetch('/api/templates');
                const json = await res.json();
                if (json.success) {
                    templates.value = json.data;
                }
            } catch (e) {
                ElMessage.error('加载模板失败');
            }
        };

        // 初始化 Monaco Editor
        const initMonaco = async () => {
            if (typeof monaco !== 'undefined') {
                editor = monaco.editor.create(editorContainer.value, {
                    value: templateContent.value,
                    language: 'plaintext',
                    theme: 'vs-dark',
                    minimap: { enabled: false },
                    fontSize: 13,
                    lineNumbers: 'on',
                    scrollBeyondLastLine: false,
                    automaticLayout: true
                });

                editor.onDidChangeModelContent(() => {
                    templateContent.value = editor.getValue();
                    parseTemplate();
                });
            }
        };

        // 解析模板占位符
        const parseTemplate = async () => {
            if (!templateContent.value) return;
            try {
                const res = await fetch('/api/templates/parse', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ content: templateContent.value })
                });
                const json = await res.json();
                if (json.success) {
                    placeholders.value = json.data.placeholders || [];
                }
            } catch (e) {
                console.error('解析模板失败', e);
            }
        };

        // 选择模板
        const selectTemplate = (t) => {
            selectedTemplate.value = t;
            templateContent.value = t.content;
            if (editor) {
                editor.setValue(t.content);
            }
            parseTemplate();
        };

        // 上传模板文件
        const handleTemplateUpload = async (file) => {
            const content = await file.raw.text();
            templateContent.value = content;
            if (editor) {
                editor.setValue(content);
            }
            await parseTemplate();
            selectedTemplate.value = { id: 'custom', name: file.name, content };
        };

        // 上传 Excel
        const handleExcelUpload = async (file) => {
            const formData = new FormData();
            formData.append('file', file.raw);

            try {
                const res = await fetch('/api/excel/parse', {
                    method: 'POST',
                    body: formData
                });
                const json = await res.json();

                if (json.success) {
                    excelData.value = json.data;
                    // 预填充所有行
                    await fillAllRows();
                    ElMessage.success(`解析成功，共 ${json.data.totalRows} 行`);
                } else {
                    ElMessage.error(json.message || '解析失败');
                }
            } catch (e) {
                ElMessage.error('上传失败');
            }
        };

        // 预填充所有行的结果
        const filledResults = ref([]);

        const fillAllRows = async () => {
            if (!templateContent.value || !excelData.value) return;

            filledResults.value = [];
            for (const row of excelData.value.rows) {
                try {
                    const res = await fetch('/api/generate/preview', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            templateContent: templateContent.value,
                            rowData: row
                        })
                    });
                    const json = await res.json();
                    if (json.success) {
                        filledResults.value.push(json.data.content);
                    } else {
                        filledResults.value.push('【渲染失败】');
                    }
                } catch (e) {
                    filledResults.value.push('【渲染失败】');
                }
            }
        };

        // 选择行
        const selectRow = (index) => {
            selectedRowIndex.value = index;
            if (index !== null && filledResults.value[index]) {
                previewContent.value = filledResults.value[index];
            }
        };

        // 步骤切换
        const goToStep = async (step) => {
            if (step === 1 && !selectedTemplate.value) {
                ElMessage.warning('请先选择或上传模板');
                return;
            }
            if (step === 2 && !excelData.value) {
                ElMessage.warning('请先上传 Excel 数据');
                return;
            }
            currentStep.value = step;

            if (step === 2 && selectedRowIndex.value === null) {
                selectRow(0);
            }
        };

        // 下载 ZIP
        const downloadZip = async () => {
            if (!templateContent.value || !excelData.value) return;

            downloading.value = true;
            try {
                const res = await fetch('/api/generate/download', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        templateContent: templateContent.value,
                        rows: excelData.value.rows
                    })
                });

                if (!res.ok) throw new Error('下载失败');

                const blob = await res.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'configs-' + Date.now() + '.zip';
                a.click();
                window.URL.revokeObjectURL(url);
                ElMessage.success('下载成功');
            } catch (e) {
                ElMessage.error('下载失败');
            } finally {
                downloading.value = false;
            }
        };

        // 初始化
        onMounted(async () => {
            await loadTemplates();
            await nextTick();
            await initMonaco();
        });

        return {
            currentStep,
            templates,
            selectedTemplate,
            templateContent,
            placeholders,
            excelData,
            selectedRowIndex,
            previewContent,
            downloading,
            editorContainer,
            selectTemplate,
            handleTemplateUpload,
            handleExcelUpload,
            selectRow,
            goToStep,
            downloadZip
        };
    }
});

app.mount('#app');
```

**src/main/resources/static/js/monaco-editor/loader.js:**
(从 https://cdn.jsdelivr.net/npm/monaco-editor@0.45.0/min/vs/loader.js 下载)

- [ ] **Step 1: 编写 app.js**
- [ ] **Step 2: 下载 Monaco Editor loader.js**
- [ ] **Step 3: 提交代码**

---

### 1.7 构建与测试验证

- [ ] **Step 1: 安装 JDK 17**
- [ ] **Step 2: 执行 `mvn clean package -DskipTests`**
- [ ] **Step 3: 运行 `java -jar target/config-gen.jar`**
- [ ] **Step 4: 浏览器访问 http://localhost:8080**
- [ ] **Step 5: 测试完整流程**
- [ ] **Step 6: 提交代码**

---

### 1.8 打包发布配置

**Files:**
- Modify: `pom.xml` (添加 jpackage 配置)
- Create: `package-exe.sh`

**package-exe.sh:**

```bash
#!/bin/bash
# Windows exe 打包脚本（在 Mac/Linux 上运行交叉编译需要配置）

mvn clean package -DskipTests

# jpackage 打包（需要 JDK 17+ 且安装 jpackage）
# jpackage --type app-image --input target/ --dest dist --name ConfigGen \
#     --main-jar config-gen.jar \
#     --vendor "ConfigGen" \
#     --app-version "1.0.0" \
#     --java-options "-Xmx512m"
```

- [ ] **Step 1: 添加 jpackage 配置到 pom.xml**
- [ ] **Step 2: 编写 package-exe.sh**
- [ ] **Step 3: 提交代码**

---

## Chunk 2: 完整流程测试

**Files:**
- Create: `TESTING.md` (测试文档)

**测试清单：**

| 测试项 | 步骤 | 预期结果 |
|--------|------|----------|
| 内置模板加载 | 打开页面 | 显示 nginx 和 spring-cloud-gateway 模板 |
| 上传模板 | 上传 .conf 文件 | 显示内容和占位符 |
| 模板解析 | 上传模板后 | 识别 `{{字段名}}` 并展示 |
| Excel 解析 | 上传示例 Excel | 正确解析列名和数据行 |
| 字段匹配 | 上传后 | 显示每个占位符与列的匹配状态 |
| 预览 | 点击某行 | 右侧显示填充后的完整配置 |
| 下载 | 点击下载 ZIP | 浏览器下载包含所有配置的 ZIP |
| 错误处理 | 上传缺 filename 列的 Excel | 显示错误提示 |

- [ ] **Step 1: 编写测试文档**
- [ ] **Step 2: 执行完整测试**
- [ ] **Step 3: 修复发现的问题**

---

**Plan complete and saved to `docs/superpowers/plans/2026-05-26-config-gen-plan.md`. Ready to execute?**