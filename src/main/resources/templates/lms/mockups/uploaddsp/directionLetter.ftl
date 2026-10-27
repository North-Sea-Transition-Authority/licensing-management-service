<#include '../../layout/layout.ftl'>
<#include '_mockupNav.ftl'>
<#import '../../document/sections/_documentSections.ftl' as documentSections>

<#-- Mirrors the real letter screen (editLetterOverview), with a signatory choice in place of Approve and issue -->
<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  breadcrumbs=breadcrumbs
  pageSize=PageSize.FULL_WIDTH
  errorSummaryItems=errorSummaryItems
  pageHeadingClass="govuk-heading-xl govuk-!-margin-bottom-2"
>
  <@mockupNavigation/>
    <#if deedRecorded>
        <@fdsInsetText.insetText>
            A deed of variation was recorded for this decision. You are preparing a direction letter instead.
        </@fdsInsetText.insetText>
    </#if>

    <#if awaitingSignatory?has_content>
        <@fdsInsetText.insetText>
            This letter is waiting for ${awaitingSignatory} to sign. Choosing a different signatory sends it to them instead.
        </@fdsInsetText.insetText>
    </#if>

    <@fdsAction.link
    linkText="Preview document"
    linkUrl=springUrl("#")
    linkClass="govuk-button govuk-button--secondary"
    role=true
    openInNewTab=true
    />

    <@fdsAction.link
    linkText="Reload document"
    linkUrl=springUrl("#")
    linkClass="govuk-button govuk-button--secondary"
    />

    <@documentSections.documentSections
    topLevelDocumentSectionSummaryViews=documentInstanceSectionsSummaryView.topLevelDocumentInstanceSectionSummaryViews()
    accordionId=accordionId
    includeRemove=hasMoreThanOneSection
    isTemplate=false
    errorList=errorList
    userHasValidPermission=userHasValidPermission
    />

    <@fdsForm.htmlForm>
        <@fdsDetails.summaryDetails summaryTitle="Who is the authorised signatory?">
            <p class="govuk-body">
                The person who will sign the letter for the NSTA. They are asked to sign after logging in through the Energy Portal.
            </p>
        </@fdsDetails.summaryDetails>

        <@fdsSelect.select
            path="form.signatory"
            options=signatoryOptions
            labelText="Authorised signatory"
        />

        <@fdsAction.submitButtons
            primaryButtonText="Send for signature"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(cancelUrl)
        />
    </@fdsForm.htmlForm>
</@defaultPage>
