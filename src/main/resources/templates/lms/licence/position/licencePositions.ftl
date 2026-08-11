<#include '../../layout/layout.ftl'>
<#import '../tabbedLicencePage.ftl' as tabbedLicencePage>
<#import '_licencePositionTimeLine.ftl' as licencePositionTimeLine>
<#import '_licencePositionDetails.ftl' as licencePositionDetails>

<#assign pageTitle>
  <#if licencePositionPageView.hasPositions()>
      ${licencePositionPageView.date()} (${licencePositionPageView.regulatorReference()})
  <#else>
      Licence positions
  </#if>
</#assign>

<@tabbedLicencePage.page
  heading=pageTitle
  caption=pageCaption
  topLevelLicenceActions=topLevelLicenceActions
  tabs=tabs
  currentTab=currentTab
  currentTabLicenceActions=currentTabLicenceActions
>

    <#if licencePositionPageView.hasPositions()>
      <@grid.gridRow>
        <@grid.threeQuarterColumn>
          <@licencePositionDetails.details
            licencePositionState=licencePositionPageView.stateView()
            licencePositionChanges=licencePositionPageView.changeViewByType()
            isCarbonStorage=licencePositionPageView.isCarbonStorage()
          />
        </@grid.threeQuarterColumn>
        <@grid.oneQuarterColumn>
          <@licencePositionTimeLine.timeline licencePositionTimelineViews=licencePositionPageView.timelineViews() selectedPositionId=licencePositionPageView.selectedPositionId()/>
        </@grid.oneQuarterColumn>
      </@grid.gridRow>
    <#else>
      <@fdsInsetText.insetText>No executed licence positions for this licence.</@fdsInsetText.insetText>
    </#if>

</@tabbedLicencePage.page>