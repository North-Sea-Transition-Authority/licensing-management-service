<#include '../../layout/layout.ftl'>
<#import 'summarylistcard/scheduleLicenceWorkProgrammeAmendmentSummaryCard.ftl' as licenceWorkProgrammeAmendmentCard>

<#assign pageTitle = "Are you sure you want to delete this work programme amendment?" />

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN breadcrumbs=breadcrumbs errorSummaryItems=errorSummaryItems>

  <@fdsForm.htmlForm actionUrl=springUrl(actionUrl)>
    <@licenceWorkProgrammeAmendmentCard.licenceWorkProgrammeAmendments
    licenceWorkProgrammeAmendment=LicenceWorkProgrammeAmendmentSummaryView
      withResultNumber=false/>

    <@fdsAction.submitButtons
      primaryButtonText="Delete"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backToSummaryUrl)
      primaryButtonClass="govuk-button govuk-button--warning"/>
  </@fdsForm.htmlForm>
</@defaultPage>