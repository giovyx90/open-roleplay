import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Frontend statico: nessuna logica server. VITE_API_URL punta all'API bridge.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
  build: { outDir: 'dist', sourcemap: false },
});
