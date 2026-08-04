<#include '../layout/layout.ftl'>
<#import '../component/actions/actionItems.ftl' as actionItems>
<#import 'schedule/timeline/scheduleComponents.ftl' as scheduleTimeline>
<#import '../component/inline/inlineInputAction.ftl' as inlineInputAction>

<@defaultPage
htmlTitle=licenceReference
pageHeading=licenceReference
caption=caption
pageSize=PageSize.FULL_COLUMN
>
    <#if timelineSummaryCardView?has_content>
        <@scheduleTimeline.timelineSummaryCard timelineSummaryCardView=timelineSummaryCardView/>
    </#if>

    <@actionItems.actionItems actionItems=licenceActions screenReaderText=licenceReference/>

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
</@defaultPage>