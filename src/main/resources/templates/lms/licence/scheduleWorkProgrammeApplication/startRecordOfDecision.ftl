<#include '../../layout/layout.ftl'>

<@defaultPage
htmlTitle=pageTitle
pageHeading=pageTitle
pageHeadingClass="govuk-heading-l"
caption=pageCaption
captionClass="govuk-caption-l"
pageSize=PageSize.TWO_THIRDS_COLUMN
backLinkUrl=springUrl(backUrl)
errorSummaryItems=errorSummaryItems>

    <@fdsStartPage.startPage startActionButton=true startActionUrl=startUrl startActionText="Start">
        <p class="govuk-body">Use this to record the decision on a ${applicationTypeDisplayName}</p>

        <ul class="govuk-list govuk-list--bullet">
            <li>[GUIDANCE TEXT TBD]</li>
        </ul>
    </@fdsStartPage.startPage>

</@defaultPage>
