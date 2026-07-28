<#include '../../layout/layout.ftl'>
<#import '../../macros/dataItems/filters.ftl' as dataItemFilter>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
>
    <div class="lms-sticky-filters">
        <@fdsSearch.searchPage>
            <@fdsSearch.searchFilter>
                <@fdsSearch.searchFilterList clearFilterText="Clear filters" clearFilterUrl=springUrl(clearFilterUrl)>
                    <@dataItemFilter.referenceFilter form=form/>
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
                    <@dataItemFilter.contactEmailFilter form=form/>
                    <@dataItemFilter.noContactAssignedFilter form=form/>
                </@fdsSearch.searchFilterList>
            </@fdsSearch.searchFilter>

            <@fdsSearch.searchPageContent>
                <p class="govuk-body"><strong>${contactCount}</strong> result<#if contactCount != 1>s</#if></p>
                <#if contactCount gt 0>
                    <@fdsTable.sortableTable tableContents=contactsTableJson tableId="licence-contacts-table" tableCaption=pageTitle/>
                <#else>
                    <h3 class="govuk-heading-s">There are no matching results</h3>
                    <p class="govuk-body">Improve your results by:</p>
                    <ul class="govuk-list govuk-list--bullet">
                        <li>removing filters</li>
                        <li>double-checking your spelling</li>
                        <li>using fewer keywords</li>
                        <li>searching for something less specific</li>
                    </ul>
                </#if>
            </@fdsSearch.searchPageContent>
        </@fdsSearch.searchPage>
    </div>
</@defaultPage>
