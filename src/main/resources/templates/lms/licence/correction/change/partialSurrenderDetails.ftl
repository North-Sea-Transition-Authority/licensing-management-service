<#include '../../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
>
  <@fdsForm.htmlForm>
    <@fdsSummaryList.summaryList>
      <@fdsSummaryList.summaryListRowNoAction keyText="Date of surrender">
        ${surrenderDate}
      </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryList>

    <#if blockOptions?has_content>
      <@fdsCheckbox.checkboxes
        path="form.featureIds"
        checkboxes=blockOptions
        fieldsetHeadingText="Blocks to surrender"
        fieldsetHeadingSize="h2"
        fieldsetHeadingClass="govuk-fieldset__legend--m"
      />

      <@fdsAction.submitButtons
        primaryButtonText="Save and continue"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(backLinkUrl)
      />
    <#else>
      <@fdsInsetText.insetText>
        There are no active licence blocks on this licence to surrender.
      </@fdsInsetText.insetText>

      <@fdsAction.link linkText="Cancel" linkUrl=springUrl(backLinkUrl) />
    </#if>
  </@fdsForm.htmlForm>
</@defaultPage>
