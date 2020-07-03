databaseChangeLog = {

	changeSet(author: "galffy (generated)", id: "1593698652589-1") {
		createTable(schemaName: "public", tableName: "license_property") {
			column(autoIncrement: "true", name: "lp_id", type: "int8") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "license_propePK")
			}

			column(name: "lp_version", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "lp_date_created", type: "timestamp")

			column(name: "lp_date_value", type: "timestamp")

			column(name: "lp_dec_value", type: "numeric(19, 2)")

			column(name: "instance_of_id", type: "int8")

			column(name: "lp_int_value", type: "int4")

			column(name: "lp_is_public", type: "bool") {
				constraints(nullable: "false")
			}

			column(name: "lp_last_updated", type: "timestamp")

			column(name: "lp_last_updated_cascading", type: "timestamp")

			column(name: "lp_note", type: "text")

			column(name: "lp_owner_fk", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "lp_paragraph", type: "text")

			column(name: "lp_ref_value", type: "int8")

			column(name: "lp_string_value", type: "text")

			column(name: "lp_tenant_fk", type: "int8")

			column(name: "lp_type_fk", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "lp_url_value", type: "varchar(255)")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-2") {
		createTable(schemaName: "public", tableName: "org_property") {
			column(autoIncrement: "true", name: "op_id", type: "int8") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "org_propertyPK")
			}

			column(name: "op_version", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "op_date_created", type: "timestamp")

			column(name: "op_date_value", type: "timestamp")

			column(name: "op_dec_value", type: "numeric(19, 2)")

			column(name: "op_int_value", type: "int4")

			column(name: "op_is_public", type: "bool") {
				constraints(nullable: "false")
			}

			column(name: "op_last_updated", type: "timestamp")

			column(name: "op_last_updated_cascading", type: "timestamp")

			column(name: "op_note", type: "text")

			column(name: "op_owner_fk", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "op_ref_value", type: "int8")

			column(name: "op_string_value", type: "text")

			column(name: "op_tenant_fk", type: "int8")

			column(name: "op_type_fk", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "op_url_value", type: "varchar(255)")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-3") {
		createTable(schemaName: "public", tableName: "person_property") {
			column(autoIncrement: "true", name: "pp_id", type: "int8") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "person_properPK")
			}

			column(name: "pp_version", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "pp_date_created", type: "timestamp")

			column(name: "pp_date_value", type: "timestamp")

			column(name: "pp_dec_value", type: "numeric(19, 2)")

			column(name: "pp_int_value", type: "int4")

			column(name: "pp_is_public", type: "bool") {
				constraints(nullable: "false")
			}

			column(name: "pp_last_updated", type: "timestamp")

			column(name: "pp_last_updated_cascading", type: "timestamp")

			column(name: "pp_note", type: "text")

			column(name: "pp_owner_fk", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "pp_ref_value", type: "int8")

			column(name: "pp_string_value", type: "text")

			column(name: "pp_tenant_fk", type: "int8")

			column(name: "pp_type_fk", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "pp_url_value", type: "varchar(255)")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-4") {
		createTable(schemaName: "public", tableName: "platform_property") {
			column(autoIncrement: "true", name: "plp_id", type: "int8") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "platform_propPK")
			}

			column(name: "plp_version", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "plp_date_created", type: "timestamp")

			column(name: "plp_date_value", type: "timestamp")

			column(name: "plp_dec_value", type: "numeric(19, 2)")

			column(name: "plp_int_value", type: "int4")

			column(name: "plp_is_public", type: "bool") {
				constraints(nullable: "false")
			}

			column(name: "plp_last_updated", type: "timestamp")

			column(name: "plp_last_updated_cascading", type: "timestamp")

			column(name: "plp_note", type: "text")

			column(name: "owner_id", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "plp_ref_value", type: "int8")

			column(name: "plp_string_value", type: "text")

			column(name: "plp_tenant_fk", type: "int8")

			column(name: "plp_type_fk", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "plp_url_value", type: "varchar(255)")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-5") {
		createTable(schemaName: "public", tableName: "subscription_property") {
			column(autoIncrement: "true", name: "sp_id", type: "int8") {
				constraints(nullable: "false", primaryKey: "true", primaryKeyName: "subscription_PK")
			}

			column(name: "sp_version", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "sp_date_created", type: "timestamp")

			column(name: "sp_date_value", type: "timestamp")

			column(name: "sp_dec_value", type: "numeric(19, 2)")

			column(name: "sp_instance_of_fk", type: "int8")

			column(name: "sp_int_value", type: "int4")

			column(name: "sp_is_public", type: "bool") {
				constraints(nullable: "false")
			}

			column(name: "sp_last_updated", type: "timestamp")

			column(name: "sp_last_updated_cascading", type: "timestamp")

			column(name: "sp_note", type: "text")

			column(name: "sp_owner_fk", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "sp_ref_value", type: "int8")

			column(name: "sp_string_value", type: "text")

			column(name: "sp_tenant_fk", type: "int8")

			column(name: "sp_type_fk", type: "int8") {
				constraints(nullable: "false")
			}

			column(name: "sp_url_value", type: "varchar(255)")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-6") {
		addColumn(schemaName: "public", tableName: "survey_result") {
			column(name: "surre_date_value", type: "timestamp")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-7") {
		addColumn(schemaName: "public", tableName: "survey_result") {
			column(name: "surre_dec_value", type: "numeric(19, 2)")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-8") {
		addColumn(schemaName: "public", tableName: "survey_result") {
			column(name: "surre_int_value", type: "int4")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-9") {
		addColumn(schemaName: "public", tableName: "survey_result") {
			column(name: "surre_is_public", type: "bool") {
				constraints(nullable: "false")
			}
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-10") {
		addColumn(schemaName: "public", tableName: "survey_result") {
			column(name: "surre_note", type: "text")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-11") {
		addColumn(schemaName: "public", tableName: "survey_result") {
			column(name: "surre_ref_value", type: "int8")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-12") {
		addColumn(schemaName: "public", tableName: "survey_result") {
			column(name: "surre_string_value", type: "text")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-13") {
		addColumn(schemaName: "public", tableName: "survey_result") {
			column(name: "surre_tenant_fk", type: "int8")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-14") {
		addColumn(schemaName: "public", tableName: "survey_result") {
			column(name: "surre_url_value", type: "varchar(255)")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-15") {
		modifyDataType(columnName: "ap_id", newDataType: "int8", tableName: "activity_profiler")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-16") {
		modifyDataType(columnName: "cid_id", newDataType: "int8", tableName: "customer_identifier")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-17") {
		modifyDataType(columnName: "ddo_id", newDataType: "int8", tableName: "due_date_object")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-18") {
		modifyDataType(columnName: "active", newDataType: "bool", tableName: "ftcontrol")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-19") {
		addNotNullConstraint(columnDataType: "bool", columnName: "active", tableName: "ftcontrol")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-20") {
		modifyDataType(columnName: "ig_id", newDataType: "int8", tableName: "issue_entitlement_group")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-21") {
		modifyDataType(columnName: "igi_id", newDataType: "int8", tableName: "issue_entitlement_group_item")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-22") {
		modifyDataType(columnName: "mt_id", newDataType: "int8", tableName: "mail_template")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-23") {
		modifyDataType(columnName: "osg_id", newDataType: "int8", tableName: "org_subject_group")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-24") {
		modifyDataType(columnName: "id", newDataType: "int8", tableName: "pending_change_configuration")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-25") {
		modifyDataType(columnName: "surconf_create_title_groups", newDataType: "bool", tableName: "survey_config")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-26") {
		addNotNullConstraint(columnDataType: "bool", columnName: "surconf_create_title_groups", tableName: "survey_config")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-27") {
		modifyDataType(columnName: "sa_id", newDataType: "int8", tableName: "system_announcement")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-28") {
		modifyDataType(columnName: "sm_content_de", newDataType: "varchar(255)", tableName: "system_message")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-29") {
		dropNotNullConstraint(columnDataType: "varchar(255)", columnName: "sm_content_de", tableName: "system_message")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-30") {
		modifyDataType(columnName: "sm_content_en", newDataType: "varchar(255)", tableName: "system_message")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-31") {
		addNotNullConstraint(columnDataType: "varchar(255)", columnName: "sm_type", tableName: "system_message")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-32") {
		dropForeignKeyConstraint(baseTableName: "license_custom_property", baseTableSchemaName: "public", constraintName: "fke8df0ae5f9c56b03")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-33") {
		dropForeignKeyConstraint(baseTableName: "license_custom_property", baseTableSchemaName: "public", constraintName: "fke8df0ae590decb4b")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-34") {
		dropForeignKeyConstraint(baseTableName: "license_custom_property", baseTableSchemaName: "public", constraintName: "fke8df0ae52992a286")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-35") {
		dropForeignKeyConstraint(baseTableName: "license_custom_property", baseTableSchemaName: "public", constraintName: "fke8df0ae5638a6383")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-36") {
		dropForeignKeyConstraint(baseTableName: "license_private_property", baseTableSchemaName: "public", constraintName: "fkf9bc354fd9657268")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-37") {
		dropForeignKeyConstraint(baseTableName: "license_private_property", baseTableSchemaName: "public", constraintName: "fkf9bc354f7ea7815a")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-38") {
		dropForeignKeyConstraint(baseTableName: "license_private_property", baseTableSchemaName: "public", constraintName: "fkf9bc354f2992a286")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-39") {
		dropForeignKeyConstraint(baseTableName: "org_custom_property", baseTableSchemaName: "public", constraintName: "fkd7848f88c115df6e")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-40") {
		dropForeignKeyConstraint(baseTableName: "org_custom_property", baseTableSchemaName: "public", constraintName: "fkd7848f882992a286")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-41") {
		dropForeignKeyConstraint(baseTableName: "org_custom_property", baseTableSchemaName: "public", constraintName: "fkd7848f88638a6383")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-42") {
		dropForeignKeyConstraint(baseTableName: "org_private_property", baseTableSchemaName: "public", constraintName: "fkdfc7450c20b176a8")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-43") {
		dropForeignKeyConstraint(baseTableName: "org_private_property", baseTableSchemaName: "public", constraintName: "fkdfc7450c3d55999d")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-44") {
		dropForeignKeyConstraint(baseTableName: "org_private_property", baseTableSchemaName: "public", constraintName: "fkdfc7450c2992a286")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-45") {
		dropForeignKeyConstraint(baseTableName: "person_private_property", baseTableSchemaName: "public", constraintName: "fk99dfa8bb56aba4d2")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-46") {
		dropForeignKeyConstraint(baseTableName: "person_private_property", baseTableSchemaName: "public", constraintName: "fk99dfa8bbd23a4c5e")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-47") {
		dropForeignKeyConstraint(baseTableName: "person_private_property", baseTableSchemaName: "public", constraintName: "fk99dfa8bb2992a286")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-48") {
		dropForeignKeyConstraint(baseTableName: "platform_custom_property", baseTableSchemaName: "public", constraintName: "fk41914b1728a77a17")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-49") {
		dropForeignKeyConstraint(baseTableName: "platform_custom_property", baseTableSchemaName: "public", constraintName: "fk41914b172992a286")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-50") {
		dropForeignKeyConstraint(baseTableName: "platform_custom_property", baseTableSchemaName: "public", constraintName: "fk41914b17638a6383")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-51") {
		dropForeignKeyConstraint(baseTableName: "subscription_custom_property", baseTableSchemaName: "public", constraintName: "fk8717a7c18bc51d79")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-52") {
		dropForeignKeyConstraint(baseTableName: "subscription_custom_property", baseTableSchemaName: "public", constraintName: "fk8717a7c14b06441")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-53") {
		dropForeignKeyConstraint(baseTableName: "subscription_custom_property", baseTableSchemaName: "public", constraintName: "fk8717a7c12992a286")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-54") {
		dropForeignKeyConstraint(baseTableName: "subscription_custom_property", baseTableSchemaName: "public", constraintName: "fk8717a7c1638a6383")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-55") {
		dropForeignKeyConstraint(baseTableName: "subscription_private_property", baseTableSchemaName: "public", constraintName: "fk229733f32992a286")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-56") {
		dropForeignKeyConstraint(baseTableName: "subscription_private_property", baseTableSchemaName: "public", constraintName: "fk229733f3831290f7")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-57") {
		dropForeignKeyConstraint(baseTableName: "subscription_private_property", baseTableSchemaName: "public", constraintName: "fk229733f390e864a1")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-58") {
		dropForeignKeyConstraint(baseTableName: "survey_result", baseTableSchemaName: "public", constraintName: "fk92ea04a22992a286")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-83") {
		createIndex(indexName: "lp_owner_idx", schemaName: "public", tableName: "license_property") {
			column(name: "lp_owner_fk")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-84") {
		createIndex(indexName: "lp_tenant_idx", schemaName: "public", tableName: "license_property") {
			column(name: "lp_tenant_fk")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-85") {
		createIndex(indexName: "ocp_owner_idx", schemaName: "public", tableName: "org_property") {
			column(name: "op_owner_fk")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-86") {
		createIndex(indexName: "op_tenant_idx", schemaName: "public", tableName: "org_property") {
			column(name: "op_tenant_fk")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-87") {
		createIndex(indexName: "pp_owner_idx", schemaName: "public", tableName: "person_property") {
			column(name: "pp_owner_fk")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-88") {
		createIndex(indexName: "pp_tenant_idx", schemaName: "public", tableName: "person_property") {
			column(name: "pp_tenant_fk")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-89") {
		createIndex(indexName: "plp_owner_idx", schemaName: "public", tableName: "platform_property") {
			column(name: "owner_id")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-90") {
		createIndex(indexName: "plp_tenant_idx", schemaName: "public", tableName: "platform_property") {
			column(name: "plp_tenant_fk")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-91") {
		createIndex(indexName: "sp_owner_idx", schemaName: "public", tableName: "subscription_property") {
			column(name: "sp_owner_fk")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-92") {
		createIndex(indexName: "sp_tenant_idx", schemaName: "public", tableName: "subscription_property") {
			column(name: "sp_tenant_fk")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-93") {
		createIndex(indexName: "surre_tenant_idx", schemaName: "public", tableName: "survey_result") {
			column(name: "surre_tenant_fk")
		}
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-200") {
		dropTable(tableName: "license_custom_property")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-202") {
		dropTable(tableName: "org_custom_property")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-205") {
		dropTable(tableName: "platform_custom_property")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-207") {
		dropTable(tableName: "subscription_private_property")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-59") {
		addForeignKeyConstraint(baseColumnNames: "instance_of_id", baseTableName: "license_property", baseTableSchemaName: "public", constraintName: "FKD33CC413CFC87F72", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "lp_id", referencedTableName: "license_property", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-60") {
		addForeignKeyConstraint(baseColumnNames: "lp_owner_fk", baseTableName: "license_property", baseTableSchemaName: "public", constraintName: "FKD33CC41346E0E350", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "lic_id", referencedTableName: "license", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-61") {
		addForeignKeyConstraint(baseColumnNames: "lp_ref_value", baseTableName: "license_property", baseTableSchemaName: "public", constraintName: "FKD33CC413C0B73B7B", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "rdv_id", referencedTableName: "refdata_value", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-62") {
		addForeignKeyConstraint(baseColumnNames: "lp_tenant_fk", baseTableName: "license_property", baseTableSchemaName: "public", constraintName: "FKD33CC413F9C604C6", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "org_id", referencedTableName: "org", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-63") {
		addForeignKeyConstraint(baseColumnNames: "lp_type_fk", baseTableName: "license_property", baseTableSchemaName: "public", constraintName: "FKD33CC413DD065372", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "pd_id", referencedTableName: "property_definition", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-64") {
		addForeignKeyConstraint(baseColumnNames: "op_owner_fk", baseTableName: "org_property", baseTableSchemaName: "public", constraintName: "FKB79DE0D035C60FB6", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "org_id", referencedTableName: "org", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-65") {
		addForeignKeyConstraint(baseColumnNames: "op_ref_value", baseTableName: "org_property", baseTableSchemaName: "public", constraintName: "FKB79DE0D0D7CC2B98", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "rdv_id", referencedTableName: "refdata_value", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-66") {
		addForeignKeyConstraint(baseColumnNames: "op_tenant_fk", baseTableName: "org_property", baseTableSchemaName: "public", constraintName: "FKB79DE0D010DAF4E3", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "org_id", referencedTableName: "org", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-67") {
		addForeignKeyConstraint(baseColumnNames: "op_type_fk", baseTableName: "org_property", baseTableSchemaName: "public", constraintName: "FKB79DE0D0B9E2A6CF", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "pd_id", referencedTableName: "property_definition", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-68") {
		addForeignKeyConstraint(baseColumnNames: "pp_owner_fk", baseTableName: "person_property", baseTableSchemaName: "public", constraintName: "FK6890637FF8F34B42", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "prs_id", referencedTableName: "person", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-69") {
		addForeignKeyConstraint(baseColumnNames: "pp_ref_value", baseTableName: "person_property", baseTableSchemaName: "public", constraintName: "FK6890637FDF7DD0F7", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "rdv_id", referencedTableName: "refdata_value", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-70") {
		addForeignKeyConstraint(baseColumnNames: "pp_tenant_fk", baseTableName: "person_property", baseTableSchemaName: "public", constraintName: "FK6890637F188C9A42", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "org_id", referencedTableName: "org", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-71") {
		addForeignKeyConstraint(baseColumnNames: "pp_type_fk", baseTableName: "person_property", baseTableSchemaName: "public", constraintName: "FK6890637FAE2C17EE", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "pd_id", referencedTableName: "property_definition", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-72") {
		addForeignKeyConstraint(baseColumnNames: "owner_id", baseTableName: "platform_property", baseTableSchemaName: "public", constraintName: "FK5208782128A77A17", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "plat_id", referencedTableName: "platform", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-73") {
		addForeignKeyConstraint(baseColumnNames: "plp_ref_value", baseTableName: "platform_property", baseTableSchemaName: "public", constraintName: "FK520878211A0A13EB", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "rdv_id", referencedTableName: "refdata_value", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-74") {
		addForeignKeyConstraint(baseColumnNames: "plp_tenant_fk", baseTableName: "platform_property", baseTableSchemaName: "public", constraintName: "FK520878215318DD36", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "org_id", referencedTableName: "org", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-75") {
		addForeignKeyConstraint(baseColumnNames: "plp_type_fk", baseTableName: "platform_property", baseTableSchemaName: "public", constraintName: "FK5208782111487E2", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "pd_id", referencedTableName: "property_definition", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-76") {
		addForeignKeyConstraint(baseColumnNames: "sp_instance_of_fk", baseTableName: "subscription_property", baseTableSchemaName: "public", constraintName: "FK6B42A6B742D0FC34", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "sp_id", referencedTableName: "subscription_property", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-77") {
		addForeignKeyConstraint(baseColumnNames: "sp_owner_fk", baseTableName: "subscription_property", baseTableSchemaName: "public", constraintName: "FK6B42A6B7CCF35F8D", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "sub_id", referencedTableName: "subscription", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-78") {
		addForeignKeyConstraint(baseColumnNames: "sp_ref_value", baseTableName: "subscription_property", baseTableSchemaName: "public", constraintName: "FK6B42A6B7F692C114", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "rdv_id", referencedTableName: "refdata_value", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-79") {
		addForeignKeyConstraint(baseColumnNames: "sp_tenant_fk", baseTableName: "subscription_property", baseTableSchemaName: "public", constraintName: "FK6B42A6B72FA18A5F", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "org_id", referencedTableName: "org", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-80") {
		addForeignKeyConstraint(baseColumnNames: "sp_type_fk", baseTableName: "subscription_property", baseTableSchemaName: "public", constraintName: "FK6B42A6B78B086B4B", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "pd_id", referencedTableName: "property_definition", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-81") {
		addForeignKeyConstraint(baseColumnNames: "surre_ref_value", baseTableName: "survey_result", baseTableSchemaName: "public", constraintName: "FK92EA04A2575433BA", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "rdv_id", referencedTableName: "refdata_value", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}

	changeSet(author: "galffy (generated)", id: "1593698652589-82") {
		addForeignKeyConstraint(baseColumnNames: "surre_tenant_fk", baseTableName: "survey_result", baseTableSchemaName: "public", constraintName: "FK92EA04A29062FD05", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "org_id", referencedTableName: "org", referencedTableSchemaName: "public", referencesUniqueColumn: "false")
	}
}
