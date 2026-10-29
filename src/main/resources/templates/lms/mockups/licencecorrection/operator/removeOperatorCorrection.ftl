<#include '../../../layout/layout.ftl'>

<#assign pageTitle>
  Are you sure you want to remove this ${operatorType.displayName?uncap_first} change?
</#assign>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  backLinkUrl=springUrl(backUrl)
  pageSize=PageSize.TWO_THIRDS_COLUMN
>

  <@fdsForm.htmlForm>
    <@fdsSummaryList.summaryList>
      <@fdsSummaryList.summaryListRowNoAction keyText="Withdrawing ${operatorType.displayName?uncap_first}">
        ${withdrawingOperator}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="Joining ${operatorType.displayName?uncap_first}">
        ${joiningOperator}
      </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryList>

    <@fdsInsetText.insetText>
      <p>
        ${withdrawingOperator} will be the ${operatorType.displayName?uncap_first} until the next change.
      </p>
    </@fdsInsetText.insetText>

    <@fdsAction.submitButtons
      primaryButtonText="Remove ${operatorType.displayName?uncap_first} change"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backUrl)
      primaryButtonClass="govuk-button govuk-button--warning"
    />
  </@fdsForm.htmlForm>

</@defaultPage>