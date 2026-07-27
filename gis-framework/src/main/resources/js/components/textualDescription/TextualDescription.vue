<template>
  <!-- eslint-disable-next-line vue/no-v-html -- server-generated, HTML-escaped fragment from the GIS framework -->
  <div v-html="textualDescription" />
</template>

<script setup lang="ts">
import { onBeforeMount, ref, Ref } from "vue";
import { getTextualDescription } from "../../api/features.api";

interface Props {
  textualDescriptionUrl: string,
}
const props = defineProps<Props>();

const textualDescription: Ref<string> = ref("");
onBeforeMount(async () => {
  try {
    textualDescription.value = await getTextualDescription(props.textualDescriptionUrl);
  } catch (e) {
    console.error(e);
  }
});
</script>
