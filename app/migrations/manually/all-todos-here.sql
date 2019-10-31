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
--update org_access_point set class = 'com.k_int.kbplus.OrgAccessPoint' where class is null;

-- 2019-10-23
-- need to refetch usage data delete contents of tables
DELETE FROM stats_triple_cursor;
DELETE FROM fact;
-- execute before startup / local dev environment only
-- changed Fact.supplier without mapping from Org to Platform!
ALTER TABLE fact DROP COLUMN supplier_id;

-- 2019-10-25
-- Set sub_is_multi_year on all subscription where the periode more than 724 days
-- update subscription set sub_is_multi_year = true where sub_id in(select sub_id from subscription where DATE_PART('day', sub_end_date - sub_start_date) >= 724 and sub_end_date is not null);
