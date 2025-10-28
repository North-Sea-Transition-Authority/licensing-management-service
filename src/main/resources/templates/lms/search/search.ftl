<#include '../layout/layout.ftl'>
<#import '../search/searchDataItem.ftl' as searchDataItem>

<#macro standardSearch clearFilterUrl="" isFilterPrimaryButton=true searchResults=[] hasSearchBeenInvoked=false selectableResults=false>
    <#if isFilterPrimaryButton>
        <#assign filterButtonClass="govuk-button"/>
    <#else>
        <#assign filterButtonClass="govuk-button govuk-button--secondary"/>
    </#if>

    <@fdsSearch.searchPage>
        <#if clearFilterUrl?has_content>
            <@fdsSearch.searchFilter>
                <@fdsSearch.searchFilterList
                  filterButtonClass=filterButtonClass
                  clearFilterText="Clear filters"
                  clearFilterUrl=springUrl(clearFilterUrl)>
                    <#nested/>
                </@fdsSearch.searchFilterList>
            </@fdsSearch.searchFilter>
        </#if>

        <@fdsSearch.searchPageContent>
            <#if searchResults?has_content>
                <@fdsResultList.resultList>
                    <#list searchResults as searchResult>
                      <@searchDataItem.resultListItem dataView=searchResult/>
                    </#list>
                </@fdsResultList.resultList>
            <#elseif !hasSearchBeenInvoked>
                <@fdsInsetText.insetText>
                    To search, add some filters and click the 'Filter results' button.
                </@fdsInsetText.insetText>
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
</#macro>
