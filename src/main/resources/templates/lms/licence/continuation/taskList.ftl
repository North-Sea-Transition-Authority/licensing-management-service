<#include '../../layout/layout.ftl'>
<#import '../../tasklist/standardTaskList.ftl' as taskList>
<#import '../../macros/dataItems/continuationDataItems.ftl' as continuationDataItem>

<@defaultPage
htmlTitle="Application"
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
breadcrumbs=breadcrumbs>

    <@continuationDataItem.continuationDataItem
    currentTermPhaseDisplay=currentTermPhaseDisplay!""
    nextTermPhaseDisplay=nextTermPhaseDisplay!""
    />

    <@taskList.standardTaskList taskListSections=taskListSections />

</@defaultPage>