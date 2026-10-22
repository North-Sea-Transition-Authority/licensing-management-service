<#include '../../layout/layout.ftl'>
<#import '../../summary/_summaryDetails.ftl' as summaryDetails>
<#import '../../tasklist/standardTaskList.ftl' as taskList>
<#import '../../licence/scheduleWorkProgrammeApplication/_applicationContext.ftl' as applicationContextInfo>

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

    <#if dspChanges?has_content>
        <@fdsWarning.warning>
            The DSP has changed since you started recording this decision.
        </@fdsWarning.warning>
        <ul class="govuk-list govuk-list--bullet">
            <#list dspChanges as change>
                <li>${change}</li>
            </#list>
        </ul>
    </#if>

    <@summaryDetails.summaryDetails summaryItem=signedDspSummaryItem/>

    <@taskList.standardTaskList taskListSections=taskListSections />

</@defaultPage>
