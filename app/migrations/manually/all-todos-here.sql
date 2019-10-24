-- 2019-10-09
-- remove table columns for local environments
-- changesets in changelog-2019-10-10.groovy

--alter table "user" drop column apikey;
--alter table "user" drop column apisecret;

-- 2019-10-10
-- fixed gorm mappings for local environments
-- changesets in changelog-2019-10-10.groovy

-- ALTER TABLE api_source RENAME as_baseurl TO as_base_url;
-- ALTER TABLE api_source RENAME as_datecreated TO as_date_created;
-- ALTER TABLE api_source RENAME "as_editUrl" TO as_edit_url;
-- ALTER TABLE api_source RENAME as_fixtoken TO as_fix_token;
-- ALTER TABLE api_source RENAME as_lastupdated TO as_last_updated;
-- ALTER TABLE api_source RENAME as_lastupdated_with_api TO as_last_updated_with_api;
-- ALTER TABLE api_source RENAME as_variabletoken TO as_variable_token;
-- ALTER TABLE cost_item RENAME ci_subpkg_fk TO ci_sub_pkg_fk;
-- ALTER TABLE creator RENAME cre_datecreated TO cre_date_created;
-- ALTER TABLE creator RENAME cre_lastupdated TO cre_last_updated;
-- ALTER TABLE creator_title RENAME ct_datecreated TO ct_date_created;
-- ALTER TABLE creator_title RENAME ct_lastupdated TO ct_last_updated;
-- ALTER TABLE doc RENAME doc_mimetype TO doc_mime_type;
-- ALTER TABLE folder_item RENAME fi_datecreated TO fi_date_created;
-- ALTER TABLE folder_item RENAME fi_lastupdated TO fi_last_updated;
-- ALTER TABLE reader_number RENAME num_create_date TO num_date_created;
-- ALTER TABLE reader_number RENAME num_lastupdate_date TO num_last_updated;
-- ALTER TABLE system_message RENAME sm_datecreated TO sm_date_created;
-- ALTER TABLE system_message RENAME sm_lastupdated TO sm_last_updated;
-- ALTER TABLE system_message RENAME sm_shownow TO sm_show_now;
-- ALTER TABLE user_folder RENAME uf_datecreated TO uf_date_created;
-- ALTER TABLE user_folder RENAME uf_lastupdated TO uf_last_updated;

-- 2019-10-18
-- changesets in changelog-2019-10-21.groovy
ALTER TABLE subscription ADD sub_is_multi_year boolean;
--UPDATE subscription set sub_is_multi_year = FALSE;

-- 2019-10-22
-- changesets in changelog-2019-10-23.groovy
-- ERMS-1785: purge originEditUrl as it is never used
ALTER TABLE package DROP COLUMN pkg_origin_edit_url;
ALTER TABLE title_instance_package_platform DROP COLUMN tipp_origin_edit_url;
ALTER TABLE title_instance DROP COLUMN ti_origin_edit_url;
ALTER TABLE platform DROP COLUMN plat_origin_edit_url;
ALTER TABLE org DROP COLUMN org_origin_edit_url;
--DELETE FROM identifier_occurrence where io_canonical_id in (select id_id from identifier left join identifier_namespace "in" on identifier.id_ns_fk = "in".idns_id where "in".idns_ns in ('originEditUrl','originediturl'));
--DELETE FROM identifier where id_ns_fk = (select idns_id from identifier_namespace where idns_ns in ('originEditUrl','originediturl'));
--DELETE FROM identifier_namespace where idns_ns in ('originEditUrl','originediturl');

-- 2019-10-22 (mbeh)
--  new column class in org_access_point is initially null
-- need to set to  com.k_int.kbplus.OrgAccessPoint for all existing rows
-- see pull request for Update access point management - ad7500ef0534c4b414e5e7cb0c9acc1acd4f8283"
update org_access_point set class = 'com.k_int.kbplus.OrgAccessPoint' where class is null;


