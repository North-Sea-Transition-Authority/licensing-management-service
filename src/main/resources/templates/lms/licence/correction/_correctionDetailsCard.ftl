<#include '../../layout/layout.ftl'>

<#macro correctionDetailsCard correction allocatedToUser updateUrl="">
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
      ${correction.getCorrectionReference()}
    </@fdsSummaryList.summaryListRowNoAction>
    <@fdsSummaryList.summaryListRowNoAction keyText="Reason for correction">
      ${correction.getReason()}
    </@fdsSummaryList.summaryListRowNoAction>
    <@fdsSummaryList.summaryListRowNoAction keyText="Allocated to">
      ${allocatedToUser}
    </@fdsSummaryList.summaryListRowNoAction>
    <@fdsSummaryList.summaryListRowNoAction keyText="Status">
      ${correction.getStatus().displayName}
    </@fdsSummaryList.summaryListRowNoAction>
  </@fdsSummaryList.summaryListCard>
</#macro>
