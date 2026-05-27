# Merge Output Feature Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add checkbox option for merging output to folders, with user-specified unified filename.

**Architecture:** Toggle checkbox in step 2 enables folder-per-row output in ZIP. Frontend passes `mergeOutput` and `mergedFilename` to backend API, which creates subdirectories using `filename` column values.

**Tech Stack:** Spring Boot, Vue 3, Element Plus, Apache POI

---

## Chunk 1: Backend API Changes

### Task 1: Update ConfigGeneratorService

**Files:**
- Modify: `src/main/java/com/configgen/service/ConfigGeneratorService.java:20-38`

- [ ] **Step 1: Add new method signature with merge output support**

```java
public byte[] generateZip(String templateContent, List<Map<String, String>> rows,
                         boolean mergeOutput, String mergedFilename) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
        for (Map<String, String> row : rows) {
            String filename = row.get("filename");
            if (filename == null || filename.trim().isEmpty()) {
                throw new IllegalArgumentException("filename 列不能为空");
            }
            String content = templateService.fillTemplate(templateContent, row);
            ZipEntry entry;
            if (mergeOutput) {
                // Create folder/filename structure
                entry = new ZipEntry(filename + "/" + mergedFilename);
            } else {
                // Original behavior: filename is the file name
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
```

- [ ] **Step 2: Keep backward-compatible overload**

```java
public byte[] generateZip(String templateContent, List<Map<String, String>> rows) {
    return generateZip(templateContent, rows, false, null);
}
```

- [ ] **Step 3: Add validation method for filename**

```java
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
```

- [ ] **Step 4: Run tests to verify**

Run: `cd /Users/zengguanqin/VSCodeWorkspace/ConfSync && mvn test -Dtest=ConfigGeneratorServiceTest -q`
Expected: PASS (existing tests + new validation)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/configgen/service/ConfigGeneratorService.java
git commit -m "feat: add merge output support to ConfigGeneratorService"
```

### Task 2: Update GenerateController

**Files:**
- Modify: `src/main/java/com/configgen/controller/GenerateController.java:41-61`

- [ ] **Step 1: Update download endpoint to accept new parameters**

```java
@PostMapping("/download")
public ResponseEntity<byte[]> download(@RequestBody Map<String, Object> request) {
    String templateContent = (String) request.get("templateContent");
    @SuppressWarnings("unchecked")
    List<Map<String, String>> rows = (List<Map<String, String>>) request.get("rows");

    Boolean mergeOutputBool = (Boolean) request.get("mergeOutput");
    String mergedFilename = (String) request.get("mergedFilename");

    boolean mergeOutput = mergeOutputBool != null && mergeOutputBool;

    if (templateContent == null || templateContent.trim().isEmpty()) {
        return ResponseEntity.badRequest().build();
    }
    if (rows == null || rows.isEmpty()) {
        return ResponseEntity.badRequest().build();
    }

    // Validate mergedFilename if mergeOutput is enabled
    if (mergeOutput) {
        if (mergedFilename == null || mergedFilename.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("INVALID_MERGED_FILENAME", "统一文件名不能为空"));
        }
        try {
            validateFilename(mergedFilename.trim(), "INVALID_MERGED_FILENAME");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error("INVALID_MERGED_FILENAME", "统一文件名不能为空或包含非法字符"));
        }
    }

    byte[] zipBytes = configGeneratorService.generateZip(templateContent, rows, mergeOutput, mergedFilename);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.setContentDispositionFormData("attachment", "configs.zip");

    return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
}
```

- [ ] **Step 2: Add helper method for validation**

```java
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
```

- [ ] **Step 3: Run tests**

Run: `cd /Users/zengguanqin/VSCodeWorkspace/ConfSync && mvn test -q`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/configgen/controller/GenerateController.java
git commit -m "feat: add mergeOutput and mergedFilename params to download API"
```

---

## Chunk 2: Frontend UI Changes

### Task 3: Add Merge Output UI in Step 2

**Files:**
- Modify: `src/main/resources/static/index.html:302-312`

- [ ] **Step 1: Add merge output checkbox and input after detected fields section**

Find this section around line 302-312:
```html
<div class="field-matching">
    <el-tag v-for="field in detectedFields" :key="field" type="info" class="field-tag" effect="plain">
        {{ field }}
    </el-tag>
    <div v-if="detectedFields.length === 0" style="color: #909399; font-size: 13px;">
        暂无占位符，请先选择模板
    </div>
</div>
```

