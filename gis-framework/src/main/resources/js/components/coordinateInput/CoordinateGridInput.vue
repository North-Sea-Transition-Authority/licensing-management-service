<template>
  <div class="govuk-form-group govuk-!-margin-bottom-0 coordinate-field" :class="[{ 'govuk-form-group--error': gridError }]">
    <label class="govuk-label" :for="`grid-ref-${index}`">Grid reference</label>
    <p v-if="gridError" :id="`grid-error-${index}`" class="govuk-error-message">
      <span class="govuk-visually-hidden">Error:</span> {{ gridError }}
    </p>
    <input
      :id="`grid-ref-${index}`"
      v-model="gridRef"
      class="govuk-input govuk-input--width-10" :class="[{ 'govuk-input--error': gridError }]"
      type="text"
      autocapitalize="characters"
      spellcheck="false"
      @input="emitCoordinates"
      @blur="showErrorAndNormaliseDisplay"
    >
  </div>
</template>

<script setup lang="ts">
import type { SupportedWkid } from "../../coordinate-system-utils";
import OsGridRef from "geodesy/osgridref.js";
import { ref } from "vue";

interface CoordinateGridInputProps {
  index: number,
  srsWkid: SupportedWkid,
  longitude: string | number,
  latitude: string | number,
  maxFiguresPerAxis?: number,
}

const props = withDefaults(defineProps<CoordinateGridInputProps>(), {
  maxFiguresPerAxis: 4,
});

const emit = defineEmits<{
  "update:coordinates": [coordinates: [number, number]],
}>();

/** A rejected entry, carrying the message to show once the user has finished typing. */
interface InvalidGridRef {
  error: string,
}

/** An accepted entry, carrying its BNG metres and canonical "XX 999 999" spacing/casing. */
interface ValidGridRef {
  normalised: string,
  easting: number,
  northing: number,
}

type ParsedGridRef = InvalidGridRef | ValidGridRef;

function isValid(parsed: ParsedGridRef): parsed is ValidGridRef {
  return !("error" in parsed);
}

// The input always starts empty; the longitude/latitude props are retained on the interface for the
// parent binding but are not used to pre-fill the displayed value.
const gridRef = ref("");
const gridError = ref("");

function figuresLabel(): string {
  return `figure${props.maxFiguresPerAxis === 1 ? "" : "s"}`;
}

function parseGridRef(raw: string): ParsedGridRef {
  const compact = raw.replace(/\s+/g, "").toUpperCase();
  if (compact === "") {
    return { error: "Enter a grid reference" };
  }

  const square = compact.match(/^[A-Z]*/)![0];
  const figures = compact.slice(square.length);

  if (square.length !== 2) {
    return { error: "Grid square must be two letters" };
  }
  if (figures === "" || !/^\d+$/.test(figures)) {
    return { error: "Easting and northing must be numbers only" };
  }
  if (figures.length % 2 !== 0) {
    return { error: "Easting and northing must have the same number of figures" };
  }

  const figuresPerAxis = figures.length / 2;
  if (figuresPerAxis > props.maxFiguresPerAxis) {
    return { error: `Easting and northing cannot have more than ${props.maxFiguresPerAxis} ${figuresLabel()}` };
  }

  const normalised = `${square} ${figures.slice(0, figuresPerAxis)} ${figures.slice(figuresPerAxis)}`;

  try {
    const parsed = OsGridRef.parse(normalised);
    return { normalised, easting: parsed.easting, northing: parsed.northing };
  } catch {
    return { error: "Enter a valid OS grid reference" };
  }
}

/**
 * Emits on every keystroke — non-finite coordinates while the entry is invalid, so the parent clears
 * the point rather than keeping the last valid easting/northing (which would leave a stale line on the
 * map). The error message itself waits for blur so the user isn't corrected mid-reference, but an
 * already-displayed message clears as soon as the entry becomes valid.
 */
function emitCoordinates(): void {
  const parsed = parseGridRef(gridRef.value);

  if (isValid(parsed)) {
    gridError.value = "";
    emit("update:coordinates", [parsed.easting, parsed.northing]);
  } else {
    emit("update:coordinates", [Number.NaN, Number.NaN]);
  }
}

// Tidying the display to canonical spacing/casing waits for blur so we don't reposition the caret while
// the user is still typing.
function showErrorAndNormaliseDisplay(): void {
  const parsed = parseGridRef(gridRef.value);

  if (isValid(parsed)) {
    gridError.value = "";
    gridRef.value = parsed.normalised;
  } else {
    gridError.value = parsed.error;
  }
}
</script>

<style scoped>
.coordinate-field {
  padding-left: 0.9375rem;
  border-left: 0.3125rem solid transparent;
}
.coordinate-field.govuk-form-group--error {
  border-left-color: #d4351c;
}
</style>
