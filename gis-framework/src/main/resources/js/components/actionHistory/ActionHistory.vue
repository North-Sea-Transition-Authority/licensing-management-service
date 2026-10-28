<template>
  <details-component :summary="historySummary">
    <p v-if="actions.length === 0" class="govuk-body">
      No actions taken yet.
    </p>
    <gv-table v-else>
      <gv-table-head>
        <gv-table-row>
          <gv-table-header>
            <span class="govuk-visually-hidden">Number</span>
          </gv-table-header>
          <gv-table-header text="Action" />
          <gv-table-header text="Inputs" />
          <gv-table-header text="Outputs" />
        </gv-table-row>
      </gv-table-head>
      <gv-table-body>
        <gv-table-row v-for="action in actions" :key="action.number">
          <gv-table-cell>
            <span class="govuk-!-font-weight-bold action-history__number">
              {{ action.number }}.
            </span>
          </gv-table-cell>
          <gv-table-cell>
            {{ getTransformationTypeLabel(action.transformationType) }}
          </gv-table-cell>
          <gv-table-cell :text="action.inputFeatureNames.join(', ')" />
          <gv-table-cell :text="action.outputFeatureNames.join(', ')" />
        </gv-table-row>
      </gv-table-body>
    </gv-table>
  </details-component>
</template>

<script setup lang="ts">
import type { JsonHistoryAction } from "@/api/history.api";
import { computed } from "vue";
import DetailsComponent from "@/components/govukVue/details/GvDetails.vue";
import GvTable from "@/components/govukVue/table/GvTable.vue";
import GvTableBody from "@/components/govukVue/table/GvTableBody.vue";
import GvTableCell from "@/components/govukVue/table/GvTableCell.vue";
import GvTableHead from "@/components/govukVue/table/GvTableHead.vue";
import GvTableHeader from "@/components/govukVue/table/GvTableHeader.vue";
import GvTableRow from "@/components/govukVue/table/GvTableRow.vue";
import { getTransformationTypeLabel } from "@/transformation-type";

interface ActionHistoryProps {
  actions: JsonHistoryAction[],
}

const props = defineProps<ActionHistoryProps>();

const historySummary = computed(() =>
  `Show action history (${props.actions.length} ${props.actions.length === 1 ? "action" : "actions"})`,
);
</script>

<style scoped>
.action-history__number {
  display: inline-block;
  font-variant-numeric: tabular-nums;
  text-align: right;
}
</style>
