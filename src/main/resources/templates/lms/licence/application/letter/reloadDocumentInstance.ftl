<#include '../../../layout/layout.ftl'>

<#assign pageTitle = "Reload document"/>
<#assign pageHeading = "Are you sure you want to reload the ${documentTitle} document for ${companyName}?"/>

<@defaultPage htmlTitle=pageTitle pageHeading=pageHeading pageSize=PageSize.FULL_WIDTH>
  <@fdsWarning.warning>
    This will clear all changes made to the document and recreate it from the template.
  </@fdsWarning.warning>

  <@fdsForm.htmlForm>
    <@fdsAction.submitButtons
      primaryButtonText="Reload document"
      primaryButtonClass="govuk-button govuk-button--warning"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(cancelUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
