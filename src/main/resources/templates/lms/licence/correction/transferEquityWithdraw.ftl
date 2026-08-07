<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle="Is ${organisationName} withdrawing?"
pageHeading="Is ${organisationName} withdrawing?"
caption=pageCaption
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