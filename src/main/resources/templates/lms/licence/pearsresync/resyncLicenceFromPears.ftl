<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(cancelUrl)
>
  <p class="govuk-body">
    This will copy the licence's positions from the licence data held in PEARS into LMS.
  </p>

  <@fdsWarning.warning>
    Every position, change and correction this licence holds in LMS, open or applied, will be deleted. This cannot be
    undone.
  </@fdsWarning.warning>

  <@fdsForm.htmlForm>
    <@fdsAction.submitButtons
      primaryButtonText="Resync from PEARS"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(cancelUrl)
      primaryButtonClass="govuk-button govuk-button--warning"
    />
  </@fdsForm.htmlForm>
</@defaultPage>
