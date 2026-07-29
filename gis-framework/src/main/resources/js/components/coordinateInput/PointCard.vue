<template>
  <div class="govuk-summary-card">
    <div class="govuk-summary-card__title-wrapper">
      <h3 class="govuk-summary-card__title">
        Point {{ index + 1 }}
      </h3>
      <ul class="govuk-summary-card__actions">
        <li v-if="index === 0" class="govuk-summary-card__action">
          <button class="fds-link-button" type="button" @click="$emit('add-before')">
            Add before
          </button>
        </li>
        <li class="govuk-summary-card__action govuk-!-padding-left-2">
          <button class="fds-link-button" type="button" @click="$emit('add-after')">
            Add after
          </button>
        </li>
        <li v-if="index === 0" class="govuk-summary-card__action">
          <button class="fds-link-button" type="button" @click="$emit('clear')">
            Clear
          </button>
        </li>
        <li v-else class="govuk-summary-card__action">
          <button class="fds-link-button" type="button" @click="$emit('remove')">
            Remove
          </button>
        </li>
      </ul>
    </div>
    <div class="govuk-summary-card__content">
      <coordinate-dms-input
        v-if="isOffshore(srsWkid)"
        :index="index"
        :longitude="longitudeOriginalSrs"
        :latitude="latitudeOriginalSrs"
        :seconds-precision="coordinatePrecision"
        @update:longitude="$emit('update:longitude', $event)"
        @update:latitude="$emit('update:latitude', $event)"
      />
      <coordinate-grid-input
        v-else
        :index="index"
        :srs-wkid="srsWkid"
        :longitude="longitudeOriginalSrs"
        :latitude="latitudeOriginalSrs"
        :max-figures-per-axis="coordinatePrecision"
        @update:coordinates="$emit('update:coordinates', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { SupportedWkid } from "../../coordinate-system-utils";
import { isOffshore } from "../../coordinate-system-utils";
import CoordinateDmsInput from "./CoordinateDmsInput.vue";
import CoordinateGridInput from "./CoordinateGridInput.vue";

interface PointCardProps {
  index: number,
  srsWkid: SupportedWkid,
  longitudeOriginalSrs: string | number,
  latitudeOriginalSrs: string | number,
  coordinatePrecision?: number,
}

withDefaults(defineProps<PointCardProps>(), {
  coordinatePrecision: 4,
});

defineEmits<{
  "update:longitude": [value: number],
  "update:latitude": [value: number],
  "update:coordinates": [coordinates: [number, number]],
  "add-before": [],
  "add-after": [],
  "clear": [],
  "remove": [],
}>();
</script>
