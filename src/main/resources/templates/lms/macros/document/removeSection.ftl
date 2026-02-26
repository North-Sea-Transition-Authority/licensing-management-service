<#include '../../layout/layout.ftl'>

<#macro removeSection documentSectionDto>
    <@fdsSummaryList.summaryListCard headingText="Section details" summaryListId="section-details-summary-card-list">
        <@fdsSummaryList.summaryListRowNoAction keyText="Section title">
            ${documentSectionDto.title()}
        </@fdsSummaryList.summaryListRowNoAction>

        <@fdsSummaryList.summaryListRowNoAction keyText="Section content">
            <p class="govuk-body govuk-body__preserve-whitespace">${(documentSectionDto.content()!)?no_esc}</p>
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryListCard>

    <#if documentSectionDto.descendants()?has_content>
        <@fdsWarning.warning>
            Removing this section will also remove all the section's subsections.
        </@fdsWarning.warning>
    </#if>
</#macro>