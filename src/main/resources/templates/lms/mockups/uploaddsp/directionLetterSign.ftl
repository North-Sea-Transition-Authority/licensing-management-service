<#include '../../layout/layout.ftl'>
<#include '_mockupNav.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=caption
  pageSize=PageSize.TWO_THIRDS_COLUMN
  breadcrumbs=breadcrumbs>
  <@mockupNavigation/>

  <#if awaitingSignature>
    <p class="govuk-body">You are signing the direction letter as ${signatoryName}.</p>

    <@fdsAction.link
      linkText="Preview document"
      linkUrl=springUrl("#")
      linkClass="govuk-button govuk-button--secondary"
      role=true
      openInNewTab=true
    />

    <@fdsForm.htmlForm>
      <@fdsAction.submitButtons primaryButtonText="Sign direction letter" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
    </@fdsForm.htmlForm>

    <@fdsAction.link linkText="Send back to the decision executor" linkUrl=springUrl(sendBackUrl)/>
  <#else>
    <@fdsInsetText.insetText>
      There is no direction letter waiting for your signature.
    </@fdsInsetText.insetText>
    <@fdsAction.link linkText="Back to the application summary" linkUrl=springUrl(backUrl)/>
  </#if>
</@defaultPage>