-- 2019-10-23
-- (ERMS-1808) purging of legacy GOKb copy tables
-- relink
ALTER TABLE doc_context DROP CONSTRAINT fk30eba9a871246d01;
alter table doc_context alter column dc_pkg_fk set data type text;
update doc_context set dc_pkg_fk = package.pkg_gokb_id from package where dc_pkg_fk = package.pkg_id::text;
alter table fact drop constraint fk2fd66c4cb39ba6;
alter table fact alter column related_title_id set data type text;
update fact set related_title_id = title_instance.ti_gokb_id from title_instance where related_title_id = ti_gokb_id::text;
ALTER TABLE identifier_occurrence DROP CONSTRAINT fkf0533f273508b27a;
ALTER TABLE identifier_occurrence DROP CONSTRAINT fkf0533f27b37dc426;
ALTER TABLE identifier_occurrence DROP CONSTRAINT fkf0533f27cddd0aff;
alter table identifier_occurrence alter column io_pkg_fk set data type text;
alter table identifier_occurrence alter column io_ti_fk set data type text;
alter table identifier_occurrence alter column io_tipp_fk set data type text;
update identifier_occurrence set io_pkg_fk = package.pkg_gokb_id from package where io_pkg_fk = package.pkg_id::text;
update identifier_occurrence set io_ti_fk = title_instance.ti_gokb_id from title_instance where io_ti_fk = title_instance.ti_id::text;
update identifier_occurrence set io_tipp_fk = title_instance_package_platform.tipp_gokb_id from title_instance_package_platform where io_tipp_fk = title_instance_package_platform.tipp_id::text;
alter table issue_entitlement drop constraint fk2d45f6c7330b4f5;
alter table issue_entitlement alter column ie_tipp_fk set data type text;
update issue_entitlement set ie_tipp_fk = title_instance_package_platform.tipp_gokb_id from title_instance_package_platform where ie_tipp_fk = title_instance_package_platform.tipp_id::text;
alter table org_role drop constraint fk4e5c38f1e646d31d;
alter table org_role drop constraint fk4e5c38f16d6b9898;
alter table org_role alter column or_pkg_fk set data type text;
alter table org_role alter column or_title_fk set data type text;
update org_role set or_pkg_fk = package.pkg_gokb_id from package where or_pkg_fk = package.pkg_id::text;
update org_role set or_title_fk = title_instance.ti_gokb_id from title_instance where or_title_fk = title_instance.ti_id::text;
alter table person_role drop constraint fke6a16b202504b59;
alter table person_role drop constraint fke6a16b207a8b421e;
alter table person_role alter column pr_title_fk set data type text;
alter table person_role alter column pr_pkg_fk set data type text;
update person_role set pr_title_fk = title_instance.ti_gokb_id from title_instance where pr_title_fk = title_instance.ti_id::text;
update person_role set pr_pkg_fk = package.pkg_gokb_id from package where pr_pkg_fk = package.pkg_id::text;
alter table pending_change drop constraint fk65cbdf586459a10d;
alter table pending_change drop column pc_pkg_fk;
alter table subscription_package drop constraint fk5122c72467963563;
alter table subscription_package alter column sp_pkg_fk set data type text;
update subscription_package set sp_pkg_fk = package.pkg_gokb_id from package where sp_pkg_fk = package.pkg_id::text;
alter table task drop constraint fk36358511779714;
alter table task alter column tsk_pkg_fk set data type text;
update task set tsk_pkg_fk = package.pkg_gokb_id from package where tsk_pkg_fk = package.pkg_id::text;
update title_instance set ti_gokb_id = ' ' where ti_gokb_id is null;
alter table title_institution_provider drop constraint fk89a2e01f47b4bd3f;
alter table title_institution_provider alter column tttnp_title set data type text;
update title_institution_provider set tttnp_title = title_instance.ti_gokb_id from title_instance where tttnp_title = title_instance.ti_id::text;
-- throw down problem-causing tables
DROP TABLE creator_title;
DROP TABLE creator;
DROP TABLE global_record_tracker;
DROP TABLE global_record_info;
DROP TABLE org_title_stats;
DROP TABLE platformtipp;
DROP TABLE title_history_event_participant;
DROP TABLE title_history_event;
DROP TABLE tippcoverage;
DROP TABLE title_instance_package_platform;
DROP TABLE title_instance;
DROP TABLE package;