<#include '../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  backLinkUrl=springUrl(backLinkUrl)
>
    <@fdsInsetText.insetText>
      <p class="govuk-body govuk-!-margin-bottom-1">Current order on this date</p>
      <ol reversed class="govuk-list govuk-list--number">
          <#list currentPositionOrder as position>
            <li>${position.reference()}<#if position.beingMoved()> <b>(the position you are moving)</b></#if></li>
          </#list>
      </ol>
    </@fdsInsetText.insetText>

    <@fdsForm.htmlForm>
        <#if singleOutcome>
            <#assign moveValue = positionMoveOptions?keys?first/>
          <input type="hidden" name="positionMove.inputValue" value="${moveValue}"/>
            <@fdsAction.submitButtons
            primaryButtonText="Confirm move"
            secondaryLinkText="Cancel"
            linkSecondaryAction=true
            linkSecondaryActionUrl=springUrl(backLinkUrl)
            />
        <#else>
            <@fdsRadio.radioGroup
            path="form.positionMove.inputValue"
            labelText="When did this position take place relative to other positions on the same date?"
            >
                <#assign firstItem=true/>
                <#list positionMoveOptions as index, value>
                    <@fdsRadio.radioItem path="form.positionMove.inputValue" itemMap={index : value} isFirstItem=firstItem></@fdsRadio.radioItem>
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