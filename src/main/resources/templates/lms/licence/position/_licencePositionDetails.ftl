<#include '../../layout/layout.ftl'>

<#macro details licencePosition canEdit=false>
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
    <p class="govuk-body">[TODO: STATE & CHANGES HERE]</p>
</#macro>