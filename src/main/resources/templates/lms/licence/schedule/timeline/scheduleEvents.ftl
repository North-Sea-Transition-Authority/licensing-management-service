<#include  '../../../layout/layout.ftl'>
<#import '../../../component/timeline/timestamp.ftl' as lmsTimeStamp>

<#macro term termView>
    <#if termView.updateUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Edit" linkUrl=springUrl(termView.updateUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Remove" linkUrl=springUrl(termView.deleteUrl()) linkClass="govuk-link"/>
        </#assign>
    <#elseif termView.addCommentUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Add comment" linkUrl=springUrl(termView.addCommentUrl()) linkClass="govuk-link"/>
        </#assign>
    <#else>
        <#assign timelineActions></#assign>
    </#if>

    <#if termView.events()?has_content && termView.hasPhases()>
        <#assign timeStampClass = "fds-timeline__time-stamp--no-border"/>

        <#if termView.showStartDateProgress()>
            <#assign timeStampClass = "fds-timeline__time-stamp--complete--no-border"/>
        </#if>

        <@fdsTimeline.timelineTimeStamp
        timeStampHeading=termView.termType().displayName
        timeStampHeadingHint=termView.dateDurationString()
        timelineActionContent=timelineActions
        timeStampClass=timeStampClass
        >
            <@fdsTimeline.timelineEvent>
                <@eventComments comments=termView.comments() canDeleteComments=termView.addCommentUrl()?has_content/>
            </@fdsTimeline.timelineEvent>
        </@fdsTimeline.timelineTimeStamp>

        <#list termView.events() as eventView>
            <#if eventView.getEventType() = "RATE">
                <@rate rateView=eventView smallDot=false/>
            <#else>
                <@phase phaseView=eventView/>
            </#if>
        </#list>

        <#assign endOfTermTimeStampClass = "fds-timeline__time-stamp--no-border"/>
        <#assign endOfTermEventsTimeStampClass = ""/>
        <#if termView.showEndDateProgress()>
            <#assign endOfTermTimeStampClass = "fds-timeline__time-stamp--complete--no-border"/>
            <#assign endOfTermEventsTimeStampClass = "fds-timeline__time-stamp--complete"/>
        </#if>

        <#if termView.endOfTermEvents()?has_content>
            <@fdsTimeline.timelineTimeStamp
            timeStampHeading="End of term requirements"
            timeStampHeadingHint=termView.endDateString()
            timeStampClass=endOfTermEventsTimeStampClass
            />

            <#list termView.endOfTermEvents() as eventView>
                <#if eventView.getEventType() = "WORK_PROGRAMME_ACTIVITY">
                    <@workProgrammeActivityEndOfPeriodRequirement activityView=eventView showProgress=termView.showEndDateProgress()/>
                <#elseif eventView.getEventType() = "OTHER">
                    <@otherScheduleEventEndOfPeriodRequirement eventView=eventView showProgress=termView.showEndDateProgress()/>
                </#if>
            </#list>

            <@fdsTimeline.timelineTimeStamp
            timeStampHeading="End of ${termView.termType().displayName}"
            timeStampHeadingHint=termView.endDateString()
            timeStampClass=endOfTermTimeStampClass
            />
        </#if>
    <#else>
        <#assign startTimeStampClass = ""/>

        <#if termView.showStartDateProgress()>
            <#assign startTimeStampClass = "fds-timeline__time-stamp--complete"/>
        </#if>

        <@fdsTimeline.timelineTimeStamp
            timeStampHeading=termView.termType().displayName
            timeStampHeadingHint=termView.dateDurationString()
            timelineActionContent=timelineActions
            timeStampClass=startTimeStampClass
        >
            <@fdsTimeline.timelineEvent>
                <@eventComments comments=termView.comments() canDeleteComments=termView.addCommentUrl()?has_content/>
            </@fdsTimeline.timelineEvent>
        </@fdsTimeline.timelineTimeStamp>

        <#list termView.events() as eventView>
            <#if eventView.getEventType() = "WORK_PROGRAMME_ACTIVITY">
                <@workProgrammeActivity activityView=eventView/>
            <#elseif eventView.getEventType() = "RATE">
                <@rate rateView=eventView/>
            <#elseif eventView.getEventType() = "OTHER">
                <@otherScheduleEvent eventView=eventView/>
            </#if>
        </#list>

        <#assign endOfTermTimeStampClass = "fds-timeline__time-stamp--no-border"/>
        <#assign endOfTermEventsTimeStampClass = ""/>
        <#if termView.showEndDateProgress()>
            <#assign endOfTermTimeStampClass = "fds-timeline__time-stamp--complete--no-border"/>
            <#assign endOfTermEventsTimeStampClass = "fds-timeline__time-stamp--complete"/>
        </#if>

        <#if termView.endOfTermEvents()?has_content>
            <@fdsTimeline.timelineTimeStamp
                timeStampHeading="End of term requirements"
                timeStampHeadingHint=termView.endDateString()
                timeStampClass=endOfTermEventsTimeStampClass
            />

            <#list termView.endOfTermEvents() as eventView>
                <#if eventView.getEventType() = "WORK_PROGRAMME_ACTIVITY">
                    <@workProgrammeActivityEndOfPeriodRequirement activityView=eventView showProgress=termView.showEndDateProgress()/>
                <#elseif eventView.getEventType() = "OTHER">
                    <@otherScheduleEventEndOfPeriodRequirement eventView=eventView showProgress=termView.showEndDateProgress()/>
                </#if>
            </#list>
        </#if>

        <@fdsTimeline.timelineTimeStamp
            timeStampHeading="End of ${termView.termType().displayName}"
            timeStampHeadingHint=termView.endDateString()
            timeStampClass=endOfTermTimeStampClass
        />
    </#if>

