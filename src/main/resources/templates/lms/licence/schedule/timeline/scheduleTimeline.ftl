<#include '../../../layout/layout.ftl'>
<#import 'scheduleEvents.ftl'as scheduleEvents>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true>

    <@fdsSummaryList.summaryListCard headingText="Schedule details" summaryListId="summary-card-list">
        <@fdsSummaryList.summaryListRow keyText="Start date" actionUrl=springUrl(updateLicenceStartDateUrl) screenReaderActionText="">
            ${timelineSummaryCardView.licenceStartDate()}
        </@fdsSummaryList.summaryListRow>
        <@fdsSummaryList.summaryListRowNoAction keyText="Status">
            ${timelineSummaryCardView.status()!""}
        </@fdsSummaryList.summaryListRowNoAction>
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
                        <@scheduleEvents.term
                            termView=termView
                        />
                    </@fdsTimeline.timelineSection>
                </@fdsTimeline.timeline>
            </@fdsAccordion.accordionSection>
        </#list>
    </@fdsAccordion.accordion>
</@defaultPage>