<#include '../../layout/layout.ftl'>

<#assign pageTitle = "Licence Expiry" />

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsDateInput.dateInput
            dayPath="form.expiryDate.dayInput.inputValue"
            monthPath="form.expiryDate.monthInput.inputValue"
            yearPath="form.expiryDate.yearInput.inputValue"
            labelText="Expiry date"
            formId="form-licence-start-date"
        />

        <@fdsTextarea.textarea
            path="form.comments"
            labelText="Comments"
            optionalLabel=true
        />

        <@fdsAction.submitButtons primaryButtonText="Continue" secondaryLinkText="Back" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>
    </@fdsForm.htmlForm>

</@defaultPage>