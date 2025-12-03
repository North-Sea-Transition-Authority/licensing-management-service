<#include '../../layout/layout.ftl'>
<#import '../../tasklist/standardTaskList.ftl' as taskList>

<@defaultPage
htmlTitle="Application"
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
breadcrumbs=breadcrumbs>
    <@fdsAction.link
    linkText="Delete draft application"
    linkUrl=springUrl(deleteScheduleWorkProgrammeApplicationUrl)
    linkClass="govuk-button govuk-button--secondary"
    role=true/>
    <@taskList.standardTaskList taskListSections=taskListSections />

</@defaultPage>