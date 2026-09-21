<#include '../../layout/layoutWithGisAssets.ftl'>
<#import "../../../gis/components/merge/mergePage.ftl" as merge>

<@defaultPage
  htmlTitle="GIS framework merge map tester"
  pageSize=PageSize.FULL_COLUMN
>
  <@merge.mergePage
    commandJourneyId=commandJourneyId
    srsWkid=srsWkid
  />
</@defaultPage>