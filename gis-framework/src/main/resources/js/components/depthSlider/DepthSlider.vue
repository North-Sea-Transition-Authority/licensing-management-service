<template>
  <div class="depth-slider-container">
    <label :for="depthInputId" class="govuk-label govuk-visually-hidden">Select depth</label>
    <input
      :id="depthInputId"
      type="range"
      class="depth-slider"
      aria-orientation="vertical"
      :min="0"
      :max="Math.max(sortedDepths.length - 1, 0)"
      :value="currentIndex"
      :aria-valuetext="options.get(modelValue)"
      :disabled="disabled"
      @input="onSliderInput"
    >
    <div class="depth-markers">
      <div v-for="(depth, index) in sortedDepths"
           :key="depth"
           class="depth-marker"
           :class="{ 'depth-marker--active': depth === modelValue }"
           :style="{ top: `${markerPosition(index)}%` }"
           @click="!disabled && (modelValue = depth)"
      >
        <span class="depth-marker-label govuk-label" :data-label="options.get(depth)">{{ options.get(depth) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { createUid } from "@/components/govukVue/util/createUid";

interface DepthSliderProps {
  options: Map<number, string>,
  disabled?: boolean,
}

const props = withDefaults(defineProps<DepthSliderProps>(), {
  disabled: false,
});

const modelValue = defineModel<number>({ required: true });

const sortedDepths = computed(() => [...props.options.keys()].sort((a, b) => a - b));
const currentIndex = computed(() => Math.max(sortedDepths.value.indexOf(modelValue.value), 0));
const depthInputId = createUid("depth-slider");

function markerPosition(index: number): number {
  return sortedDepths.value.length <= 1 ? 50 : (1 - index / (sortedDepths.value.length - 1)) * 100;
}
function onSliderInput(event: Event) {
  const depth = sortedDepths.value[Number((event.target as HTMLInputElement).value)];
  if (depth !== undefined) {
    modelValue.value = depth;
  }
}
</script>

<style scoped>
.depth-slider-container {
  --thumb-length: 20px;
  --thumb-breadth: 28px;
  position: relative;
  height: 100%;
  padding-right: calc(var(--thumb-breadth) + 0.25rem);
}

.depth-slider {
  writing-mode: vertical-lr;
  direction: rtl;
  position: absolute;
  top: 0;
  right: calc((var(--thumb-breadth) - 8px) / 2);
  width: 8px;
  height: 100%;
  margin: 0;
  -webkit-appearance: none;
  appearance: none;
  background: #0b0c0c;
  cursor: pointer;
}

.depth-slider:focus {
  outline: none;
  box-shadow: 0 0 0 3px #ffdd00;
}

.depth-slider:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.depth-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  box-sizing: border-box;
  width: var(--thumb-breadth);
  height: var(--thumb-length);
  background: #ffffff;
  cursor: pointer;
  border: 2px solid #0b0c0c;
}

.depth-slider::-moz-range-thumb {
  box-sizing: border-box;
  width: var(--thumb-breadth);
  height: var(--thumb-length);
  background: #ffffff;
  border-radius: 0;
  cursor: pointer;
  border: 2px solid #0b0c0c;
}

.depth-slider:focus::-webkit-slider-thumb {
  border-color: #ffdd00;
}

.depth-slider:focus::-moz-range-thumb {
  border-color: #ffdd00;
}

.depth-markers {
  display: grid;
  grid-template-rows: 100%;
  height: calc(100% - var(--thumb-length));
  margin-top: calc(var(--thumb-length) / 2);
}

.depth-marker {
  grid-area: 1 / 1;
  align-self: start;
  justify-self: end;
  position: relative;
  display: flex;
  flex-direction: row-reverse;
  align-items: center;
  transform: translateY(-50%);
  cursor: pointer;
}

.depth-marker::before {
  content: '';
  flex: none;
  width: 0.5rem;
  height: 0.2rem;
  margin-left: 0.5rem;
  background: #505a5f;
}

.depth-marker--active::before {
  height: 0.3rem;
  background: #0b0c0c;
}

.depth-marker-label {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-end;
  white-space: nowrap;
  margin-bottom: 0;
}

/* Invisible bold copy so the label is always as wide as its bold active state */
.depth-marker-label::after {
  content: attr(data-label);
  font-weight: bold;
  height: 0;
  overflow: hidden;
  visibility: hidden;
}

.depth-marker--active .depth-marker-label {
  font-weight: bold;
}
</style>
