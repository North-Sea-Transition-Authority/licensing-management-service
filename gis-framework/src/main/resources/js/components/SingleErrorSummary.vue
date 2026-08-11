<template>
  <div v-if="message" class="govuk-error-summary" aria-labelledby="error-summary-title" role="alert" data-module="govuk-error-summary">
    <p id="error-summary-title" class="govuk-error-message govuk-error-message--inline">
      {{ message }}
    </p>
  </div>
</template>

<script setup lang="ts">
interface SingleErrorSummaryProps {
  message?: string | null,
}

defineProps<SingleErrorSummaryProps>();
</script>
