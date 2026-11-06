<#include '../../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageSize=PageSize.TWO_THIRDS_COLUMN
>
  <@fdsForm.htmlForm>
    <@fdsSummaryList.summaryList>
      <#if withdrawingStorageOperatorName?has_content>
        <@fdsSummaryList.summaryListRowNoAction keyText="Withdrawing storage operator">
          ${withdrawingStorageOperatorName}
        </@fdsSummaryList.summaryListRowNoAction>
      </#if>
      <@fdsSummaryList.summaryListRowNoAction keyText="Joining storage operator">
        ${joiningStorageOperatorName}
      </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryList>

    <@fdsAction.submitButtons
      primaryButtonText=primaryButton.buttonText()
      primaryButtonClass=primaryButton.buttonClass()
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(cancelUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
