<#include '../../../../layout/layout.ftl'>

<@defaultPage
htmlTitle=blockName
pageHeading=blockName
caption=pageCaption
backLinkUrl=springUrl(backLinkUrl)
>
  <@fdsForm.htmlForm>
    <@fdsRadio.radio
      path="form.surrenderType"
      radioItems=surrenderTypeOptions
      labelText="Type of surrender"
      fieldsetHeadingClass="govuk-fieldset__legend--m"
    />

    <@fdsDetails.summaryDetails summaryTitle="I no longer want to surrender this block">
      You can remove this block from the application from the "Surrender details" page.
    </@fdsDetails.summaryDetails>

    <@fdsAction.submitButtons
      primaryButtonText="Save and continue"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>