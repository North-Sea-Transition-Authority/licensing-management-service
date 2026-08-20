<#include '../layout/layout.ftl'>
<#import '../component/actions/actionItems.ftl' as actionItems>

<#-- @ftlvariable name="licenceOverviewView" type="uk.co.nstauthority.licensingmanagementservice.licence.overview.LicenceOverviewView" -->
<#-- @ftlvariable name="topLevelLicenceActions" type="java.util.Collection<uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView>" -->
<#-- @ftlvariable name="tabs" type="java.util.Collection<uk.co.nstauthority.licensingmanagementservice.fds.tab.FdsBackendTab>" -->
<#-- @ftlvariable name="currentTab" type="uk.co.nstauthority.licensingmanagementservice.fds.tab.FdsBackendTab" -->
<#-- @ftlvariable name="currentTabLicenceActions" type="java.util.Collection<uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView>" -->

<#macro page licenceOverviewView topLevelLicenceActions tabs currentTab currentTabLicenceActions>
  <@defaultPage
    htmlTitle=licenceOverviewView.licenceReference()
    pageHeading=licenceOverviewView.licenceReference()
    caption=licenceOverviewView.caption()
    pageSize=PageSize.FULL_COLUMN
  >
    <#if licenceOverviewView.csRegisterUrl()?has_content>
      <p class="govuk-body">
        <@fdsAction.link linkText="View in public register" linkUrl=licenceOverviewView.csRegisterUrl() openInNewTab=true/>
      </p>
    </#if>

    <@actionItems.actionItems actionItems=topLevelLicenceActions screenReaderText="Actions for ${licenceOverviewView.licenceReference()}"/>

    <@fdsBackendTabs.tabs tabsHeading="Licence tabs">
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
        <@actionItems.actionItems actionItems=currentTabLicenceActions screenReaderText="${currentTab.label()} actions"/>
        <#nested/>
      </@fdsBackendTabs.tabContent>
    </@fdsBackendTabs.tabs>
  </@defaultPage>
</#macro>
