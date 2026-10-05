<#include '../../../layout/layout.ftl'>
<#import '../../../search/search.ftl' as search>

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
  />
</@defaultPage>