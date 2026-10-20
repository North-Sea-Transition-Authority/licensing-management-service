<#include '../../../../layout/layoutWithGisAssets.ftl'>
<#import "../../../../../gis/components/mapActions/mapActionsPage.ftl" as mapActions>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
  pageSize=PageSize.FULL_COLUMN
>
  <@mapActions.mapActionsPage
    commandJourneyId=commandJourneyId
    srsWkid=srsWkid
    id="subarea-change"
    error=mapErrorMessage!""
  />

  <@fdsForm.htmlForm>
    <@fdsAction.submitButtons
      primaryButtonText="Complete"
      secondaryLinkText="Back"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
