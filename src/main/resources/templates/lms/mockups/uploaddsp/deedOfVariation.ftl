<#include '../../layout/layout.ftl'>
<#include '_mockupNav.ftl'>
<#include '_upload.ftl'>
<#include '_deedInstructions.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=caption
  pageSize=PageSize.TWO_THIRDS_COLUMN
  breadcrumbs=breadcrumbs>
  <@mockupNavigation/>
  <#if hasLicenseeWithoutContact>
    <@fdsError.singleErrorSummary
      errorMessage="The deed of variation cannot be sent until every licensee has a contact for this licence. Provide a contact for each licensee in the Licence contacts list."/>
  </#if>

  <@fdsForm.htmlForm>

    <@dspFileUpload attributes=deedUpload legend="Upload the deed of variation"/>

    <@fdsDateInput.dateInput
      dayPath="form.signingDeadline.dayInput.inputValue"
      monthPath="form.signingDeadline.monthInput.inputValue"
      yearPath="form.signingDeadline.yearInput.inputValue"
      labelText="Signing deadline"
      hintText="The date by which every licensee must have uploaded their signed deed."
      formId="form-signing-deadline"
    />

    <@fdsSummaryList.summaryList>
      <#list licensees as licensee>
        <@fdsSummaryList.summaryListRowNoAction keyText=licensee.name()>
          <#if licensee.contact()??>
            ${licensee.contact()}
          <#else>
            <span class="govuk-error-message">No contact provided</span>
          </#if>
        </@fdsSummaryList.summaryListRowNoAction>
      </#list>
    </@fdsSummaryList.summaryList>

    <@fdsDetails.summaryDetails summaryTitle="Signing instructions the licensees will see">
      <@signingInstructions/>
    </@fdsDetails.summaryDetails>

    <@fdsAction.submitButtons primaryButtonText="Send to licensees" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
  </@fdsForm.htmlForm>
</@defaultPage>
