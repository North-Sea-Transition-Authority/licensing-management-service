<#include '../layout/layout.ftl'>

<#assign pageTitle="Test harness"/>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.TWO_THIRDS_COLUMN
>

    <@fdsResultList.resultList>
      <@fdsResultList.resultListItem
        linkHeadingUrl=springUrl(licencePositionTestHarnessUrl)
        linkHeadingText="Licence positions & transactions"/>
    </@fdsResultList.resultList>

</@defaultPage>