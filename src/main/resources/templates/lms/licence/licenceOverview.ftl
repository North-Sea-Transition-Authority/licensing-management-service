<#include '../layout/layout.ftl'>
<#import '../component/actions/actionItems.ftl' as actionItems>
<#import 'schedule/timeline/scheduleComponents.ftl' as scheduleTimeline>

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

    <#if scheduleEventViews?has_content>
        <@scheduleTimeline.timelineWithFilters
        scheduleEventViews=scheduleEventViews
        timelineFilterOptions=timelineFilterOptions
        clearFilterUrl=clearFilterUrl
        />
    </#if>
</@defaultPage>