<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
breadcrumbs=breadcrumbs
errorSummaryItems=errorSummaryItems>

    <@fdsForm.htmlForm>
        <@fdsRadio.radioGroup
        path="form.hasAdditionalSupportingInformation"
        labelText="Do you have any further supporting information you would like to provide to support this application?"
        hiddenContent=true>
            <@fdsRadio.radioYes path="form.hasAdditionalSupportingInformation">
                <@fdsFileUpload.fileUpload
                path=fileUploadAttributes.path()
                allowedExtensions=fileUploadAttributes.allowedExtensions()
                uploadUrl=fileUploadAttributes.uploadUrl()
                downloadUrl=fileUploadAttributes.downloadUrl()
                deleteUrl=fileUploadAttributes.deleteUrl()
                existingFiles=fileUploadAttributes.existingFiles()
                maxAllowedSize=fileUploadAttributes.maxAllowedSize()/>
            </@fdsRadio.radioYes>
            <@fdsRadio.radioNo path="form.hasAdditionalSupportingInformation"/>
        </@fdsRadio.radioGroup>
        <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Back" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>
    </@fdsForm.htmlForm>
</@defaultPage>
