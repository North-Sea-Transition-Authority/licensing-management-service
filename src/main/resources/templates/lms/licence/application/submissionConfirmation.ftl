<#include '../../layout/layout.ftl'>

<#assign pageTitle = "Application submitted" />
<#-- @ftlvariable name="workAreaUrl" type="String" -->
<#-- @ftlvariable name="applicationReference" type="String" -->
<#-- @ftlvariable name="customerBranding" type="uk.co.nstauthority.licensingmanagementservice.branding.CustomerConfigurationProperties" -->

<@defaultPage
htmlTitle=pageTitle
pageHeading=""
pageSize=PageSize.TWO_THIRDS_COLUMN>

  <@fdsPanel.panel
    panelTitle=pageTitle
    panelText="Your reference number is ${applicationReference}"/>

  <h2 class="govuk-heading-m">What happens next</h2>

  <p class="govuk-body">
    We've sent your application to the ${customerBranding.name()} (${customerBranding.mnemonic()}).
  </p>
  <p class="govuk-body">
    They will contact you either to confirm if accepted, or to ask for more information.
  </p>

  <p class="govuk-body">
    <@fdsAction.link
      linkClass="govuk-link"
      linkText="What did you think of this service?"
      linkUrl=springUrl(feedbackUrl)
      openInNewTab=true
    />
    (takes 30 seconds)
  </p>

  <p class="govuk-body">
    <a href="${springUrl(workAreaUrl)}" class="govuk-link">Back to work area</a>
  </p>

</@defaultPage>