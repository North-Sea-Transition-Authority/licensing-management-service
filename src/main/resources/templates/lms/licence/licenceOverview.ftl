<#include '../layout/layout.ftl'>
<#import './tabbedLicencePage.ftl' as tabbedLicencePage>
<#import 'schedule/timeline/scheduleComponents.ftl' as scheduleTimeline>
<#import '../component/inline/inlineInputAction.ftl' as inlineInputAction>

<@tabbedLicencePage.page
    heading=licenceReference
    caption=caption
    topLevelLicenceActions=topLevelLicenceActions
    tabs=tabs
    currentTab=currentTab
    currentTabLicenceActions=currentTabLicenceActions
>
    <#if timelineSummaryCardView?has_content>
        <@scheduleTimeline.timelineSummaryCard timelineSummaryCardView=timelineSummaryCardView/>
    </#if>

    <#if scheduleHistoryOptions?has_content>
        <@fdsForm.htmlForm actionUrl=springUrl(viewScheduleHistoryUrl)>
            <@inlineInputAction.inlineInputAction>
                <@fdsSelect.select path="historyForm.licenceScheduleDetailId" options=scheduleHistoryOptions labelText="Schedule history"/>

                <@fdsAction.button buttonText="Show version" buttonClass="govuk-button govuk-button--secondary"/>
            </@inlineInputAction.inlineInputAction>
        </@fdsForm.htmlForm>
    </#if>

    <#if scheduleEventViews?has_content>
        <@scheduleTimeline.timelineWithFilters
        scheduleEventViews=scheduleEventViews
        timelineFilterOptions=timelineFilterOptions
        clearFilterUrl=clearFilterUrl
        />
    </#if>

</@tabbedLicencePage.page>