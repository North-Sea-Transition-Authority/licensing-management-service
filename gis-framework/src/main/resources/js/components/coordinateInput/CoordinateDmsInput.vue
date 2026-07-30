<template>
  <div class="coordinate-groups">
    <div class="govuk-form-group govuk-!-margin-bottom-0 coordinate-field" :class="[{ 'govuk-form-group--error': latError }]">
      <fieldset class="govuk-fieldset" @focusout="showLatErrorOnLeaving">
        <legend class="govuk-fieldset__legend govuk-fieldset__legend--s">
          Latitude
        </legend>

        <p v-if="latError" :id="`lat-error-${index}`" class="govuk-error-message">
          <span class="govuk-visually-hidden">Error:</span> {{ latError }}
        </p>

        <div class="coordinate-inputs">
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="`lat-deg-${index}`">Degrees</label>
            <input
              :id="`lat-deg-${index}`"
              v-model="localLat.degrees"
              class="govuk-input govuk-input--width-3" :class="[{ 'govuk-input--error': latError }]"
              type="number"
              step="1"
              @input="emitLat"
            >
          </div>
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="`lat-min-${index}`">Minutes</label>
            <input
              :id="`lat-min-${index}`"
              v-model="localLat.minutes"
              class="govuk-input govuk-input--width-3" :class="[{ 'govuk-input--error': latError }]"
              type="number"
              step="1"
              @input="emitLat"
            >
          </div>
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="`lat-sec-${index}`">Seconds</label>
            <input
              :id="`lat-sec-${index}`"
              v-model="localLat.seconds"
              class="govuk-input govuk-input--width-4" :class="[{ 'govuk-input--error': latError }]"
              type="number"
              :step="secondsStep"
              @input="emitLat"
            >
          </div>
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label">Hemisphere</label>
            <p class="govuk-body govuk-!-margin-bottom-0 govuk-!-padding-top-1">
              North
            </p>
          </div>
        </div>
      </fieldset>
    </div>
    <div class="govuk-form-group coordinate-field" :class="[{ 'govuk-form-group--error': lonError }]">
      <fieldset class="govuk-fieldset govuk-!-margin-bottom-3" @focusout="showLonErrorOnLeaving">
        <legend class="govuk-fieldset__legend govuk-fieldset__legend--s">
          Longitude
        </legend>

        <p v-if="lonError" :id="`lon-error-${index}`" class="govuk-error-message">
          <span class="govuk-visually-hidden">Error:</span> {{ lonError }}
        </p>

        <div class="coordinate-inputs">
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="`lon-deg-${index}`">Degrees</label>
            <input
              :id="`lon-deg-${index}`"
              v-model="localLon.degrees"
              class="govuk-input govuk-input--width-3" :class="[{ 'govuk-input--error': lonError }]"
              type="number"
              step="1"
              @input="emitLon"
            >
          </div>
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="`lon-min-${index}`">Minutes</label>
            <input
              :id="`lon-min-${index}`"
              v-model="localLon.minutes"
              class="govuk-input govuk-input--width-3" :class="[{ 'govuk-input--error': lonError }]"
              type="number"
              step="1"
              @input="emitLon"
            >
          </div>
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="`lon-sec-${index}`">Seconds</label>
            <input
              :id="`lon-sec-${index}`"
              v-model="localLon.seconds"
              class="govuk-input govuk-input--width-4" :class="[{ 'govuk-input--error': lonError }]"
              type="number"
              :step="secondsStep"
              @input="emitLon"
            >
          </div>
          <div class="govuk-form-group govuk-!-margin-bottom-0">
            <label class="govuk-label" :for="`lon-hemi-${index}`">Hemisphere</label>
            <select
              :id="`lon-hemi-${index}`"
              v-model="localLon.hemisphere"
              class="govuk-select hemisphere-select" :class="[{ 'govuk-select--error': lonError }]"
              @change="emitLon"
            >
              <option value="E">
                East
              </option>
              <option value="W">
                West
              </option>
            </select>
          </div>
        </div>
      </fieldset>
    </div>
  </div>
</template>

<script setup lang="ts">
import Dms from "geodesy/dms.js";
import { computed, reactive, ref } from "vue";

type Hemisphere = "N" | "S" | "E" | "W";

interface CoordinateDmsInputProps {
  index: number,
  longitude: string | number,
  latitude: string | number,
  secondsPrecision?: number,
}

