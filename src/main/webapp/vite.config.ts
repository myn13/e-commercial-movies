import {defineConfig, loadEnv} from "vite";
// @ts-ignore
import react from "@vitejs/plugin-react";
import * as path from "node:path";

export default defineConfig(({mode}) => {
    const env = loadEnv(mode, process.cwd())

    return {
        plugins: [react()],
        base: env.VITE_API_BASE_URL,
        build: {
            outDir: 'dist',
            assetsDir: 'assets',
            emptyOutDir: true,
        },
        resolve: {
            alias: {
                '@': path.resolve(__dirname, 'src'),
            },
        },
        server: {
            port: 5173,
            host: true,
            proxy: {
                "/api": {
                    target: "http://localhost:8080/MT_movie_project_war",
                    changeOrigin: true,
                    secure: false,
                    ws: true,
                    // Don't rewrite /api - backend now handles /api/* endpoints directly
                    // rewrite: (path) => path.replace(/^\/api/, ''), // Removed - backend handles /api/* now
                    configure: (proxy, _options) => {
                        proxy.on('error', (err, _req, _res) => {
                            console.log('proxy error', err);
                        });
                        proxy.on('proxyReq', (proxyReq, req, _res) => {
                            console.log('Sending Request to the Target:', req.method, req.url);
                        });
                        proxy.on('proxyRes', (proxyRes, req, _res) => {
                            console.log('Received Response from the Target:', proxyRes.statusCode, req.url);
                        });
                    },
                    cookieDomainRewrite: {
                        "localhost:8080": "localhost:5173"
                    },
                    cookiePathRewrite: {
                        "/MT_movie_project_war": "/"
                    }
                },
            },
        },
    }
});