Add after it:
```html
<el-divider></el-divider>

<div class="merge-output-section" style="margin-top: 15px;">
    <div style="font-size: 14px; margin-bottom: 10px; color: #606266;">合并输出选项</div>
    <el-checkbox v-model="mergeOutput" :disabled="!selectedTemplate">合并输出到同一文件夹</el-checkbox>
    <div v-if="mergeOutput" style="margin-top: 10px;">
        <el-input
            v-model="mergedFilename"
            placeholder="输入统一文件名，如 nginx.conf"
            size="default"
        ></el-input>
        <div v-if="mergedFilenameError" style="color: #f56c6c; font-size: 12px; margin-top: 5px;">
            {{ mergedFilenameError }}
        </div>
    </div>
</div>
```

- [ ] **Step 2: Add new state variables**

Find `const { createApp, ref, reactive, computed, onMounted } = Vue;` around line 395 and add to the setup function's return block, plus add to data:

```javascript
const mergeOutput = ref(false);
const mergedFilename = ref('');
const mergedFilenameError = ref('');
```

- [ ] **Step 3: Add watcher for mergeOutput validation**

```javascript
watch(mergedFilename, (newVal) => {
    if (!newVal || newVal.trim() === '') {
        mergedFilenameError.value = '统一文件名不能为空';
    } else if (/[<>:"\\|?*]/.test(newVal) || newVal.includes('/') || newVal.includes('\\')) {
        mergedFilenameError.value = '文件名不能包含非法字符';
    } else {
        mergedFilenameError.value = '';
    }
});
```

Note: Vue 3 `watch` needs to be imported - ensure it's in the destructured imports from Vue.

- [ ] **Step 4: Add styles for merge output section**

Add to `<style>` section:
```css
.merge-output-section {
    background: #f5f7fa;
    border-radius: 8px;
    padding: 15px;
}
```

- [ ] **Step 5: Test locally** (manual verification)
- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add merge output UI components in step 2"
```

### Task 4: Update Step 3 Config List Display

**Files:**
- Modify: `src/main/resources/static/index.html:350-364`

- [ ] **Step 1: Update config item display for merged mode**

Find the config-item rendering:
```html
<div
    v-for="(config, index) in generatedConfigs"
    :key="index"
    class="config-item"
    :class="{ selected: selectedConfigIndex === index }"
    @click="selectConfig(index)"
>
    <div class="config-name">{{ config.name }}</div>
    <div class="config-path">{{ config.path }}</div>
</div>
```

Replace with:
```html
<div
    v-for="(config, index) in generatedConfigs"
    :key="index"
    class="config-item"
    :class="{ selected: selectedConfigIndex === index }"
    @click="selectConfig(index)"
>
    <template v-if="config.folder">
        <div style="font-size: 16px; font-weight: 600; color: #303133;">{{ config.folder }}</div>
        <div style="font-size: 12px; color: #909399;">{{ config.filename }}</div>
    </template>
    <template v-else>
        <div class="config-name">{{ config.name }}</div>
        <div class="config-path">{{ config.path }}</div>
    </template>
