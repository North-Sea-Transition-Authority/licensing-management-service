<#include '../layout.ftl'>

<#-- @ftlvariable name="serviceBranding" type="uk.co.nstauthority.template.branding.ServiceConfigurationProperties" -->
<#-- @ftlvariable name="customerBranding" type="uk.co.nstauthority.template.branding.CustomerConfigurationProperties" -->

<#assign heading="Accessibility statement"/>

<@defaultPage
htmlTitle=heading
pageHeading=heading
>
    <@_section heading="Using this website">
      <p>
        This website is run by the ${customerBranding.name()} (${customerBranding.mnemonic()}). We want as many
        people as possible to be able to use this website.
      </p>
      <p>
        We have also made the website text as simple as possible to understand.
      </p>
      <p>
          <@fdsAction.link
          linkText="AbilityNet"
          linkUrl="https://mcmw.abilitynet.org.uk/"
          openInNewTab=true
          />
        has advice on making your device easier to use if you have a disability.
      </p>
    </@_section>
    <@_section heading="How accessible is this website">
      <p>
        We know some parts of this website are not fully accessible. We've listed the issues we know about in the
        <a href="#non-accessible-content" class="govuk-link"> non-accessible content</a> section.
      </p>
      <p>
        For users of voice dictation software you may have to use in built features in order to input information.
      </p>
    </@_section>
    <@_section heading="Reporting accessibility problems with this website">
      <p class="govuk-body">
        We are always looking to improve the accessibility of this website. If you need information on this website in a
        different format like accessible PDF, large print, easy read, audio recording or braille or if you find any
        problems that are not listed on this page or think we are not meeting the requirements of the accessibility
        regulations:
      </p>
      <ul class="govuk-list govuk-list--bullet">
        <li>
          email
            <@fdsAction.link
            linkUrl="mailto:${serviceBranding.supportContact().email()}?subject=${serviceBranding.name()} - Accessibility"
            linkText=serviceBranding.supportContact().email()
            />
        </li>
        <li>
          call ${serviceBranding.supportContact().phone()}
        </li>
      </ul>
      <p>We will consider your request and get back to you in 5 working days.</p>
    </@_section>
    <@_section heading="Technical information about this website’s accessibility">
      <p>
          ${customerBranding.name()} is committed to making this website accessible, in accordance with the Public
        Sector Bodies (Websites and Mobile Applications) (No.2) Accessibility Regulations 2018.
      </p>
    </@_section>
    <@_section heading="Compliance status">
      <p>
        This website is partially compliant with the
          <@fdsAction.link
          linkText="Web Content Accessibility Guidelines version 2.1"
          linkUrl="https://www.w3.org/TR/WCAG21/"
          openInNewTab=true
          />
        AA standard, due to the non-compliances listed below.
      </p>
    </@_section>
    <@_section heading="Non-accessible content">
      <p>The content listed below is non-accessible for the following reasons.</p>
      <h3 class="govuk-heading-s">Non-compliance with the accessibility regulations</h3>
      <ul class="govuk-list govuk-list--bullet">
        <li>
          users are not always notified when conditionally revealed content associated with a radio button or checkbox
          is expanded or collapsed. This fails WCAG 2.1 success criterion 4.1.3 (Status Messages).
        </li>
        <li>
          breadcrumb navigation links are not identified by ARIA landmarks. This fails WCAG 2.1 success criterion 1.3.1
          (Info and Relationships).
        </li>
        <li>
          when uploading a file on macOS with VoiceOver enabled, the user is unable to select the ‘Choose a file’ via
          keyboard only. This fails WCAG 2.1 success criterion 2.1.1 (Keyboard).
        </li>
      </ul>
    </@_section>
    <@_section heading="Preparation of this accessibility statement">
      <p>
        This statement was prepared on [TODO XYZ]. It was last reviewed on [TODO XYZ].
      </p>
      <p>
        This website was last tested on [TODO XYZ]. The test was carried out by [TODO XYZ].
      </p>
      <p>
        The ${serviceBranding.name()} (${serviceBranding.mnemonic()}) service has been developed using the Energy Portal
        Design System. The Design System was last accessibility tested on [TODO XYZ]. All features
        on ${serviceBranding.mnemonic()} were accessibility tested using automated tools as part of the quality
        assurance process.
      </p>
    </@_section>

</@defaultPage>

<#macro _section heading>
  <h2 class="govuk-heading-m" id="${heading?replace(" ", "-")?lower_case}">${heading}</h2>
  <div class="govuk-body">
      <#nested />
  </div>
</#macro>