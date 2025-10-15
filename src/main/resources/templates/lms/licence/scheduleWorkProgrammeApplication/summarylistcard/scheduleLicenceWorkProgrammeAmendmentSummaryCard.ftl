<#include '../../../layout/layout.ftl'>

<#macro licenceWorkProgrammeAmendments licenceWorkProgrammeAmendment withResultNumber=true>
  <#if licenceWorkProgrammeAmendment.summaryMode() == "EDIT">
    <#assign content>
      <@fdsSummaryList.summaryListCardActionList>
        <@fdsSummaryList.summaryListCardActionItem itemUrl=springUrl(licenceWorkProgrammeAmendment.changeUrl()) itemText="Change" itemScreenReaderText="Change work programme amendment"/>
        <@fdsSummaryList.summaryListCardActionItem itemUrl=springUrl(licenceWorkProgrammeAmendment.deleteUrl()) itemText="Remove" itemScreenReaderText="Remove work programme amendment"/>
      </@fdsSummaryList.summaryListCardActionList>
    </#assign>
  <#else>
    <#assign content = ""/>
  </#if>
  <@fdsSummaryList.summaryListCard
    headingText="${licenceWorkProgrammeAmendment.workProgrammeAmendmentLabel()}"
    summaryListId="licenceworkprogrammeamendmentvaluesresult"
    cardActionsContent=content>
      <@fdsSummaryList.summaryListRowNoAction keyText="Completion date change requested">
          ${licenceWorkProgrammeAmendment.workProgrammeCompletionDateChangeRequestedDisplay()}
      </@fdsSummaryList.summaryListRowNoAction>
      <#if (licenceWorkProgrammeAmendment.workProgrammeCompletionDateChangeRequested())!false>
          <@fdsSummaryList.summaryListRowNoAction keyText="Requested extension to completion date">
              ${licenceWorkProgrammeAmendment.workProgrammeExtensionDuration()!""}
          </@fdsSummaryList.summaryListRowNoAction>
      </#if>
      <@fdsSummaryList.summaryListRowNoAction keyText="Work programme content change requested">
          ${licenceWorkProgrammeAmendment.workProgrammeChangeRequestedDisplay()}
      </@fdsSummaryList.summaryListRowNoAction>
      <#if (licenceWorkProgrammeAmendment.workProgrammeChangeRequested())!false>
          <@fdsSummaryList.summaryListRowNoAction keyText="Requested change to content">
              ${licenceWorkProgrammeAmendment.workProgrammeAmendmentInformation()!""}
          </@fdsSummaryList.summaryListRowNoAction>
      </#if>
  </@fdsSummaryList.summaryListCard>
</#macro>