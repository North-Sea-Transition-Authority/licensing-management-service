<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true>

    <@fdsSummaryList.summaryListCard headingText="Schedule details" summaryListId="summary-card-list">
        <@fdsSummaryList.summaryListRow keyText="Start date" actionUrl=springUrl(updateLicenceStartDateUrl) screenReaderActionText="">
            ${timelineSummaryCardView.licenceStartDate()}
        </@fdsSummaryList.summaryListRow>
        <@fdsSummaryList.summaryListRowNoAction keyText="Round number">
            ${timelineSummaryCardView.roundIssuedOn()!""}
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryListCard>

    <@fdsActionDropdown.actionDropdown dropdownButtonText="Add an event">
        <#list actions as actionView>
            <@fdsActionDropdown.actionDropdownItem actionText=actionView.action().displayText linkActionUrl=springUrl(actionView.url()) linkAction=true/>
        </#list>
    </@fdsActionDropdown.actionDropdown>

    <@fdsAccordion.accordion accordionId="schedule-accordion">
        <#list scheduleEventViews as termView>
            <@fdsAccordion.accordionSection sectionHeading=termView.termType().displayName summaryText=termView.dateDurationString()>
                <@fdsTimeline.timeline>
                    <@fdsTimeline.timelineSection>
                        <#assign timelineActions>
                            <@fdsAction.link linkText="Edit" linkUrl=springUrl(termView.updateUrl()) linkClass="govuk-link"/>
                            <@fdsAction.link linkText="Remove" linkUrl=springUrl(termView.deleteUrl()) linkClass="govuk-link"/>
                        </#assign>
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
                    </@fdsTimeline.timelineSection>
                </@fdsTimeline.timeline>
            </@fdsAccordion.accordionSection>
        </#list>
    </@fdsAccordion.accordion>
</@defaultPage>