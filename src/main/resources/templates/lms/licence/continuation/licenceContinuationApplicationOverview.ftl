<#include '../../layout/layout.ftl'>
<#import '_applicationContext.ftl' as applicationContextInfo>
<#import '../../component/actions/actionItems.ftl' as actionItems>
<#import 'continuationApplicationSummary.ftl' as continuationApplicationSummary>
<#import 'continuationLetterIssuedSummary.ftl' as continuationLetterIssuedSummary>
<#import '../../macros/caseprocessingtabs/caseProccessingTabs.ftl' as caseProccessingTabs>

<@defaultPage
htmlTitle="Application overview"
pageHeading=""
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
>
    <@caseProccessingTabs.caseProcessingTabsWithContent
    tabs=availableTabs
    selectedTab={"value": selectedTab.value()} <#--TODO: Passing inline hash to avoid changing the shared macro. Fix change macro to use unified field or method instead of both -->
    controllerUrl=controllerUrl>
        <#if selectedTab.value() == "overview">
            <@applicationContextInfo.applicationContextInfo applicationContext=applicationContext/>
            <@actionItems.actionItems actionItems=applicationActions screenReaderText=applicationContext.reference()/>
            <@continuationApplicationSummary.continuationApplicationSummary accordionId=accordionId summarySections=summarySections showWorkProgrammeActivities=showWorkProgrammeActivities workProgrammeActivities=workProgrammeActivities![]/>
        </#if>

        <#if selectedTab.value() == "letter">
            <#if letterIssueSummarySection?has_content>
                <@continuationLetterIssuedSummary.continuationLetterIssuedSummary
                accordionId=accordionId
                summarySection=letterIssueSummarySection
                />
            <#else>
                <@fdsInsetText.insetText>
                  No continuation letter has been issued for this application.
                </@fdsInsetText.insetText>
            </#if>
        </#if>
    </@caseProccessingTabs.caseProcessingTabsWithContent>
</@defaultPage>