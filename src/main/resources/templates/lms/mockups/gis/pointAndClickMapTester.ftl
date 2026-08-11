<#include '../../layout/layoutWithGisAssets.ftl'>
<#import "../../../gis/components/splitByPointAndClickPage/splitByPointAndClickPage.ftl" as pointAndClick>

<@defaultPage
htmlTitle="GIS framework point and click map tester"
pageSize=PageSize.FULL_COLUMN
>
  <@pointAndClick.splitByPointAndClickPage
    commandJourneyId=commandJourneyId
    srsWkid=srsWkid
  />
</@defaultPage>
