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
        ${licenceScheduleTermSummaryView.duration()}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="Start date">
        ${licenceScheduleTermSummaryView.startDate()}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="End date">
        ${licenceScheduleTermSummaryView.endDate()}
      </@fdsSummaryList.summaryListRowNoAction>
      <#if pendingComment?has_content>
        <@fdsSummaryList.summaryListRowNoAction keyText="Comment">
          ${pendingComment}
        </@fdsSummaryList.summaryListRowNoAction>
      </#if>
    </@fdsSummaryList.summaryList>

    <#if canDelete>
      <@fdsAction.submitButtons
      primaryButtonText="Delete"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(cancelUrl)
      primaryButtonClass="govuk-button govuk-button--warning"
      />
    <#else>
      <@fdsWarning.warning>
        This cannot be deleted as it is referenced by other schedule events.
      </@fdsWarning.warning>

      <@fdsBackLink.backLink backLinkUrl=springUrl(cancelUrl)/>
    </#if>


  </@fdsForm.htmlForm>
</@defaultPage>