const props = withDefaults(defineProps<CoordinateDmsInputProps>(), {
  secondsPrecision: 4,
});

const emit = defineEmits<{
  "update:longitude": [value: number],
  "update:latitude": [value: number],
}>();

interface DmsFields {
  degrees: string | number,
  minutes: string | number,
  seconds: string | number,
  hemisphere: Hemisphere,
}

// The inputs always start empty; the longitude/latitude props are retained on the interface for the
// parent binding but are not used to pre-fill the displayed values.
const localLat = reactive<DmsFields>({ degrees: "", minutes: "", seconds: "", hemisphere: "N" });
const localLon = reactive<DmsFields>({ degrees: "", minutes: "", seconds: "", hemisphere: "E" });

const lonError = ref("");
const latError = ref("");

const secondsStep = computed(() => 1 / 10 ** props.secondsPrecision);

function numberOfDecimalPlaces(value: string): number {
  const parts = value.split(".");
  return parts.length > 1 ? parts[1].length : 0;
}

function validate(fields: DmsFields, maxDegrees: number): string {
  const degrees = Number.parseFloat(String(fields.degrees));
  const minutes = Number.parseFloat(String(fields.minutes));
  const seconds = Number.parseFloat(String(fields.seconds));

  if (Number.isNaN(degrees) || Number.isNaN(minutes) || Number.isNaN(seconds)) {
    return "Enter degrees, minutes and seconds";
  }
  if (!Number.isInteger(degrees) || String(fields.degrees).includes(".")) {
    return "Degrees must be a whole number";
  }
  if (!Number.isInteger(minutes) || String(fields.minutes).includes(".")) {
    return "Minutes must be a whole number";
  }
  if (numberOfDecimalPlaces(String(fields.seconds)) > props.secondsPrecision) {
    return `Seconds cannot exceed ${props.secondsPrecision} decimal place${props.secondsPrecision === 1 ? "" : "s"}`;
  }
  if (degrees < 0 || degrees > maxDegrees) {
    return `Degrees must be between 0 and ${maxDegrees}`;
  }
  if (minutes < 0 || minutes > 59) {
    return "Minutes must be between 0 and 59";
  }
  if (seconds < 0 || seconds >= 60) {
    return "Seconds must be between 0 and 59";
  }
  return "";
}

function toDecimalDegrees(fields: DmsFields): number {
  return Dms.parse(`${fields.degrees} ${fields.minutes} ${fields.seconds}${fields.hemisphere}`);
}

/**
 * Emits on every keystroke — non-finite when invalid, so the parent clears the point rather than keeping
 * the last valid coordinate (which would leave a stale line on the map that no longer matches what's
 * typed). Raising the error message waits for focus to leave the fieldset, since degrees, minutes and
 * seconds are separate inputs and a partly-filled entry is not yet wrong. An already-displayed message
 * still clears as soon as the entry becomes valid.
 */
function emitLat(): void {
  const error = validate(localLat, 90);
  if (!error) {
    latError.value = "";
  }
  emit("update:latitude", error ? Number.NaN : toDecimalDegrees(localLat));
}

function emitLon(): void {
  const error = validate(localLon, 180);
  if (!error) {
    lonError.value = "";
  }
  emit("update:longitude", error ? Number.NaN : toDecimalDegrees(localLon));
}

// A focusout that lands on another field in the same fieldset means the user is still working through
// degrees/minutes/seconds, so it isn't a point at which to correct them.
function hasLeftFieldset(event: FocusEvent): boolean {
  const fieldset = event.currentTarget as HTMLElement;
  return !fieldset.contains(event.relatedTarget as Node | null);
}

function showLatErrorOnLeaving(event: FocusEvent): void {
  if (hasLeftFieldset(event)) {
    latError.value = validate(localLat, 90);
  }
}

function showLonErrorOnLeaving(event: FocusEvent): void {
  if (hasLeftFieldset(event)) {
    lonError.value = validate(localLon, 180);
  }
}
</script>

<style scoped>
.coordinate-groups {
  display: flex;
  flex-direction: column;
  gap: 0.9375rem;
}
.coordinate-inputs {
  display: flex;
  gap: 0.9375rem;
}
.hemisphere-select {
  width: 7.5rem;
  min-width: 7.5rem;
}
.coordinate-field {
  padding-left: 0.9375rem;
  border-left: 0.3125rem solid transparent;
}
.coordinate-field.govuk-form-group--error {
  border-left-color: #d4351c;
}
</style>
