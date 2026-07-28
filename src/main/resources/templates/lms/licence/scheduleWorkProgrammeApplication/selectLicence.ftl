<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=""
caption=pageCaption
captionClass="govuk-caption-m"
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsSearchSelector.searchSelectorRest
            path="form.licenceId"
            restUrl=springUrl(searchUrl)
            labelText=pageTitle
            pageHeading=true
            labelHeadingClass="govuk-label--m"
        />

        <@fdsDetails.summaryDetails summaryTitle="My licence is not listed">
            <p class="govuk-body">
                You must be a licensee on the licence your application is for. If you are a licensee and your licence is
                not listed, contact <@fdsAction.link linkText=customerBranding.approvalsContactEmail() linkUrl="mailto:${customerBranding.approvalsContactEmail()}"/>.
            </p>
        </@fdsDetails.summaryDetails>

        <@fdsAction.submitButtons primaryButtonText="Continue" secondaryLinkText="Back" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
    </@fdsForm.htmlForm>

</@defaultPage>