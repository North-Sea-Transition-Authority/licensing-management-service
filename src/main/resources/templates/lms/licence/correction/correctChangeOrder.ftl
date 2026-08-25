<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  backLinkUrl=springUrl(backLinkUrl)
>
  <@fdsInsetText.insetText>
    <p class="govuk-body govuk-!-margin-bottom-1">Current order of changes on this position</p>
    <ol reversed class="govuk-list govuk-list--number">
      <#list currentChangeOrder as change>
        <li>${change.reference()}<#if change.beingMoved()> <b>(the change you are moving)</b></#if></li>
      </#list>
    </ol>
  </@fdsInsetText.insetText>

  <@fdsForm.htmlForm>
    <#if singleOutcome>
      <#assign moveValue = changeMoveOptions?keys?first/>
      <input type="hidden" name="changeMove.inputValue" value="${moveValue}"/>
        <@fdsAction.submitButtons
          primaryButtonText="Confirm move"
          secondaryLinkText="Cancel"
          linkSecondaryAction=true
          linkSecondaryActionUrl=springUrl(backLinkUrl)
        />
    <#else>
      <@fdsRadio.radioGroup
        path="form.changeMove.inputValue"
        labelText="When did this change occur relative to other changes?"
      >
        <#assign firstItem=true/>
        <#list changeMoveOptions as index, value>
          <@fdsRadio.radioItem path="form.changeMove.inputValue" itemMap={index : value} isFirstItem=firstItem></@fdsRadio.radioItem>
          <#assign firstItem=false/>
        </#list>
      </@fdsRadio.radioGroup>
      <@fdsAction.submitButtons
        primaryButtonText="Update order"
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(backLinkUrl)
      />
    </#if>
  </@fdsForm.htmlForm>
</@defaultPage>