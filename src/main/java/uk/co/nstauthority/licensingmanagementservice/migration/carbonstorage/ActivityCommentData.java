package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import java.time.Instant;

record ActivityCommentData(String comment, Instant caseInstant) {}
