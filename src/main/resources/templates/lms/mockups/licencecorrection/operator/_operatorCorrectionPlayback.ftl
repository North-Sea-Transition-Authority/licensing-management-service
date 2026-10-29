<#include '../../../layout/layout.ftl'/>

<#macro operatorChangeCard
  operatorType
  joiningOperator
  withdrawingOperator
  subarea
  marker
  correctUrl
  removeUrl
  summaryListId
>
  <#local screenReaderText="${operatorType.displayName?uncap_first} change"/>

  <#local headingText>
    <div style="display: flex; gap: 1rem">
      ${operatorType.displayName} change
      <@fdsTag.tag tagClass=marker.tagClass>${marker.label}</@fdsTag.tag>
    </div>
  </#local>

  <#local cardActions>
    <@fdsSummaryList.summaryListCardActionList>
      <@fdsSummaryList.summaryListCardActionItem
        itemUrl=correctUrl
        itemText="Correct"
        itemScreenReaderText=screenReaderText
      />
      <@fdsSummaryList.summaryListCardActionItem
        itemUrl=removeUrl
        itemText="Remove"
        itemScreenReaderText=screenReaderText
      />
    </@fdsSummaryList.summaryListCardActionList>
  </#local>

  <#local withdrawing>
    <@fdsTag.tag tagClass="govuk-tag--red">Withdrawing</@fdsTag.tag>
  </#local>
  <#local joining>
    <@fdsTag.tag tagClass="govuk-tag--green">Joining</@fdsTag.tag>
  </#local>

  <@fdsSummaryList.summaryListCard
    headingText=headingText
    summaryListId=summaryListId
    cardActionsContent=cardActions
  >
    <#if subarea?has_content>
      <@fdsSummaryList.summaryListRowNoAction keyText="Subarea">
        ${subarea}
      </@fdsSummaryList.summaryListRowNoAction>
    </#if>
    <@fdsSummaryList.summaryListRowNoAction keyText=withdrawing>
      ${withdrawingOperator}
    </@fdsSummaryList.summaryListRowNoAction>
    <@fdsSummaryList.summaryListRowNoAction keyText=joining>
      ${joiningOperator}
    </@fdsSummaryList.summaryListRowNoAction>
  </@fdsSummaryList.summaryListCard>
</#macro>