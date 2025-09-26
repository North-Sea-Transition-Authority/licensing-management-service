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
    </@fdsSummaryList.summaryListCard>

    <@fdsActionDropdown.actionDropdown dropdownButtonText="Add an event">
        <#list actions as actionView>
            <@fdsActionDropdown.actionDropdownItem actionText=actionView.action().displayText linkActionUrl=springUrl(actionView.url()) linkAction=true/>
        </#list>
    </@fdsActionDropdown.actionDropdown>

</@defaultPage>