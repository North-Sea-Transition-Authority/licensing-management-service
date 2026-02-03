<#include '../../layout/layout.ftl'>
<#import 'summaryCardList/licenceContinuationWpaRequirementSummaryCard.ftl' as licenceContinuationWpaRequirementCard>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
captionClass="govuk-caption-m"
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>

    <@fdsForm.htmlForm>
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
        <@fdsAction.submitButtons primaryButtonText="Save" secondaryLinkText="Back" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>
    </@fdsForm.htmlForm>
</@defaultPage>