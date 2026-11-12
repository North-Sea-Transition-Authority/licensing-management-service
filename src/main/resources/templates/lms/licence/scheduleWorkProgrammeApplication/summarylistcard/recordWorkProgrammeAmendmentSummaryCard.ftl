<#include '../../../layout/layout.ftl'>

<#macro recordWorkProgrammeAmendmentCard workProgrammeAmendment summaryListId showActions=true>
  <#if showActions>
    <#assign cardActions>
      <@fdsSummaryList.summaryListCardActionList>
        <@fdsSummaryList.summaryListCardActionItem
          itemUrl=springUrl(workProgrammeAmendment.changeUrl())
          itemText="Change"
          itemScreenReaderText=workProgrammeAmendment.workProgrammeDescription()/>
        <@fdsSummaryList.summaryListCardActionItem
          itemUrl=springUrl(workProgrammeAmendment.removeUrl())
          itemText="Remove"
          itemScreenReaderText=workProgrammeAmendment.workProgrammeDescription()/>
      </@fdsSummaryList.summaryListCardActionList>
    </#assign>
  <#else>
    <#assign cardActions = ""/>
  </#if>
  <@fdsSummaryList.summaryListCard
    headingText=workProgrammeAmendment.workProgrammeDescription()
    headingSize="h2"
    summaryListId=summaryListId
    cardActionsContent=cardActions>

    <@fdsSummaryList.summaryListRowNoAction keyText="Due date">
      ${workProgrammeAmendment.dueDate()}
    </@fdsSummaryList.summaryListRowNoAction>

    <@fdsSummaryList.summaryListRowNoAction keyText="Decision">
      ${workProgrammeAmendment.decision()}
    </@fdsSummaryList.summaryListRowNoAction>

    <#if workProgrammeAmendment.amendedDuration()?has_content>
      <@fdsSummaryList.summaryListRowNoAction keyText="Amended duration">
        ${workProgrammeAmendment.amendedDuration()}
      </@fdsSummaryList.summaryListRowNoAction>
    </#if>

    <#if workProgrammeAmendment.amendedText()?has_content>
      <@fdsSummaryList.summaryListRowNoAction keyText="Amended text">
        ${workProgrammeAmendment.amendedText()}
      </@fdsSummaryList.summaryListRowNoAction>
    </#if>

  </@fdsSummaryList.summaryListCard>
</#macro>
