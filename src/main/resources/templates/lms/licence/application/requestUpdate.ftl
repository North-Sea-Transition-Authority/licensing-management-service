<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=caption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
backLinkUrl=springUrl(backUrl)>
    <@fdsForm.htmlForm>

        <@fdsDateInput.dateInput
            dayPath="form.updateDeadlineDate.dayInput.inputValue"
            monthPath="form.updateDeadlineDate.monthInput.inputValue"
            yearPath="form.updateDeadlineDate.yearInput.inputValue"
            labelText="When is the update needed by?"
            formId="form-update-deadline-date"
        />

        <@fdsTextarea.textarea
            path="form.updateReason"
            labelText="Why is an update needed?"
        />

        <@fdsAction.submitButtons primaryButtonText="Request update" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>
