<#include '../../layout/layout.ftl'>

<#macro administratorChange change>
    <#assign removed>
        <@fdsTag.tag tagClass="govuk-tag--red">Removed</@fdsTag.tag>
    </#assign>
    <#assign added>
        <@fdsTag.tag tagClass="govuk-tag--green">Added</@fdsTag.tag>
    </#assign>

    <@fdsSummaryList.summaryListCard headingText="Licence administrator change" summaryListId="licence-administrator">
        <#if change.withdrawingOrganisationName()??>
          <@fdsSummaryList.summaryListRowNoAction keyText=removed>
            ${change.withdrawingOrganisationName()}
          </@fdsSummaryList.summaryListRowNoAction>
        </#if>
        <@fdsSummaryList.summaryListRowNoAction keyText=added>
          ${change.joiningOrganisationName()}
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryListCard>
</#macro>