<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems>
  <@fdsForm.htmlForm>
    <@fdsSummaryList.summaryList>
      <@fdsSummaryList.summaryListRowNoAction keyText="Duration">
        ${licenceSchedulePhaseSummaryView.duration()}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="Start date">
        ${licenceSchedulePhaseSummaryView.startDate()}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="End date">
        ${licenceSchedulePhaseSummaryView.endDate()}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="Comments">
        ${licenceSchedulePhaseSummaryView.comments()!""}
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