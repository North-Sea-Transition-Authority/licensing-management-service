<#include '../../layout/layout.ftl'>
<#include '../../macros/document/removeSection.ftl'>

<#assign pageTitle="Are you sure you want to delete this section?"/>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_WIDTH
  breadcrumbs=breadcrumbs
  errorSummaryItems=errorList
>
  <@removeSection documentSectionDto=documentSectionDto/>
  <@fdsForm.htmlForm>

    <@fdsAction.submitButtons
      primaryButtonText="Remove"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(cancelUrl)
      primaryButtonClass="govuk-button govuk-button--warning"
    />
  </@fdsForm.htmlForm>
</@defaultPage>
