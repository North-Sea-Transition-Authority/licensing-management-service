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
        <@fdsSummaryList.summaryListRow keyText="Expiry date" actionUrl=springUrl(updateExpiryDateUrl) screenReaderActionText="">
            ${timelineSummaryCardView.licenceExpiryDate()!""}
        </@fdsSummaryList.summaryListRow>
        <@fdsSummaryList.summaryListRowNoAction keyText="Status">
            ${timelineSummaryCardView.status()!""}
        </@fdsSummaryList.summaryListRowNoAction>
        <#if timelineSummaryCardView.showRoundIssuedOn()>
            <@fdsSummaryList.summaryListRowNoAction keyText="Round number">
                ${timelineSummaryCardView.roundIssuedOn()!""}
            </@fdsSummaryList.summaryListRowNoAction>
        </#if>
    </@fdsSummaryList.summaryListCard>

    <@fdsActionDropdown.actionDropdown dropdownButtonText="Add an event">
        <#list actions as actionView>
            <@fdsActionDropdown.actionDropdownItem actionText=actionView.action().displayText linkActionUrl=springUrl(actionView.url()) linkAction=true/>
        </#list>
    </@fdsActionDropdown.actionDropdown>

    <br><br>

    <@fdsSearch.searchPage>
        <@fdsSearch.searchFilter>
            <@fdsSearch.searchFilterList
            clearFilterText="Clear filters"
            clearFilterUrl=springUrl(clearFilterUrl)>
                <@fdsSearch.searchFilterItem itemName="Show" expanded=true>
                    <@fdsSearch.searchCheckboxes path="form.eventTypes" checkboxes=timelineFilterOptions/>
                </@fdsSearch.searchFilterItem>
            </@fdsSearch.searchFilterList>
        </@fdsSearch.searchFilter>

        <@fdsSearch.searchPageContent>
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
        </@fdsSearch.searchPageContent>
    </@fdsSearch.searchPage>

    <@fdsAction.link linkText="Review and apply" linkUrl=springUrl(reviewAndApplyUrl) linkClass="govuk-button"/>
</@defaultPage>