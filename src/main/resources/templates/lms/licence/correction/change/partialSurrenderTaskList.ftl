<#include '../../../layout/layout.ftl'>
<#import '../../../tasklist/standardTaskList.ftl' as taskList>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  pageSize=PageSize.TWO_THIRDS_COLUMN
  backLinkUrl=springUrl(backLinkUrl)
>
  <@fdsDataItems.dataItem>
    <@fdsDataItems.dataValues key="Position reference" value=positionReference/>
    <@fdsDataItems.dataValues key="Position date" value=positionDate/>
  </@fdsDataItems.dataItem>

  <@taskList.standardTaskList taskListSections=taskListSections />
</@defaultPage>
