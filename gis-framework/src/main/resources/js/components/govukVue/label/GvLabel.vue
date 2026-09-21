<template>
  <component :is="computedWrapperElement" :class="isPageHeading ? 'govuk-label-wrapper' : ''">
    <label :for="forId" class="govuk-label" v-bind="$attrs">
      <slot>
        {{ text }}
      </slot>
    </label>
  </component>
</template>

<script lang="ts">
</script>

<script setup lang="ts">
import { computed } from "vue";
import GvFragment from "../util/GvFragment.vue";

defineOptions({
  inheritAttrs: false,
});

const props = defineProps({
  text: String,
  // Can't use 'for' because it's a reserved word in JS so doesn't work in dynamic props (ie we can't do :for="for")
  forId: {
    type: String,
    required: true,
  },
  isPageHeading: {
    type: Boolean,
    default: false,
  },
});

const computedWrapperElement = computed(() => {
  if (props.isPageHeading) {
    return "h1";
  } else {
    return GvFragment;
  }
});
</script>
