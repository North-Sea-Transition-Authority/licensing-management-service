<#include '../../layout/layout.ftl'>
<#import '_applicationContext.ftl' as applicationContextInfo>
<#import '../../component/actions/actionItems.ftl' as actionItems>
<#import 'scheduleApplicationSummary.ftl' as scheduleApplicationSummary>

<@defaultPage
htmlTitle="Application overview"
pageHeading=""
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
>
  <@applicationContextInfo.applicationContextInfo applicationContext=applicationContext/>
  <@actionItems.actionItems actionItems=applicationActions screenReaderText=applicationContext.reference()/>
  <@scheduleApplicationSummary.scheduleApplicationSummary accordionId=accordionId summarySections=summarySections/>
</@defaultPage>
