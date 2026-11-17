<#include '../layout/layout.ftl'>
<#import 'summaryValue/_fileSummaryValue.ftl' as fileSummaryValue>
<#import 'summaryValue/_stringSummaryValue.ftl' as stringSummaryValue>
<#import 'summaryValue/_externalUrlSummaryValue.ftl' as externalUrlSummaryValue>
<#import 'summaryValue/_mapSummaryValue.ftl' as mapSummaryValue>

<#-- @ftlvariable name="summaryDataView" type="uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView" -->

<#macro simpleSummary summaryDataView summaryHeading actions=[] headingSize="h3">
  <#local cardActions>
    <#if actions?has_content>
      <@fdsSummaryList.summaryListCardActionList>
        <#list actions as action>
          <#if action.post()>
            <li class="govuk-summary-card__action">
              <button type="submit" class="govuk-link fds-link-button fds-link-button--inline" formaction="${springUrl(action.url())}">
                ${action.text()}<span class="govuk-visually-hidden"> ${summaryHeading}</span>
              </button>
            </li>
          <#else>
            <@fdsSummaryList.summaryListCardActionItem
              itemUrl=springUrl(action.url())
              itemText=action.text()
              itemScreenReaderText=summaryHeading
            />
          </#if>
        </#list>
      </@fdsSummaryList.summaryListCardActionList>
    </#if>
  </#local>
  <@fdsSummaryList.summaryListCard
    headingText=summaryHeading
    headingSize=headingSize
    summaryListId="summary-data-card-list"
    cardActionsContent=cardActions
  >
    <#list summaryDataView.keyValues() as keyValue>
      <@fdsSummaryList.summaryListRowNoAction keyText=keyValue.key()>
        <#if keyValue.summaryValueType() == "STRING_VALUE">
          <@stringSummaryValue.stringValueDisplay keyValue.summaryValueData()/>
        <#elseif keyValue.summaryValueType() == "FILE_VALUE">
          <@fileSummaryValue.fileValueDisplay keyValue.summaryValueData()/>
        <#elseif keyValue.summaryValueType() == "EXTERNAL_URL_VALUE">
          <@externalUrlSummaryValue.externalUrlValueDisplay keyValue.summaryValueData()/>
        <#elseif keyValue.summaryValueType() == "MAP_VALUE">
          <@mapSummaryValue.mapValueDisplay keyValue.summaryValueData()/>
        </#if>
      </@fdsSummaryList.summaryListRowNoAction>
    </#list>
  </@fdsSummaryList.summaryListCard>
</#macro>
