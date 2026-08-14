<#include '../../../layout/layout.ftl'>
<#import '_licenseeCorrectionPlayback.ftl' as licenseeCorrectionPlayback>

<@defaultPage
  htmlTitle="Are you sure you want to remove this licensee change?"
  pageHeading="Are you sure you want to remove this licensee change?"
  pageSize=PageSize.TWO_THIRDS_COLUMN
  backLinkUrl=springUrl(backUrl)
>
<@fdsForm.htmlForm>
  <@licenseeCorrectionPlayback.licenseeChangePlayback isEditable=false/>

    <@fdsSummaryList.summaryList>
        <@fdsSummaryList.summaryListRowNoAction keyText="Resulting licensees">
          BP EXPLORATION (ALPHA) LIMITED
        </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryList>

    <@fdsAction.submitButtons
      primaryButtonText="Remove licensee change"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backUrl)
      primaryButtonClass="govuk-button govuk-button--warning"
    />
  </@fdsForm.htmlForm>
</@defaultPage>
