const { createApp, ref, computed, watch, nextTick, onMounted } = Vue;
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