<#include '../layout/layout.ftl'>
<#import 'summaryValue/_fileSummaryValue.ftl' as fileSummaryValue>
<#import 'summaryValue/_stringSummaryValue.ftl' as stringSummaryValue>
<#import 'summaryValue/_externalUrlSummaryValue.ftl' as externalUrlSummaryValue>

<#-- @ftlvariable name="summaryDataView" type="uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView" -->

<#macro simpleSummary summaryDataView summaryHeading>
  <@fdsSummaryList.summaryListCard
    headingText=summaryHeading
    headingSize="h3"
    summaryListId="summary-data-card-list">
    <#list summaryDataView.keyValues() as keyValue>
      <@fdsSummaryList.summaryListRowNoAction keyText=keyValue.key()>
        <#if keyValue.summaryValueType() == "STRING_VALUE">
          <@stringSummaryValue.stringValueDisplay keyValue.summaryValueData()/>
        <#elseif keyValue.summaryValueType() == "FILE_VALUE">
          <@fileSummaryValue.fileValueDisplay keyValue.summaryValueData()/>
        <#elseif keyValue.summaryValueType() == "EXTERNAL_URL_VALUE">
          <@externalUrlSummaryValue.externalUrlValueDisplay keyValue.summaryValueData()/>
        </#if>
      </@fdsSummaryList.summaryListRowNoAction>
    </#list>
  </@fdsSummaryList.summaryListCard>
</#macro>