</#macro>

<#macro phase phaseView>
    <#if phaseView.updateUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Edit" linkUrl=springUrl(phaseView.updateUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Remove" linkUrl=springUrl(phaseView.deleteUrl()) linkClass="govuk-link"/>
        </#assign>
    <#elseif phaseView.addCommentUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Add comment" linkUrl=springUrl(phaseView.addCommentUrl()) linkClass="govuk-link"/>
        </#assign>
    <#else>
        <#assign timelineActions></#assign>
    </#if>

    <#assign phaseStartTimeStampClass = ""/>

    <#if phaseView.showStartDateProgress()>
        <#assign phaseStartTimeStampClass = "fds-timeline__time-stamp--complete"/>
    </#if>

    <@fdsTimeline.timelineTimeStamp
        timeStampHeading=phaseView.phaseType().displayName
        timeStampHeadingHint=phaseView.dateDurationString()
        timelineActionContent=timelineActions
        timeStampClass=phaseStartTimeStampClass
    >
        <@fdsTimeline.timelineEvent>
            <@eventComments comments=phaseView.comments() canDeleteComments=phaseView.addCommentUrl()?has_content/>
        </@fdsTimeline.timelineEvent>
    </@fdsTimeline.timelineTimeStamp>

    <#list phaseView.events() as eventView>
        <#if eventView.getEventType() = "WORK_PROGRAMME_ACTIVITY">
            <@workProgrammeActivity activityView=eventView/>
        <#elseif eventView.getEventType() = "RATE">
            <@rate eventView/>
        <#elseif eventView.getEventType() = "OTHER">
            <@otherScheduleEvent eventView=eventView/>
        </#if>
    </#list>
    <#assign phaseEndTimeStampClass = "fds-timeline__time-stamp--no-border"/>
    <#assign phaseEventsTimeStampClass = ""/>

    <#if phaseView.showEndDateProgress()>
        <#assign phaseEndTimeStampClass = "fds-timeline__time-stamp--complete--no-border"/>
        <#assign phaseEventsTimeStampClass = "fds-timeline__time-stamp--complete"/>
    </#if>
    <#if phaseView.endOfPhaseEvents()?has_content>
        <@fdsTimeline.timelineTimeStamp
        timeStampHeading="End of phase requirements"
        timeStampHeadingHint=phaseView.endDateString()
        timeStampClass=phaseEventsTimeStampClass
        />

        <#list phaseView.endOfPhaseEvents() as eventView>
            <#if eventView.getEventType() = "WORK_PROGRAMME_ACTIVITY">
                <@workProgrammeActivityEndOfPeriodRequirement activityView=eventView showProgress=phaseView.showEndDateProgress()/>
            <#elseif eventView.getEventType() = "OTHER">
                <@otherScheduleEventEndOfPeriodRequirement eventView=eventView showProgress=phaseView.showEndDateProgress()/>
            </#if>
        </#list>
    </#if>

    <@fdsTimeline.timelineTimeStamp
        timeStampHeading="End of ${phaseView.phaseType().displayName}"
        timeStampHeadingHint=phaseView.endDateString()
        timeStampClass=phaseEndTimeStampClass
    />
</#macro>

