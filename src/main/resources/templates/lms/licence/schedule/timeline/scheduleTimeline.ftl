<#include '../../../layout/layout.ftl'>
<#import 'scheduleComponents.ftl'as scheduleComponents>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true>

    <@scheduleComponents.timelineSummaryCard
        timelineSummaryCardView=timelineSummaryCardView
        updateLicenceStartDateUrl=updateLicenceStartDateUrl
        updateExpiryDateUrl=updateExpiryDateUrl
    />

    <@fdsActionDropdown.actionDropdown dropdownButtonText="Add an event">
        <#list actions as actionView>
            <@fdsActionDropdown.actionDropdownItem actionText=actionView.action().displayText linkActionUrl=springUrl(actionView.url()) linkAction=true/>
        </#list>
    </@fdsActionDropdown.actionDropdown>

    <br><br>

    <@scheduleComponents.timelineWithFilters
        scheduleEventViews=scheduleEventViews
        timelineFilterOptions=timelineFilterOptions
        clearFilterUrl=clearFilterUrl
        invalidScheduleEvents=invalidScheduleEvents
    />

    <@fdsAction.buttonGroup>
        <@fdsAction.link linkText="Review and apply" linkUrl=springUrl(reviewAndApplyUrl) linkClass="govuk-button"/>
        <@fdsAction.link linkText="Delete" linkUrl=springUrl(deleteScheduleUrl) linkClass="govuk-button govuk-button--warning"/>
    </@fdsAction.buttonGroup>
</@defaultPage>