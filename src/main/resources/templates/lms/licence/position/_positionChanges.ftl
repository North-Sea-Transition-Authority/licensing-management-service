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

<#macro transferEquityChange change>
    <#assign headingText>
      <div style="display: flex; gap: 1rem">
        Transfer equity
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

    <@fdsSummaryList.summaryListCard headingText=headingText summaryListId="transfer-equity">
      <table class="govuk-table govuk-!-margin-top-2 govuk-!-margin-bottom-0">
        <thead class="govuk-table__head">
        <tr class="govuk-table__row">
          <th scope="col" class="govuk-table__header">Transfer from</th>
          <th scope="col" class="govuk-table__header">Transfer to</th>
          <th scope="col" class="govuk-table__header govuk-table__header--numeric">Amount</th>
        </tr>
        </thead>
        <tbody class="govuk-table__body">
        <#list change.holdings() as holding>
          <tr class="govuk-table__row">
            <td class="govuk-table__cell">${holding.transferFromOrganisationName()}</td>
            <td class="govuk-table__cell">${holding.transferToOrganisationName()}</td>
            <td class="govuk-table__cell govuk-table__cell--numeric">${holding.equity()}%</td>
          </tr>
        </#list>
        </tbody>
      </table>
    </@fdsSummaryList.summaryListCard>
</#macro>