<#include '../../layout/layout.ftl'>

<#macro changeHeading change headingText>
  <div style="display: flex; gap: 1rem">
    ${headingText}
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
</#macro>

<#macro changeCardActions screenReaderText correctUrl="" removeUrl="" undoUrl="">
  <@fdsSummaryList.summaryListCardActionList>
    <#if correctUrl?has_content>
      <@fdsSummaryList.summaryListCardActionItem
        itemUrl=springUrl(correctUrl)
        itemText="Correct"
        itemScreenReaderText=screenReaderText
      />
    </#if>
    <#if removeUrl?has_content>
      <@fdsSummaryList.summaryListCardActionItem
        itemUrl=springUrl(removeUrl)
        itemText="Remove"
        itemScreenReaderText=screenReaderText
      />
    </#if>
    <#if undoUrl?has_content>
      <@fdsSummaryList.summaryListCardActionItem
        itemUrl=springUrl(undoUrl)
        itemText="Undo"
        itemScreenReaderText=screenReaderText
      />
    </#if>
  </@fdsSummaryList.summaryListCardActionList>
</#macro>

<#macro administratorChange change>
  <#assign removed>
    <@fdsTag.tag tagClass="govuk-tag--red">Removed</@fdsTag.tag>
  </#assign>

  <#assign added>
    <@fdsTag.tag tagClass="govuk-tag--green">Added</@fdsTag.tag>
  </#assign>

  <#assign isRemoved = (change.changeType()!) == "remove-change">

  <#assign headingText>
    <@changeHeading change=change headingText="Licence administrator change"/>
  </#assign>

  <#assign cardActions>
    <@changeCardActions
      screenReaderText="licence administrator change"
      correctUrl=change.correctUrl()!''
      removeUrl=change.removeUrl()!''
      undoUrl=change.undoUrl()!''
    />
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
    <@changeHeading change=change headingText="Set equity"/>
  </#assign>

  <#assign cardActions>
    <@fdsSummaryList.summaryListCardActionList>
      <#if change.updateUrl()?has_content>
        <@fdsSummaryList.summaryListCardActionItem
          itemUrl=springUrl(change.updateUrl())
          itemText="Correct"
          itemScreenReaderText="set equity change"
        />
      </#if>
    <#if change.removeUrl()?has_content>
                <@fdsSummaryList.summaryListCardActionItem
                itemUrl=springUrl(change.removeUrl())
                itemText="Remove"
                itemScreenReaderText="set equity change"
                />
            </#if>
            <#if change.undoUrl()?has_content>
                <@fdsSummaryList.summaryListCardActionItem
                itemUrl=springUrl(change.undoUrl())
                itemText="Undo"
                itemScreenReaderText="set equity change"
                />
            </#if>
        </@fdsSummaryList.summaryListCardActionList>
  </#assign>

  <@fdsSummaryList.summaryListCard headingText=headingText summaryListId="set-equity" cardActionsContent=cardActions>
    <#list change.rows() as row>
        <@fdsSummaryList.summaryListRowNoAction keyText=row.organisationName()>
            ${row.equity()}%
        </@fdsSummaryList.summaryListRowNoAction>
    </#list>
    </@fdsSummaryList.summaryListCard>
</#macro>

<#macro transferEquityChange change>
  <#assign headingText>
    <@changeHeading change=change headingText="Transfer equity"/>
  </#assign>

  <#assign cardActions>
    <@fdsSummaryList.summaryListCardActionList>
      <#if change.updateUrl()?has_content>
        <@fdsSummaryList.summaryListCardActionItem
        itemUrl=springUrl(change.updateUrl())
        itemText="Correct"
        itemScreenReaderText="equity transfer change"
        />
      </#if>
    <#if change.removeUrl()?has_content>
                <@fdsSummaryList.summaryListCardActionItem
                itemUrl=springUrl(change.removeUrl())
                itemText="Remove"
                itemScreenReaderText="equity transfer change"
                />
            </#if>
            <#if change.undoUrl()?has_content>
                <@fdsSummaryList.summaryListCardActionItem
                itemUrl=springUrl(change.undoUrl())
                itemText="Undo"
                itemScreenReaderText="equity transfer change"
                />
            </#if>
        </@fdsSummaryList.summaryListCardActionList>
  </#assign>

  <@fdsSummaryList.summaryListCard headingText=headingText summaryListId="transfer-equity" cardActionsContent=cardActions>
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
        <td class="govuk-table__cell">
        <div>${holding.transferFromOrganisationName()}</div>
        <div class="govuk-hint govuk-!-margin-bottom-0">before this position they had ${holding.transferFromStartingEquity()}%</div>
        </td>
        <td class="govuk-table__cell">
      <div>${holding.transferToOrganisationName()}</div>
      <div class="govuk-hint govuk-!-margin-bottom-0">before this position they had ${holding.transferToStartingEquity()}%</div>
    </td>
      <td class="govuk-table__cell govuk-table__cell--numeric">${holding.equity()}%</td>
    </tr>
  </#list>
      </tbody>
    </table>
  </@fdsSummaryList.summaryListCard>
</#macro>

<#macro partialSurrenderChange change>
  <#assign headingText>
    <@changeHeading change=change headingText="Partial surrender"/>
  </#assign>

  <#assign cardActions>
    <@changeCardActions screenReaderText="partial surrender" correctUrl=change.correctUrl()!''/>
  </#assign>

  <@fdsSummaryList.summaryListCard
    headingText=headingText
    summaryListId="partial-surrender"
    cardActionsContent=cardActions
  >
    <#if change.surrenderDate()??>
      <@fdsSummaryList.summaryListRowNoAction keyText="Date of surrender">
        ${change.surrenderDate()}
      </@fdsSummaryList.summaryListRowNoAction>
    </#if>
    <@fdsSummaryList.summaryListRowNoAction keyText="Blocks to surrender">
      <dl>
        <#list change.blockRows() as blockRow>
          <dt style="white-space: nowrap;">${blockRow.blockLabel()}<#if blockRow.surrenderType()??> - ${blockRow.surrenderType()}</#if></dt>
        </#list>
      </dl>
    </@fdsSummaryList.summaryListRowNoAction>
  </@fdsSummaryList.summaryListCard>
</#macro>