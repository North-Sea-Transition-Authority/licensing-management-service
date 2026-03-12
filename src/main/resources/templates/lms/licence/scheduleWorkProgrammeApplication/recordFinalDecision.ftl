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
            dayPath="form.decisionDate.dayInput.inputValue"
            monthPath="form.decisionDate.monthInput.inputValue"
            yearPath="form.decisionDate.yearInput.inputValue"
            labelText="Decision date"
            formId="form-decision-date"
        />

        <@fdsFieldset.fieldset
        legendHeading="Upload Final Decision Support Paper"
        legendHeadingClass="govuk-heading-m">
            <@fdsFileUpload.fileUpload
            path=fileUploadAttributes.path()
            allowedExtensions=fileUploadAttributes.allowedExtensions()
            uploadUrl=fileUploadAttributes.uploadUrl()
            downloadUrl=fileUploadAttributes.downloadUrl()
            deleteUrl=fileUploadAttributes.deleteUrl()
            existingFiles=fileUploadAttributes.existingFiles()
            maxAllowedSize=fileUploadAttributes.maxAllowedSize()/>
        </@fdsFieldset.fieldset>

        <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>

    </@fdsForm.htmlForm>
</@defaultPage>
