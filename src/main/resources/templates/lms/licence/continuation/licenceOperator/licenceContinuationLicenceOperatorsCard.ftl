<#include '../../../layout/layout.ftl'>

<#macro subareaCard subarea>
    <@fdsSummaryList.summaryListCard
    headingText="${subarea.name} - Block ${subarea.licenceBlock.reference}"
    summaryListId="subarea-${subarea.id}">

        <@fdsSummaryList.summaryListRowNoAction keyText="Status">
            ${subarea.status!""}
        </@fdsSummaryList.summaryListRowNoAction>

        <@fdsSummaryList.summaryListRowNoAction keyText="Assigned Operator">
            <#if subarea.operator??>
                ${subarea.operator.name}
            <#else>
              <span>None assigned</span>
            </#if>
        </@fdsSummaryList.summaryListRowNoAction>

    </@fdsSummaryList.summaryListCard>
</#macro>