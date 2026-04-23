<#include '../../layout/layout.ftl'>

<#assign pageTitle = "Are you sure you want to withdraw this application?" />

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>
        <@fdsAction.submitButtons primaryButtonText="Withdraw" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl) primaryButtonClass="govuk-button govuk-button--warning"/>
    </@fdsForm.htmlForm>
</@defaultPage>