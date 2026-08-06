<#include '../../layout/layout.ftl'>

<#macro administratorChange change>
  <#assign removed>
      <@fdsTag.tag tagClass="govuk-tag--red">Removed</@fdsTag.tag>
  </#assign>

  <#assign added>
      <@fdsTag.tag tagClass="govuk-tag--green">Added</@fdsTag.tag>
  </#assign>

  <#assign isRemoved = (change.changeType()!) == "remove-change">

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
        <#case "remove-change">
          <@fdsTag.tag tagClass="govuk-tag--red">Removed</@fdsTag.tag>
        <#break>
      </#switch>
    </div>
  </#assign>

  <#assign cardActions>
    <@fdsSummaryList.summaryListCardActionList>
      <#if change.correctUrl()?has_content>
        <@fdsSummaryList.summaryListCardActionItem
          itemUrl=springUrl(change.correctUrl())
          itemText="Correct"
          itemScreenReaderText="licence administrator change"
        />
      </#if>
      <#if change.removeUrl()?has_content>
        <@fdsSummaryList.summaryListCardActionItem
          itemUrl=springUrl(change.removeUrl())
          itemText="Remove"
          itemScreenReaderText="licence administrator change"
        />
      </#if>
      <#if change.undoUrl()?has_content>
        <@fdsSummaryList.summaryListCardActionItem
          itemUrl=springUrl(change.undoUrl())
          itemText="Undo"
          itemScreenReaderText="licence administrator change"
        />
      </#if>
    </@fdsSummaryList.summaryListCardActionList>
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