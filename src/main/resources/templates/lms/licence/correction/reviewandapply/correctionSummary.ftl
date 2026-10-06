<#include '../../../layout/layout.ftl'>
<#import '../_correctionDetailsCard.ftl' as correctionDetailsCard>
<#import '_correctedPositionsAccordion.ftl' as correctedPositionsAccordion>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  pageSize=PageSize.FULL_COLUMN
  backLinkUrl=springUrl(backLinkUrl)
>
  <@correctionDetailsCard.correctionDetailsCard
    details=correctionDetails
  />

  <h2 class="govuk-heading-l">Corrected positions</h2>

  <@correctedPositionsAccordion.correctedPositionsAccordion
    correctionId=correction.getId()
    positions=positions
  />
</@defaultPage>
