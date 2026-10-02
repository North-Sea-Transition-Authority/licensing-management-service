<#include '../../layout/layout.ftl'>
<#import '../tabbedLicencePage.ftl' as tabbedLicencePage>
<#import '_licencePositionTimeLine.ftl' as licencePositionTimeLine>
<#import '_licencePositionDetails.ftl' as licencePositionDetails>
<#import '_licencePositionTimelineFilters.ftl' as licencePositionTimelineFilters>

<@tabbedLicencePage.page
  licenceOverviewView=licenceOverviewView
  licenceSummaryCardView=licenceSummaryCardView
  topLevelLicenceActions=topLevelLicenceActions
  tabs=tabs
  currentTab=currentTab
  currentTabLicenceActions=currentTabLicenceActions
>

    <@licencePositionTimelineFilters.filters
      filterOptions=licencePositionPageView.filterOptions()
      filterUrl=filterUrl
      clearFilterUrl=clearFilterUrl
    />

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
          <#if licencePositionPageView.filterApplied() && !licencePositionPageView.orderedChangeViews()?has_content>
            <@fdsInsetText.insetText>This position has no changes of the selected types.</@fdsInsetText.insetText>
          </#if>
        </@grid.threeQuarterColumn>
        <@grid.oneQuarterColumn>
          <@licencePositionTimeLine.timeline licencePositionTimelineViews=licencePositionPageView.timelineViews() selectedPositionId=licencePositionPageView.selectedPositionId()/>
        </@grid.oneQuarterColumn>
      </@grid.gridRow>
    <#elseif licencePositionPageView.filterApplied()>
      <@fdsInsetText.insetText>No positions match the selected filters.</@fdsInsetText.insetText>
    <#else>
      <@fdsInsetText.insetText>No timeline exists for this licence.</@fdsInsetText.insetText>
    </#if>

</@tabbedLicencePage.page>
