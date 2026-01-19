<#include '../../layout/layout.ftl'>

<#macro caseProcessingTabsWithContent controllerUrl tabs selectedTab>
  <@fdsBackendTabs.tabs tabsHeading="case processing tabs">
    <@fdsBackendTabs.tabList>
      <#list tabs as tab>
        <@fdsBackendTabs.tab
          tabLabel=tab.label()
          tabUrl="${controllerUrl}?tab=${tab.anchor()}"
          tabAnchor=tab.anchor()
          tabValue=tab.value()
          currentTab=selectedTab.value/>
      </#list>
    </@fdsBackendTabs.tabList>
    <#list tabs as tab>
      <#if tab.value() == selectedTab.value>
        <@fdsBackendTabs.tabContent
          tabAnchor=tab.anchor()
          currentTab=selectedTab.value
          tabValue=tab.value()>
          <#nested>
        </@fdsBackendTabs.tabContent>
      </#if>
    </#list>
  </@fdsBackendTabs.tabs>
</#macro>
