<template>
  <div class="depth-slider-container">
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
    <div class="depth-markers">
      <div v-for="(depth, index) in sortedDepths"
           :key="depth"
           class="depth-marker"
           :class="{
             'depth-marker--active': depth === modelValue,
             'depth-marker--start': markerPosition(index) === 0,
             'depth-marker--end': markerPosition(index) === 100,
           }"
           :style="{ left: `${markerPosition(index)}%` }"
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
  --thumb-width: 20px;
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 10px 0;
}

.depth-slider {
  width: 100%;
  height: 8px;
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
  width: var(--thumb-width);
  height: 28px;
  background: #ffffff;
  cursor: pointer;
  border: 2px solid #0b0c0c;
}

.depth-slider::-moz-range-thumb {
  box-sizing: border-box;
  width: var(--thumb-width);
  height: 28px;
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
  /* Native range thumbs travel the track width minus the thumb width, inset by half a thumb at each end */
  width: calc(100% - var(--thumb-width));
  left: calc(var(--thumb-width) / 2);
  margin-top: 12px;
  height: 2.5rem;
}

.depth-marker {
  position: absolute;
  top: 0;
  width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
}

/* Keep the labels at either end inside the track rather than overhanging it */
.depth-marker--start {
  align-items: flex-start;
}

.depth-marker--end {
  align-items: flex-end;
}

.depth-marker::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  transform: translateX(-50%);
  width: 0.2rem;
  height: 0.5rem;
  background: #505a5f;
}

.depth-marker--active::before {
  width: 0.3rem;
  background: #0b0c0c;
}

.depth-marker-label {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  margin-top: 0.75rem;
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
