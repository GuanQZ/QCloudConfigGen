# 离线前端实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 使前端完全离线运行，bundling 所有 CDN 依赖到本地

**Architecture:** 下载 Element Plus、Vue.js 的 CSS/JS 文件到本地 static 目录，修改 index.html 引用本地资源而非 CDN

**Tech Stack:** Vue.js 3.4.x, Element Plus 2.4.x, Monaco Editor (已有本地文件)

---

## Chunk 1: 准备本地资源目录结构

**Files:**
- Create: `src/main/resources/static/css/element-plus/index.css`
- Create: `src/main/resources/static/js/vue/vue.global.prod.js`
- Create: `src/main/resources/static/js/element-plus/index.full.js`
- Create: `src/main/resources/static/js/@element-plus/icons/dist/index.iife.min.js`

- [ ] **Step 1: 创建目录结构**

```bash
mkdir -p src/main/resources/static/css/element-plus
mkdir -p src/main/resources/static/js/vue
mkdir -p src/main/resources/static/js/element-plus
mkdir -p src/main/resources/static/js/@element-plus/icons/dist
```

- [ ] **Step 2: 下载 Vue.js 3.4.0**

Run: `curl -o src/main/resources/static/js/vue/vue.global.prod.js https://cdn.jsdelivr.net/npm/vue@3.4.0/dist/vue.global.prod.js`

- [ ] **Step 3: 下载 Element Plus 2.4.4 CSS**

Run: `curl -o src/main/resources/static/css/element-plus/index.css https://cdn.jsdelivr.net/npm/element-plus@2.4.4/dist/index.css`

- [ ] **Step 4: 下载 Element Plus 2.4.4 JS**

Run: `curl -o src/main/resources/static/js/element-plus/index.full.js https://cdn.jsdelivr.net/npm/element-plus@2.4.4/dist/index.full.js`

- [ ] **Step 5: 下载 Element Plus Icons**

Run: `curl -o src/main/resources/static/js/@element-plus/icons/dist/index.iife.min.js https://cdn.jsdelivr.net/npm/@element-plus/icons-vue@2.3.8/dist/index.iife.min.js`

- [ ] **Step 6: 验证文件存在且大小正确**

Run: `ls -la src/main/resources/static/js/vue/ src/main/resources/static/css/element-plus/ src/main/resources/static/js/element-plus/ src/main/resources/static/js/@element-plus/icons/dist/`

Expected: 每个目录有对应文件，大小 > 0

- [ ] **Step 7: 提交**

```bash
git add src/main/resources/static/css/ src/main/resources/static/js/vue/ src/main/resources/static/js/element-plus/ src/main/resources/static/js/@element-plus/
git commit -m "feat: add local CDN resources for offline support"
```

---

## Chunk 2: 修改 index.html 使用本地资源

**Files:**
- Modify: `src/main/resources/static/index.html:1-712`

- [ ] **Step 1: 修改 Element Plus CSS 链接**

Old (line 8):
```html
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/element-plus@2.4.4/dist/index.css">
```

New:
```html
<link rel="stylesheet" href="./css/element-plus/index.css">
```

- [ ] **Step 2: 修改 Element Plus Icons CSS 链接**

Old (line 10):
```html
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@element-plus/icons-vue@2.3.8/dist/index.css">
```

New:
```html
<link rel="stylesheet" href="./js/@element-plus/icons/dist/index.iife.min.js">
```

**注意:** Element Plus Icons 的 CSS 实际上不需要单独引用，Icons 是通过 JS 注册组件方式使用的

- [ ] **Step 3: 修改 Vue.js 引用**

Old (line 416):
```html
<script src="https://cdn.jsdelivr.net/npm/vue@3.4.0/dist/vue.global.js"></script>
```

New:
```html
<script src="./js/vue/vue.global.prod.js"></script>
```

- [ ] **Step 4: 修改 Element Plus JS 引用**

Old (line 418):
```html
<script src="https://cdn.jsdelivr.net/npm/element-plus@2.4.4/dist/index.full.js"></script>
```

New:
```html
<script src="./js/element-plus/index.full.js"></script>
```

- [ ] **Step 5: 验证修改**

Run: `grep -E "(cdn\.jsdelivr|src=\")" src/main/resources/static/index.html | grep -v "monaco-editor"`

Expected: 无 cdn.jsdelivr 引用，所有 src 指向本地 `./` 路径

- [ ] **Step 6: 提交**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: update index.html to use local CDN resources"
```

---

## Chunk 3: 测试离线运行

**Files:**
- Test: `src/main/resources/static/index.html`

- [ ] **Step 1: 启动应用**

Run: `cd /Users/zengguanqin/VSCodeWorkspace/ConfSync && ./mvnw spring-boot:run`

Expected: 应用启动，控制台显示 "QCloud 配置文件生成工具已启动"

- [ ] **Step 2: 验证页面加载**

打开浏览器访问 `http://localhost:8080`，验证:
- 页面正常显示
- Element Plus 组件正常渲染
- 模板选择卡片正常显示
- 无网络请求失败错误

- [ ] **Step 3: 测试基本功能**

1. 选择 Nginx 模板
2. 验证 Monaco Editor 正常加载
3. 上传 Excel 测试文件（使用 ExampleController 下载示例）
4. 验证数据解析正常

- [ ] **Step 4: 停止应用**

按 Ctrl+C 停止 Spring Boot 应用

- [ ] **Step 5: 最终提交**

```bash
git add -A
git commit -m "feat: complete offline frontend support"
```

---

## 验证清单

- [ ] `index.html` 无 CDN 引用
- [ ] 所有本地资源文件存在且大小 > 0
- [ ] 应用启动无错误
- [ ] 页面正常渲染
- [ ] 模板编辑功能正常
- [ ] Excel 上传解析功能正常
- [ ] 配置下载功能正常