/**
 * Monaco Editor Loader - Local version
 * Loads Monaco Editor from local assets
 */

(function() {
    'use strict';

    // Monaco Editor configuration - use local paths
    window.MonacoEnvironment = {
        getWorkerUrl: function(moduleId, label) {
            return `data:text/javascript;charset=utf-8,${encodeURIComponent(`
                self.MonacoEnvironment = { baseUrl: '${window.location.origin}/js/monaco-editor/min/min/' };
                importScripts('${window.location.origin}/js/monaco-editor/min/min/vs/base/worker/workerMain.js');
            `)}`;
        }
    };

    // Load Monaco Editor from local path
    var monacoScript = document.createElement('script');
    monacoScript.src = '/js/monaco-editor/min/min/vs/loader.js';
    monacoScript.onload = function() {
        require.config({
            paths: {
                'vs': '/js/monaco-editor/min/min/vs'
            }
        });

        require(['vs/editor/editor.main'], function() {
            console.log('Monaco Editor loaded successfully');
            window.dispatchEvent(new Event('monaco-loaded'));
        });
    };
    monacoScript.onerror = function() {
        console.error('Failed to load Monaco Editor from local');
    };
    document.head.appendChild(monacoScript);
})();