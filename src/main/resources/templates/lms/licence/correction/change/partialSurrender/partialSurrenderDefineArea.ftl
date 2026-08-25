<#include '../../../../layout/layoutWithGisAssets.ftl'>
<#import "../../../../../gis/components/splitByPointAndClickPage/splitByPointAndClickPage.ftl" as pointAndClick>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
backLinkUrl=springUrl(backLinkUrl)
>
    <@fdsForm.htmlForm>
        <@pointAndClick.splitByPointAndClickPage
          commandJourneyId=commandJourneyId
          srsWkid=srsWkid
          id="split-area"
          error=mapErrorMessage!""
        />
        <@fdsAction.button buttonText="Continue"/>
    </@fdsForm.htmlForm>
</@defaultPage>