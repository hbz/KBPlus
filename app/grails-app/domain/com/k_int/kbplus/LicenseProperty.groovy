package com.k_int.kbplus

import de.laser.base.AbstractPropertyWithCalculatedLastUpdated
import com.k_int.properties.PropertyDefinition
import de.laser.interfaces.AuditableSupport

import javax.persistence.Transient

class LicenseProperty extends AbstractPropertyWithCalculatedLastUpdated implements AuditableSupport {

    @Transient
    def genericOIDService
    @Transient
    def changeNotificationService
    @Transient
    def messageSource
    @Transient
    def pendingChangeService
    @Transient
    def deletionService
    @Transient
    def auditService

    static auditable            = [ ignore: ['version', 'lastUpdated', 'lastUpdatedCascading'] ]
    static controlledProperties = ['stringValue','intValue','decValue','refValue','paragraph','note','dateValue']

    PropertyDefinition type
    License owner
    LicenseProperty instanceOf
    String paragraph

    Date dateCreated
    Date lastUpdated

    static mapping = {
        id          column: 'lp_id'
        version     column: 'lp_version'
        stringValue column: 'lp_string_value'
        intValue    column: 'lp_int_value'
        decValue    column: 'lp_dec_value'
        refValue    column: 'lp_ref_value_rv_fk'
        urlValue    column: 'lp_url_value'
        note        column: 'lp_note'
        dateValue   column: 'lp_date_value'
        instanceOf  column: 'lp_instance_of_fk', index: 'lp_instance_of_idx'
        paragraph   column: 'lp_paragraph', type: 'text'
        owner       column: 'lp_owner_fk', index:'lcp_owner_idx'
        type        column: 'lp_type_fk', index: 'lp_type_idx'
        tenant      column: 'lp_tenant_fk', index: 'lp_tenant_idx'
        isPublic    column: 'lp_is_public'
        dateCreated column: 'lp_date_created'
        lastUpdated column: 'lp_last_updated'
        lastUpdatedCascading column: 'lp_last_updated_cascading'
    }

    static constraints = {
        importFrom AbstractPropertyWithCalculatedLastUpdated
        instanceOf (nullable: true)
        paragraph  (nullable: true)

        // Nullable is true, because values are already in the database
        lastUpdated (nullable: true)
        dateCreated (nullable: true)
    }

    static belongsTo = [
        type : PropertyDefinition,
        owner: License
    ]

    @Override
    Collection<String> getLogIncluded() {
        [ 'stringValue', 'intValue', 'decValue', 'refValue', 'paragraph', 'note', 'dateValue' ]
    }
    @Override
    Collection<String> getLogExcluded() {
        [ 'version', 'lastUpdated', 'lastUpdatedCascading' ]
    }

    @Override
    def beforeInsert() {
        super.beforeInsertHandler()
    }
    @Override
    def afterInsert() {
        super.afterInsertHandler()
    }
    @Override
    def beforeUpdate(){
        Map<String, Object> changes = super.beforeUpdateHandler()

        auditService.beforeUpdateHandler(this, changes.oldMap, changes.newMap)
    }
    @Override
    def afterUpdate() {
        super.afterUpdateHandler()
    }
    @Override
    def beforeDelete() {
        super.beforeDeleteHandler()

        auditService.beforeDeleteHandler(this)
    }
    @Override
    def afterDelete() {
        super.afterDeleteHandler()

        deletionService.deleteDocumentFromIndex(this.getClass().getSimpleName().toLowerCase()+":"+this.id)
    }

    @Override
    def copyInto(AbstractPropertyWithCalculatedLastUpdated newProp){
        newProp = super.copyInto(newProp)

        newProp.paragraph = paragraph
        newProp
    }

    def notifyDependencies(changeDocument) {
        log.debug("notifyDependencies(${changeDocument})")

        if (changeDocument.event.equalsIgnoreCase('LicenseProperty.updated')) {
            // legacy ++

            Locale locale = org.springframework.context.i18n.LocaleContextHolder.getLocale()
            ContentItem contentItemDesc = ContentItem.findByKeyAndLocale("kbplus.change.subscription."+changeDocument.prop, locale.toString())
            def description = messageSource.getMessage('default.accept.placeholder',null, locale)
            if (contentItemDesc) {
                description = contentItemDesc.content
            }
            else {
                ContentItem defaultMsg = ContentItem.findByKeyAndLocale("kbplus.change.subscription.default", locale.toString())
                if( defaultMsg)
                    description = defaultMsg.content
            }

            def propName
            try {
                // UGLY
                propName = changeDocument.name ? ((messageSource.getMessage("license.${changeDocument.name}", null, locale)) ?: (changeDocument.name)) : (messageSource.getMessage("license.${changeDocument.prop}", null, locale) ?: (changeDocument.prop))

            } catch(Exception e) {
                propName = changeDocument.name ?: changeDocument.prop
            }

            // legacy ++

            List<PendingChange> slavedPendingChanges = []

            def depedingProps = LicenseProperty.findAllByInstanceOf( this )
            depedingProps.each{ lcp ->

                String definedType = 'text'
                if (lcp.type.type == RefdataValue.class.toString()) {
                    definedType = 'rdv'
                }
                else if (lcp.type.type == Date.class.toString()) {
                    definedType = 'date'
                }

                // overwrite specials ..
                if (changeDocument.prop == 'note') {
                    definedType = 'text'
                    description = '(NOTE)'
                }
                else if (changeDocument.prop == 'paragraph') {
                    definedType = 'text'
                    description = '(PARAGRAPH)'
                }

                def msgParams = [
                        definedType,
                        "${lcp.type.class.name}:${lcp.type.id}",
                        (changeDocument.prop in ['note', 'paragraph'] ? "${changeDocument.oldLabel}" : "${changeDocument.old}"),
                        (changeDocument.prop in ['note', 'paragraph'] ? "${changeDocument.newLabel}" : "${changeDocument.new}"),
                        "${description}"
                ]

                PendingChange newPendingChange =  changeNotificationService.registerPendingChange(
                        PendingChange.PROP_LICENSE,
                        lcp.owner,
                        lcp.owner.getLicensee(),
                        [
                                changeTarget:"com.k_int.kbplus.License:${lcp.owner.id}",
                                changeType:PendingChangeService.EVENT_PROPERTY_CHANGE,
                                changeDoc:changeDocument
                        ],
                        PendingChange.MSG_LI02,
                        msgParams,
                        "Das Merkmal <b>${lcp.type.name}</b> hat sich von <b>\"${changeDocument.oldLabel?:changeDocument.old}\"</b> zu <b>\"${changeDocument.newLabel?:changeDocument.new}\"</b> von der Vertragsvorlage geändert. " + description
                )
                if (newPendingChange && lcp.owner.isSlaved) {
                    slavedPendingChanges << newPendingChange
                }
            }

            slavedPendingChanges.each { spc ->
                log.debug('autoAccept! performing: ' + spc)
                pendingChangeService.performAccept(spc)
            }
        }
        else if (changeDocument.event.equalsIgnoreCase('LicenseProperty.deleted')) {

            List<PendingChange> openPD = PendingChange.executeQuery("select pc from PendingChange as pc where pc.status is null and pc.oid = :objectID",
                    [objectID: "${this.class.name}:${this.id}"] )
            openPD.each { pc ->
                if (pc.payload) {
                    def payload = JSON.parse(pc.payload)
                    if (payload.changeDoc) {
                        def scp = genericOIDService.resolveOID(payload.changeDoc.OID)
                        if (scp?.id == id) {
                            pc.delete(flush:true)
                        }
                    }
                }
            }
        }
    }
}
