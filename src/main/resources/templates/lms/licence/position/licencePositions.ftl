<#include '../../layout/layout.ftl'>
<#import '../tabbedLicencePage.ftl' as tabbedLicencePage>
<#import '_licencePositionTimeLine.ftl' as licencePositionTimeLine>
<#import '_licencePositionDetails.ftl' as licencePositionDetails>

<@tabbedLicencePage.page
  licenceOverviewView=licenceOverviewView
  licenceSummaryCardView=licenceSummaryCardView
  topLevelLicenceActions=topLevelLicenceActions
  tabs=tabs
  currentTab=currentTab
  currentTabLicenceActions=currentTabLicenceActions
>

    <#if licencePositionPageView.hasPositions()>
      <h2 class="govuk-heading-m">
        ${licencePositionPageView.date()} (${licencePositionPageView.regulatorReference()})
      </h2>
      <@grid.gridRow>
        <@grid.threeQuarterColumn>
          <@licencePositionDetails.details
            licencePositionState=licencePositionPageView.stateView()
            licencePositionChanges=licencePositionPageView.orderedChangeViews()
            isCarbonStorage=licencePositionPageView.isCarbonStorage()
          />
        </@grid.threeQuarterColumn>
        <@grid.oneQuarterColumn>
          <@licencePositionTimeLine.timeline licencePositionTimelineViews=licencePositionPageView.timelineViews() selectedPositionId=licencePositionPageView.selectedPositionId()/>
        </@grid.oneQuarterColumn>
      </@grid.gridRow>
    <#else>
      <@fdsInsetText.insetText>No timeline exists for this licence.</@fdsInsetText.insetText>
    </#if>

</@tabbedLicencePage.page>
