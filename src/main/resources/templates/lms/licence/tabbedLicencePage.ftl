<#include '../layout/layout.ftl'>
<#import '../component/actions/actionItems.ftl' as actionItems>
<#import 'schedule/timeline/scheduleComponents.ftl' as scheduleTimeline>
<#import '../component/inline/inlineInputAction.ftl' as inlineInputAction>

<#-- @ftlvariable name="heading" type="String" -->
<#-- @ftlvariable name="caption" type="String" -->
<#-- @ftlvariable name="topLevelLicenceActions" type="java.util.Collection<uk.co.nstauthority.licensingmanagementservice.components.actions.ActionItemView>" -->
<#-- @ftlvariable name="tabs" type="java.util.Collection<uk.co.nstauthority.licensingmanagementservice.fds.tab.FdsBackendTab>" -->
<#-- @ftlvariable name="currentTab" type="uk.co.nstauthority.licensingmanagementservice.fds.tab.FdsBackendTab" -->
<#-- @ftlvariable name="currentTabLicenceActions" type="java.util.Collection<uk.co.nstauthority.licensingmanagementservice.fds.tab.FdsBackendTab>" -->

<#macro page heading caption topLevelLicenceActions tabs currentTab currentTabLicenceActions>
  <@defaultPage
    htmlTitle=heading
    pageHeading=heading
    caption=caption
    pageSize=PageSize.FULL_COLUMN
  >
    <@actionItems.actionItems actionItems=topLevelLicenceActions screenReaderText="Actions for ${heading}"/>

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