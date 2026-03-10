<#include '../../layout/layout.ftl'>
<#import '../../summary/_summaryMissingInformationNotificationBanner.ftl' as banner>
<#import 'continuationApplicationSummary.ftl' as continuationApplicationSummary>

<#assign pageTitle = "Review your submission before submitting"/>

<@defaultPage
htmlTitle=pageTitle
caption=pageCaption
pageHeading=pageTitle
pageHeadingClass="govuk-heading-xl"
pageSize=PageSize.FULL_COLUMN
extendContainerWidth=true
backLinkUrl=springUrl(cancelUrl)>
    <@fdsForm.htmlForm>
        <@continuationApplicationSummary.continuationApplicationSummary accordionId=accordionId summarySections=summarySections/>
    </@fdsForm.htmlForm>
</@defaultPage>