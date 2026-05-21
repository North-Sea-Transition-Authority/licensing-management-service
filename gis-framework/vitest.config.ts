import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: "jsdom",
    setupFiles: ["src/test/resources/js/setup-tests.ts"],
    include: ["src/test/resources/js/**/*.test.ts"],
    exclude: [
      "arcgis-node/**",
      "build/**",
      "node_modules/**",
    ],
  },
});
