package com.k_int.kbplus

import com.k_int.kbplus.abstract_domain.AbstractPropertyWithCalculatedLastUpdated
import com.k_int.properties.PropertyDefinition

/**Org custom (and private) properties are used to store Org related settings and options**/
class OrgProperty extends AbstractPropertyWithCalculatedLastUpdated {

    PropertyDefinition type
    Org owner

    Date dateCreated
    Date lastUpdated

    static mapping = {
        id          column: 'op_id'
        version     column: 'op_version'
        stringValue column: 'op_string_value', type: 'text'
        intValue    column: 'op_int_value'
        decValue    column: 'op_dec_value'
        refValue    column: 'op_ref_value'
        urlValue    column: 'op_url_value'
        note        column: 'op_note', type: 'text'
        dateValue   column: 'op_date_value'
        isPublic    column: 'op_is_public'
        tenant      column: 'op_tenant_fk', index:'op_tenant_idx'
        type        column: 'op_type_fk'
        owner       column: 'op_owner_fk', index:  'ocp_owner_idx'
        lastUpdatedCascading column: 'op_last_updated_cascading'
        dateCreated column: 'op_date_created'
        lastUpdated column: 'op_last_updated'
    }

    static constraints = {
        // Nullable is true, because values are already in the database
        lastUpdated (nullable: true, blank: false)
        dateCreated (nullable: true, blank: false)
    }

    static belongsTo = [
        type : PropertyDefinition,
        owner: Org
    ]

    @Override
    def afterDelete() {
        super.afterDeleteHandler()
    }
    @Override
    def afterInsert() {
        super.afterInsertHandler()
    }
    @Override
    def afterUpdate() {
        super.afterUpdateHandler()
    }
}
