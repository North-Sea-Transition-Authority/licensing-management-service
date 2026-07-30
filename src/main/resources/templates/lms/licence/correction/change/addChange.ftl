<#include '../../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
>
    <@fdsForm.htmlForm>
        <@fdsRadio.radio
        path="form.changeType"
        labelText="What type of change do you want to add?"
        radioItems=changeTypeOptions
        />
        <@fdsAction.submitButtons
        primaryButtonText="Continue"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(backLinkUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>