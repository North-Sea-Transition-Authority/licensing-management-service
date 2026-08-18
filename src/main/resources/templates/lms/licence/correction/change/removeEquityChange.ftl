<#include '../../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.TWO_THIRDS_COLUMN
>
  <@fdsForm.htmlForm>
    <#if setEquityRows?has_content>
      <@fdsSummaryList.summaryList>
        <#list setEquityRows as row>
          <@fdsSummaryList.summaryListRowNoAction keyText=row.organisationName()>
            ${row.equity()}%
          </@fdsSummaryList.summaryListRowNoAction>
        </#list>
      </@fdsSummaryList.summaryList>
    </#if>

    <#if transferEquityRows?has_content>
      <table class="govuk-table govuk-!-margin-bottom-6">
        <thead class="govuk-table__head">
        <tr class="govuk-table__row">
          <th scope="col" class="govuk-table__header">Transfer from</th>
          <th scope="col" class="govuk-table__header">Transfer to</th>
          <th scope="col" class="govuk-table__header govuk-table__header--numeric">Amount</th>
        </tr>
        </thead>
        <tbody class="govuk-table__body">
        <#list transferEquityRows as row>
          <tr class="govuk-table__row">
            <td class="govuk-table__cell">${row.transferFromOrganisationName()}</td>
            <td class="govuk-table__cell">${row.transferToOrganisationName()}</td>
            <td class="govuk-table__cell govuk-table__cell--numeric">${row.equity()}%</td>
          </tr>
        </#list>
        </tbody>
      </table>
    </#if>

    <@fdsAction.submitButtons
      primaryButtonText=primaryButtonText
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(cancelUrl)
      primaryButtonClass="govuk-button govuk-button--warning"
    />
  </@fdsForm.htmlForm>
</@defaultPage>