# QCloudConfigGen 合并输出功能设计

> **日期：** 2026-05-27
> **功能：** ZIP打包时提供合并输出选项，支持按文件夹组织配置文件

---

## 一、需求背景

用户场景：
- 36台nginx服务器对应36家分行
- 配置文件结构相同，但参数不同（端口、域名等）
- 用户希望能够将每个分行的配置放入独立文件夹，便于管理

---

## 二、功能设计

### 1. UI位置

**步骤2（数据绑定）左侧面板**
- 位置：`上传数据文件` 区域下方

### 2. 新增UI组件

```
┌─────────────────────────────┐
│ 已识别的占位符                │
│ [port] [server_name] ...     │
└─────────────────────────────┘

┌─────────────────────────────┐
│ ☐ 合并输出到同一文件夹        │  ← 复选框
│   └─ 文件名: [___________]  │  ← 勾选后显示文本框
└─────────────────────────────┘
```

### 3. 组件状态

| 状态 | 复选框 | 文本框 |
|------|--------|--------|
| 未上传Excel | 可用，默认不勾 | 隐藏 |
| 已上传Excel，未勾选 | 可用，默认不勾 | 隐藏 |
| 已上传Excel，已勾选 | 已勾选 | 显示，允许输入 |

**说明：** 复选框在选择模板后即可使用，不依赖Excel上传。文本输入框在勾选后显示。

---

## 三、步骤3展示变化

### 场景A：不勾选（原有逻辑不变）

**配置文件列表：**
```
├── beijing.conf
├── shanghai.conf
└── guangzhou.conf
```

**预览区域：**
```
┌─────────────────────────────┐
│ beijing.conf               │  ← 单行显示
│ server { ... }             │
└─────────────────────────────┘
```

### 场景B：勾选后

**配置文件列表：**
```
├── beijing / nginx.conf
├── shanghai / nginx.conf
└── guangzhou / nginx.conf
```

**预览区域：**
```
┌─────────────────────────────┐
│ beijing                     │  ← 大字，文件夹名
│ nginx.conf                  │  ← 小字，统一文件名
├─────────────────────────────┤
│ server { ... }              │
└─────────────────────────────┘
```

---

## 四、ZIP打包逻辑

### 场景A：不勾选（原有逻辑）

```
configs-{timestamp}.zip
├── beijing.conf
├── shanghai.conf
└── guangzhou.conf
```

- 每个配置文件直接放在zip根目录
- 文件名：`filename`列值

### 场景B：勾选后

```
configs-{timestamp}.zip
├── beijing/
│   └── nginx.conf
├── shanghai/
│   └── nginx.conf
└── guangzhou/
    └── nginx.conf
```

- 按 `filename` 列值创建文件夹
- 文件夹内放入统一文件名的配置文件

---

## 五、后端改动

### 5.1 API请求参数变化

**POST /api/generate/download**

**现有请求体：**
```json
{
  "templateContent": "...",
  "rows": [...]
}
```

**新增请求体：**
```json
{
  "templateContent": "...",
  "rows": [...],
  "mergeOutput": true,
  "mergedFilename": "nginx.conf"
}
```

- `mergeOutput`: boolean，是否启用合并模式
- `mergedFilename`: string，统一文件名（仅在mergeOutput=true时使用）

### 5.2 ConfigGeneratorService改动

```java
public byte[] generateZip(String templateContent, List<Map<String, String>> rows,
                         boolean mergeOutput, String mergedFilename)
```

**逻辑：**
- `mergeOutput=false`：原有逻辑，`filename`列值作为文件名
- `mergeOutput=true`：
  - 按 `filename` 列值创建文件夹
  - 所有文件使用 `mergedFilename` 作为文件名
  - 每个文件夹内只放一个配置文件

### 5.3 校验规则

**mergedFilename 校验：**
- 不能为空字符串（trim后）
- 不能包含路径分隔符：`/`、`\`
- 不能包含Windows保留字符：`<`、`>`、`:`、`"`、`|`、`?`、`*`
- 最大长度：255字符
- 校验失败返回错误码 `INVALID_MERGED_FILENAME`

**filename列值（文件夹名）校验：**
- 同样不能包含路径分隔符和Windows保留字符
- 校验失败返回错误码 `INVALID_FOLDER_NAME`

**错误响应格式：**
```json
{
  "success": false,
  "error": "INVALID_MERGED_FILENAME",
  "message": "统一文件名不能为空或包含非法字符"
}
```

---

## 六、前端状态管理

### 6.1 新增状态

```javascript
const mergeOutput = ref(false);        // 是否启用合并模式
const mergedFilename = ref('');         // 统一文件名
const configList = ref([]);            // 生成的配置列表（含folder/filename）
```

### 6.2 generatedConfigs结构变化

**不勾选时：**
```javascript
{
  name: 'beijing.conf',
  path: 'beijing.conf',
  content: '...'
}
```

**勾选后：**
```javascript
{
  name: 'beijing / nginx.conf',
  folder: 'beijing',      // 新增
  filename: 'nginx.conf', // 新增
  path: 'beijing/nginx.conf',
  content: '...'
}
```

---

## 七、测试用例

### 场景1：原始模式（mergeOutput=false）
- 上传Excel，点击下载
- ZIP包含 `beijing.conf`、`shanghai.conf` 等
- 配置文件列表显示文件名

### 场景2：合并模式（mergeOutput=true）
- 勾选复选框，输入 `nginx.conf`
- 点击下载
- ZIP包含 `beijing/nginx.conf`、`shanghai/nginx.conf` 等
- 配置文件列表显示 `beijing / nginx.conf`

### 场景3：切换回原始模式
- 勾选后取消勾选
- 配置文件列表恢复原始显示
- 重新下载得到原始ZIP结构

### 场景4：输入校验
- 勾选后不输入统一文件名 → 提示不能为空
- 输入 `/` 或 `\` → 提示包含非法字符
- 输入 `config:nginx` → 提示包含非法字符

### 场景5：中间态切换
- 切换模式后，ZIP结构和列表同步更新
- 预览区域显示内容不变（因为配置内容由数据决定）

---

## 八、向后兼容

- 默认 `mergeOutput=false`，所有现有逻辑不变
- API请求不传这两个参数时视为 `mergeOutput=false`
- 不影响现有用户使用习惯