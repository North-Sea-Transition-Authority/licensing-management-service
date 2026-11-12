<#include '../../layout/layout.ftl'>

<#assign pageTitle = "Which licences is this application for?" />

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageHeadingClass="govuk-heading-l"
  caption=pageCaption
  captionClass="govuk-caption-l"
  pageSize=PageSize.TWO_THIRDS_COLUMN
  errorSummaryItems=errorSummaryItems![]
  backLinkUrl=springUrl(backUrl)
>
  <@fdsForm.htmlForm>
    <@fdsAddToList.addToList
      pathForList="form.licenceIds"
      pathForSelector="form.licenceSelector"
      restUrl=springUrl(licenceSearchUrl)
      alreadyAdded=addedLicences
      itemName="Licences"
      noItemText="No licences have been added yet."
      selectorLabelText="Select a licence to add"
      selectorLabelHeadingClass="govuk-label--m"
      addToListId="licenceIds"
    />

    <@fdsAction.submitButtons
      primaryButtonText="Continue"
      secondaryLinkText="Back"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