</div>
```

- [ ] **Step 2: Update nextStep logic to pass merge params and handle folder/filename**

Find the `nextStep` function around line 571 and update the step 3 generation logic:

```javascript
const nextStep = () => {
    if (currentStep.value === 1 && tableData.value.length > 0) {
        const templateContentStr = templateContent.value;
        const rows = tableData.value;

        const configs = [];
        let completed = 0;

        rows.forEach((row, index) => {
            fetch('/api/generate/preview', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ templateContent: templateContentStr, rowData: row })
            })
            .then(res => res.json())
            .then(data => {
                if (data.success && data.data) {
                    if (mergeOutput.value && mergedFilename.value) {
                        // Merged mode: folder/filename structure
                        configs.push({
                            name: row.filename + ' / ' + mergedFilename.value,
                            folder: row.filename,
                            filename: mergedFilename.value,
                            path: row.filename + '/' + mergedFilename.value,
                            content: data.data.content
                        });
                    } else {
                        // Original mode
                        configs.push({
                            name: row.filename || `config-${index + 1}.conf`,
                            path: row.filename || `config-${index + 1}.conf`,
                            content: data.data.content
                        });
                    }
                }
                completed++;
                if (completed === rows.length) {
                    generatedConfigs.value = configs;
                    if (configs.length > 0) {
                        selectedConfigIndex.value = 0;
                    }
                    currentStep.value++;
                }
            });
        });
        return;
    }

    if (currentStep.value < 2) {
        currentStep.value++;
    }
};
```

- [ ] **Step 3: Update downloadZip to pass merge params**

Find `downloadZip` around line 529 and update:

```javascript
const downloadZip = () => {
    if (generatedConfigs.value.length === 0) {
        ElementPlus.ElMessage.warning('没有可下载的配置文件');
        return;
    }

    const templateContentStr = templateContent.value;
    const rows = tableData.value;

    const requestBody = {
        templateContent: templateContentStr,
        rows: rows
    };

    if (mergeOutput.value && mergedFilename.value) {
        requestBody.mergeOutput = true;
        requestBody.mergedFilename = mergedFilename.value;
    }

    fetch('/api/generate/download', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
    })
    .then(res => res.blob())
    .then(blob => {
        const timestamp = new Date().toISOString().replace(/[-:T]/g, '').replace(/\.\d{3}/, '');
        const filename = `configs-${timestamp}.zip`;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        ElementPlus.ElMessage.success('下载成功');
    })
    .catch(err => {
        ElementPlus.ElMessage.error('下载失败: ' + err);
    });
};
```

- [ ] **Step 4: Add mergeOutput and mergedFilename to return statement**

Find the return block around line 629 and add:
```javascript
return {
    // ... existing returns
    mergeOutput,
    mergedFilename,
    mergedFilenameError,
    // ... existing returns
};
```

- [ ] **Step 5: Test locally** (manual verification)
- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: update step 3 display and download for merge output mode"
```

---

## Chunk 3: Testing

### Task 5: Add Tests for Merge Output

**Files:**
- Modify: `src/test/java/com/configgen/service/ConfigGeneratorServiceTest.java`

- [ ] **Step 1: Add test for merge output with folder structure**

```java
@Test
void testGenerateZipWithMergeOutput() throws Exception {
    TemplateService templateService = new TemplateService();
    ConfigGeneratorService service = new ConfigGeneratorService(templateService);

    String template = "server {\n    listen {{port}};\n}";
    List<Map<String, String>> rows = List.of(
            Map.of("filename", "beijing", "port", "8080"),
            Map.of("filename", "shanghai", "port", "8081")
    );

    byte[] zip = service.generateZip(template, rows, true, "nginx.conf");
    assertNotNull(zip);
    assertTrue(zip.length > 0);

    // Verify ZIP content with folder structure
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
        ZipEntry entry1 = zis.getNextEntry();
        assertEquals("beijing/nginx.conf", entry1.getName());

        byte[] content1 = zis.readAllBytes();
        assertTrue(new String(content1).contains("8080"));

        ZipEntry entry2 = zis.getNextEntry();
        assertEquals("shanghai/nginx.conf", entry2.getName());

        byte[] content2 = zis.readAllBytes();
        assertTrue(new String(content2).contains("8081"));
    }
}
```

- [ ] **Step 2: Add test for validation of mergedFilename**

```java
@Test
void testGenerateZipWithInvalidMergedFilename() {
    TemplateService templateService = new TemplateService();
    ConfigGeneratorService service = new ConfigGeneratorService(templateService);

    String template = "server {\n    listen {{port}};\n}";
    List<Map<String, String>> rows = List.of(
            Map.of("filename", "beijing", "port", "8080")
    );

    // Test invalid character in mergedFilename
    assertThrows(IllegalArgumentException.class, () -> {
        service.generateZip(template, rows, true, "ng:in.conf");
    });
}
```

- [ ] **Step 3: Run tests**

Run: `cd /Users/zengguanqin/VSCodeWorkspace/ConfSync && mvn test -Dtest=ConfigGeneratorServiceTest -q`
Expected: PASS (5 tests)

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/configgen/service/ConfigGeneratorServiceTest.java
git commit -m "test: add tests for merge output feature"
```

---

## Verification

1. Run all tests: `mvn test -q`
2. Manual verification:
   - Step 1: Select nginx template
   - Step 2: Check "合并输出到同一文件夹" checkbox
   - Step 2: Enter "nginx.conf" in input field
   - Step 2: Upload Excel with `filename` column
   - Step 3: Verify folder/filename display (larger folder name, smaller filename)
   - Step 3: Click download, verify ZIP has `beijing/nginx.conf` structure