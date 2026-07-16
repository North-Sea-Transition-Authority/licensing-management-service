import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";

export default defineConfig(({ mode }) => {
  return {
    plugins: [vue()],
    define: {
      "process.env.NODE_ENV": JSON.stringify(mode),
    },
    server: {
      port: 5173,
      strictPort: true,
      cors: true,
      origin: "http://localhost:5173",
    },
    build: {
      target: "es2019",
      minify: "oxc",
      outDir: "src/main/resources/public/gis/dist",
      emptyOutDir: true,
      lib: {
        entry: "src/main/resources/js/gis-all.ts",
        name: "GisFramework",
        formats: ["es"],
        fileName: () => "gis-bundle.js",
        cssFileName: "gis-framework",
      },
      rolldownOptions: {
        output: {
          chunkFileNames: "[name]-[hash].mjs",
          assetFileNames: assetInfo => assetInfo.name?.endsWith(".css")
            ? "gis-framework[extname]"
            : "[name]-[hash][extname]",
        },
      },
      sourcemap: false,
    },
  };
});
