<#include '../../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
>
  <@fdsForm.htmlForm>
    <@fdsSummaryList.summaryList>
      <@fdsSummaryList.summaryListRowNoAction keyText="Position date">
        ${positionDate}
      </@fdsSummaryList.summaryListRowNoAction>
      <@fdsSummaryList.summaryListRowNoAction keyText="Position reference">
        ${positionReference}
      </@fdsSummaryList.summaryListRowNoAction>
    </@fdsSummaryList.summaryList>

    <@fdsRadio.radioGroup
      path="form.changeTypePositionMove.inputValue"
      labelText="Which position do you want to move this change to?"
      hiddenContent=true
    >
      <#assign firstItem=true/>
      <#list changeTypePositionMoveOptions as positionId, positionLabel>
        <@fdsRadio.radioItem path="form.changeTypePositionMove.inputValue" itemMap={positionId : positionLabel} isFirstItem=firstItem/>
        <#assign firstItem=false/>
      </#list>

      <@fdsRadio.radioItem path="form.changeTypePositionMove.inputValue" itemMap={"OTHER_DATE" : "Other date"} isFirstItem=firstItem>
        <@fdsDateInput.dateInput
          dayPath="form.correctPositionDate.dayInput.inputValue"
          monthPath="form.correctPositionDate.monthInput.inputValue"
          yearPath="form.correctPositionDate.yearInput.inputValue"
          labelText="Position date"
          formId="correctPositionDate"
          nestingPath="form.changeTypePositionMove.inputValue"
        />
      </@fdsRadio.radioItem>
    </@fdsRadio.radioGroup>

    <@fdsAction.submitButtons
      primaryButtonText="Continue"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
