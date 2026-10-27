<#include '../../layout/layout.ftl'>
<#include '_mockupNav.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=caption
  pageSize=PageSize.TWO_THIRDS_COLUMN
  breadcrumbs=breadcrumbs>
  <@mockupNavigation/>

  <@fdsSummaryList.summaryList>
    <@fdsSummaryList.summaryListRowNoAction keyText="Licensee">
      ${licenseeName}
    </@fdsSummaryList.summaryListRowNoAction>
  </@fdsSummaryList.summaryList>

  <p class="govuk-body">${licenseeName} was asked to post the 2 signed hardcopies when their signed deed was accepted. Record them when they arrive.</p>

  <@fdsForm.htmlForm>
    <@fdsAction.submitButtons primaryButtonText="Confirm hardcopies received" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
  </@fdsForm.htmlForm>
</@defaultPage>
