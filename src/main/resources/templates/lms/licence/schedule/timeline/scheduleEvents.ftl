<#include  '../../../layout/layout.ftl'>
<#import '../../../component/timeline/timestamp.ftl' as lmsTimeStamp>

<#macro term termView>
    <#if termView.updateUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Edit" linkUrl=springUrl(termView.updateUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Remove" linkUrl=springUrl(termView.deleteUrl()) linkClass="govuk-link"/>
        </#assign>
    </#if>

    <#if termView.events()?has_content && termView.hasPhases()>
        <#list termView.events() as eventView>
            <#if eventView.getEventType() = "RATE">
                <@rate rateView=eventView smallDot=false/>
            <#else>
                <@phase phaseView=eventView/>
            </#if>
        </#list>

        <#if termView.endOfTermEvents()?has_content>
            <@fdsTimeline.timelineTimeStamp
            timeStampHeading="End of term requirements"
            timeStampHeadingHint=termView.endDateString()
            />

            <#list termView.endOfTermEvents() as activityView>
                <@workProgrammeActivityEndOfPeriodRequirement activityView=activityView/>
            </#list>

            <@fdsTimeline.timelineTimeStamp
            timeStampHeading="End of ${termView.termType().displayName}"
            timeStampHeadingHint=termView.endDateString()
            timeStampClass="fds-timeline__time-stamp--no-border"
            />
        </#if>
    <#else>
        <@fdsTimeline.timelineTimeStamp
            timeStampHeading=termView.termType().displayName
            timeStampHeadingHint=termView.dateDurationString()
            timelineActionContent=timelineActions
        />

        <#list termView.events() as eventView>
            <#if eventView.getEventType() = "WORK_PROGRAMME_ACTIVITY">
                <@workProgrammeActivity activityView=eventView/>
            <#elseif eventView.getEventType() = "RATE">
                <@rate rateView=eventView/>
            </#if>
        </#list>

        <#if termView.endOfTermEvents()?has_content>
            <@fdsTimeline.timelineTimeStamp
                timeStampHeading="End of term requirements"
                timeStampHeadingHint=termView.endDateString()
            />

            <#list termView.endOfTermEvents() as activityView>
                <@workProgrammeActivityEndOfPeriodRequirement activityView=activityView/>
            </#list>
        </#if>

        <@fdsTimeline.timelineTimeStamp
            timeStampHeading="End of ${termView.termType().displayName}"
            timeStampHeadingHint=termView.endDateString()
            timeStampClass="fds-timeline__time-stamp--no-border"
        />
    </#if>

</#macro>

<#macro phase phaseView>
    <#if phaseView.updateUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Edit" linkUrl=springUrl(phaseView.updateUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Remove" linkUrl=springUrl(phaseView.deleteUrl()) linkClass="govuk-link"/>
        </#assign>
    </#if>

    <@fdsTimeline.timelineTimeStamp
        timeStampHeading=phaseView.phaseType().displayName
        timeStampHeadingHint=phaseView.dateDurationString()
        timelineActionContent=timelineActions
    />

    <#list phaseView.events() as eventView>
        <#if eventView.getEventType() = "WORK_PROGRAMME_ACTIVITY">
            <@workProgrammeActivity activityView=eventView/>
        <#elseif eventView.getEventType() = "RATE">
            <@rate eventView/>
        </#if>
    </#list>

    <#if phaseView.endOfPhaseEvents()?has_content>
        <@fdsTimeline.timelineTimeStamp
        timeStampHeading="End of phase requirements"
        timeStampHeadingHint=phaseView.endDateString()
        />

        <#list phaseView.endOfPhaseEvents() as activityView>
            <@workProgrammeActivityEndOfPeriodRequirement activityView=activityView/>
        </#list>
    </#if>

    <@fdsTimeline.timelineTimeStamp
        timeStampHeading="End of ${phaseView.phaseType().displayName}"
        timeStampHeadingHint=phaseView.endDateString()
        timeStampClass="fds-timeline__time-stamp--no-border"
    />
</#macro>

<#macro workProgrammeActivity activityView>
    <#if activityView.updateUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Edit" linkUrl=springUrl(activityView.updateUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Remove" linkUrl=springUrl(activityView.deleteUrl()) linkClass="govuk-link"/>
        </#assign>
    </#if>

    <@lmsTimeStamp.lmsTimeStamp
        timeStampHeading=activityView.category()
        timeStampHeadingHint=activityView.dueDateString()
        timelineActionContent=timelineActions
        nodeNumberClass="fds-timeline__node-number--small-dot"
    >
        <@fdsTimeline.timelineEvent>
            <p class="govuk-body">
                ${activityView.description()}
            </p>
        </@fdsTimeline.timelineEvent>
    </@lmsTimeStamp.lmsTimeStamp>
</#macro>

<#macro workProgrammeActivityEndOfPeriodRequirement activityView>
    <#if activityView.updateUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Edit" linkUrl=springUrl(activityView.updateUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Remove" linkUrl=springUrl(activityView.deleteUrl()) linkClass="govuk-link"/>
        </#assign>
    </#if>

    <@lmsTimeStamp.lmsTimeStamp
        timeStampHeading=activityView.category()
        timelineActionContent=timelineActions
        nodeNumberClass="fds-timeline__node-number--no-dot"
    >
        <@fdsTimeline.timelineEvent>
            <p class="govuk-body">
                ${activityView.description()}
            </p>
        </@fdsTimeline.timelineEvent>
    </@lmsTimeStamp.lmsTimeStamp>
</#macro>

<#macro rate rateView smallDot=true>
    <#if rateView.updateUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Edit" linkUrl=springUrl(rateView.updateUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Remove" linkUrl=springUrl(rateView.deleteUrl()) linkClass="govuk-link"/>
        </#assign>
    </#if>

    <#if smallDot=true>
        <#assign nodeClass = "fds-timeline__node-number--small-dot">
        <#assign timeStampClass = "">
    <#else>
        <#assign nodeClass = "">
        <#assign timeStampClass = "fds-timeline__time-stamp--no-border">
    </#if>

    <@lmsTimeStamp.lmsTimeStamp
        timeStampHeading=rateView.title()
        timeStampHeadingHint=rateView.startDateString()
        timelineActionContent=timelineActions
        timeStampClass=timeStampClass
        nodeNumberClass=nodeClass
    >
        <@fdsTimeline.timelineEvent>
            <p class="govuk-body">
                ${rateView.rentalRateString()} per km<sup>2</sup>
            </p>
        </@fdsTimeline.timelineEvent>
    </@lmsTimeStamp.lmsTimeStamp>
</#macro>