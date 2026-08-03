<#include '../../layout/layout.ftl'>
<#import '_positionChanges.ftl' as positionChanges>

<#macro details licencePositionChanges licencePositionState actions={} canEdit=false>
    <#if canEdit>
        <@fdsAction.buttonGroup>
            <#if actions.addChangeUrl()??>
                <@fdsAction.link linkText="Add change" linkUrl=springUrl(actions.addChangeUrl()) linkClass="govuk-button"/>
            </#if>
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
    <#assign correctUrl = "">
    <#if canEdit && actions.addChangeUrl()??>
      <#assign correctUrl = actions.addChangeUrl()>
    </#if>
    <@positionChanges.administratorChange change=licencePositionChanges["licence-administrator"] correctUrl=correctUrl/>
  </#if>
  <#if licencePositionChanges["set-equity"]??>
      <@positionChanges.setEquityChange change=licencePositionChanges["set-equity"]/>
  </#if>
</#macro>