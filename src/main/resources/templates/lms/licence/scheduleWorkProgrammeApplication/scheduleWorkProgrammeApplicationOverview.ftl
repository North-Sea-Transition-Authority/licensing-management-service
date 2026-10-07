<#include '../../layout/layout.ftl'>
<#import '_applicationContext.ftl' as applicationContextInfo>
<#import '../../component/actions/actionItems.ftl' as actionItems>
<#import 'scheduleApplicationSummary.ftl' as scheduleApplicationSummary>
<#import '../../summary/_summaryDetails.ftl' as summaryDetails>
<#import '../../macros/caseprocessingtabs/caseProccessingTabs.ftl' as caseProccessingTabs>

<#macro overviewContent>
  <@applicationContextInfo.applicationContextInfo applicationContext=applicationContext/>
  <@actionItems.actionItems actionItems=applicationActions screenReaderText=applicationContext.reference()/>
  <@scheduleApplicationSummary.scheduleApplicationSummary accordionId=accordionId summarySections=summarySections/>
</#macro>

<@defaultPage
htmlTitle="Application overview"
pageHeading=""
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
>
  <#if availableTabs?size gt 1>
    <@caseProccessingTabs.caseProcessingTabsWithContent
    tabs=availableTabs
    selectedTab={"value": selectedTab.value()}
    controllerUrl=controllerUrl>
      <#if selectedTab.value() == "overview">
        <@overviewContent/>
      </#if>

      <#if selectedTab.value() == "case-events">
        <#if caseEventsSummaryItem.summaryCards()?has_content>
          <@summaryDetails.summaryDetails summaryItem=caseEventsSummaryItem/>
        <#else>
          <@fdsInsetText.insetText>
            No case events have been recorded for this application.
          </@fdsInsetText.insetText>
        </#if>
      </#if>
    </@caseProccessingTabs.caseProcessingTabsWithContent>
  <#else>
    <@overviewContent/>
  </#if>
</@defaultPage>
