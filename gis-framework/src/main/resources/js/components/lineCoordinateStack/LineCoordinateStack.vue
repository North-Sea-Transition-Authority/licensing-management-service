<template>
  <gv-summary-list class="line-coordinate-stack" card-title="Line points">
    <div v-if="stackedPoints.length === 0" class="govuk-summary-list__row govuk-summary-list__row--no-actions">
      <dt class="govuk-summary-list__key">
        No points added yet
      </dt>
    </div>
    <gv-summary-list-row
      v-for="(point, index) in stackedPoints"
      :key="`${point.id}-${index}`"
      :key-text="`Point ${stackedPoints.length - index}`"
      :value-text="`${point.displayName}`"
    >
      <template v-if="index === 0" #actions>
        <gv-summary-list-row-action
          text="Remove"
          @click.prevent="emit('undo-last-point')"
        />
      </template>
    </gv-summary-list-row>
  </gv-summary-list>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { SupportedWkid } from "../../coordinate-system-utils";
import { SnapPoint } from "../../grid-utils";
import GvSummaryList from "../govukVue/summary-list/GvSummaryList.vue";
import GvSummaryListRow from "../govukVue/summary-list/GvSummaryListRow.vue";
import GvSummaryListRowAction from "../govukVue/summary-list/GvSummaryListRowAction.vue";

interface LineCoordinatesProps {
  points: SnapPoint[],
  srsWkid: SupportedWkid,
}

const props = defineProps<LineCoordinatesProps>();

const emit = defineEmits<{
  "undo-last-point": [],
}>();

// Displayed with the most recently clicked point on top, like a stack, so the undo link always targets index 0.
const stackedPoints = computed(() => [...props.points].reverse());
</script>

<style scoped>
.line-coordinate-stack {
  margin-top: 1.5rem;
}
</style>
