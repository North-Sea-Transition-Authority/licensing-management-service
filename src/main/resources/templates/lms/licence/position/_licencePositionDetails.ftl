<#include '../../layout/layout.ftl'>
<#import '_positionChanges.ftl' as positionChanges>

<#macro details licencePositionChanges licencePositionState actions={}  canEdit=false>
  <#if canEdit>
    <@fdsAction.buttonGroup>
      <@fdsActionDropdown.actionDropdown dropdownButtonText="Add change">
        <#if !licencePositionChanges["licence-administrator"]??>
          <@fdsActionDropdown.actionDropdownItem
            actionText="Administrator change"
            linkAction=true
            linkActionUrl=springUrl(actions.addAdministratorChangeUrl())
          />
        </#if>
      </@fdsActionDropdown.actionDropdown>
    </@fdsAction.buttonGroup>
  </#if>
    <#assign adminName>
      <#if licencePositionState.administratorStateView().organisationName()?has_content>
        ${licencePositionState.administratorStateView().organisationName()}
      <#else>
          None
      </#if>
    </#assign>
    <@fdsDataItems.dataItem>
        <@fdsDataItems.dataValues key="Licence administrator" value=adminName/>
    </@fdsDataItems.dataItem>
  <#if licencePositionChanges["licence-administrator"]??>
    <@positionChanges.administratorChange change=licencePositionChanges["licence-administrator"]/>
  </#if>
</#macro>