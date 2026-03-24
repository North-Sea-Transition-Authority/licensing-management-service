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
                <@fdsSearch.searchFilterItem itemName="Event from" expanded=false>
                    <@fdsDatePicker.datePicker path="form.fromDate" labelText="Event from" hintText="For example, 31/08/2025" labelClass="govuk-visually-hidden"/>
                </@fdsSearch.searchFilterItem>
                <@fdsSearch.searchFilterItem itemName="Event to" expanded=false>
                    <@fdsDatePicker.datePicker path="form.toDate" labelText="Event to" hintText="For example, 31/08/2025" labelClass="govuk-visually-hidden"/>
                </@fdsSearch.searchFilterItem>
                <@fdsSearch.searchFilterItem itemName="Request type" expanded=false>
                    <@fdsSearch.searchCheckboxes path="form.requestTypes" checkboxes=requestTypes/>
                </@fdsSearch.searchFilterItem>
                <@fdsSearch.searchFilterItem itemName="Event status" expanded=false>
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
                        <th scope="col" class="govuk-table__header">Status</th>
                        <th scope="col" class="govuk-table__header">Licensee(s)</th>
                        <th scope="col" class="govuk-table__header">Round</th>
                        <th scope="col" class="govuk-table__header">Steward</th>
                    </tr>
                </thead>
                <tbody class="govuk-table__body">
                    <tr class="govuk-table__row">
                        <th scope="row" class="govuk-table__header">
                            <@fdsAction.link linkText="P400 (14/2)" linkUrl="#"/>
                            <@fdsTag.tag tagClass="govuk-tag--purple govuk-!-margin-top-1">Continuation</@fdsTag.tag>
                        </th>
                        <td class="govuk-table__cell">Phase A to Phase C</td>
                        <td class="govuk-table__cell"></td>
                        <td class="govuk-table__cell">1 February 2026</td>
                        <td class="govuk-table__cell">Decision issued</td>
                        <td class="govuk-table__cell">BP EXPLORATION</td>
                        <td class="govuk-table__cell">31</td>
                        <td class="govuk-table__cell">Andy Admin</td>
                    </tr>
                    <tr class="govuk-table__row">
                        <th scope="row" class="govuk-table__header">
                            <@fdsAction.link linkText="P100 (12/2)" linkUrl="#"/>
                            <@fdsTag.tag tagClass="govuk-tag--turquoise govuk-!-margin-top-1">Relinquishment</@fdsTag.tag>
                        </th>
                        <td class="govuk-table__cell">Initial term to Second term</td>
                        <td class="govuk-table__cell"></td>
                        <td class="govuk-table__cell">2 February 2026</td>
                        <td class="govuk-table__cell"></td>
                        <td class="govuk-table__cell">APACHE NORTH SEA LIMITED, BP EXPLORATION</td>
                        <td class="govuk-table__cell">31</td>
                        <td class="govuk-table__cell"></td>
                    </tr>
                    <tr class="govuk-table__row">
                        <th scope="row" class="govuk-table__header">
                            <@fdsAction.link linkText="CS002 (10/2)" linkUrl="#"/>
                            <@fdsTag.tag tagClass="govuk-tag--blue govuk-!-margin-top-1">Extension</@fdsTag.tag>
                        </th>
                        <td class="govuk-table__cell">Appraisal Term to Operational Term</td>
                        <td class="govuk-table__cell">Early risk assessment</td>
                        <td class="govuk-table__cell">25 February 2026</td>
                        <td class="govuk-table__cell">Decision issued</td>
                        <td class="govuk-table__cell">BP EXPLORATION</td>
                        <td class="govuk-table__cell"></td>
                        <td class="govuk-table__cell">Andy Admin</td>
                    </tr>
                    <tr class="govuk-table__row">
                        <th scope="row" class="govuk-table__header">
                            <@fdsAction.link linkText="P600 (16/1)" linkUrl="#"/>
                            <@fdsTag.tag tagClass="govuk-tag--blue govuk-!-margin-top-1">Extension</@fdsTag.tag>
                        </th>
                        <td class="govuk-table__cell">Second Term to Third Term</td>
                        <td class="govuk-table__cell"></td>
                        <td class="govuk-table__cell">1 March 2026</td>
                        <td class="govuk-table__cell">Request submitted</td>
                        <td class="govuk-table__cell">SHELL CLAIR UK LIMITED, BP EXPLORATION</td>
                        <td class="govuk-table__cell">30</td>
                        <td class="govuk-table__cell">Andy Admin</td>
                    </tr>
                    <tr class="govuk-table__row">
                        <th scope="row" class="govuk-table__header">
                            <@fdsAction.link linkText="P700 (17/1)" linkUrl="#"/>
                            <@fdsTag.tag tagClass="govuk-tag--blue govuk-!-margin-top-1">Extension</@fdsTag.tag>
                        </th>
                        <td class="govuk-table__cell">Phase C to Second Term</td>
                        <td class="govuk-table__cell"></td>
                        <td class="govuk-table__cell">10 March 2026</td>
                        <td class="govuk-table__cell">Request submitted</td>
                        <td class="govuk-table__cell">BP EXPLORATION</td>
                        <td class="govuk-table__cell">29</td>
                        <td class="govuk-table__cell">Andy Admin</td>
                    </tr>
                    <tr class="govuk-table__row">
                        <th scope="row" class="govuk-table__header">
                            <@fdsAction.link linkText="PEDL300 (NS3)" linkUrl="#"/>
                            <@fdsTag.tag tagClass="govuk-tag--purple govuk-!-margin-top-1">Continuation</@fdsTag.tag>
                        </th>
                        <td class="govuk-table__cell">Second Term to Third Term</td>
                        <td class="govuk-table__cell"></td>
                        <td class="govuk-table__cell">1 April 2026</td>
                        <td class="govuk-table__cell">Request submitted</td>
                        <td class="govuk-table__cell">BP EXPLORATION, SHELL CLAIR UK LIMITED</td>
                        <td class="govuk-table__cell">30</td>
                        <td class="govuk-table__cell">Andy Admin</td>
                    </tr>
                    <tr class="govuk-table__row">
                        <th scope="row" class="govuk-table__header">
                            <@fdsAction.link linkText="PEDL450 (NS4)" linkUrl="#"/>
                            <@fdsTag.tag tagClass="govuk-tag--blue govuk-!-margin-top-1">Extension</@fdsTag.tag>
                        </th>
                        <td class="govuk-table__cell">Initial Term to Second Term</td>
                        <td class="govuk-table__cell">New shoot 2D seismic data</td>
                        <td class="govuk-table__cell">5 April 2026</td>
                        <td class="govuk-table__cell">Request submitted</td>
                        <td class="govuk-table__cell">BP EXPLORATION</td>
                        <td class="govuk-table__cell">29</td>
                        <td class="govuk-table__cell">Andy Admin</td>
                    </tr>
                    <tr class="govuk-table__row">
                        <th scope="row" class="govuk-table__header">
                            <@fdsAction.link linkText="PEDL200 (NS2)" linkUrl="#"/>
                            <@fdsTag.tag tagClass="govuk-tag--blue govuk-!-margin-top-1">Extension</@fdsTag.tag>
                        </th>
                        <td class="govuk-table__cell">Second Term to Third Term</td>
                        <td class="govuk-table__cell"></td>
                        <td class="govuk-table__cell">20 April 2026</td>
                        <td class="govuk-table__cell">Request submitted</td>
                        <td class="govuk-table__cell">BP EXPLORATION, SHELL CLAIR UK LIMITED</td>
                        <td class="govuk-table__cell">30</td>
                        <td class="govuk-table__cell">Andy Admin</td>
                    </tr>
                    <tr class="govuk-table__row">
                        <th scope="row" class="govuk-table__header"><@fdsAction.link linkText="P1 (83/5)" linkUrl="#"/></th>
                        <td class="govuk-table__cell">Phase C to Second Term</td>
                        <td class="govuk-table__cell"></td>
                        <td class="govuk-table__cell">1 July 2026</td>
                        <td class="govuk-table__cell">Request pending</td>
                        <td class="govuk-table__cell">BP EXPLORATION</td>
                        <td class="govuk-table__cell">30</td>
                        <td class="govuk-table__cell"></td>
                    </tr>
                    <tr class="govuk-table__row">
                        <th scope="row" class="govuk-table__header"><@fdsAction.link linkText="PEDL100 (NS1)" linkUrl="#"/></th>
                        <td class="govuk-table__cell">Second Term to Third Term</td>
                        <td class="govuk-table__cell"></td>
                        <td class="govuk-table__cell">1 July 2026</td>
                        <td class="govuk-table__cell">Request pending</td>
                        <td class="govuk-table__cell">BP EXPLORATION, COASTAL OIL AND GAS LIMITED</td>
                        <td class="govuk-table__cell">28</td>
                        <td class="govuk-table__cell"></td>
                    </tr>
                </tbody>
            </table>
        </@fdsSearch.searchPageContent>
    </@fdsSearch.searchPage>
</@defaultPage>