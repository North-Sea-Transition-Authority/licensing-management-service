<#include '../../layout/layout.ftl'>
<#include '_mockupNav.ftl'>
<#include '_upload.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=caption
  pageSize=PageSize.TWO_THIRDS_COLUMN
  breadcrumbs=breadcrumbs>
  <@mockupNavigation/>

  <@fdsSummaryList.summaryList>
    <@fdsSummaryList.summaryListRowNoAction keyText="Licensee">
      ${licenseeName}
    </@fdsSummaryList.summaryListRowNoAction>
    <#if !onBehalf>
      <@fdsSummaryList.summaryListRowNoAction keyText="Signed deed">
        <a class="govuk-link" href="#">signed-deed.pdf</a>
      </@fdsSummaryList.summaryListRowNoAction>
    </#if>
  </@fdsSummaryList.summaryList>

  <@fdsForm.htmlForm>
    <#if onBehalf>
      <p class="govuk-body">Use this when a licensee has posted a hardcopy before their signed deed was accepted, which they are told not to do. Scan it and upload it here. If you accept it, the licensee does not need to post a hardcopy later.</p>
      <@dspFileUpload attributes=hardcopyUpload legend="Upload the scan of the posted hardcopy"/>
    </#if>

    <@fdsRadio.radioGroup
      path="form.outcome"
      labelText="Is the signed deed correct?"
      hiddenContent=true
      fieldsetHeadingSize="h2"
      fieldsetHeadingClass="govuk-fieldset__legend--m">
      <#if onBehalf>
        <@fdsRadio.radioItem path="form.outcome" itemMap={"ACCEPT": "Yes, accept it"} isFirstItem=true />
      <#else>
        <@fdsRadio.radioItem
          path="form.outcome"
          itemMap={"ACCEPT": "Yes, accept it and ask the licensee to post the hardcopies"}
          isFirstItem=true />
        <@fdsRadio.radioItem
          path="form.outcome"
          itemMap={"ACCEPT_WITHOUT_HARDCOPY": "Yes, accept it. No hardcopies are needed"}
          itemHintText="Use this when the scanned deed is enough, for example when the licensee cannot post hardcopies." />
      </#if>
      <@fdsRadio.radioItem path="form.outcome" itemMap={"SEND_BACK": "No, send it back to the licensee"}>
        <@fdsTextarea.textarea
          path="form.reason"
          labelText="What does the licensee need to put right?"
          labelHeadingClass="govuk-label--s"
          nestingPath="form.outcome"/>
      </@fdsRadio.radioItem>
    </@fdsRadio.radioGroup>

    <@fdsAction.submitButtons primaryButtonText="Save" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
  </@fdsForm.htmlForm>
</@defaultPage>
