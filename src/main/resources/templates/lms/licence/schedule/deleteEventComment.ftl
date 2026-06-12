<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle="Delete comment"
pageHeading="Delete comment"
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
  <@fdsForm.htmlForm>
    <@fdsSummaryList.summaryList>
      <@fdsSummaryList.summaryListRowNoAction keyText="Comment">
        ${commentView.comment()}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="Author">
        ${commentView.author()}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="Date and time">
        ${commentView.datetime()}
      </@fdsSummaryList.summaryListRowNoAction>
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
