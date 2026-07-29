<#include '../../layout/layout.ftl'>
<#import '../../summary/_summaryDetails.ftl' as summaryDetails>
<#import '../../tasklist/standardTaskList.ftl' as taskList>
<#import '_applicationContext.ftl' as applicationContextInfo>

<@defaultPage
htmlTitle=pageTitle
pageHeading=""
pageSize=PageSize.TWO_THIRDS_COLUMN
breadcrumbs=breadcrumbs>

    <@applicationContextInfo.applicationContextInfo applicationContext=applicationContext/>

    <p class="govuk-body">
        <@fdsAction.link
            linkText="View the full application (opens in a new tab)"
            linkUrl=springUrl(viewApplicationUrl)
            openInNewTab=true/>
    </p>

    <#if signedDspSummaryItem??>
        <@summaryDetails.summaryDetails summaryItem=signedDspSummaryItem/>
    </#if>

    <@taskList.standardTaskList taskListSections=taskListSections />

</@defaultPage>
