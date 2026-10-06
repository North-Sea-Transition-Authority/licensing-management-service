<template>
  <div class="depth-slider-container">
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
    <label :for="depthInputId" class="govuk-label govuk-visually-hidden">Select depth</label>
    <input
      :id="depthInputId"
      type="range"
      class="depth-slider"
      :min="0"
      :max="Math.max(sortedDepths.length - 1, 0)"
      :value="sliderValue"
      :aria-valuetext="options.get(modelValue)"
      :disabled="disabled"
      @input="onSliderInput"
    >
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
const lastIndex = computed(() => Math.max(sortedDepths.value.length - 1, 0));
const sliderValue = computed(() => lastIndex.value - currentIndex.value);
const depthInputId = createUid("depth-slider");

function markerPosition(index: number): number {
  return sortedDepths.value.length <= 1 ? 50 : (1 - index / (sortedDepths.value.length - 1)) * 100;
}
function onSliderInput(event: Event) {
  const depth = sortedDepths.value[lastIndex.value - Number((event.target as HTMLInputElement).value)];
  if (depth !== undefined) {
    modelValue.value = depth;
  }
}
</script>

<style scoped>
.depth-slider-container {
  --track-height: 300px;
  --thumb-height: 20px;
  position: relative;
  height: 320px;
  padding: 10px 0;
  display: flex;
}

.depth-slider {
  writing-mode: vertical-lr;
  width: 8px;
  height: var(--track-height);
  margin: 0;
  -webkit-appearance: none;
  appearance: none;
  background: #0b0c0c;
  cursor: pointer;
  position: relative;
  z-index: 2;
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
  width: 28px;
  height: var(--thumb-height);
  background: #ffffff;
  cursor: pointer;
  border: 2px solid #0b0c0c;
}

.depth-slider::-moz-range-thumb {
  box-sizing: border-box;
  width: 28px;
  height: var(--thumb-height);
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
  position: relative;
  /* Native range thumbs travel track height minus thumb height, inset by half a thumb at each end */
  height: calc(var(--track-height) - var(--thumb-height));
  top: calc(var(--thumb-height) / 2);
  margin-right: 12px;
  /* Every marker shares one grid cell so the column is as wide as the longest label */
  display: grid;
  grid-template-rows: 100%;
}

.depth-marker {
  grid-area: 1 / 1;
  align-self: start;
  justify-self: end;
  position: relative;
  transform: translateY(-50%);
  display: flex;
  flex-direction: row-reverse;
  align-items: center;
  cursor: pointer;
}

.depth-marker::before {
  content: '';
  display: block;
  width: 0.5rem;
  height: 0.2rem;
  background: #505a5f;
  margin-left: 0.4rem;
}

.depth-marker--active::before {
  background: #0b0c0c;
  height: 0.3rem;
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
  user-select: none;
  pointer-events: none;
}

.depth-marker--active .depth-marker-label {
  font-weight: bold;
}
</style>
