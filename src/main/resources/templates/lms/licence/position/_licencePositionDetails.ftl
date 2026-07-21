<#include '../../layout/layout.ftl'>
<#import '_positionChanges.ftl' as positionChanges>

<#macro details licencePositionChanges actions={} licencePositionState={} canEdit=false>
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
<#--     TODO LMS2-79: this check (along with default value above) will be removed when we add state to added licence positions-->
    <#if licencePositionState?has_content>
        <@fdsDataItems.dataItem>
            <@fdsDataItems.dataValues key="Licence administrator" value=licencePositionState.administratorStateView().organisationName()/>
        </@fdsDataItems.dataItem>
    </#if>
    <#if licencePositionChanges["licence-administrator"]??>
        <@positionChanges.administratorChange change=licencePositionChanges["licence-administrator"]/>
    </#if>
</#macro>