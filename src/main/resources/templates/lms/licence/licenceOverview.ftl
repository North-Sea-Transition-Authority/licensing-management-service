<#include '../layout/layout.ftl'>
<#import '../component/actions/actionItems.ftl' as actionItems>
<#import 'schedule/timeline/scheduleComponents.ftl' as scheduleTimeline>

<@defaultPage
htmlTitle=licenceReference
pageHeading=licenceReference
caption=caption
pageSize=PageSize.FULL_COLUMN
>
  <@scheduleTimeline.timelineSummaryCard timelineSummaryCardView=timelineSummaryCardView/>

  <@actionItems.actionItems actionItems=licenceActions screenReaderText=licenceReference/>

  <@scheduleTimeline.timelineWithFilters
      scheduleEventViews=scheduleEventViews
      timelineFilterOptions=timelineFilterOptions
      clearFilterUrl=clearFilterUrl
  />
</@defaultPage>