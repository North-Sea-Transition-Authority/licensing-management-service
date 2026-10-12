<#include '../../../layout/layout.ftl'>
<#import '../../../search/search.ftl' as search>
<#import '../../../macros/dataItems/filters.ftl' as dataItemFilter>

<#assign pageTitle = "Corrections" />

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
>
  <@search.standardSearch
    searchResults=searchItems
    hasSearchBeenInvoked=true
    clearFilterUrl=clearFilterUrl
  >
    <@dataItemFilter.correctionLicenceFilter
      form=form
    />

    <@dataItemFilter.correctionStatusFilter
      form=form
      correctionStatuses=correctionStatuses
    />

    <@dataItemFilter.correctionAllocatedUserFilter
      form=form
      allocatedUsers=allocatedUsers
    />
  </@search.standardSearch>
</@defaultPage>