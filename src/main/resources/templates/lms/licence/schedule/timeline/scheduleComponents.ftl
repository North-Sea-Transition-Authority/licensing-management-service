<#include '../../../layout/layout.ftl'>
<#import 'scheduleEvents.ftl'as scheduleEvents>

<#macro timelineSummaryCard timelineSummaryCardView updateLicenceStartDateUrl="" updateExpiryDateUrl="">
    <@fdsSummaryList.summaryListCard headingText="Schedule details" summaryListId="summary-card-list">
        <#if updateLicenceStartDateUrl?has_content>
            <@fdsSummaryList.summaryListRow keyText="Start date" actionUrl=springUrl(updateLicenceStartDateUrl) screenReaderActionText="">
                ${timelineSummaryCardView.licenceStartDate()}
            </@fdsSummaryList.summaryListRow>
        <#else>
            <@fdsSummaryList.summaryListRowNoAction keyText="Start date">
                ${timelineSummaryCardView.licenceStartDate()}
            </@fdsSummaryList.summaryListRowNoAction>
        </#if>

        <#if updateExpiryDateUrl?has_content>
            <@fdsSummaryList.summaryListRow keyText="Expiry date" actionUrl=springUrl(updateExpiryDateUrl) screenReaderActionText="">
                ${timelineSummaryCardView.licenceExpiryDate()!""}
            </@fdsSummaryList.summaryListRow>
        <#else>
            <@fdsSummaryList.summaryListRowNoAction keyText="Expiry date">
                ${timelineSummaryCardView.licenceExpiryDate()!""}
            </@fdsSummaryList.summaryListRowNoAction>
        </#if>

        <#if timelineSummaryCardView.licenceEndedDate()?has_content>
            <@fdsSummaryList.summaryListRowNoAction keyText="Licence ended on">
                ${timelineSummaryCardView.licenceEndedDate()}
            </@fdsSummaryList.summaryListRowNoAction>
        <#else>
            <@fdsSummaryList.summaryListRowNoAction keyText="Projected licence end date">
                ${timelineSummaryCardView.finalTermEndDate()!""}
            </@fdsSummaryList.summaryListRowNoAction>
        </#if>

        <@fdsSummaryList.summaryListRowNoAction keyText="Status">
            ${timelineSummaryCardView.status()!""}
        </@fdsSummaryList.summaryListRowNoAction>

        <#if timelineSummaryCardView.showRoundIssuedOn()>
            <@fdsSummaryList.summaryListRowNoAction keyText="Round number">
                ${timelineSummaryCardView.roundIssuedOn()!""}
            </@fdsSummaryList.summaryListRowNoAction>
        </#if>
    </@fdsSummaryList.summaryListCard>
</#macro>

<#macro timelineWithFilters scheduleEventViews timelineFilterOptions clearFilterUrl invalidScheduleEvents=[]>
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
</#macro>