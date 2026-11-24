<#include '../../layout/layout.ftl'>

<#macro applicationUpdateSummaryCard applicationUpdateSummaryCardView>
    <@fdsSummaryList.summaryListCard headingText="Update requested" summaryListId="application-update-summary-card-list">
        <@fdsSummaryList.summaryListRowNoAction keyText="Update reason">
            ${applicationUpdateSummaryCardView.updateReason()!""}
        </@fdsSummaryList.summaryListRowNoAction>

        <@fdsSummaryList.summaryListRowNoAction keyText="Update deadline">
            ${applicationUpdateSummaryCardView.updateDeadline()!""}
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryListCard>
</#macro>
