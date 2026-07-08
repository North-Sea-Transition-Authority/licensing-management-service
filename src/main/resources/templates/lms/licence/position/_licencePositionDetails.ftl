<#include '../../layout/layout.ftl'>
<#import '_positionChanges.ftl' as positionChanges>

<#macro details licencePositionState licencePositionChanges canEdit=false>
    <#if canEdit>
      <@fdsAction.buttonGroup>
          <@fdsAction.link
            linkText="Edit"
            linkUrl="#"
            linkClass="govuk-button govuk-button--secondary"
          />
          <@fdsAction.link
            linkText="Delete"
            linkUrl="#"
            linkClass="govuk-button govuk-button--warning"
          />
      </@fdsAction.buttonGroup>
    </#if>
    <@fdsDataItems.dataItem>
        <@fdsDataItems.dataValues key="Licence administrator" value=licencePositionState.administratorStateView().organisationName()/>
    </@fdsDataItems.dataItem>
    <#if licencePositionChanges["licence-administrator"]??>
        <@positionChanges.administratorChange change=licencePositionChanges["licence-administrator"]/>
    </#if>
</#macro>