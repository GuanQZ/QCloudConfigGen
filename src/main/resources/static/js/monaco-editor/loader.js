/**
 * Monaco Editor Loader
 * This is a minimal loader that loads Monaco Editor from CDN
 * In production, you would replace this with the actual Monaco Editor assets
 */

(function() {
    'use strict';

    // Monaco Editor configuration
    window.MonacoEnvironment = {
        getWorkerUrl: function(moduleId, label) {
            return `data:text/javascript;charset=utf-8,${encodeURIComponent(`
                self.MonacoEnvironment = { baseUrl: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/' };
                importScripts('https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs/base/worker/workerMain.js');
            `)}`;
        }
    };

    // Load Monaco Editor from CDN
    var monacoScript = document.createElement('script');
    monacoScript.src = 'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs/loader.js';
    monacoScript.onload = function() {
        require.config({
            paths: {
                'vs': 'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs'
            }
        });

        require(['vs/editor/editor.main'], function() {
            console.log('Monaco Editor loaded successfully');
            // Dispatch event for components waiting for Monaco
            window.dispatchEvent(new Event('monaco-loaded'));
        });
    };
    monacoScript.onerror = function() {
        console.error('Failed to load Monaco Editor from CDN');
    };
    document.head.appendChild(monacoScript);
})();