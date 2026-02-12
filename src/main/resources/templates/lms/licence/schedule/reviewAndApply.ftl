<#include '../../layout/layout.ftl'>

<#assign pageTitle = "Do you want to apply changes to the licence schedule?"/>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
    <@fdsForm.htmlForm>
        <@fdsSummaryList.summaryListCard headingText="Schedule details" summaryListId="summary-card-list">
            <@fdsSummaryList.summaryListRowNoAction keyText="Start date">
                ${summaryCardView.licenceStartDate()}
            </@fdsSummaryList.summaryListRowNoAction>
            <@fdsSummaryList.summaryListRowNoAction keyText="Expiry date">
                ${summaryCardView.licenceExpiryDate()!""}
            </@fdsSummaryList.summaryListRowNoAction>
            <@fdsSummaryList.summaryListRowNoAction keyText="Status">
                ${summaryCardView.status()!""}
            </@fdsSummaryList.summaryListRowNoAction>
            <#if summaryCardView.showRoundIssuedOn()>
                <@fdsSummaryList.summaryListRowNoAction keyText="Round number">
                    ${summaryCardView.roundIssuedOn()!""}
                </@fdsSummaryList.summaryListRowNoAction>
            </#if>
        </@fdsSummaryList.summaryListCard>

        <@fdsAction.submitButtons
        primaryButtonText="Apply"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(cancelUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>