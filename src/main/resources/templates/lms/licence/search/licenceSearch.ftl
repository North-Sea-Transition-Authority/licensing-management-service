<#include '../../layout/layout.ftl'>
<#import '../../search/search.ftl' as search>
<#import '../../macros/dataItems/filters.ftl' as dataItemFilter>

<#assign pageTitle = "Licence search" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
>
    <@search.standardSearch
    searchResults=searchItems
    hasSearchBeenInvoked=hasSearchBeenInvoked
    clearFilterUrl=clearFilterUrl
    >
        <@dataItemFilter.licenceTypeFilter
        form=form
        licenceTypes=licenceTypes
        />
        <@dataItemFilter.referenceFilter
        form=form
        />
        <@dataItemFilter.licenseeOrgUnitFilter
        form=form
        licenseeOrgUnitUrl=licenseeOrgUnitUrl
        preSelectedLicenseeOrgUnit=preSelectedLicenseeOrgUnit
        />
    </@search.standardSearch>
</@defaultPage>