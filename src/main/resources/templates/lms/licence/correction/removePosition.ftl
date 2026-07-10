<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
>
    <@fdsForm.htmlForm>
        <@fdsSummaryList.summaryList>
            <@fdsSummaryList.summaryListRowNoAction keyText="Position date">
                ${positionDate}
            </@fdsSummaryList.summaryListRowNoAction>
        </@fdsSummaryList.summaryList>

        <@fdsAction.submitButtons
        primaryButtonText="Remove position"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(cancelUrl)
        primaryButtonClass="govuk-button govuk-button--warning"
        />
    </@fdsForm.htmlForm>
</@defaultPage>