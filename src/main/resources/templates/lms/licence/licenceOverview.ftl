<#include '../layout/layout.ftl'>
<#import '../component/actions/actionItems.ftl' as actionItems>

<@defaultPage
htmlTitle=licenceReference
pageHeading=licenceReference
caption=caption
pageSize=PageSize.FULL_COLUMN
>
  <@actionItems.actionItems actionItems=licenceActions screenReaderText=licenceReference/>
</@defaultPage>