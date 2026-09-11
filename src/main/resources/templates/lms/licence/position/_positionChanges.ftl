<#include '../../layout/layout.ftl'>

<#macro changeHeading change headingText correction="">
  <#local marker = change.marker()!''>
  <#if correction?has_content>
    <#local marker = correction.marker()!''>
  </#if>

  <div style="display: flex; gap: 1rem">
    ${headingText}
    <#if marker?has_content>
      <@fdsTag.tag tagClass=marker.tagClass>${marker.label}</@fdsTag.tag>
    </#if>
  </div>
</#macro>

<#macro changeCardActions screenReaderText urls>
  <#local correctUrl = urls.correct()!''>
  <#local removeUrl = urls.remove()!''>
  <#local undoUrl = urls.undo()!''>
  <#local correctChangeOrderUrl = urls.correctChangeOrder()!''>
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
    <#if correctChangeOrderUrl?has_content>
      <@fdsSummaryList.summaryListCardActionItem
        itemUrl=springUrl(correctChangeOrderUrl)
        itemText="Change order"
        itemScreenReaderText="of ${screenReaderText}"
      />
    </#if>
  </@fdsSummaryList.summaryListCardActionList>
</#macro>

<#macro administratorRows change>
  <#local removed>
    <@fdsTag.tag tagClass="govuk-tag--red">Withdrawing</@fdsTag.tag>
  </#local>

  <#local added>
    <@fdsTag.tag tagClass="govuk-tag--green">Joining</@fdsTag.tag>
  </#local>

  <#if change.withdrawingOrganisationName()??>
    <@fdsSummaryList.summaryListRowNoAction keyText=removed>
      ${change.withdrawingOrganisationName()}
    </@fdsSummaryList.summaryListRowNoAction>
  </#if>
  <@fdsSummaryList.summaryListRowNoAction keyText=added>
    ${change.joiningOrganisationName()}
  </@fdsSummaryList.summaryListRowNoAction>
</#macro>

<#macro administratorChange change summaryListId="licence-administrator" correction="">
  <#assign headingText>
    <@changeHeading change=change headingText="Licence administrator change" correction=correction/>
  </#assign>

  <#assign cardActions>
    <@changeCardActions screenReaderText="licence administrator change" urls=change.urls()/>
  </#assign>

  <@fdsSummaryList.summaryListCard
    headingText=headingText
    summaryListId=summaryListId
    cardActionsContent=cardActions
  >
    <@correctionRows correction=correction/>
    <@administratorRows change=change/>
  </@fdsSummaryList.summaryListCard>
</#macro>

<#macro setEquityRows change>
  <#list change.rows() as row>
    <@fdsSummaryList.summaryListRowNoAction keyText=row.organisationName()>
      ${row.equity()}%
    </@fdsSummaryList.summaryListRowNoAction>
  </#list>
</#macro>

<#macro setEquityChange change summaryListId="set-equity" correction="">
  <#assign headingText>
    <@changeHeading change=change headingText="Set equity" correction=correction/>
  </#assign>

  <#assign cardActions>
    <@changeCardActions screenReaderText="set equity change" urls=change.urls()/>
  </#assign>

  <@fdsSummaryList.summaryListCard headingText=headingText summaryListId=summaryListId cardActionsContent=cardActions>
    <@correctionRows correction=correction/>
    <@setEquityRows change=change/>
  </@fdsSummaryList.summaryListCard>
</#macro>

<#macro transferEquityRows change>
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
</#macro>

<#macro transferEquityChange change summaryListId="transfer-equity" correction="">
  <#assign headingText>
    <@changeHeading change=change headingText="Transfer equity" correction=correction/>
  </#assign>

  <#assign cardActions>
    <@changeCardActions screenReaderText="equity transfer change" urls=change.urls()/>
  </#assign>

  <@fdsSummaryList.summaryListCard
    headingText=headingText
    summaryListId=summaryListId
    cardActionsContent=cardActions
  >
    <div>
      <#if correction?has_content && correction.previousChange()??>
        <@transferEquityHoldingsHeading>Before correction</@transferEquityHoldingsHeading>
        <@transferEquityRows change=correction.previousChange()/>
        <@transferEquityHoldingsHeading>After correction</@transferEquityHoldingsHeading>
      </#if>
      <@transferEquityRows change=change/>
    </div>
  </@fdsSummaryList.summaryListCard>
