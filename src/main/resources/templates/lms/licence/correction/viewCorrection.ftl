<#include '../../layout/layout.ftl'>
<#import '../position/_licencePositionTimeLine.ftl' as licencePositionTimeLine>
<#import '../position/_licencePositionDetails.ftl' as licencePositionDetails>
<#import '_correctionDetailsCard.ftl' as correctionDetailsCard>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  caption=pageCaption
  errorSummaryItems=licencePositionPageView.errorSummaryItems()
>
  <@correctionDetailsCard.correctionDetailsCard
    correction=correction
    allocatedToUser=allocatedToUser
    updateUrl=isCorrectionInProgress?then(updateGeneralDetailsUrl, "")
  />

  <@fdsAction.buttonGroup>
    <@fdsAction.link linkText="Add position" linkUrl=springUrl(addPositionUrl) linkClass="govuk-button"/>
    <@fdsAction.link linkText="Cancel correction" linkUrl=springUrl(cancelCorrectionUrl) linkClass="govuk-button govuk-button--secondary"/>
    <#if isCorrectionInProgress>
      <@fdsAction.link linkText="Review correction" linkUrl=springUrl(reviewCorrectionUrl) linkClass="govuk-button govuk-button--secondary"/>
    </#if>
  </@fdsAction.buttonGroup>

  <#if licencePositionPageView.hasPositions()>
    <h2 class="govuk-heading-m">
      ${licencePositionPageView.date()} (${licencePositionPageView.regulatorReference()})
      <#if licencePositionPageView.isAddedPosition()>
        <@fdsTag.tag tagClass="govuk-tag--green">Added position</@fdsTag.tag>
      </#if>
    </h2>
      <@grid.gridRow>
        <@grid.threeQuarterColumn>
          <@licencePositionDetails.details
            licencePositionState=licencePositionPageView.stateView()
            licencePositionChanges=licencePositionPageView.orderedChangeViews()
            actions=licencePositionPageView.actions()
            canEdit=licencePositionPageView.canEdit()
            isCarbonStorage=licencePositionPageView.isCarbonStorage()
          />
        </@grid.threeQuarterColumn>
        <@grid.oneQuarterColumn>
          <@licencePositionTimeLine.timeline
            licencePositionTimelineViews=licencePositionPageView.timelineViews()
            selectedPositionId=licencePositionPageView.selectedPositionId()
          />
        </@grid.oneQuarterColumn>
      </@grid.gridRow>
  <#else>
    <@fdsInsetText.insetText>No licence positions for this licence.</@fdsInsetText.insetText>
  </#if>
</@defaultPage>