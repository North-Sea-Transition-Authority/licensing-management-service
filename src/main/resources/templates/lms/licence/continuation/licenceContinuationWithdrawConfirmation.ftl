<#include '../../layout/layout.ftl'>

<#assign pageTitle = "Are you sure you want to withdraw this application?" />

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle caption=pageCaption pageSize=PageSize.TWO_THIRDS_COLUMN errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>

        <@fdsTextarea.textarea
        path="form.reasonForWithdrawal"
        labelText="What is the reason for withdrawing the continuation application?"/>

        <@fdsAction.submitButtons primaryButtonText="Withdraw" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl) primaryButtonClass="govuk-button govuk-button--warning"/>

    </@fdsForm.htmlForm>
</@defaultPage>