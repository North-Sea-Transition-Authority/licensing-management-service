<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
caption=pageCaption
pageSize=PageSize.TWO_THIRDS_COLUMN
errorSummaryItems=errorSummaryItems
backLinkUrl=springUrl(backUrl)>

    <@fdsStartPage.startPage startActionButton=false startActionUrl=springUrl(startUrl) startActionText="Start">
        <p class="govuk-body">Use this to create a new licence schedule:</p>

        <ul class="govuk-list govuk-list--bullet">
            <li>[GUIDANCE TEXT TBD]</li>
        </ul>
    </@fdsStartPage.startPage>

</@defaultPage>