<#macro workProgrammeActivity activityView smallDot=true>
    <#if activityView.updateUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Edit" linkUrl=springUrl(activityView.updateUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Remove" linkUrl=springUrl(activityView.deleteUrl()) linkClass="govuk-link"/>
        </#assign>
    <#elseif activityView.updateStatusUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Add comment" linkUrl=springUrl(activityView.addCommentUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Update status" linkUrl=springUrl(activityView.updateStatusUrl()) linkClass="govuk-link"/>
        </#assign>
    <#else>
        <#assign timelineActions></#assign>
    </#if>

    <#if smallDot=true>
        <#assign nodeClass = "fds-timeline__node-number--small-dot">
        <#assign timeStampClass = "">
        <#if activityView.showProgress()>
            <#assign timeStampClass = "fds-timeline__time-stamp--complete">
        </#if>
    <#else>
        <#assign nodeClass = "">
        <#assign timeStampClass = "fds-timeline__time-stamp--no-border">
        <#if activityView.showProgress()>
            <#assign timeStampClass = "fds-timeline__time-stamp--complete--no-border">
        </#if>
    </#if>

    <@lmsTimeStamp.lmsTimeStamp
        timeStampHeading=activityView.category()
        timeStampHeadingHint=activityView.dueDateString()
        timelineActionContent=timelineActions
        timeStampClass=timeStampClass
        nodeNumberClass=nodeClass
    >
        <@fdsTimeline.timelineEvent>
            <#if activityView.status()?has_content>
                <@fdsTag.tag tagClass=activityView.status().getTagDisplayClass()>
                    ${activityView.status().getDisplayName()}
                </@fdsTag.tag>
                <br>
            </#if>

            <p class="govuk-body">
                ${activityView.description()}
            </p>
            <@eventComments comments=activityView.comments() canDeleteComments=activityView.addCommentUrl()?has_content/>
        </@fdsTimeline.timelineEvent>
    </@lmsTimeStamp.lmsTimeStamp>
</#macro>

<#macro workProgrammeActivityEndOfPeriodRequirement activityView showProgress=false>
    <#if activityView.updateUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Edit" linkUrl=springUrl(activityView.updateUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Remove" linkUrl=springUrl(activityView.deleteUrl()) linkClass="govuk-link"/>
        </#assign>
    <#elseif activityView.updateStatusUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Add comment" linkUrl=springUrl(activityView.addCommentUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Update status" linkUrl=springUrl(activityView.updateStatusUrl()) linkClass="govuk-link"/>
        </#assign>
    <#else>
        <#assign timelineActions></#assign>
    </#if>

    <#assign timeStampClass = "">
    <#if showProgress>
        <#assign timeStampClass = "fds-timeline__time-stamp--complete">
    </#if>

    <@lmsTimeStamp.lmsTimeStamp
        timeStampHeading=activityView.category()
        timelineActionContent=timelineActions
        timeStampClass=timeStampClass
        nodeNumberClass="fds-timeline__node-number--no-dot"
    >
        <@fdsTimeline.timelineEvent>
            <#if activityView.status()?has_content>
                <br>
                <@fdsTag.tag tagClass=activityView.status().getTagDisplayClass()>
                    ${activityView.status().getDisplayName()}
                </@fdsTag.tag>
                <br>
                <br>
            </#if>
            <p class="govuk-body">
                ${activityView.description()}
            </p>
            <@eventComments comments=activityView.comments() canDeleteComments=activityView.addCommentUrl()?has_content/>
        </@fdsTimeline.timelineEvent>
    </@lmsTimeStamp.lmsTimeStamp>
</#macro>

<#macro rate rateView smallDot=true>
    <#if rateView.updateUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Edit" linkUrl=springUrl(rateView.updateUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Remove" linkUrl=springUrl(rateView.deleteUrl()) linkClass="govuk-link"/>
        </#assign>
    <#elseif rateView.addCommentUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Add comment" linkUrl=springUrl(rateView.addCommentUrl()) linkClass="govuk-link"/>
        </#assign>
    <#else>
        <#assign timelineActions></#assign>
    </#if>

    <#if smallDot=true>
        <#assign nodeClass = "fds-timeline__node-number--small-dot">
        <#assign timeStampClass = "">
        <#if rateView.showProgress()>
            <#assign timeStampClass = "fds-timeline__time-stamp--complete">
        </#if>
    <#else>
        <#assign nodeClass = "">
        <#assign timeStampClass = "fds-timeline__time-stamp--no-border">
        <#if rateView.showProgress()>
            <#assign timeStampClass = "fds-timeline__time-stamp--complete--no-border">
        </#if>
    </#if>

    <@lmsTimeStamp.lmsTimeStamp
        timeStampHeading=rateView.title()
        timeStampHeadingHint=rateView.startEndDateString()
        timelineActionContent=timelineActions
        timeStampClass=timeStampClass
        nodeNumberClass=nodeClass
    >
        <@fdsTimeline.timelineEvent>
            <p class="govuk-body">
                ${rateView.rentalRateString()} per km<sup>2</sup>
            </p>
            <@eventComments comments=rateView.comments() canDeleteComments=rateView.addCommentUrl()?has_content/>
        </@fdsTimeline.timelineEvent>
    </@lmsTimeStamp.lmsTimeStamp>
