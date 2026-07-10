<#include '../../layout/layout.ftl'>

<#assign pageTitle = "Are you sure you want to cancel correction ${correctionReference}?">

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
>
  <@fdsForm.htmlForm>
    <@fdsAction.submitButtons
      primaryButtonText="Cancel correction"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(cancelUrl)
      primaryButtonClass="govuk-button govuk-button--warning"
    />
  </@fdsForm.htmlForm>
</@defaultPage>