<#include '../../../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
>
  <@fdsForm.htmlForm>
    <@fdsSearchSelector.searchSelectorEnhanced
      path="form.featureId"
      options=blockOptions
      labelText="Select a licence block"
    />

    <@fdsAction.submitButtons
      primaryButtonText="Save and continue"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
