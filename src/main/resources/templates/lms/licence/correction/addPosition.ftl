<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
backLinkUrl=springUrl(backLinkUrl)
>
    <@fdsForm.htmlForm>

        <@fdsDateInput.dateInput
        dayPath="form.positionDate.dayInput.inputValue"
        monthPath="form.positionDate.monthInput.inputValue"
        yearPath="form.positionDate.yearInput.inputValue"
        labelText="Position date"
        formId="positionDate"
        />

        <@fdsTextInput.textInput path="form.correctionReference.inputValue" labelText="Correction reference"/>
        <@fdsAction.submitButtons
        primaryButtonText="Add position"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(backLinkUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>