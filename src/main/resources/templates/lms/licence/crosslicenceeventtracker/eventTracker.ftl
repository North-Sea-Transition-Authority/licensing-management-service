<#include '../../layout/layout.ftl'>
<#import '../../search/search.ftl' as search>
<#import '../../macros/dataItems/filters.ftl' as dataItemFilter>

<@defaultPage
htmlTitle="Event tracker"
pageHeading="Event tracker"
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
>

    <@fdsSearch.searchPage>
        <@fdsSearch.searchFilter>
            <@fdsSearch.searchFilterList
            clearFilterText="Clear filters"
            clearFilterUrl="#">
                <@dataItemFilter.licenceTypeFilter form=form licenceTypes=licenceTypes/>
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
            <table class="govuk-table">
                <thead class="govuk-table__head">
                <tr class="govuk-table__row">
                    <th scope="col" class="govuk-table__header">Licence</th>
                    <th scope="col" class="govuk-table__header">Term / phase transition</th>
                    <th scope="col" class="govuk-table__header">Work programme activity</th>
                    <th scope="col" class="govuk-table__header">Event end / due date</th>
                    <th scope="col" class="govuk-table__header">Application status</th>
                    <th scope="col" class="govuk-table__header">Licensee(s)</th>
                    <th scope="col" class="govuk-table__header">Quad/block</th>
                    <th scope="col" class="govuk-table__header">Steward</th>
                </tr>
                </thead>
            </table>
        </@fdsSearch.searchPageContent>
    </@fdsSearch.searchPage>
</@defaultPage>