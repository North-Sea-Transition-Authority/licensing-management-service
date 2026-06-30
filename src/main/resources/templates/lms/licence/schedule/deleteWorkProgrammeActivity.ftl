<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
  <@fdsForm.htmlForm>
    <@fdsSummaryList.summaryList>
      <@fdsSummaryList.summaryListRowNoAction keyText="Description">
        ${summaryView.description()}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="Commitment">
        ${summaryView.commitment()}
      </@fdsSummaryList.summaryListRowNoAction>
      <#if summaryView.dueDate()?has_content>
        <@fdsSummaryList.summaryListRowNoAction keyText="Due date">
          ${summaryView.dueDate()}
        </@fdsSummaryList.summaryListRowNoAction>
      </#if>
      <#if pendingComment?has_content>
        <@fdsSummaryList.summaryListRowNoAction keyText="Comment">
          ${pendingComment}
        </@fdsSummaryList.summaryListRowNoAction>
      </#if>
    </@fdsSummaryList.summaryList>

    <@fdsAction.submitButtons
      primaryButtonText="Delete"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(cancelUrl)
      primaryButtonClass="govuk-button govuk-button--warning"
    />
  </@fdsForm.htmlForm>
</@defaultPage>