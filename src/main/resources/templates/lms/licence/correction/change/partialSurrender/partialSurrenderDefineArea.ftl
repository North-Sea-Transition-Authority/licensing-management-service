<#include '../../../../layout/layoutWithGisAssets.ftl'>
<#import "../../../../../gis/components/splitByPointAndClickPage/splitByPointAndClickPage.ftl" as pointAndClick>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
  pageSize=PageSize.FULL_COLUMN
>
  <@fdsForm.htmlForm>
    <@pointAndClick.splitByPointAndClickPage
      commandJourneyId=commandJourneyId
      srsWkid=srsWkid
      id="split-area"
      error=mapErrorMessage!""
    />

    <@fdsAction.submitButtons
      primaryButtonText="Save and continue"
      secondaryLinkText="Back"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>