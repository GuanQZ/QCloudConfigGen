# 离线前端设计文档

> **日期:** 2026-05-27
> **状态:** 已批准

## 目标

使 QCloudConfigGen 前端完全离线运行，不依赖任何互联网 CDN 资源。

## 架构

保持现有 Vue.js 3 SPA 结构不变，将 CDN 依赖改为本地资源：

```
src/main/resources/static/
├── index.html              # 修改 CDN 链接为本地路径
├── css/
│   └── element-plus/       # Element Plus 样式
├── js/
│   ├── vue/                # Vue.js 3
│   ├── element-plus/       # Element Plus 主库
│   └── @element-plus/       # Element Plus 图标
└── monaco-editor/          # 已存在的 Monaco Editor 本地文件
```

## 依赖版本

| 库 | 版本 | 用途 |
|---|---|---|
| Vue.js | 3.4.x | 前端框架 |
| Element Plus | 2.4.x | UI 组件库 |
| @element-plus/icons | 2.4.x | 图标库 |
| Monaco Editor | 0.44.x | 代码编辑器 |

## 文件变更

### 新增文件

- `src/main/resources/static/css/element-plus/index.css`
- `src/main/resources/static/js/vue/vue.global.prod.js`
- `src/main/resources/static/js/element-plus/index.full.js`
- `src/main/resources/static/js/@element-plus/icons/dist/index.iife.min.js`

### 修改文件

**src/main/resources/static/index.html:**
- 将 `https://cdn.jsdelivr.net/npm/vue@3.4.x/dist/vue.global.prod.js` 改为 `./js/vue/vue.global.prod.js`
- 将 `https://cdn.jsdelivr.net/npm/element-plus@2.4.x/dist/index.full.js` 改为 `./js/element-plus/index.full.js`
- 将 Element Plus CSS CDN 改为本地 `./css/element-plus/index.css`
- 将 Element Plus Icons CDN 改为本地 `./js/@element-plus/icons/dist/index.iife.min.js`

## 离线能力

- ✅ 无任何外部网络请求
- ✅ 在内网隔离环境中完整运行
- ✅ 保持所有现有功能

## 测试验证

1. 断开网络，访问 `http://localhost:8080`
2. 验证页面正常加载
3. 验证所有交互功能正常
4. 验证配置文件生成功能正常