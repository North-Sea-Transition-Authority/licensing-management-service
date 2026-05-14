import {defineConfig} from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  define: {
    'process.env.NODE_ENV': JSON.stringify('production'),
  },
  build: {
    target: "es2019",
    minify: "oxc",
    outDir: 'src/main/resources/public/gis/dist',
    emptyOutDir: true,
    lib: {
      entry: 'src/main/resources/js/gis-all.ts',
      name: 'GisFramework',
      formats: ['es'],
      fileName: () => 'gis-bundle.js',
      cssFileName: 'gis-framework',
    },
    rolldownOptions: {
      output: {
        chunkFileNames: '[name]-[hash].mjs',
        assetFileNames: assetInfo => assetInfo.name?.endsWith('.css')
          ? 'gis-framework[extname]'
          : '[name]-[hash][extname]',
      },
    },
    sourcemap: false,
  },
});
