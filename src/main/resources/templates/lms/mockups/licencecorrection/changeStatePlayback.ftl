<#include '../../layout/layout.ftl'>
<#import '_adminCorrectionPlayback.ftl' as adminCorrectionPlayback>

<@defaultPage
  htmlTitle="P1"
  caption="Seaward production"
>

  <@fdsTimeline.timeline>

      <@fdsTimeline.timelineSection sectionHeading="Licence positions">
          <@fdsTimeline.timelineTimeStamp
            timeStampHeading="27 May 2026"
            timeStampHeadingHint="REF-4A652C2B">
              <@fdsTimeline.timelineEvent>
                  <@fdsTimeline.timelineEvent>
                      <@adminCorrectionPlayback.adminStatePlayback licenceAdmin="SHELL U.K. LIMITED (00140141)"/>
                  </@fdsTimeline.timelineEvent>
              </@fdsTimeline.timelineEvent>
          </@fdsTimeline.timelineTimeStamp>

          <@fdsTimeline.timelineTimeStamp
            timeStampHeading="16 Feb 2026"
            timeStampHeadingHint="REF-674C56EB">
              <@fdsTimeline.timelineEvent>
                <@adminCorrectionPlayback.adminChangePlayback editUrl=springUrl(editUrl) deleteUrl=springUrl(deleteUrl)/>
                <@adminCorrectionPlayback.adminStatePlayback licenceAdmin="SHELL U.K. LIMITED (00140141)"/>
              </@fdsTimeline.timelineEvent>
          </@fdsTimeline.timelineTimeStamp>

          <@fdsTimeline.timelineTimeStamp
            timeStampHeading="15 Jun 2025"
            timeStampHeadingHint="REF-1234FE2A"
            timeStampClass="fds-timeline__time-stamp--no-border"
          >
              <@fdsTimeline.timelineEvent>
                <@adminCorrectionPlayback.adminStatePlayback licenceAdmin="BP EXPLORATION (ALPHA) LIMITED (01021007)"/>
              </@fdsTimeline.timelineEvent>
          </@fdsTimeline.timelineTimeStamp>

      </@fdsTimeline.timelineSection>
  </@fdsTimeline.timeline>

</@defaultPage>
