<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle="${isUpdate?then('Update', 'Add')} contact"
  pageHeading="${isUpdate?then('Update', 'Add')} contact for ${licenceReference}"
>
  <@fdsForm.htmlForm>
    <@fdsTextInput.textInput
      path="form.contactEmail"
      labelText="Contact email address"
    />
    <@fdsAction.submitButtons
      primaryButtonText="Save"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
