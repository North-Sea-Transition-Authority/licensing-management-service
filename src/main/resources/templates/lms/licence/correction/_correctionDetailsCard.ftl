<#include '../../layout/layout.ftl'>

<#macro correctionDetailsCard details updateUrl="" showCreatedDate=true>
  <#assign cardActions>
    <#if updateUrl?has_content>
      <@fdsSummaryList.summaryListCardActionList>
        <@fdsSummaryList.summaryListCardActionItem
          itemUrl=springUrl(updateUrl)
          itemText="Update"
          itemScreenReaderText="correction details"
        />
      </@fdsSummaryList.summaryListCardActionList>
    </#if>
  </#assign>

  <@fdsSummaryList.summaryListCard
    summaryListId="correction-details"
    headingText="Correction details"
    cardActionsContent=cardActions
  >
    <@fdsSummaryList.summaryListRowNoAction keyText="Correction reference">
      ${details.correctionReference()}
    </@fdsSummaryList.summaryListRowNoAction>
    <@fdsSummaryList.summaryListRowNoAction keyText="Reason for correction">
      ${details.reason()}
    </@fdsSummaryList.summaryListRowNoAction>
    <@fdsSummaryList.summaryListRowNoAction keyText="Allocated to">
      ${details.allocatedToUserName()}
    </@fdsSummaryList.summaryListRowNoAction>
    <@fdsSummaryList.summaryListRowNoAction keyText="Status">
      ${details.statusDisplayName()}
    </@fdsSummaryList.summaryListRowNoAction>
    <@fdsSummaryList.summaryListRowNoAction keyText="Licence reference">
      ${details.licenceReference()}
    </@fdsSummaryList.summaryListRowNoAction>
    <#if showCreatedDate>
      <@fdsSummaryList.summaryListRowNoAction keyText="Created">
        ${details.createdDate()}
      </@fdsSummaryList.summaryListRowNoAction>
    </#if>
  </@fdsSummaryList.summaryListCard>
</#macro>
