<#include '../../layout/layout.ftl'>
<#import 'summaryCardList/licenceContinuationWpaRequirementSummaryCard.ftl' as licenceContinuationWpaRequirementCard>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
captionClass="govuk-caption-m"
pageSize=PageSize.TWO_THIRDS_COLUMN
breadcrumbs=breadcrumbs
errorSummaryItems=errorSummaryItems>

    <@fdsForm.htmlForm>
        <#if otherRequirementsVisibility.showFinancialCapacity()>
            <@fdsFieldset.fieldset legendHeading="Financial capacity" showHeadingOnly=true legendHeadingSize="h2">
                <@fdsRadio.radioGroup
                path="form.financialCapacityEvidenceSubmissionStatus"
                labelText="Has evidence of financial capacity been submitted to the NSTA Finance Team?"
                hiddenContent=true>
                    <@fdsRadio.radioYes path="form.financialCapacityEvidenceSubmissionStatus"/>
                    <@fdsRadio.radioNo path="form.financialCapacityEvidenceSubmissionStatus">
                        <@fdsTextarea.textarea
                        path="form.actionsToProvideFinancialEvidence"
                        nestingPath="form.financialCapacityEvidenceSubmissionStatus"
                        labelText="What actions are being taken to provide evidence to the NSTA Finance Team?"/>
                    </@fdsRadio.radioNo>
                </@fdsRadio.radioGroup>
            </@fdsFieldset.fieldset>
        </#if>

        <#if otherRequirementsVisibility.showRelinquishment()>
            <@fdsFieldset.fieldset legendHeading="Relinquishment" showHeadingOnly=true legendHeadingSize="h2">
                <@fdsRadio.radioGroup
                path="form.relinquishmentRequirementStatus"
                labelText="Has the required amount of the licensed area been relinquished?"
                hiddenContent=true>
                    <@fdsRadio.radioYes path="form.relinquishmentRequirementStatus"/>
                    <@fdsRadio.radioNo path="form.relinquishmentRequirementStatus">
                        <@fdsTextarea.textarea
                        path="form.actionsToRelinquishRequiredLicenceArea"
                        nestingPath="form.relinquishmentRequirementStatus"
                        labelText="What actions are being taken to relinquish the required amount of the licence area?"/>
                    </@fdsRadio.radioNo>
                </@fdsRadio.radioGroup>
            </@fdsFieldset.fieldset>
        </#if>

        <#if otherRequirementsVisibility.showDevelopmentConsent()>
            <@fdsFieldset.fieldset legendHeading="Development Consent" showHeadingOnly=true legendHeadingSize="h2">
                <@fdsRadio.radioGroup
                path="form.developmentConsentGrantStatus"
                labelText="Has Development Consent (PCON) been granted by the NSTA?"
                hiddenContent=true>
                    <@fdsRadio.radioYes path="form.developmentConsentGrantStatus"/>
                    <@fdsRadio.radioNo path="form.developmentConsentGrantStatus">
                        <@fdsTextarea.textarea
                        path="form.actionsToApproveDevelopmentConsent"
                        nestingPath="form.developmentConsentGrantStatus"
                        labelText="What actions are being taken to get the Development Consent approved?"/>
                    </@fdsRadio.radioNo>
                </@fdsRadio.radioGroup>
            </@fdsFieldset.fieldset>
        </#if>

        <@fdsFieldset.fieldset legendHeading="Supporting documents" showHeadingOnly=true legendHeadingSize="h2" optionalLabel=true>
            <@fdsFileUpload.fileUpload
            path=fileUploadAttributes.path()
            allowedExtensions=fileUploadAttributes.allowedExtensions()
            showAllowedExtensions=true
            uploadUrl=fileUploadAttributes.uploadUrl()
            downloadUrl=fileUploadAttributes.downloadUrl()
            deleteUrl=fileUploadAttributes.deleteUrl()
            existingFiles=fileUploadAttributes.existingFiles()
            maxAllowedSize=fileUploadAttributes.maxAllowedSize()/>
        </@fdsFieldset.fieldset>

        <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Back" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>
    </@fdsForm.htmlForm>
</@defaultPage>