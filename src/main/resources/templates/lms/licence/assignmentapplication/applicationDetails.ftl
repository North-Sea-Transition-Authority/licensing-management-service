<#include '../../layout/layout.ftl'>

<#assign pageTitle = "Application details" />

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
    <@fdsSelect.select
      path="form.organisationUnitId"
      options=organisationOptions
      labelText="Which organisation is this application on behalf of?"
      hintText="You cannot change the organisation once the application has been started"
      labelClass="govuk-label--m"
    />

    <@fdsTextInput.textInput
      path="form.applicationReference.inputValue"
      labelText="Application reference"
      hintText="Enter a meaningful reference (alpha-numeric) that can be quoted in any communications with NSTA."
      labelClass="govuk-label--m"
      maxCharacterLength="100"
    />

    <@fdsDateInput.dateInput
      dayPath="form.plannedDate.dayInput.inputValue"
      monthPath="form.plannedDate.monthInput.inputValue"
      yearPath="form.plannedDate.yearInput.inputValue"
      labelText="Planned completion date"
      hintText="The date you plan to complete the changes on this application"
      formId="form-planned-date"
      fieldsetHeadingClass="govuk-fieldset__legend--m"
    />

    <@fdsAction.submitButtons
      primaryButtonText="Continue"
      secondaryLinkText="Back"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
