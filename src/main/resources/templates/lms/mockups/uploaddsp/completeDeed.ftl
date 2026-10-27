<#include '../../layout/layout.ftl'>
<#include '_mockupNav.ftl'>
<#include '_upload.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=caption
  pageSize=PageSize.TWO_THIRDS_COLUMN
  breadcrumbs=breadcrumbs>
  <@mockupNavigation/>

  <@fdsSummaryList.summaryList>
    <#list licensees as licensee>
      <@fdsSummaryList.summaryListRowNoAction keyText="Signed by ${licensee.name()}">
        <a class="govuk-link" href="#">signed-deed-${licensee?counter}.pdf</a>
      </@fdsSummaryList.summaryListRowNoAction>
    </#list>
  </@fdsSummaryList.summaryList>

  <@fdsForm.htmlForm>
    <@dspFileUpload attributes=executedUpload legend="Upload the scan of the deed signed by the NSTA"/>

    <p class="govuk-body">The scanned conformed copy is emailed to the licensees.</p>

    <@fdsAction.submitButtons primaryButtonText="Send to licensees" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
  </@fdsForm.htmlForm>
</@defaultPage>
