<#include '../../layout/layout.ftl'>
<#import '_positionChanges.ftl' as positionChanges>

<#macro details licencePosition licencePositionChanges canEdit=false>
    <#assign headingText>
      ${licencePosition.getFormattedPositionDate()} (${licencePosition.getLicenceTransaction().getRegulatorReference()})
    </#assign>
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
    <p class="govuk-body">[TODO: Licence state]</p>
    <#if licencePositionChanges["licence-administrator"]??>
        <@positionChanges.administratorChange change=licencePositionChanges["licence-administrator"]/>
    </#if>
</#macro>