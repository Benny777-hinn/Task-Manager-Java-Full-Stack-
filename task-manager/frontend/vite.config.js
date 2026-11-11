import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Vite config.
// The proxy in "server" is what lets the React dev server (5173) talk to the
// Spring Boot API (8080) without CORS problems: any request to /api is forwarded.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
