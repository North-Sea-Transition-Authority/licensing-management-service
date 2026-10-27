<#include '../../layout/layout.ftl'>
<#include '_mockupNav.ftl'>

<#include '_upload.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=caption
  pageSize=PageSize.TWO_THIRDS_COLUMN
  errorSummaryItems=errorSummaryItems
  backLinkUrl=springUrl(backUrl)>
  <@mockupNavigation/>

  <#if decisionInProgress>
    <@fdsWarning.warning>
      A decision is already being recorded for this application.
    </@fdsWarning.warning>
  </#if>

  <#if dspUploaded>
    <h2 class="govuk-heading-m">Current DSP</h2>
    <@fdsSummaryList.summaryList>
      <@fdsSummaryList.summaryListRowNoAction keyText="Decision date">
        ${decisionDate}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="Final Decision Support Paper">
        <a class="govuk-link" href="#">${currentDspFileName}</a>
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="Uploaded by">
        <#if dspCorrected>Jane Smith, 20 Oct 2026 14:05 (corrected)<#else>Jane Smith, 14 Apr 2026 15:40</#if>
      </@fdsSummaryList.summaryListRowNoAction>
      <#if addendumFileNames?has_content>
        <@fdsSummaryList.summaryListRowNoAction keyText="DSP addendums">
          <#list addendumFileNames as addendumFileName>
            <div><a class="govuk-link" href="#">${addendumFileName}</a></div>
          </#list>
        </@fdsSummaryList.summaryListRowNoAction>
      </#if>
    </@fdsSummaryList.summaryList>

    <@fdsForm.htmlForm>
      <@fdsRadio.radioGroup
        path="form.dspAction"
        labelText="What do you want to do?"
        hiddenContent=true
        fieldsetHeadingSize="h2"
        fieldsetHeadingClass="govuk-fieldset__legend--m">
        <@fdsRadio.radioItem
          path="form.dspAction"
          itemMap={"CORRECT": "Replace the DSP to correct a mistake"}
          itemHintText="Use this if the wrong document was uploaded. The new document replaces the current DSP."
          isFirstItem=true>
          <#if addendumFileNames?has_content>
            <@fdsRadio.radioGroup
              path="form.replaceTarget"
              labelText="Which document do you want to replace?"
              hiddenContent=true
              nestingPath="form.dspAction"
              fieldsetHeadingSize="h3"
              fieldsetHeadingClass="govuk-fieldset__legend--s">
              <@fdsRadio.radioItem
                path="form.replaceTarget"
                itemMap={"FINAL_DSP": "Final DSP"}
                itemHintText=currentDspFileName
                isFirstItem=true>
                <@dspFileUpload attributes=correctedUpload legend="Upload the corrected DSP" nestingPath="form.replaceTarget"/>
              </@fdsRadio.radioItem>
              <#list addendumFileNames as addendumFileName>
                <#assign addendumNumber = addendumFileName?index + 1>
                <@fdsRadio.radioItem
                  path="form.replaceTarget"
                  itemMap={("ADDENDUM_" + addendumNumber): ("DSP addendum " + addendumNumber)}
                  itemHintText=addendumFileName>
                  <@dspFileUpload attributes=correctedAddendumUploads[addendumFileName?index] legend="Upload the corrected DSP addendum" nestingPath="form.replaceTarget"/>
                </@fdsRadio.radioItem>
              </#list>
            </@fdsRadio.radioGroup>
          <#else>
            <@dspFileUpload attributes=correctedUpload legend="Upload the corrected DSP" nestingPath="form.dspAction"/>
          </#if>
        </@fdsRadio.radioItem>
        <@fdsRadio.radioItem
          path="form.dspAction"
          itemMap={"ADDENDUM": "Add a DSP addendum"}
          itemHintText="Use this to add a further document. The current DSP stays as it is.">
          <@dspFileUpload attributes=addendumUpload legend="Upload the DSP addendum" nestingPath="form.dspAction"/>
        </@fdsRadio.radioItem>
      </@fdsRadio.radioGroup>

      <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>
    </@fdsForm.htmlForm>
  <#else>
    <@fdsForm.htmlForm>

      <@fdsDateInput.dateInput
        dayPath="form.decisionDate.dayInput.inputValue"
        monthPath="form.decisionDate.monthInput.inputValue"
        yearPath="form.decisionDate.yearInput.inputValue"
        labelText="Decision date"
        formId="form-decision-date"
      />

      <@dspFileUpload attributes=initialUpload legend="Upload Final Decision Support Paper"/>

      <@fdsAction.submitButtons primaryButtonText="Save and continue" secondaryLinkText="Cancel" linkSecondaryAction=true linkSecondaryActionUrl=springUrl(backUrl)/>

    </@fdsForm.htmlForm>
  </#if>
</@defaultPage>
