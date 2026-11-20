<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=caption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
backLinkUrl=springUrl(backUrl)>
    <#if currentDsp??>
        <h2 class="govuk-heading-m">Current DSP</h2>
        <@fdsSummaryList.summaryList>
            <@fdsSummaryList.summaryListRowNoAction keyText="Decision date">
                ${currentDsp.decisionDate()}
            </@fdsSummaryList.summaryListRowNoAction>
            <@fdsSummaryList.summaryListRowNoAction keyText="Final Decision Support Paper">
                <#list currentDsp.files() as file>
                    <div><a class="govuk-link" href="${springUrl(file.downloadUrl())}">${file.name()}</a></div>
                </#list>
            </@fdsSummaryList.summaryListRowNoAction>
            <@fdsSummaryList.summaryListRowNoAction keyText="Uploaded by">
                ${currentDsp.uploadedBy()!""}
            </@fdsSummaryList.summaryListRowNoAction>
        </@fdsSummaryList.summaryList>

        <@fdsForm.htmlForm>
            <@fdsRadio.radioGroup
                path="form.dspAction"
                labelText="What do you want to do?"
                hiddenContent=true
                fieldsetHeadingSize="h2"
                fieldsetHeadingClass="govuk-fieldset__legend--m">
                <#assign firstItem=true/>
                <#list dspActionOptions as key, value>
                    <#list value as displayName, hintText>
                        <@fdsRadio.radioItem
                            path="form.dspAction"
                            itemMap={key : displayName}
                            itemHintText=hintText
                            isFirstItem=firstItem>
                            <#if key == "CORRECT">
                                <@fdsFieldset.fieldset legendHeading="Upload the corrected DSP" legendHeadingClass="govuk-fieldset__legend--s">
                                    <@fdsFileUpload.fileUpload
                                        path=fileUploadAttributes.path()
                                        allowedExtensions=fileUploadAttributes.allowedExtensions()
                                        uploadUrl=fileUploadAttributes.uploadUrl()
                                        downloadUrl=fileUploadAttributes.downloadUrl()
                                        deleteUrl=fileUploadAttributes.deleteUrl()
                                        existingFiles=fileUploadAttributes.existingFiles()
                                        maxAllowedSize=fileUploadAttributes.maxAllowedSize()/>
                                </@fdsFieldset.fieldset>
                                <#-- fileUpload rebinds spring status to its own path, so restore the parent radio's binding -->
                                <@spring.bind "form.dspAction"/>
                            </#if>
                            <#assign firstItem=false/>
                        </@fdsRadio.radioItem>
                    </#list>
                </#list>
            </@fdsRadio.radioGroup>

            <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
        </@fdsForm.htmlForm>
    <#else>
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
    </#if>
</@defaultPage>
