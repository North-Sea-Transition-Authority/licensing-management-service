<#include '../../../layout/layout.ftl'/>

<#macro adminChangePlayback isEditable=true editUrl="#" deleteUrl="#">
    <#if isEditable>
      <#assign content>
          <@fdsSummaryList.summaryListCardActionList>
              <@fdsSummaryList.summaryListCardActionItem itemUrl=editUrl itemText="Edit" itemScreenReaderText="something"/>
              <@fdsSummaryList.summaryListCardActionItem itemUrl=deleteUrl itemText="Delete" itemScreenReaderText="something"/>
          </@fdsSummaryList.summaryListCardActionList>
      </#assign>
    </#if>
    <#assign removed>
      <@fdsTag.tag tagClass="govuk-tag--red">Removed</@fdsTag.tag>
    </#assign>
    <#assign added>
      <@fdsTag.tag tagClass="govuk-tag--green">Added</@fdsTag.tag>
    </#assign>

    <@fdsSummaryList.summaryListCard headingText="Licence administrator change" cardActionsContent=content!"" summaryListId="summary-card-list">
        <@fdsSummaryList.summaryListRowNoAction keyText=removed>
          BP EXPLORATION (ALPHA) LIMITED (01021007)
        </@fdsSummaryList.summaryListRowNoAction>
        <@fdsSummaryList.summaryListRowNoAction keyText=added>
          SHELL U.K. LIMITED (00140141)
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryListCard>
</#macro>

<#macro adminStatePlayback licenceAdmin>
    <@fdsDataItems.dataItem>
        <@fdsDataItems.dataValues key="Licence Administrator" value=licenceAdmin/>
    </@fdsDataItems.dataItem>
</#macro>
