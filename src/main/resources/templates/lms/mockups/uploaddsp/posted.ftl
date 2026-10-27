<#include '../../layout/layout.ftl'>
<#include '_mockupNav.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=caption
  pageSize=PageSize.TWO_THIRDS_COLUMN
  breadcrumbs=breadcrumbs>
  <@mockupNavigation/>

  <p class="govuk-body">The scanned conformed copy has been emailed. Post the signed hardcopies back to:</p>
  <ul class="govuk-list govuk-list--bullet">
    <#list licensees as licensee>
      <li>${licensee.name()}</li>
    </#list>
  </ul>

  <@fdsForm.htmlForm>
    <@fdsAction.submitButtons primaryButtonText="Confirm hardcopies posted" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
  </@fdsForm.htmlForm>
</@defaultPage>
