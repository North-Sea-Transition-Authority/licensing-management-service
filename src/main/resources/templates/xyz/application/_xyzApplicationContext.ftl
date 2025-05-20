<#include '../layout/layout.ftl'>
<#import '../application/_contextHeaderSummaryDataView.ftl' as contextHeaderSummaryDataView>

<#macro applicationContextInfo applicationContext>
  <h1 class="govuk-heading-xl govuk-!-margin-0">
      ${applicationContext.reference()}
  </h1>
  <span class="govuk-caption-l">
    <#if applicationContext.type()?has_content>
      ${applicationContext.type()}
    </#if>
  </span>
  <@contextHeaderSummaryDataView.summaryDataView applicationContext.summaryDataView()/>
</#macro>
