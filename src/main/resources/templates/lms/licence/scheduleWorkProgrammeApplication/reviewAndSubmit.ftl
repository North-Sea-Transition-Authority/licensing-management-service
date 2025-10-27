<#include '../../layout/layout.ftl'>
<#import 'scheduleSummary.ftl' as scheduleSummary>

<#assign pageTitle = "Review your submission before submitting"/>

<@defaultPage
  htmlTitle=pageTitle
  caption=pageCaption
  pageHeading=pageTitle
  pageHeadingClass="govuk-heading-xl"
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
  backLinkUrl=springUrl(cancelUrl)>
    <@scheduleSummary.scheduleSummary accordionId=accordionId summarySections=summarySections/>
</@defaultPage>