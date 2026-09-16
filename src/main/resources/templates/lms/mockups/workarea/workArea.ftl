<#include '../../layout/layout.ftl'>
<#import '../../search/search.ftl' as search>
<#import '../../macros/dataItems/filters.ftl' as dataItemFilter>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
>
  <@fdsInsetText.insetText>
    Mockup navigation (not part of the design):
    <#list mockupPersonas as persona>
      <#if persona.key == currentPersona>
        <strong>${persona.label}</strong>
      <#else>
        <a class="govuk-link" href="${springUrl(persona.url)}">${persona.label}</a>
      </#if>
      <#if persona?has_next> | </#if>
    </#list>
  </@fdsInsetText.insetText>

  <@fdsAction.link linkText="Start application" linkUrl=springUrl(currentTab.url()) linkClass="govuk-button"/>

  <@fdsBackendTabs.tabs tabsHeading="Work area tabs">
    <@fdsBackendTabs.tabList>
      <#list tabs as tab>
        <@fdsBackendTabs.tab
          tabAnchor=tab.anchor()
          tabValue=tab.anchor()
          currentTab=currentTab.anchor()
          tabLabel=tab.label()
          tabUrl=tab.url()
        />
      </#list>
    </@fdsBackendTabs.tabList>
    <@fdsBackendTabs.tabContent
      tabAnchor=currentTab.anchor()
      tabValue=currentTab.anchor()
      currentTab=currentTab.anchor()
    >
      <@search.standardSearch
        clearFilterUrl=clearFilterUrl
        searchResults=workAreaItems
        hasSearchBeenInvoked=workAreaItems?has_content
        isFilterPrimaryButton=false
      >
        <@dataItemFilter.referenceFilter form=form/>
        <@dataItemFilter.licenceTypeFilter form=form licenceTypes=licenceTypes/>
        <@dataItemFilter.applicationReferenceFilter form=form/>
        <@dataItemFilter.applicationTypeFilter form=form applicationTypes=applicationTypes/>
        <@dataItemFilter.applicationStatusFilter form=form applicationStatuses=applicationStatuses/>
        <@dataItemFilter.licenseeOrgUnitFilter
          form=form
          licenseeOrgUnitUrl=licenseeOrgUnitUrl
          preSelectedLicenseeOrgUnit=preSelectedLicenseeOrgUnit
        />
      </@search.standardSearch>
    </@fdsBackendTabs.tabContent>
  </@fdsBackendTabs.tabs>
</@defaultPage>
