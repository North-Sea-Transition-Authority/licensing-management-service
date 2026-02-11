<#include '../../layout/layout.ftl'>
<#import '_applicationContext.ftl' as applicationContextInfo>
<#import 'scheduleApplicationSummary.ftl' as scheduleApplicationSummary>

<@defaultPage
htmlTitle="Application overview"
pageHeading=""
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
>
  <@applicationContextInfo.applicationContextInfo applicationContext=applicationContext/>
  <@scheduleApplicationSummary.scheduleApplicationSummary accordionId=accordionId summarySections=summarySections/>
</@defaultPage>
