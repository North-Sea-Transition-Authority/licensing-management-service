<#include '../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageName
  pageHeading=pageName
  pageSize=PageSize.TWO_THIRDS_COLUMN
  phaseBanner=false
  errorSummaryItems=errorSummaryItems>
  <@fdsForm.htmlForm actionUrl=springUrl(actionUrl)>
    <@fdsRadio.radio
      path="form.serviceRating"
      labelText="Overall, how did you feel about using this service?"
      radioItems=serviceRatings/>
    <@fdsTextarea.textarea
      path="form.feedback.inputValue"
      labelText="How could we improve this service?"
      hintText="Do not include any personal or financial information, for example your National Insurance or credit card numbers."
      optionalLabel=true
      maxCharacterLength="2000"
      characterCount=true
      rows="10"/>
    <@fdsAction.button buttonText="Send feedback"/>
  </@fdsForm.htmlForm>
</@defaultPage>
