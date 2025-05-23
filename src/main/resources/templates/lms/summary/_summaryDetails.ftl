<#include '../layout/layout.ftl'>
<#import '_simpleSummary.ftl' as simpleSummary>
<#import '_tableSummary.ftl' as tableSummary>
<#import '_emptySummary.ftl' as emptySummary>
<#import '_filesSummary.ftl' as filesSummary>

<#-- @ftlvariable name="summaryItem" type="uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem" -->
<#-- @ftlvariable name="summaryCard" type="uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard" -->

<#macro summaryDetails summaryItem>
  <#list summaryItem.summaryCards() as summaryCard>
    <#if summaryCard.summaryCardType() == "SIMPLE_SUMMARY">
      <@simpleSummary.simpleSummary
        summaryDataView=summaryCard.summaryData()
        summaryHeading=summaryCard.displayName()!""/>
    <#elseif summaryCard.summaryCardType() == "FILES_SUMMARY">
      <@filesSummary.summary
        fileViews=summaryCard.summaryData()
        heading=summaryCard.displayName()!""/>
    <#elseif summaryCard.summaryCardType() == "TABLE_SUMMARY">
      <@tableSummary.tableSummary
        summaryTableView=summaryCard.summaryData()
        summaryHeading=summaryCard.displayName()!""/>
    <#elseif summaryCard.summaryCardType() == "EMPTY_SUMMARY">
      <@emptySummary.emptySummary/>
    </#if>
  </#list>
</#macro>
