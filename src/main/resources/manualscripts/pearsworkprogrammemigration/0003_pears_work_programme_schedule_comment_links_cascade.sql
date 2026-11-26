-- Lets a schedule comment the work programme migration wrote be removed in LMS: its link goes with it. Run by hand
-- after 0001, in each environment the work programme migration runs in.
ALTER TABLE lms.pears_work_programme_schedule_comment_links
    DROP CONSTRAINT pears_work_programme_schedule_comment_links_comment_fk;

ALTER TABLE lms.pears_work_programme_schedule_comment_links
    ADD CONSTRAINT pears_work_programme_schedule_comment_links_comment_fk FOREIGN KEY (schedule_comment_id)
        REFERENCES lms.schedule_comments (id) ON DELETE CASCADE;