</#macro>

<#macro transferEquityHoldingsHeading>
  <h3 class="govuk-heading-s govuk-!-margin-top-4 govuk-!-margin-bottom-0"><#nested></h3>
</#macro>

<#macro partialSurrenderRows change>
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
</#macro>

<#macro partialSurrenderChange change summaryListId="partial-surrender" correction="">
  <#assign headingText>
    <@changeHeading change=change headingText="Partial surrender" correction=correction/>
  </#assign>

  <#assign cardActions>
    <@changeCardActions screenReaderText="partial surrender" urls=change.urls()/>
  </#assign>

  <@fdsSummaryList.summaryListCard
    headingText=headingText
    summaryListId=summaryListId
    cardActionsContent=cardActions
  >
    <@correctionRows correction=correction/>
    <@partialSurrenderRows change=change/>
  </@fdsSummaryList.summaryListCard>
</#macro>

<#macro subareaRows change>
  <@fdsSummaryList.summaryListRowNoAction keyText="Licence block">
    ${change.featureName()}
  </@fdsSummaryList.summaryListRowNoAction>
</#macro>

<#macro subAreaChange change summaryListId="subarea" correction="">
  <#assign headingText>
    <@changeHeading change=change headingText="Subarea change" correction=correction/>
  </#assign>

  <#assign cardActions>
    <@changeCardActions screenReaderText="subarea change" urls=change.urls()/>
  </#assign>

  <@fdsSummaryList.summaryListCard
    headingText=headingText
    summaryListId=summaryListId
    cardActionsContent=cardActions
  >
    <@correctionRows correction=correction/>
    <@subareaRows change=change/>
  </@fdsSummaryList.summaryListCard>
</#macro>

<#macro changeRows change>
  <#if change.type() == "licence-administrator">
    <@administratorRows change=change/>
  <#elseif change.type() == "set-equity">
    <@setEquityRows change=change/>
  <#elseif change.type() == "transfer-equity">
    <@transferEquityRows change=change/>
  <#elseif change.type() == "partial-surrender">
    <@partialSurrenderRows change=change/>
  <#elseif change.type() == "subarea">
    <@subareaRows change=change/>
  </#if>
</#macro>

<#macro beforeCorrectionRow previousChange>
  <@fdsSummaryList.summaryListRowNoAction keyText="Before correction">
    <@fdsSummaryList.summaryList showBorder=false summaryListClass="govuk-!-margin-bottom-0">
      <@changeRows change=previousChange/>
    </@fdsSummaryList.summaryList>
  </@fdsSummaryList.summaryListRowNoAction>
</#macro>

<#macro correctionRows correction>
  <#if correction?has_content && correction.previousChange()??>
    <@beforeCorrectionRow previousChange=correction.previousChange()/>
  </#if>
</#macro>

<#macro changeCard change summaryListId isCarbonStorage=false correction="">
  <#if change.type() == "licence-administrator" && !isCarbonStorage>
    <@administratorChange change=change summaryListId=summaryListId correction=correction/>
  <#elseif change.type() == "set-equity">
    <@setEquityChange change=change summaryListId=summaryListId correction=correction/>
  <#elseif change.type() == "transfer-equity">
    <@transferEquityChange change=change summaryListId=summaryListId correction=correction/>
  <#elseif change.type() == "partial-surrender">
    <@partialSurrenderChange change=change summaryListId=summaryListId correction=correction/>
  <#elseif change.type() == "subarea">
    <@subAreaChange change=change summaryListId=summaryListId correction=correction/>
  </#if>
</#macro>
