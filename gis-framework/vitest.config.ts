import path from "node:path";
import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src/main/resources/js"),
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["src/test/resources/js/setup-tests.ts"],
    include: ["src/test/resources/js/**/*.test.ts"],
    exclude: [
      "arcgis-node/**",
      "**/visual-regression-tests/**",
      "build/**",
      "node_modules/**",
    ],
  },
});
