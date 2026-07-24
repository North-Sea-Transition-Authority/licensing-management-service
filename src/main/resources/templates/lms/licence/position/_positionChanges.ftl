<#include '../../layout/layout.ftl'>

<#macro administratorChange change>
    <#assign removed>
        <@fdsTag.tag tagClass="govuk-tag--red">Removed</@fdsTag.tag>
    </#assign>
    <#assign added>
        <@fdsTag.tag tagClass="govuk-tag--green">Added</@fdsTag.tag>
    </#assign>
    <#assign headingText>
      <div style="display: flex; gap: 1rem">
        Licence administrator change
          <#switch change.changeType()!>
            <#case "add-change">
              <@fdsTag.tag tagClass="govuk-tag--green">Added change</@fdsTag.tag>
            <#break>
          </#switch>
      </div>
    </#assign>

    <@fdsSummaryList.summaryListCard headingText=headingText summaryListId="licence-administrator">
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