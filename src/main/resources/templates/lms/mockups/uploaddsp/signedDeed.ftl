<#include '../../layout/layout.ftl'>
<#include '_mockupNav.ftl'>
<#include '_upload.ftl'>
<#include '_deedInstructions.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=caption
  pageSize=PageSize.TWO_THIRDS_COLUMN
  breadcrumbs=breadcrumbs>
  <@mockupNavigation/>

  <#if sendBackReason??>
    <@fdsWarning.warning>
      The NSTA sent your signed deed back: ${sendBackReason}
    </@fdsWarning.warning>
  </#if>

  <@fdsSummaryList.summaryList>
    <@fdsSummaryList.summaryListRowNoAction keyText="Deed of variation">
      <a class="govuk-link" href="#">deed-of-variation.pdf</a>
    </@fdsSummaryList.summaryListRowNoAction>
    <@fdsSummaryList.summaryListRowNoAction keyText="Licensee">
      ${licenseeName}
    </@fdsSummaryList.summaryListRowNoAction>
    <@fdsSummaryList.summaryListRowNoAction keyText="Signing deadline">
      ${signingDeadline}
    </@fdsSummaryList.summaryListRowNoAction>
  </@fdsSummaryList.summaryList>

  <h2 class="govuk-heading-m">How to sign the deed</h2>
  <@signingInstructions/>

  <@fdsForm.htmlForm>
    <@dspFileUpload attributes=signedUpload legend="Upload the signed deed"/>

    <@fdsAction.submitButtons primaryButtonText="Send signed deed" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
  </@fdsForm.htmlForm>
</@defaultPage>
