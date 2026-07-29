<template>
  <div>
    <point-card
      v-for="(point, index) in modelValue"
      :key="point.id"
      :index="index"
      :srs-wkid="srsWkid"
      :longitude-original-srs="point.originalSrsCoordinates[0]"
      :latitude-original-srs="point.originalSrsCoordinates[1]"
      :coordinate-precision="coordinatePrecision"
      @update:longitude="updateLongitude(index, $event)"
      @update:latitude="updateLatitude(index, $event)"
      @update:coordinates="updateCoordinates(index, $event)"
      @add-before="addBefore(index)"
      @add-after="addAfter(index)"
      @clear="clear(index)"
      @remove="remove(index)"
    />
  </div>
</template>

<script setup lang="ts">
import type { SupportedWkid } from "../../coordinate-system-utils";
import { toWgs84 } from "../../coordinate-system-utils";
import PointCard from "./PointCard.vue";

/**
 * A coordinate-list point being edited. It is structurally a {@link LinePoint} except that
 * `coordinates` (the derived WGS84 position) is undefined while the entered value is invalid,
 * and it carries a stable `id` for list rendering.
 */
export interface EditablePoint {
  id: number,
  originalSrsCoordinates: [number, number],
  coordinates: [number, number] | undefined,
}

const props = withDefaults(defineProps<CoordinateListProps>(), {
  coordinatePrecision: 4,
});

const modelValue = defineModel<EditablePoint[]>({ required: true });

interface CoordinateListProps {
  srsWkid: SupportedWkid,
  coordinatePrecision?: number,
}

// Seed from the ids the parent supplied so a fresh point never collides with an existing one
// (the parent seeds its initial point with id 0). Math.max(-1, ...[]) is -1, so an empty list
// starts at 0.
let nextId = Math.max(-1, ...modelValue.value.map(p => p.id)) + 1;

function deriveWgs84(point: EditablePoint): void {
  const [x, y] = point.originalSrsCoordinates;
  if (Number.isFinite(x) && Number.isFinite(y)) {
    point.coordinates = toWgs84(props.srsWkid, x, y);
  } else {
    point.coordinates = undefined;
  }
}

/**
 * Creates a blank point with no coordinates. The non-finite `originalSrsCoordinates` make both the DMS
 * and grid inputs render empty fields, and the fresh `id` forces the child input to remount so it
 * re-reads the now-blank props (the inputs initialise their displayed value from props only once).
 * Used for points added via "Add before"/"Add after" and for "Clear"; the parent supplies the initial
 * point via `modelValue`, so nothing ever defaults to a misleading 0,0 origin.
 */
function createBlankPoint(): EditablePoint {
  return { id: nextId++, originalSrsCoordinates: [Number.NaN, Number.NaN], coordinates: undefined };
}

function updateAxis(index: number, axis: 0 | 1, value: number): void {
  const updated = [...modelValue.value];
  const originalSrsCoordinates: [number, number] = [...updated[index].originalSrsCoordinates];
  originalSrsCoordinates[axis] = value;
  const point: EditablePoint = { ...updated[index], originalSrsCoordinates, coordinates: undefined };
  deriveWgs84(point);
  updated[index] = point;
  modelValue.value = updated;
}

function updateLongitude(index: number, value: number): void {
  updateAxis(index, 0, value);
}

function updateLatitude(index: number, value: number): void {
  updateAxis(index, 1, value);
}

/**
 * Applies both coordinate axes in a single model write. Used by inputs (e.g. the grid reference
 * input) that produce both values at once — emitting them as two separate `updateAxis` calls in the
 * same tick would race, because `defineModel`'s local value is not refreshed synchronously when the
 * parent binds `v-model`, so the second write would read a stale base and drop the first axis.
 */
function updateCoordinates(index: number, [x, y]: [number, number]): void {
  const updated = [...modelValue.value];
  const point: EditablePoint = { ...updated[index], originalSrsCoordinates: [x, y], coordinates: undefined };
  deriveWgs84(point);
  updated[index] = point;
  modelValue.value = updated;
}

function addBefore(index: number): void {
  const updated = [...modelValue.value];
  updated.splice(index, 0, createBlankPoint());
  modelValue.value = updated;
}

function addAfter(index: number): void {
  const updated = [...modelValue.value];
  updated.splice(index + 1, 0, createBlankPoint());
  modelValue.value = updated;
}

function clear(index: number): void {
  const updated = [...modelValue.value];
  updated[index] = createBlankPoint();
  modelValue.value = updated;
}

function remove(index: number): void {
  if (modelValue.value.length <= 1) {
    return;
  }
  const updated = [...modelValue.value];
  updated.splice(index, 1);
  modelValue.value = updated;
}
</script>
