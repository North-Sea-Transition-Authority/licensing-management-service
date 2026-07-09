<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
>
    <@fdsTable.sortableTable tableContents=contactsTableJson tableId="licence-contacts-table" tableCaption=pageTitle/>
</@defaultPage>
