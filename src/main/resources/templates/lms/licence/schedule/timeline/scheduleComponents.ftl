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

        <#assign licenseeList>
          <ul class="govuk-list">
            <#list timelineSummaryCardView.licenseeNames() as licenseeName>
              <li>${licenseeName}</li>
            </#list>
          </ul>
        </#assign>

        <@fdsSummaryList.summaryListRowNoAction keyText="Licensees">
            ${licenseeList}
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryListCard>
</#macro>

<#macro timelineSummaryCardReadOnly timelineSummaryCardView>
    <@fdsSummaryList.summaryListCard headingText="Schedule details" summaryListId="summary-card-list">
        <@fdsSummaryList.summaryListRowNoAction keyText="Start date">
            ${timelineSummaryCardView.licenceStartDate()}
        </@fdsSummaryList.summaryListRowNoAction>

        <@fdsSummaryList.summaryListRowNoAction keyText="Expiry date">
            ${timelineSummaryCardView.licenceExpiryDate()!""}
        </@fdsSummaryList.summaryListRowNoAction>

        <#if timelineSummaryCardView.licenceEndedDate()?has_content>
            <@fdsSummaryList.summaryListRowNoAction keyText="Licence ended on">
                ${timelineSummaryCardView.licenceEndedDate()}
            </@fdsSummaryList.summaryListRowNoAction>
        <#else>
            <@fdsSummaryList.summaryListRowNoAction keyText="Projected licence end date">
                ${timelineSummaryCardView.finalTermEndDate()!""}
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
                <#if scheduleEventViews?has_content && scheduleEventViews?last.eventsAfterTerm()?has_content>
                    <@areaEventsAfterTerms
                    sectionHeading="After the ${scheduleEventViews?last.termType().displayName}"
                    summaryText="Area events dated after ${scheduleEventViews?last.endDateString()}, the end of the licence's terms"
                    areaEvents=scheduleEventViews?last.eventsAfterTerm()
                    />
                </#if>
            </@fdsAccordion.accordion>
        </@fdsSearch.searchPageContent>
    </@fdsSearch.searchPage>
</#macro>

<#macro areaEventsAfterTerms sectionHeading summaryText areaEvents>
    <@fdsAccordion.accordionSection sectionHeading=sectionHeading summaryText=summaryText>
        <@fdsTimeline.timeline>
            <@fdsTimeline.timelineSection>
                <#list areaEvents as eventView>
                    <@scheduleEvents.areaEvent eventView=eventView last=eventView?is_last/>
                </#list>
            </@fdsTimeline.timelineSection>
        </@fdsTimeline.timeline>
    </@fdsAccordion.accordionSection>
</#macro>

<#macro scheduleCommentsSection comments>
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
                    <#if comment.removeCommentUrl()?has_content>
                        <@fdsAction.link linkText="Remove" linkUrl=springUrl(comment.removeCommentUrl())/>
                    </#if>
                </@fdsCard.card>
            </#list>
        </@fdsDetails.summaryDetails>
    </#if>
</#macro>
