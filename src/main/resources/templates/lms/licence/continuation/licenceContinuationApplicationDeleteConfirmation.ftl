<#include '../../layout/layout.ftl'>
<#import 'continuationApplicationSummary.ftl' as continuationApplicationSummary>

<#assign pageTitle = "Are you sure you want to delete this application?" />

<@defaultPage htmlTitle=pageTitle pageHeading=pageTitle pageSize=PageSize.TWO_THIRDS_COLUMN breadcrumbs=breadcrumbs errorSummaryItems=errorSummaryItems>

  <@fdsForm.htmlForm actionUrl=springUrl(actionUrl)>
      <@continuationApplicationSummary.continuationApplicationSummary accordionId=accordionId summarySections=summarySections/>

      <@fdsAction.submitButtons
      primaryButtonText="Delete"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backToTaskListUrl)
      primaryButtonClass="govuk-button govuk-button--warning"/>
  </@fdsForm.htmlForm>
</@defaultPage>
