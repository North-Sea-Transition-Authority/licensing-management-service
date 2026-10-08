<#include '../../layout/layoutWithGisAssets.ftl'>
<#import "../../../gis/components/mapActions/mapActionsPage.ftl" as mapActions>

<@defaultPage
  htmlTitle="GIS framework depth actions tester"
  pageSize=PageSize.FULL_COLUMN
>
  <@mapActions.mapActionsPage
    commandJourneyId=commandJourneyId
    srsWkid=srsWkid
  />
</@defaultPage>
