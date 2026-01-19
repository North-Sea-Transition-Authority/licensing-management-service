<#include '../layout/layout.ftl'>
<#import '../search/search.ftl' as search>
<#import '../search/searchDataItem.ftl' as searchDataItem>
<#import '_documentFilters.ftl' as dataItemFilter>
<#import '../macros/caseprocessingtabs/caseProccessingTabs.ftl' as caseProcessingTabs>
<#import '../macros/search/noMatchingResults.ftl' as searchMacros>

<#assign pageTitle = "Document library" />

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
>
  <@fdsSearch.searchPage>
    <#if clearFilterUrl?has_content>
      <@fdsSearch.searchFilter oneThirdWidth=true>
        <@fdsSearch.searchFilterList
          filterButtonClass="govuk-button govuk-button--secondary"
          clearFilterText="Clear filters"
          clearFilterUrl=springUrl(clearFilterUrl)>
            <@dataItemFilter.documentTitleFilter form=form/>
            <@dataItemFilter.licenceTypeFilter form=form licenceTypes=licenceTypes/>
        </@fdsSearch.searchFilterList>
      </@fdsSearch.searchFilter>
    </#if>

    <@fdsSearch.searchPageContent twoThirdsWidth=true>
      <@caseProcessingTabs.caseProcessingTabsWithContent
        tabs=tabs
        selectedTab=selectedTab
        controllerUrl=controllerUrl
      >
        <#list searchItemsByTab as searchTabItem>
          <#if searchTabItem.searchResultsForTab()?has_content && searchTabItem.searchResultsForTab().pageContent?has_content && selectedTab=searchTabItem.tabView().name()>
            <@fdsResultList.resultList resultCount=searchTabItem.searchResultsForTab().totalElements>
              <#list searchTabItem.searchResultsForTab().pageContent as searchResult>
                <@searchDataItem.resultListItem dataView=searchResult/>
              </#list>
            </@fdsResultList.resultList>
            <@fdsPagination.pagination pageView=searchTabItem.searchResultsForTab()/>
          <#elseif selectedTab=searchTabItem.tabView().name()>
            <@searchMacros.noMatchingResults/>
          </#if>
        </#list>
      </@caseProcessingTabs.caseProcessingTabsWithContent>
    </@fdsSearch.searchPageContent>
  </@fdsSearch.searchPage>
</@defaultPage>
