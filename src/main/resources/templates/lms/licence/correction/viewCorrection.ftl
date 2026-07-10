<#include '../../layout/layout.ftl'>
<#import '../position/_licencePositionTimeLine.ftl' as licencePositionTimeLine>
<#import '../position/_licencePositionDetails.ftl' as licencePositionDetails>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  caption=pageCaption
>
  <@fdsSummaryList.summaryListCard summaryListId="correction-details" headingText="Correction details">
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

    <@fdsAction.buttonGroup>
      <@fdsAction.link linkText="Add position" linkUrl=springUrl(addPositionUrl) linkClass="govuk-button"/>
      <@fdsAction.link linkText="Cancel correction" linkUrl=springUrl(cancelCorrectionUrl) linkClass="govuk-button govuk-button--secondary"/>
    </@fdsAction.buttonGroup>

    <#if licencePositionPageView.hasPositions()>
      <h2 class="govuk-heading-m">${licencePositionPageView.date()} (${licencePositionPageView.regulatorReference()})</h2>
        <@grid.gridRow>
            <@grid.threeQuarterColumn>
            <#if licencePositionPageView.isAddedPosition()>
              <h2 class="govuk-heading-m">${licencePositionPageView.date()}</h2>
                <@fdsSummaryList.summaryListCard headingText="Added position" summaryListId="added-position">
                    <@fdsSummaryList.summaryListRowNoAction keyText="Regulator reference">
                        ${licencePositionPageView.regulatorReference()}
                    </@fdsSummaryList.summaryListRowNoAction>
                </@fdsSummaryList.summaryListCard>
            <#else>
                <@licencePositionDetails.details
                licencePositionState=licencePositionPageView.stateView()
                licencePositionChanges=licencePositionPageView.changeViewByType()
                />
            </#if>
            </@grid.threeQuarterColumn>
            <@grid.oneQuarterColumn>
                <@licencePositionTimeLine.timeline licencePositionTimelineViews=licencePositionPageView.timelineViews() selectedPositionId=licencePositionPageView.selectedPositionId()/>
            </@grid.oneQuarterColumn>
        </@grid.gridRow>
    <#else>
      <@fdsInsetText.insetText>No licence positions for this licence.</@fdsInsetText.insetText>
    </#if>
</@defaultPage>