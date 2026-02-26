<#include '../../layout/layout.ftl'>
<#include '../../../fds/components/richTextEditor/richTextEditor.ftl'>
<#import '../../macros/document/mailMergeFieldSummaryDetails.ftl' as _mailMergeFieldSummaryDetails>

<#macro editDocumentSectionForm form mailMergeFieldViews conditionsFdsSelectMap={}>
  <@grid.gridRow>
    <@grid.twoThirdsColumn>
      <@fdsTextInput.textInput
        path="form.title"
        labelText="Section title"
        hintText="This will be shown in the document sidebar."
      />

      <#if conditionsFdsSelectMap?has_content>
        <@fdsSelect.select
          path="form.conditionMnemonic"
          options=conditionsFdsSelectMap
          labelText="Select a condition"
          hintText="This section will only be included in a document if the condition is met."
          optionalLabel=true
          optionalInputDefault="No condition"
        />
      </#if>

      <@richTextEditor path="form.content" labelText="Section content"/>

      <@fdsRadio.radioGroup path="form.numbered" labelText="Should this section be numbered?">
        <@fdsRadio.radioYes path="form.numbered" />
        <@fdsRadio.radioNo path="form.numbered" />
      </@fdsRadio.radioGroup>

      <@fdsRadio.radioGroup path="form.hasPageBreakBefore" labelText="Should this section start on a new page?">
        <@fdsRadio.radioYes path="form.hasPageBreakBefore" />
        <@fdsRadio.radioNo path="form.hasPageBreakBefore" />
      </@fdsRadio.radioGroup>
    </@grid.twoThirdsColumn>
  </@grid.gridRow>

  <#if mailMergeFieldViews?has_content>
  <@_mailMergeFieldSummaryDetails.mailMergeFieldSummaryDetails mailMergeFieldViews/>
  </#if>
</#macro>