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
    hasSearchBeenInvoked=searchItems?has_content
    clearFilterUrl=clearFilterUrl
    >
        <@dataItemFilter.referenceFilter
        form=form
        />
    </@search.standardSearch>
</@defaultPage>