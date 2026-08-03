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
          <#case "update-change-operations">
            <@fdsTag.tag tagClass="govuk-tag--blue">Corrected change</@fdsTag.tag>
          <#break>
        </#switch>
      </div>
    </#assign>

    <#assign cardActions>
      <#if change.url()?has_content>
        <@fdsSummaryList.summaryListCardActionList>
          <@fdsSummaryList.summaryListCardActionItem
            itemUrl=springUrl(change.url())
            itemText="Correct"
            itemScreenReaderText="licence administrator change"
          />
        </@fdsSummaryList.summaryListCardActionList>
      </#if>
    </#assign>

    <@fdsSummaryList.summaryListCard
      headingText=headingText
      summaryListId="licence-administrator"
      cardActionsContent=cardActions
    >
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

<#macro setEquityChange change>
    <#assign headingText>
      <div style="display: flex; gap: 1rem">
        Set equity
          <#switch change.changeType()!>
              <#case "add-change">
                  <@fdsTag.tag tagClass="govuk-tag--green">Added change</@fdsTag.tag>
                  <#break>
              <#case "update-change-operations">
                  <@fdsTag.tag tagClass="govuk-tag--blue">Corrected change</@fdsTag.tag>
                  <#break>
          </#switch>
      </div>
    </#assign>

    <@fdsSummaryList.summaryListCard headingText=headingText summaryListId="set-equity">
        <#list change.rows() as row>
            <@fdsSummaryList.summaryListRowNoAction keyText=row.organisationName()>
                ${row.equity()}%
            </@fdsSummaryList.summaryListRowNoAction>
        </#list>
    </@fdsSummaryList.summaryListCard>
</#macro>