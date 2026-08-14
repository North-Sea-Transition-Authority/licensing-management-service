<#include '../../../layout/layout.ftl'/>

<#macro licenseeChangePlayback isEditable=true correctUrl="#" removeUrl="#">
    <#if isEditable>
      <#assign content>
          <@fdsSummaryList.summaryListCardActionList>
              <@fdsSummaryList.summaryListCardActionItem itemUrl=correctUrl itemText="Correct" itemScreenReaderText="licensee change"/>
              <@fdsSummaryList.summaryListCardActionItem itemUrl=removeUrl itemText="Remove" itemScreenReaderText="licensee change"/>
          </@fdsSummaryList.summaryListCardActionList>
      </#assign>
    </#if>
    <#assign removed>
      <@fdsTag.tag tagClass="govuk-tag--red">Removed</@fdsTag.tag>
    </#assign>
    <#assign added>
      <@fdsTag.tag tagClass="govuk-tag--green">Added</@fdsTag.tag>
    </#assign>

    <@fdsSummaryList.summaryListCard headingText="Licensee change" cardActionsContent=content!"" summaryListId="summary-card-list">
        <@fdsSummaryList.summaryListRowNoAction keyText=removed>
          BP EXPLORATION (ALPHA) LIMITED
        </@fdsSummaryList.summaryListRowNoAction>
        <@fdsSummaryList.summaryListRowNoAction keyText=added>
          SHELL U.K. LIMITED
        </@fdsSummaryList.summaryListRowNoAction>
        <@fdsSummaryList.summaryListRowNoAction keyText=added>
          TOTAL E&P UK LIMITED
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryListCard>
</#macro>

<#macro licenseeStatePlayback licensees>
    <#assign licenseeList>
      <ul class="govuk-list">
        <#list licensees as licensee>
          <li>${licensee}</li>
        </#list>
      </ul>
    </#assign>
    <@fdsDataItems.dataItem>
        <@fdsDataItems.dataValues key="Licensees" value=licenseeList/>
    </@fdsDataItems.dataItem>
</#macro>
