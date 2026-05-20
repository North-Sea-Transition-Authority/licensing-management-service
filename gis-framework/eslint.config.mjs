import antfu from "@antfu/eslint-config";

const resourceVueFiles = ["src/main/resources/js/**/*.vue"];
const resourceTestFiles = ["src/test/resources/js/**/*.ts"];
const arcgisNodeTypeScriptFiles = [
  "arcgis-node/*.ts",
  "arcgis-node/src/**/*.ts",
  "arcgis-node/tests/**/*.ts",
];

export default antfu({
  ignores: [
    "**/*",
    "!src/",
    "!src/main/",
    "!src/main/resources/",
    "!src/main/resources/js/",
    "!src/main/resources/js/**/",
    "!src/main/resources/js/**/*.vue",
    "!src/test/",
    "!src/test/resources/",
    "!src/test/resources/js/",
    "!src/test/resources/js/**/",
    "!src/test/resources/js/**/*.ts",
    "!arcgis-node/",
    "!arcgis-node/*.ts",
    "!arcgis-node/src/",
    "!arcgis-node/src/**/",
    "!arcgis-node/src/**/*.ts",
    "!arcgis-node/tests/",
    "!arcgis-node/tests/**/",
    "!arcgis-node/tests/**/*.ts",
  ],
  markdown: false,
  stylistic: {
    // indent: 2, // 4, or 'tab'
    quotes: "double", // or 'double'
    semi: true,
    overrides: {
      "style/brace-style": ["error", "1tbs"],
      // "style/indent-binary-ops": ["error", 4],
      "style/member-delimiter-style": ["error", {
        multiline: {
          delimiter: "comma",
          requireLast: true,
        },
        singleline: {
          delimiter: "comma",
          requireLast: false,
        },
      }],
    },
  },
  typescript: {
    files: [
      ...resourceTestFiles,
      ...arcgisNodeTypeScriptFiles,
    ],
    overrides: {
      "ts/no-explicit-any": "error",
      "ts/switch-exhaustiveness-check": ["error", {
        considerDefaultExhaustiveForUnions: true,
      }],
      "ts/consistent-type-definitions": "off",
    },
  },
  unicorn: {
    overrides: {
      // Auto fixing this corrupts vue files so it's not worth having on
      "unicorn/prefer-includes": "off",
    },
  },
  vue: {
    files: resourceVueFiles,
    overrides: {
      "vue/component-name-in-template-casing": "off",
      "vue/block-order": ["error", { order: ["template", "script", "style"] }],
      "vue/v-slot-style": "off",
      "vue/component-options-name-casing": "off",
      "vue/custom-event-name-casing": "off",
      "vue/html-closing-bracket-newline": "off",
      "vue/html-closing-bracket-spacing": "off",
      "vue/first-attribute-linebreak": "off",
      "vue/object-curly-spacing": "off",
    },
  },
  languageOptions: {
    globals: {
      $: "readonly",
    },
    parserOptions: {
      projectService: true,
      tsconfigRootDir: import.meta.dirname,
    },
  },
}, {
  files: [
    ...resourceTestFiles,
    "arcgis-node/tests/**/*.ts",
  ],
  rules: {
    "ts/no-explicit-any": "off",
  },
});
