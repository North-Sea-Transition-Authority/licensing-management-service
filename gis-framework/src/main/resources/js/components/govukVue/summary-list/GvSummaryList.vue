<template>
  <div v-if="isCard" class="govuk-summary-card" :class="cardClass">
    <div class="govuk-summary-card__title-wrapper">
      <component
        :is="`h${cardTitleHeadingLevel}`"
        v-if="hasCardTitle"
        class="govuk-summary-card__title"
        :class="cardTitleClass"
      >
        <!-- @slot The content of the card title. If content is provided in this slot, the `cardTitle` prop will be ignored. -->
        <slot name="card-title">
          {{ cardTitle }}
        </slot>
      </component>
      <component
        :is="computedActionsWrapperElement"
        v-if="hasSlot('card-actions')"
        class="govuk-summary-card__actions"
        :class="cardActionsClass"
      >
        <!-- @slot A list of `GvSummaryCardAction`s -->
        <slot name="card-actions" />
      </component>
    </div>

    <div class="govuk-summary-card__content">
      <dl class="govuk-summary-list" v-bind="$attrs">
        <!-- @slot A list of `GvSummaryListRow`s -->
        <slot />
      </dl>
    </div>
  </div>
  <dl
    v-else
    class="govuk-summary-list"
    v-bind="$attrs"
    :class="{ 'govuk-summary-list--no-border': !showBorders }"
  >
    <!-- @slot A list of `GvSummaryListRow`s -->
    <slot />
  </dl>
</template>

<script lang="ts">
</script>

<script setup lang="ts">
import type { Ref } from "vue";
import { computed, provide, ref } from "vue";
import getSlotText from "../../gdsComponents/composables/useGetSlotText";
import hasSlot from "../../gdsComponents/composables/useHasSlot";
import { SummaryListCardAction } from "./SummaryListCardAction";
import {
  SummaryListAnyRowHasActionsInjectionKey,
  SummaryListCardActionsCountInjectionKey,
  SummaryListCardTitleInjectionKey,
  SummaryListRegisterCardActionFunctionInjectionKey,
  SummaryListRegisterRowFunctionInjectionKey,
  SummaryListUnregisterCardActionFunctionInjectionKey,
  SummaryListUnregisterRowFunctionInjectionKey,
} from "./SummaryListInjectionKeys";
import { SummaryListRow } from "./SummaryListRow";

const props = defineProps({
  /**
   * Whether to show borders under each row.
   */
  showBorders: {
    type: Boolean,
    default: true,
  },
  /**
   * Classes to add to the summary card. Setting this prop will force the list to display as a summary card.
   * You can bind a string, an array or an object, as with normal [Vue class bindings](https://vuejs.org/guide/essentials/class-and-style.html#binding-html-classes).
   */
  // Card props
  cardClass: {
    type: [String, Array, Object],
    default: "",
  },
  // Card title props
  /**
   * Text to use within the card title. If content is provided in the `card-title` slot, this prop will be ignored.
   */
  cardTitle: String,
  /**
   * Heading level of the card title, from `1` to `6`.
   */
  cardTitleHeadingLevel: {
    type: Number,
    default: 2,
    validator(value: number) {
      return value >= 1 && value <= 6;
    },
  },
  /**
   * Classes to add to the title wrapper.
   * You can bind a string, an array or an object, as with normal [Vue class bindings](https://vuejs.org/guide/essentials/class-and-style.html#binding-html-classes).
   */
  cardTitleClass: {
    type: [String, Array, Object],
    default: "",
  },
  // Card actions props
  /**
   * Classes to add to the actions wrapper.
   * You can bind a string, an array or an object, as with normal [Vue class bindings](https://vuejs.org/guide/essentials/class-and-style.html#binding-html-classes).
   */
  cardActionsClass: {
    type: [String, Array, Object],
    default: "",
  },
});
const cardActions: Ref<SummaryListCardAction[]> = ref([]);
const rows: Ref<SummaryListRow[]> = ref([]);

function registerCardAction(cardAction: SummaryListCardAction) {
  cardActions.value.push(cardAction);
}

function unregisterCardAction(key: Symbol) {
  cardActions.value = cardActions.value.filter((a) => {
    return a.key !== key;
  });
}

const cardActionsCount = computed(() => {
  return cardActions.value.length;
});

function registerRow(row: SummaryListRow) {
  rows.value.push(row);
}

function unregisterRow(key: Symbol) {
  rows.value = rows.value.filter((r) => {
    return r.key !== key;
  });
}

const anyRowHasActions = computed(() => {
  // @ts-ignore: Row gets passed to the callback as a Ref<SummaryListRow> but TS expects it to be a SummaryListRow so complains about the .value
  return rows.value.some(r => r.value.actions.length > 0);
});

provide(SummaryListRegisterCardActionFunctionInjectionKey, registerCardAction);
provide(SummaryListUnregisterCardActionFunctionInjectionKey, unregisterCardAction);
provide(SummaryListCardActionsCountInjectionKey, cardActionsCount);
provide(SummaryListRegisterRowFunctionInjectionKey, registerRow);
provide(SummaryListUnregisterRowFunctionInjectionKey, unregisterRow);
provide(SummaryListAnyRowHasActionsInjectionKey, anyRowHasActions);

const isCard = computed(() => {
  return (
    props.cardTitle !== undefined
    || hasSlot("card-title")
    || hasSlot("card-actions")
    || props.cardClass !== ""
  );
});

const hasCardTitle = computed(() => {
  return props.cardTitle || hasSlot("card-title");
});

// We provide the card title to child components so that actions can use it as visually hidden text at the end of link text
const cardTitleVisuallyHidden = computed(() => {
  if (hasSlot("card-title")) {
    return getSlotText("card-title");
  } else if (props.cardTitle) {
    return props.cardTitle;
  } else {
    return undefined;
  }
});

provide(SummaryListCardTitleInjectionKey, cardTitleVisuallyHidden);

const computedActionsWrapperElement = computed(() => {
  if (cardActionsCount.value > 1) {
    return "ul";
  } else {
    return "div";
  }
});
</script>
