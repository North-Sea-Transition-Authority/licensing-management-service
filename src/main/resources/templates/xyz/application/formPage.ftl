<#include '../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
breadcrumbs=breadcrumbs>
    <@fdsForm.htmlForm>
        <@fdsTextInput.textInput
        path="form.applicationName.inputValue"
        labelText="Application name"/>

        <@fdsTextInput.textInput
        path="form.applicationNumber.inputValue"
        inputClass="govuk-input--width-2"
        labelText="Application number"/>

        <@fdsFieldset.fieldset
        legendHeading="Application documents"
        legendHeadingClass="govuk-heading-m"
        hintText="Upload documents for this application.">
            <@fdsFileUpload.fileUpload
            path=fileUploadAttributes.path()
            allowedExtensions=fileUploadAttributes.allowedExtensions()
            uploadUrl=fileUploadAttributes.uploadUrl()
            downloadUrl=fileUploadAttributes.downloadUrl()
            deleteUrl=fileUploadAttributes.deleteUrl()
            existingFiles=fileUploadAttributes.existingFiles()
            maxAllowedSize=fileUploadAttributes.maxAllowedSize()/>
        </@fdsFieldset.fieldset>

        <@fdsSearchSelector.searchSelectorRest
        path="form.selectedApplication"
        restUrl=springUrl(applicationEndpoint)
        labelText="What is the other application?"
        selectorMinInputLength=3
        preselectedItems=preselectedApplication!{}/>

        <@fdsAction.submitButtons primaryButtonText="Save and continue"
        linkSecondaryAction=true
        linkSecondaryActionUrl="${springUrl(taskListUrl)}"
        secondaryLinkText="Cancel"/>

    </@fdsForm.htmlForm>
</@defaultPage>