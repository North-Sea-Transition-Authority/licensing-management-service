<#include '../../../layout/layout.ftl'>
<#import '../_correctionDetailsCard.ftl' as correctionDetailsCard>
<#import '_correctedPositionsAccordion.ftl' as correctedPositionsAccordion>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  pageSize=PageSize.FULL_COLUMN
  backLinkUrl=springUrl(backLinkUrl)
  errorSummaryItems=errorSummaryItems
>
  <@fdsForm.htmlForm>
    <@correctionDetailsCard.correctionDetailsCard
      details=correctionDetails
    />

    <h2 class="govuk-heading-l"><#if isCorrectionApplied>Applied changes<#else>Corrected positions</#if></h2>

    <@correctedPositionsAccordion.correctedPositionsAccordion
      correctionId=correction.getId()
      positions=positions
    />

    <#if canApply>
      <@fdsAction.submitButtons
        primaryButtonText="Apply correction"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(backLinkUrl)
      />
    </#if>
  </@fdsForm.htmlForm>
</@defaultPage>
