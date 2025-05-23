<#include '../layout/layout.ftl'>

<#-- @ftlvariable name="summaryTableView" type="uk.co.nstauthority.licensingmanagementservice.summary.SummaryTableView" -->

<#macro tableSummary summaryTableView summaryHeading>
  <@fdsSummaryList.summaryListCard
    headingText=summaryHeading
    headingSize="h3"
    summaryListId="table-summary-card-list">
    <table class="govuk-table">
      <thead class="govuk-table__head">
        <tr class="govuk-table__row">
          <!-- the first row contains the headers -->
          <#list summaryTableView.tableRows()[0].rowValues() as rowValue>
            <th class="govuk-table__header">${rowValue!""}</th>
          </#list>
        </tr>
      </thead>
      <tbody class="govuk-table__body">
        <#list summaryTableView.tableRows() as tableRow>
          <!-- ignore the first row as this contains the headers -->
          <#if tableRow?index != 0>
            <tr class="govuk-table__row">
              <#list tableRow.rowValues() as rowValue>
                  <#if rowValue?index == 0>
                    <th scope="row" class="govuk-table__header">
                      <@multiLineText.multiLineText contentText=rowValue!""/>
                    </th>
                  <#else>
                    <td class="govuk-table__cell">
                      <@multiLineText.multiLineText contentText=rowValue!""/>
                    </td>
                  </#if>
              </#list>
            </tr>
          </#if>
        </#list>
      </tbody>
    </table>
  </@fdsSummaryList.summaryListCard>
</#macro>
