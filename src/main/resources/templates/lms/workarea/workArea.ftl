<#include '../layout/layout.ftl'>
<#import '../search/search.ftl' as search>
<#import '../macros/dataItems/filters.ftl' as dataItemFilter>

<#assign pageTitle = "Work area" />

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
>
  <#if canStartApplication>
      <@fdsAction.link linkText="Create submission" linkUrl=springUrl(startApplicationUrl) linkClass="govuk-button"/>
  </#if>
  <@search.standardSearch
  clearFilterUrl=clearFilterUrl
  searchResults=workAreaItems
  hasSearchBeenInvoked=workAreaItems?has_content
  >
    <@dataItemFilter.referenceFilter
    form=form
    />
  </@search.standardSearch>
</@defaultPage>
