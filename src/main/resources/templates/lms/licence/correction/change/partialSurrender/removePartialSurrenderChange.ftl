<#include '../../../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.TWO_THIRDS_COLUMN
>
  <@fdsForm.htmlForm>
    <@fdsSummaryList.summaryList>
      <#if surrenderDate??>
        <@fdsSummaryList.summaryListRowNoAction keyText="Date of surrender">
          ${surrenderDate}
        </@fdsSummaryList.summaryListRowNoAction>
      </#if>
      <@fdsSummaryList.summaryListRowNoAction keyText="Blocks to surrender">
        <dl>
          <#list blockRows as blockRow>
            <dt style="white-space: nowrap;">${blockRow.blockLabel()}<#if blockRow.surrenderType()??> - ${blockRow.surrenderType()}</#if></dt>
          </#list>
        </dl>
      </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryList>

    <@fdsAction.submitButtons
      primaryButtonText=primaryButtonText
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(cancelUrl)
      primaryButtonClass="govuk-button govuk-button--warning"
    />
  </@fdsForm.htmlForm>
</@defaultPage>
