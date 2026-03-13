<#include '../../layout/layout.ftl'>
<#import '_applicationContext.ftl' as applicationContextInfo>
<#import '../../component/actions/actionItems.ftl' as actionItems>
<#import 'continuationApplicationSummary.ftl' as continuationApplicationSummary>

<@defaultPage
htmlTitle="Application overview"
pageHeading=""
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
>
  <@applicationContextInfo.applicationContextInfo applicationContext=applicationContext/>
    <@actionItems.actionItems actionItems=applicationActions screenReaderText=applicationContext.reference()/>
  <@continuationApplicationSummary.continuationApplicationSummary accordionId=accordionId summarySections=summarySections showWorkProgrammeActivities=showWorkProgrammeActivities workProgrammeActivities=workProgrammeActivities![]/>
</@defaultPage>