<#include '../../../layout/layout.ftl'>
<#import '_adminCorrectionPlayback.ftl' as adminCorrectionPlayback>

<@defaultPage
  htmlTitle="Are you sure you want to delete this licence administrator change?"
  backLinkUrl=springUrl(backUrl)
>

  <@adminCorrectionPlayback.adminChangePlayback isEditable=false/>

  <@fdsInsetText.insetText>
    <p>
      BP EXPLORATION (ALPHA) LIMITED (01021007) will be the administrator for this licence until the next change.
    </p>
  </@fdsInsetText.insetText>

  <@fdsAction.submitButtons
    primaryButtonText="Delete change"
    secondaryLinkText="Cancel"
    linkSecondaryAction=true
    linkSecondaryActionUrl=springUrl(backUrl)
    primaryButtonClass="govuk-button govuk-button--warning"
  />
</@defaultPage>