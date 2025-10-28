<#include '../../layout/layout.ftl'>
<#import '../../tasklist/standardTaskList.ftl' as taskList>

<@defaultPage
htmlTitle="Application"
pageHeading=pageTitle
    pageHeadingClass="govuk-heading-l"
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
breadcrumbs=breadcrumbs>

    <@taskList.standardTaskList taskListSections=taskListSections />

</@defaultPage>