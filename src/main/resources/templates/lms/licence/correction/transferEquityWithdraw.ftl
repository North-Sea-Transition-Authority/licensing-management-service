<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle="Withdrawal page"
pageHeading="Should ${organisationName} retain a beneficial interest in the licence despite holding no equity?"
backLinkUrl=springUrl(backLinkUrl)
>
    <@fdsForm.htmlForm actionUrl=springUrl(submitUrl)>

        <@fdsRadio.radio
          path="form.withdrawalDecision"
          radioItems=withdrawalOptions
          labelText=""
          fieldsetHeadingSize="h1"
        />

        <@fdsAction.submitButtons
        primaryButtonText="Continue"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(backLinkUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>