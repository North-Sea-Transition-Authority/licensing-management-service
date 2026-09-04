import { createReadStream } from "node:fs";
import { stat } from "node:fs/promises";
import { createRequire } from "node:module";
import { dirname, join, normalize, resolve, sep } from "node:path";
import vue from "@vitejs/plugin-vue";
import { playwright } from "@vitest/browser-playwright";
import { defineConfig } from "vitest/config";
import type { Plugin } from "vitest/config";

const govukAssetsDir = join(
  dirname(createRequire(import.meta.url).resolve("govuk-frontend/package.json")),
  "dist/govuk/assets",
);

function govukAssets(): Plugin {
  return {
    name: "gis-visual-tests-govuk-assets",
    configureServer(server) {
      server.middlewares.use("/assets", (req, res, next) => {
        const path = decodeURIComponent(new URL(req.url ?? "/", "http://localhost").pathname);
        const file = join(govukAssetsDir, normalize(path));
        if (!file.startsWith(govukAssetsDir + sep)) {
          next();
          return;
        }
        stat(file)
          .then((stats) => {
            if (!stats.isFile()) {
              next();
              return;
            }
            res.setHeader("Content-Type", file.endsWith(".woff2") ? "font/woff2" : "font/woff");
            createReadStream(file).pipe(res);
          })
          .catch(() => next());
      });
    },
  };
}

const viewport = { width: 1280, height: 1024 };

export default defineConfig({
  plugins: [vue(), govukAssets()],
  publicDir: "public-test",
  resolve: {
    alias: {
      "@": resolve(__dirname, "src/main/resources/js"),
    },
  },
  define: {
    "process.env.NODE_ENV": JSON.stringify("test"),
  },
  test: {
    browser: {
      enabled: true,
      viewport,
      provider: playwright({
        contextOptions: {
          viewport,
        },
      }),
      instances: [{ browser: "chromium" }],
      expect: {
        toMatchScreenshot: {
          comparatorName: "pixelmatch",
          comparatorOptions: {
            threshold: 0.1,
            allowedMismatchedPixelRatio: 0.01,
          },
          screenshotOptions: {
            animations: "disabled",
          },
        },
      },
    },
    testTimeout: 60000,
    reporters: ["default", "html"],
    include: ["src/test/resources/js/visual-regression-tests/**/*.visual.test.ts"],
    setupFiles: [
      "src/test/resources/js/visual-regression-tests/visual-styles.ts",
      "src/test/resources/js/visual-regression-tests/setup.ts",
    ],
  },
});
