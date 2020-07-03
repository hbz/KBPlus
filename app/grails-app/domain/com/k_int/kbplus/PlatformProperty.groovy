package com.k_int.kbplus

import com.k_int.kbplus.abstract_domain.AbstractPropertyWithCalculatedLastUpdated
import com.k_int.properties.PropertyDefinition

class PlatformProperty extends AbstractPropertyWithCalculatedLastUpdated {

    PropertyDefinition type
    Platform owner

    Date dateCreated
    Date lastUpdated

    static mapping = {
        id          column: 'plp_id'
        version     column: 'plp_version'
        stringValue column: 'plp_string_value', type: 'text'
        intValue    column: 'plp_int_value'
        decValue    column: 'plp_dec_value'
        refValue    column: 'plp_ref_value'
        urlValue    column: 'plp_url_value'
        note        column: 'plp_note', type: 'text'
        dateValue   column: 'plp_date_value'
        isPublic    column: 'plp_is_public'
        tenant      column: 'plp_tenant_fk', index:'plp_tenant_idx'
        type        column: 'plp_type_fk'
        owner       index:  'plp_owner_idx'
        lastUpdatedCascading column: 'plp_last_updated_cascading'
        dateCreated column: 'plp_date_created'
        lastUpdated column: 'plp_last_updated'
    }

    static constraints = {
        lastUpdated (nullable: true, blank: false)
        dateCreated (nullable: true, blank: false)
    }

    static belongsTo = [
        type : PropertyDefinition,
        owner : Platform
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
