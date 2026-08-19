<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  backLinkUrl=springUrl(backLinkUrl)
>

    <@fdsForm.htmlForm>
        <@fdsTextInput.textInput path="form.correctionReference.inputValue" labelText="Correction reference"/>
        <@fdsTextarea.textarea path="form.reason.inputValue" labelText="Reason for correction"/>
        <@fdsAction.submitButtons
          primaryButtonText="Save and continue"
          secondaryLinkText="Cancel"
          linkSecondaryAction=true
          linkSecondaryActionUrl=springUrl(backLinkUrl)
        />
    </@fdsForm.htmlForm>

</@defaultPage>