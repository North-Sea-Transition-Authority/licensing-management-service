<#include '../../layout/layout.ftl'>
<#import 'scheduleApplicationSummary.ftl' as scheduleApplicationSummary>

<#assign pageTitle = "Are you sure you want to delete this application ?" />

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN errorSummaryItems=errorSummaryItems>

  <@fdsForm.htmlForm actionUrl=springUrl(actionUrl)>
      <@scheduleApplicationSummary.scheduleApplicationSummary accordionId=accordionId summarySections=summarySections/>

      <@fdsAction.submitButtons
      primaryButtonText="Delete"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backToSummaryUrl)
      primaryButtonClass="govuk-button govuk-button--warning"/>
  </@fdsForm.htmlForm>
</@defaultPage>