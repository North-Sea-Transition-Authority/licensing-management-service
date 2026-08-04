<#include '../../layout/layout.ftl'>
<#import '../../search/search.ftl' as search>
<#import '../../macros/dataItems/filters.ftl' as dataItemFilter>

<#assign pageTitle = "Licences" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
>
    <#if canCreateLicence>
      <@fdsAction.link linkText="Create licence" linkUrl=springUrl(createLicenceUrl) linkClass="govuk-button"/>
    </#if>
    <div class="lms-sticky-filters">
        <@search.standardSearch
        searchResults=searchItems
        hasSearchBeenInvoked=hasSearchBeenInvoked
        clearFilterUrl=clearFilterUrl
        >
            <@dataItemFilter.licenceTypeFilter
            form=form
            licenceTypes=licenceTypes
            />
            <#if isRegulatorUser>
                <@dataItemFilter.licenceStatusFilter
                form=form
                licenceStatuses=licenceStatuses
                />
            </#if>
            <@dataItemFilter.referenceFilter
            form=form
            />
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
        </@search.standardSearch>
    </div>
</@defaultPage>