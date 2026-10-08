<template>
  <div v-if="divider" class="govuk-radios__divider">
    {{ divider }}
  </div>
  <div class="govuk-radios__item">
    <input
      :id="computedId"
      class="govuk-radios__input"
      :name="name"
      type="radio"
      :value="value"
      :disabled="disabled"
      :aria-controls="conditionalId"
      :aria-expanded="hasConditional && radiosModelValue === value"
      :aria-describedby="hintId"
      :checked="radiosModelValue === value"
      v-bind="$attrs"
      @change="radiosUpdateModelValueFunction(value)"
    >
    <gv-label :text="label" class="govuk-radios__label" :class="labelClass" :for-id="computedId">
      <!-- @slot The content of the label. If content is provided in this slot, the `label` prop will be ignored. -->
      <slot />
    </gv-label>
    <gv-hint v-if="hasHint" :id="hintId" :text="hint" class="govuk-radios__hint" :class="hintClass">
      <!-- @slot The content of the hint. If content is provided in this slot, the `hint` prop will be ignored. -->
      <slot name="hint" />
    </gv-hint>
  </div>
  <div
    v-if="hasConditional"
    :id="conditionalId"
    class="govuk-radios__conditional"
    :class="{ 'govuk-radios__conditional--hidden': value !== radiosModelValue }"
  >
    <!-- @slot Content to show if the radio is checked. -->
    <slot name="conditional" />
  </div>
</template>

<script lang="ts">
</script>

<script setup lang="ts">
import { computed, inject, ref } from "vue";
import hasSlot from "../../gdsComponents/composables/useHasSlot";
import GvHint from "../hint/GvHint.vue";
import GvLabel from "../label/GvLabel.vue";
import { createUid } from "../util/createUid";
import {
  RadiosModelValueInjectionKey,
  RadiosNameInjectionKey,
  RadiosUpdateModelValueFunctionInjectionKey,
} from "./RadiosInjectionKeys";

defineOptions({
  inheritAttrs: false,
});

const props = defineProps({
  /**
   * The ID for this radio.
   *
   * If you don't provide an ID, one will be generated automatically.
   */
  id: String,
  /**
   * The value of this radio. The parent `gv-radios`'  `v-model` will be set to this value if the radio is selected.
   */
  value: {
    type: [String, Number, Boolean],
    required: true,
  },
  /**
   * The divider text to show above the radio. This should usually be 'or'.
   */
  divider: String,
  /**
   * If `true`, radio will be disabled.
   */
  disabled: Boolean,
  // label props
  /**
   * Text to use within the label. If content is provided in the default slot, this prop will be ignored.
   */
  label: String,
  /**
   * Classes to add to the label tag. You can bind a string, an array or an object, as with normal [Vue class bindings](https://vuejs.org/guide/essentials/class-and-style.html#binding-html-classes).
   */
  labelClass: {
    type: [String, Array, Object],
    default: "",
  },
  // hint props
  /**
   * Text to use within the hint. If content is provided in the `hint` slot, this prop will be ignored.
   */
  hint: String,
  /**
   * Classes to add to the hint span tag. You can bind a string, an array or an object, as with normal [Vue class bindings](https://vuejs.org/guide/essentials/class-and-style.html#binding-html-classes).
   */
  hintClass: {
    type: [String, Array, Object],
    default: "",
  },
});

const radiosModelValue = inject(RadiosModelValueInjectionKey, ref(undefined));
const radiosUpdateModelValueFunction = inject(RadiosUpdateModelValueFunctionInjectionKey, () => {});
const name = inject(RadiosNameInjectionKey);

const hasHint = computed(() => {
  return props.hint || hasSlot("hint");
});

const hintId = computed(() => {
  return hasHint.value ? `${computedId.value}-hint` : undefined;
});

const hasConditional = computed(() => {
  return hasSlot("conditional");
});

const conditionalId = computed(() => {
  return hasConditional.value ? `conditional-${computedId.value}` : undefined;
});

const computedId = computed(() => {
  return props.id ? props.id : createUid("gv-radio");
});
</script>
