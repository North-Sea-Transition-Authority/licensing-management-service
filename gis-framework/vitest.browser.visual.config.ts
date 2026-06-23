import vue from "@vitejs/plugin-vue";
import { playwright } from "@vitest/browser-playwright";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [vue()],
  publicDir: "public-test",
  define: {
    "process.env.NODE_ENV": JSON.stringify("test"),
  },
  test: {
    browser: {
      enabled: true,
      provider: playwright({
        contextOptions: {
          viewport: {
            width: 1280,
            height: 720
          }
        }
      }),
      instances: [{ browser: "chromium" }],
      expect: {
        toMatchScreenshot: {
          comparatorName: "pixelmatch",
          comparatorOptions: {
            threshold: 0.1,
            allowedMismatchedPixelRatio: 0.01,
          },
        },
      },
    },
    reporters: ["default", "html"],
    include: ["src/test/resources/js/visual-regression-tests/**/*.visual.test.ts"],
    setupFiles: ["src/test/resources/js/visual-regression-tests/setup.ts"],
  },
});
