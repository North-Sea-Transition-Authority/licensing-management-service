<#include  '../../../layout/layout.ftl'>

<#macro term termView>
    <#assign timelineActions>
        <@fdsAction.link linkText="Edit" linkUrl=springUrl(termView.updateUrl()) linkClass="govuk-link"/>
        <@fdsAction.link linkText="Remove" linkUrl=springUrl(termView.deleteUrl()) linkClass="govuk-link"/>
    </#assign>

    <#if termView.events()?has_content && termView.hasPhases()>
        <#list termView.events() as phaseView>
            <@phase phaseView=phaseView/>
        </#list>
    <#else>
        <@fdsTimeline.timelineTimeStamp
        timeStampHeading=termView.termType().displayName
        timeStampHeadingHint=termView.dateDurationString()
        timelineActionContent=timelineActions>
        </@fdsTimeline.timelineTimeStamp>
        <@fdsTimeline.timelineTimeStamp
        timeStampHeading="End of ${termView.termType().displayName}"
        timeStampHeadingHint=termView.endDateString()
        timeStampClass="fds-timeline__time-stamp--no-border">
        </@fdsTimeline.timelineTimeStamp>
    </#if>

</#macro>

<#macro phase phaseView>
    <#assign timelineActions>
        <@fdsAction.link linkText="Edit" linkUrl=springUrl(phaseView.updateUrl()) linkClass="govuk-link"/>
        <@fdsAction.link linkText="Remove" linkUrl=springUrl(phaseView.deleteUrl()) linkClass="govuk-link"/>
    </#assign>

    <@fdsTimeline.timelineTimeStamp
        timeStampHeading=phaseView.phaseType().displayName
        timeStampHeadingHint=phaseView.dateDurationString()
        timelineActionContent=timelineActions>
    </@fdsTimeline.timelineTimeStamp>
    <@fdsTimeline.timelineTimeStamp
        timeStampHeading="End of ${phaseView.phaseType().displayName}"
        timeStampHeadingHint=phaseView.endDateString()
        timeStampClass="fds-timeline__time-stamp--no-border">
    </@fdsTimeline.timelineTimeStamp>
</#macro>