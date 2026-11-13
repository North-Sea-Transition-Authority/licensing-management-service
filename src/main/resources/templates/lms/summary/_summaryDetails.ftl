<#include '../layout/layout.ftl'>
<#import '_simpleSummary.ftl' as simpleSummary>
<#import '_tableSummary.ftl' as tableSummary>
<#import '_emptySummary.ftl' as emptySummary>
<#import '_filesSummary.ftl' as filesSummary>
<#import '_fileAndDetailsSummary.ftl' as filesAndDetailsSummary>
<#import 'summaryValue/_mapSummaryValue.ftl' as mapSummaryValue>

<#-- @ftlvariable name="summaryItem" type="uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem" -->
<#-- @ftlvariable name="summaryCard" type="uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard" -->

<#macro summaryDetails summaryItem>
  <@summaryCards cards=summaryItem.summaryCards()/>
  <@summaryRows rows=summaryItem.summaryRows()/>
</#macro>

<#macro summaryRows rows>
  <#list rows as row>
    <#if row.heading()?has_content>
      <h3 class="govuk-heading-m">
        ${row.heading()}
        <#if row.tag()??>
          <@fdsTag.tag tagClass=row.tag().tagClass()>${row.tag().tagText()}</@fdsTag.tag>
        </#if>
      </h3>
    </#if>
    <div class="lms-summary-comparison">
      <#list row.columns() as column>
        <div>
          <#if column.heading()?has_content && row.heading()?has_content>
            <h4 class="govuk-heading-s">${column.heading()}</h4>
          <#elseif column.heading()?has_content>
            <h3 class="govuk-heading-m">${column.heading()}</h3>
          </#if>
        </div>
        <div>
          <#if column.map()??>
            <div class="govuk-!-margin-bottom-4">
              <@mapSummaryValue.mapValueDisplay mapValues=[column.map()]/>
            </div>
          <#elseif column.insetText()?has_content>
            <@fdsInsetText.insetText insetTextClass="govuk-!-margin-top-0">${column.insetText()}</@fdsInsetText.insetText>
          </#if>
        </div>
        <div>
          <@summaryCards
            cards=column.summaryCards()
            cardHeadingSize=row.heading()?has_content?then("h5", column.heading()?has_content?then("h4", "h3"))
          />
        </div>
      </#list>
    </div>
  </#list>
</#macro>

<#macro summaryCards cards cardHeadingSize="h3">
  <#list cards as summaryCard>
    <#if summaryCard.summaryCardType() == "SIMPLE_SUMMARY">
      <@simpleSummary.simpleSummary
        summaryDataView=summaryCard.summaryData()
        summaryHeading=summaryCard.displayName()!""
        actions=summaryCard.actions()
        headingSize=cardHeadingSize
      />
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
    <#elseif summaryCard.summaryCardType() == "FILES_AND_DETAILS_SUMMARY">
        <@filesAndDetailsSummary.filesAndDetailsSummary
        mixedView=summaryCard.summaryData()
        heading=summaryCard.displayName()!""/>
    </#if>
  </#list>
</#macro>
