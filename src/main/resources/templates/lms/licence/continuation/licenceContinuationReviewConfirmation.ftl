<#include '../../layout/layout.ftl'>

<#assign pageTitle = "Are you sure you want to confirm continuation?" />

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>
    <@fdsAction.submitButtons primaryButtonText="Confirm" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(cancelUrl)/>
    </@fdsForm.htmlForm>
</@defaultPage>