</#macro>

<#macro otherScheduleEvent eventView smallDot=true>
    <#if eventView.updateUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Edit" linkUrl=springUrl(eventView.updateUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Remove" linkUrl=springUrl(eventView.deleteUrl()) linkClass="govuk-link"/>
        </#assign>
    <#elseif eventView.addCommentUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Add comment" linkUrl=springUrl(eventView.addCommentUrl()) linkClass="govuk-link"/>
        </#assign>
    <#else>
        <#assign timelineActions></#assign>
    </#if>

    <#if smallDot=true>
        <#assign nodeClass = "fds-timeline__node-number--small-dot">
        <#assign timeStampClass = "">
        <#if eventView.showProgress()>
            <#assign timeStampClass = "fds-timeline__time-stamp--complete">
        </#if>
    <#else>
        <#assign nodeClass = "">
        <#assign timeStampClass = "fds-timeline__time-stamp--no-border">
        <#if eventView.showProgress()>
            <#assign timeStampClass = "fds-timeline__time-stamp--complete--no-border">
        </#if>
    </#if>

    <@lmsTimeStamp.lmsTimeStamp
    timeStampHeading=eventView.category()
    timeStampHeadingHint=eventView.eventDateString()
    timelineActionContent=timelineActions
    timeStampClass=timeStampClass
    nodeNumberClass=nodeClass
    >
        <@fdsTimeline.timelineEvent>
            <p class="govuk-body">
                ${eventView.description()}
            </p>
            <@eventComments comments=eventView.comments() canDeleteComments=eventView.addCommentUrl()?has_content/>
        </@fdsTimeline.timelineEvent>
    </@lmsTimeStamp.lmsTimeStamp>
</#macro>

<#macro otherScheduleEventEndOfPeriodRequirement eventView showProgress=false>
    <#if eventView.updateUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Edit" linkUrl=springUrl(eventView.updateUrl()) linkClass="govuk-link"/>
            <@fdsAction.link linkText="Remove" linkUrl=springUrl(eventView.deleteUrl()) linkClass="govuk-link"/>
        </#assign>
    <#elseif eventView.addCommentUrl()?has_content>
        <#assign timelineActions>
            <@fdsAction.link linkText="Add comment" linkUrl=springUrl(eventView.addCommentUrl()) linkClass="govuk-link"/>
        </#assign>
    <#else>
        <#assign timelineActions></#assign>
    </#if>

    <#assign timeStampClass = "">
    <#if showProgress>
        <#assign timeStampClass = "fds-timeline__time-stamp--complete">
    </#if>

    <@lmsTimeStamp.lmsTimeStamp
    timeStampHeading=eventView.category()
    timelineActionContent=timelineActions
    timeStampClass=timeStampClass
    nodeNumberClass="fds-timeline__node-number--no-dot"
    >
        <@fdsTimeline.timelineEvent>
            <p class="govuk-body">
                ${eventView.description()}
            </p>
            <@eventComments comments=eventView.comments() canDeleteComments=eventView.addCommentUrl()?has_content/>
        </@fdsTimeline.timelineEvent>
    </@lmsTimeStamp.lmsTimeStamp>
</#macro>

<#macro eventComments comments canDeleteComments>
    <#if comments?has_content>
        <@fdsDetails.summaryDetails summaryTitle="Comments">
            <#list comments as comment>
                <@fdsCard.card>
                    <@fdsDataItems.dataItem>
                        <@fdsDataItems.dataValues key="Author" value=comment.author()/>
                        <@fdsDataItems.dataValues key="Posted" value=comment.datetime()/>
                    </@fdsDataItems.dataItem>
                    <@fdsDataItems.dataItem>
                        <@fdsDataItems.dataValues key="Comment" value=comment.comment()/>
                    </@fdsDataItems.dataItem>
                    <#if canDeleteComments>
                        <@fdsAction.link linkText="Remove" linkUrl=springUrl(comment.removeCommentUrl())/>
                    </#if>
                </@fdsCard.card>
            </#list>
        </@fdsDetails.summaryDetails>
    </#if>
</#macro>