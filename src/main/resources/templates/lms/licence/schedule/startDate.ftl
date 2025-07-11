<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=""
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsDateInput.dateInput
            dayPath="form.licenceStartDate.dayInput.inputValue"
            monthPath="form.licenceStartDate.monthInput.inputValue"
            yearPath="form.licenceStartDate.yearInput.inputValue"
            labelText=pageTitle
            formId="form-licence-start-date"
            showLabelOnly=true
            fieldsetHeadingSize="h1"
            fieldsetHeadingClass="govuk-fieldset__legend--xl"
            noFieldsetHeadingSize="--xl"
        />

        <@fdsAction.submitButtons primaryButtonText="Continue" secondaryLinkText="Back" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
    </@fdsForm.htmlForm>

</@defaultPage>