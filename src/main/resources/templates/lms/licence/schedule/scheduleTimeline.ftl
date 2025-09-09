<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true>

    <@fdsSummaryList.summaryListCard headingText="Schedule details" summaryListId="summary-card-list">
        <@fdsSummaryList.summaryListRow keyText="Start date" actionUrl="#" screenReaderActionText="">
            ${timelineSummaryCardView.licenceStartDate()}
        </@fdsSummaryList.summaryListRow>
    </@fdsSummaryList.summaryListCard>

    <@fdsActionDropdown.actionDropdown dropdownButtonText="Add an event">
        <@fdsActionDropdown.actionDropdownItem actionText="Add a term"/>
        <@fdsActionDropdown.actionDropdownItem actionText="Add a phase"/>
        <@fdsActionDropdown.actionDropdownItem actionText="Add a schedule event"/>
        <@fdsActionDropdown.actionDropdownItem actionText="Add a rate"/>
        <@fdsActionDropdown.actionDropdownItem actionText="Add a work programme element"/>
    </@fdsActionDropdown.actionDropdown>

</@defaultPage>