<#include '../../layout/layout.ftl'>
<#import '../../search/search.ftl' as search>
<#import '../../macros/dataItems/filters.ftl' as dataItemFilter>

<@defaultPage
htmlTitle="Event tracker"
pageHeading="Event tracker"
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
>
    <div class="lms-sticky-filters">
        <@fdsSearch.searchPage>
            <@fdsSearch.searchFilter>
                <@fdsSearch.searchFilterList
                clearFilterText="Clear filters"
                clearFilterUrl="#">
                    <@dataItemFilter.licenceTypeFilter form=form licenceTypes=licenceTypes/>
                    <#if isRegulatorUser>
                        <@dataItemFilter.licenseeGroupFilter
                        form=form
                        licenseeGroupOrgUnitUrl=licenseeGroupOrgUnitUrl
                        preSelectedLicenseeGroup=preSelectedLicenseeGroupOrgUnit
                        />
                    </#if>
                    <@dataItemFilter.licenseeOrgUnitFilter
                    form=form
                    licenseeOrgUnitUrl=licenseeOrgUnitUrl
                    preSelectedLicenseeOrgUnit=preSelectedLicenseeOrgUnit
                    />
                    <@fdsSearch.searchFilterItem itemName="Event from" expanded=false>
                        <@fdsDatePicker.datePicker path="form.fromDate" labelText="Event from" hintText="For example, 31/08/2025" labelClass="govuk-visually-hidden"/>
                    </@fdsSearch.searchFilterItem>
                    <@fdsSearch.searchFilterItem itemName="Event to" expanded=false>
                        <@fdsDatePicker.datePicker path="form.toDate" labelText="Event to" hintText="For example, 31/08/2025" labelClass="govuk-visually-hidden"/>
                    </@fdsSearch.searchFilterItem>
                    <@fdsSearch.searchFilterItem itemName="Request type" expanded=true>
                        <@fdsSearch.searchCheckboxes path="form.requestTypes" checkboxes=requestTypes/>
                    </@fdsSearch.searchFilterItem>
                    <@fdsSearch.searchFilterItem itemName="Application status" expanded=true>
                        <@fdsSearch.searchCheckboxes path="form.eventStatuses" checkboxes=eventStatuses/>
                    </@fdsSearch.searchFilterItem>
                </@fdsSearch.searchFilterList>
            </@fdsSearch.searchFilter>

            <@fdsSearch.searchPageContent>
                <@fdsTable.sortableTable tableContents=eventTrackerTableJson tableId="event-tracker-table" tableCaption="Event tracker"/>
            </@fdsSearch.searchPageContent>
        </@fdsSearch.searchPage>
    </div>
</@defaultPage>