<#include '../../../layout/layout.ftl'>
<#import '_licenseeCorrectionPlayback.ftl' as licenseeCorrectionPlayback>

<@defaultPage
  htmlTitle="P1"
  caption="Seaward production"
>

  <@fdsAction.link linkText="Add licensee change" linkUrl=springUrl(addUrl) linkClass="govuk-button"/>

  <@fdsTimeline.timeline>

      <@fdsTimeline.timelineSection sectionHeading="Licence positions">
          <@fdsTimeline.timelineTimeStamp
            timeStampHeading="27 May 2026"
            timeStampHeadingHint="REF-4A652C2B">
              <@fdsTimeline.timelineEvent>
                  <@licenseeCorrectionPlayback.licenseeStatePlayback licensees=[
                    "SHELL U.K. LIMITED",
                    "TOTAL E&P UK LIMITED"
                  ]/>
              </@fdsTimeline.timelineEvent>
          </@fdsTimeline.timelineTimeStamp>

          <@fdsTimeline.timelineTimeStamp
            timeStampHeading="16 Feb 2026"
            timeStampHeadingHint="REF-674C56EB">
              <@fdsTimeline.timelineEvent>
                  <@licenseeCorrectionPlayback.licenseeStatePlayback licensees=[
                  "SHELL U.K. LIMITED",
                  "TOTAL E&P UK LIMITED"
                  ]/>
                <@licenseeCorrectionPlayback.licenseeChangePlayback correctUrl=springUrl(correctUrl) removeUrl=springUrl(removeUrl)/>
              </@fdsTimeline.timelineEvent>
          </@fdsTimeline.timelineTimeStamp>

          <@fdsTimeline.timelineTimeStamp
            timeStampHeading="15 Jun 2025"
            timeStampHeadingHint="REF-1234FE2A"
            timeStampClass="fds-timeline__time-stamp--no-border"
          >
              <@fdsTimeline.timelineEvent>
                <@licenseeCorrectionPlayback.licenseeStatePlayback licensees=[
                  "BP EXPLORATION (ALPHA) LIMITED"
                ]/>
              </@fdsTimeline.timelineEvent>
          </@fdsTimeline.timelineTimeStamp>

      </@fdsTimeline.timelineSection>
  </@fdsTimeline.timeline>

</@defaultPage>
