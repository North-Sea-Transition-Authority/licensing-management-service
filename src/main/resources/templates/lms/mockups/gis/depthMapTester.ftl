<#include '../../layout/layoutWithGisAssets.ftl'>
<#import "../../../gis/components/depthMap/depthMapPage.ftl" as depthMap>

<@defaultPage
  htmlTitle="GIS framework depth map tester"
  pageSize=PageSize.FULL_COLUMN
>
    <@depthMap.depthMapPage
      commandJourneyId=commandJourneyId
      srsWkid=srsWkid
    />
</@defaultPage>