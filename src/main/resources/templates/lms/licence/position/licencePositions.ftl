<#include '../../layout/layout.ftl'>
<#import '_licencePositionTimeLine.ftl' as licencePositionTimeLine>
<#import '_licencePositionDetails.ftl' as licencePositionDetails>

<#assign pageTitle>
  <#if licencePositionPageView.hasPositions()>
      ${licencePositionPageView.date()} (${licencePositionPageView.regulatorReference()})
  <#else>
      Licence positions
  </#if>
</#assign>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  pageSize=PageSize.FULL_COLUMN
>

    <#if licencePositionPageView.hasPositions()>
      <@grid.gridRow>
        <@grid.threeQuarterColumn>
          <@licencePositionDetails.details
            licencePositionState=licencePositionPageView.stateView()
            licencePositionChanges=licencePositionPageView.changeViewByType()
          />
        </@grid.threeQuarterColumn>
        <@grid.oneQuarterColumn>
          <@licencePositionTimeLine.timeline licencePositionTimelineViews=licencePositionPageView.timelineViews() selectedPositionId=licencePositionPageView.selectedPositionId()/>
        </@grid.oneQuarterColumn>
      </@grid.gridRow>
    <#else>
      <@fdsInsetText.insetText>No executed licence positions for this licence.</@fdsInsetText.insetText>
    </#if>

</@defaultPage>