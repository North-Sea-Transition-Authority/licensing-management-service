<template>
  <!-- eslint-disable-next-line vue/no-v-html -- server-generated, HTML-escaped fragment from the GIS framework -->
  <div v-html="textualDescription" />
</template>

<script setup lang="ts">
import { computed, onBeforeMount, ref, Ref, watch } from "vue";
import { getTextualDescription } from "../../api/features.api";

interface Props {
  textualDescriptionUrl: string,
  commandJourneyId?: string,
  refreshCounter?: number,
}
const props = defineProps<Props>();

const url = computed(() =>
  props.commandJourneyId
    ? `${props.textualDescriptionUrl}/${props.commandJourneyId}`
    : props.textualDescriptionUrl,
);

const textualDescription: Ref<string> = ref("");

async function load() {
  try {
    textualDescription.value = await getTextualDescription(url.value);
  } catch (e) {
    console.error(e);
  }
}

onBeforeMount(load);
watch(() => props.refreshCounter, load);
</script>
