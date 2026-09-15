import { vanillaExtractPlugin } from "@vanilla-extract/vite-plugin";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

/** Vite configuration for the standalone public website. */
export default defineConfig({
  plugins: [react(), vanillaExtractPlugin()],
  server: {
    port: 45173,
    strictPort: true,
  },
  preview: {
    port: 45173,
    strictPort: true,
  },
});
