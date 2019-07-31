package com.k_int.kbplus

import com.k_int.kbplus.auth.Role
import com.k_int.properties.PropertyDefinitionGroup
import com.k_int.properties.PropertyDefinitionGroupBinding
import de.laser.helper.RDStore
import de.laser.helper.RefdataAnnotation
import de.laser.interfaces.DeleteFlag
import de.laser.domain.AbstractBaseDomain
import de.laser.interfaces.Permissions
import de.laser.interfaces.ShareSupport
import de.laser.interfaces.TemplateSupport
import de.laser.traits.AuditableTrait
import de.laser.traits.ShareableTrait

import javax.persistence.Transient
import java.text.Normalizer
import com.k_int.properties.PropertyDefinition
import com.k_int.ClassUtils

class License
        extends AbstractBaseDomain
        implements TemplateSupport, Permissions, ShareSupport, Comparable<License>,
                AuditableTrait {

    @Transient
    def grailsApplication
    @Transient
    def contextService
    @Transient
    def accessService
    @Transient
    def genericOIDService
    @Transient
    def messageSource
    @Transient
    def pendingChangeService
    @Transient
    def changeNotificationService


    // AuditableTrait
    static auditable            = [ ignore: ['version', 'lastUpdated', 'pendingChanges'] ]
    static controlledProperties = [ 'startDate', 'endDate', 'licenseUrl', 'status', 'type' ]

    License instanceOf

    // If a license is slaved then any changes to instanceOf will automatically be applied to this license
    @RefdataAnnotation(cat = 'YN')
    RefdataValue isSlaved

    @RefdataAnnotation(cat = 'License Status')
    RefdataValue status

    @RefdataAnnotation(cat = 'License Type')
    RefdataValue type

    @RefdataAnnotation(cat = 'LicenseCategory')
    RefdataValue licenseCategory

    @RefdataAnnotation(cat = 'YN')
    RefdataValue isPublic

  String reference
  String sortableReference

  String noticePeriod
  String licenseUrl
  String licenseType
  //String licenseStatus
  String impId

  long lastmod
  Date startDate
  Date endDate

  Date dateCreated
  Date lastUpdated

  static hasOne = [onixplLicense: OnixplLicense]

  static hasMany = [
          ids: IdentifierOccurrence,
          pkgs:         Package,
          subscriptions:Subscription,
          documents:    DocContext,
          orgLinks:     OrgRole,
          prsLinks:     PersonRole,
          derivedLicenses:    License,
          pendingChanges:     PendingChange,
          customProperties:   LicenseCustomProperty,
          privateProperties:  LicensePrivateProperty
  ]

  static mappedBy = [
          ids:           'lic',
          pkgs:          'license',
          subscriptions: 'owner',
          documents:     'license',
          orgLinks:      'lic',
          prsLinks:      'lic',
          derivedLicenses: 'instanceOf',
          pendingChanges:  'license',
          customProperties:  'owner',
          privateProperties: 'owner'
  ]

  static mapping = {
                    sort sortableReference: 'asc'
                     id column:'lic_id'
                version column:'lic_version'
              globalUID column:'lic_guid'
                 status column:'lic_status_rv_fk'
                   type column:'lic_type_rv_fk'
              reference column:'lic_ref'
      sortableReference column:'lic_sortable_ref'
               isPublic column:'lic_is_public_rdv_fk'
           noticePeriod column:'lic_notice_period'
             licenseUrl column:'lic_license_url'
             instanceOf column:'lic_parent_lic_fk', index:'lic_parent_idx'
               isSlaved column:'lic_is_slaved'
            licenseType column:'lic_license_type_str'
          //licenseStatus column:'lic_license_status_str'
                lastmod column:'lic_lastmod'
              documents sort:'owner.id', order:'desc', batchSize: 10
          onixplLicense column: 'lic_opl_fk'
        licenseCategory column: 'lic_category_rdv_fk'
              startDate column: 'lic_start_date'
                endDate column: 'lic_end_date'
       customProperties sort:'type', order:'desc', batchSize: 10
      privateProperties sort:'type', order:'desc', batchSize: 10
         pendingChanges sort: 'ts', order: 'asc', batchSize: 10

              ids               batchSize: 10
              pkgs              batchSize: 10
              subscriptions     batchSize: 10
              orgLinks          batchSize: 10
              prsLinks          batchSize: 10
              derivedLicenses   batchSize: 10
  }

    static constraints = {
        globalUID(nullable:true, blank:false, unique:true, maxSize:255)
        status(nullable:false, blank:false)
        type(nullable:true, blank:false)
        impId(nullable:true, blank:false)
        reference(nullable:false, blank:false)
        sortableReference(nullable:true, blank:true) // !! because otherwise, the beforeInsert() method which generates a value is not executed
        isPublic(nullable:true, blank:true)
        noticePeriod(nullable:true, blank:true)
        licenseUrl(nullable:true, blank:true)
        instanceOf(nullable:true, blank:false)
        isSlaved(nullable:true, blank:false)
        licenseType(nullable:true, blank:true)
        //licenseStatus(nullable:true, blank:true)
        lastmod(nullable:true, blank:true)
        onixplLicense(nullable: true, blank: true)
        licenseCategory(nullable: true, blank: true)
        startDate(nullable: true, blank: false, validator: { val, obj ->
            if(obj.startDate != null && obj.endDate != null) {
                if(obj.startDate > obj.endDate) return ['startDateAfterEndDate']
            }
        })
        endDate(nullable: true, blank: false, validator: { val, obj ->
            if(obj.startDate != null && obj.endDate != null) {
                if(obj.startDate > obj.endDate) return ['endDateBeforeStartDate']
            }
        })
        lastUpdated(nullable: true, blank: true)
    }

    @Override
    boolean isTemplate() {
        return (type != null) && (type == RefdataValue.getByValueAndCategory('Template', 'License Type'))
    }

    @Override
    boolean hasTemplate() {
        return instanceOf ? instanceOf.isTemplate() : false
    }

    @Override
    boolean checkSharePreconditions(ShareableTrait sharedObject) {
        // needed to differentiate OrgRoles
        if (sharedObject instanceof OrgRole) {
            if (showUIShareButton() && sharedObject.roleType.value == 'Licensor') {
                return true
            }
        }
        false
    }

    boolean showUIShareButton() {
        getCalculatedType() == TemplateSupport.CALCULATED_TYPE_CONSORTIAL
    }

    void updateShare(ShareableTrait sharedObject) {
        log.debug('updateShare: ' + sharedObject)

        if (sharedObject instanceof DocContext || sharedObject instanceof OrgRole) {
            if (sharedObject.isShared) {
                List<License> newTargets = License.findAllByInstanceOfAndStatusNotEqual(this, RDStore.LICENSE_DELETED)
                log.debug('found targets: ' + newTargets)

                newTargets.each{ lic ->
                    log.debug('adding for: ' + lic)
                    sharedObject.addShareForTarget_trait(lic)
                }
            }
            else {
                sharedObject.deleteShare_trait()
            }
        }
    }

    void syncAllShares(List<ShareSupport> targets) {
        log.debug('synAllShares: ' + targets)

        documents.each{ sharedObject ->
            targets.each{ lic ->
                if (sharedObject.isShared) {
                    log.debug('adding for: ' + lic)
                    sharedObject.addShareForTarget_trait(lic)
                }
                else {
                    log.debug('deleting all shares')
                    sharedObject.deleteShare_trait()
                }
            }
        }
        orgLinks.each{ sharedObject ->
            targets.each{ sub ->
                if (sharedObject.isShared) {
                    log.debug('adding for: ' + sub)
                    sharedObject.addShareForTarget_trait(sub)
                }
                else {
                    log.debug('deleting all shares')
                    sharedObject.deleteShare_trait()
                }
            }
        }
    }

    @Override
    String getCalculatedType() {
        def result = TemplateSupport.CALCULATED_TYPE_UNKOWN

        if (isTemplate()) {
            result = TemplateSupport.CALCULATED_TYPE_TEMPLATE
        }
        else if(getLicensingConsortium() && ! getAllLicensee() && ! isTemplate()) {
            result = TemplateSupport.CALCULATED_TYPE_CONSORTIAL
        }
        else if(getLicensingConsortium() /*&& getAllLicensee()*/ && instanceOf && ! hasTemplate()) {
            // current and deleted member licenses
            result = TemplateSupport.CALCULATED_TYPE_PARTICIPATION
        }
        else if(! getLicensingConsortium() && getAllLicensee() && ! isTemplate()) {
            result = TemplateSupport.CALCULATED_TYPE_LOCAL
        }
        result
    }

    def getDerivedLicensees() {
        def result = []

        License.findAllByInstanceOf(this).each { l ->
            def ors = OrgRole.findAllWhere( lic: l )
            ors.each { or ->
                if (or.roleType?.value in ['Licensee', 'Licensee_Consortial']) {
                    result << or.org
                }
            }
        }
        result = result.sort {it.name}
    }

    // used for views and dropdowns
    def getReferenceConcatenated() {
        def cons = getLicensingConsortium()
        def subscr = getAllLicensee()
        if (subscr) {
            "${reference} (" + subscr.join(', ') + ")"
        }
        else if (cons){
            "${reference} (${cons})"
        }
        else {
            reference
        }
    }

    def getLicensingConsortium() {
        def result = null;
        orgLinks.each { or ->
            if ( or?.roleType?.value in ['Licensing Consortium'] )
                result = or.org;
            }
        result
    }

    def getLicensor() {
        def result = null;
        orgLinks.each { or ->
            if ( or?.roleType?.value in ['Licensor'] )
                result = or.org;
        }
        result
    }

    def getLicensee() {
        def result = null;
        orgLinks.each { or ->
            if ( or?.roleType?.value in ['Licensee', 'Licensee_Consortial'] )
                result = or.org;
        }
        result
    }
    def getAllLicensee() {
        def result = [];
        orgLinks.each { or ->
            if ( or?.roleType?.value in ['Licensee', 'Licensee_Consortial'] )
                result << or.org
        }
        result
  }

  @Transient
  def getLicenseType() {
    return type?.value
  }

  def getNote(domain) {
    def note = DocContext.findByLicenseAndDomain(this, domain)
    note
  }

  def setNote(domain, note_content) {
    def note = DocContext.findByLicenseAndDomain(this, domain)
    if ( note ) {
      log.debug("update existing note...");
      if ( note_content == '' ) {
        log.debug("Delete note doc ctx...");
        note.delete();
        note.owner.delete(flush:true);
      }
      else {
        note.owner.content = note_content
        note.owner.save(flush:true);
      }
    }
    else {
      log.debug("Create new note...");
      if ( ( note_content ) && ( note_content.trim().length() > 0 ) ) {
        def doc = new Doc(content:note_content, lastUpdated:new Date(), dateCreated: new Date())
        def newctx = new DocContext(license: this, owner: doc, domain:domain)
        doc.save();
        newctx.save(flush:true);
      }
    }
  }

  def getGenericLabel() {
    return reference
  }

    boolean isEditableBy(user) {
        hasPerm("edit", user)
    }

    boolean isVisibleBy(user) {
      hasPerm('view', user)
    }

    boolean hasPerm(perm, user) {
        if (perm == 'view' && this.isPublic?.value == 'Yes') {
            return true
        }
        def adm = Role.findByAuthority('ROLE_ADMIN')
        def yda = Role.findByAuthority('ROLE_YODA')

        if (user.getAuthorities().contains(adm) || user.getAuthorities().contains(yda)) {
            return true
        }

        if (user.getAuthorizedOrgsIds().contains(contextService.getOrg()?.id)) {

            OrgRole cons = OrgRole.findByLicAndOrgAndRoleType(
                    this, contextService.getOrg(), RDStore.OR_LICENSING_CONSORTIUM
            )
            OrgRole licseeCons = OrgRole.findByLicAndOrgAndRoleType(
                    this, contextService.getOrg(), RDStore.OR_LICENSEE_CONS
            )
            OrgRole licsee = OrgRole.findByLicAndOrgAndRoleType(
                    this, contextService.getOrg(), RDStore.OR_LICENSEE
            )

            if (perm == 'view') {
                return cons || licseeCons || licsee
            }
            if (perm == 'edit') {
                if(accessService.checkPermAffiliationX('ORG_INST,ORG_CONSORTIUM','INST_EDITOR','ROLE_ADMIN'))
                    return cons || licsee
            }
        }

        return false
    }

  @Override
  public boolean equals (Object o) {
    def obj = ClassUtils.deproxy(o)
    if (obj != null) {
      if ( obj instanceof License ) {
        return obj.id == id
      }
    }
    return false
  }

  @Override
  public String toString() {
    reference ? "${reference}" : "License ${id}"
  }
  
  @Override
  public int compareTo(License other){
      return other.id? other.id.compareTo(this.id) : -1
  }


    @Transient
    def notifyDependencies_trait(changeDocument) {
        log.debug("notifyDependencies_trait(${changeDocument})")

        def slavedPendingChanges = []
        // Find any licenses derived from this license
        // create a new pending change object
        //def derived_licenses = License.executeQuery('select l from License as l where exists ( select link from Link as link where link.toLic=l and link.fromLic=? )',this)
        def derived_licenses = getNonDeletedDerivedLicenses()

        derived_licenses.each { dl ->
            log.debug("Send pending change to ${dl.id}")

            def locale = org.springframework.context.i18n.LocaleContextHolder.getLocale()
            def description = messageSource.getMessage('default.accept.placeholder',null, locale)

            def definedType = 'text'
            if (this."${changeDocument.prop}" instanceof RefdataValue) {
                definedType = 'rdv'
            }
            else if (this."${changeDocument.prop}" instanceof Date) {
                definedType = 'date'
            }

            def msgParams = [
                    definedType,
                    "${changeDocument.prop}",
                    "${changeDocument.old}",
                    "${changeDocument.new}",
                    "${description}"
            ]

            def newPendingChange = changeNotificationService.registerPendingChange(
                        PendingChange.PROP_LICENSE,
                        dl,
                        dl.getLicensee(),
                              [
                                changeTarget:"com.k_int.kbplus.License:${dl.id}",
                                changeType:PendingChangeService.EVENT_PROPERTY_CHANGE,
                                changeDoc:changeDocument
                              ],
                        PendingChange.MSG_LI01,
                        msgParams,
                    "<b>${changeDocument.prop}</b> hat sich von <b>\"${changeDocument.oldLabel?:changeDocument.old}\"</b> zu <b>\"${changeDocument.newLabel?:changeDocument.new}\"</b> von der Vertragsvorlage geändert. " + description
            )

            if (newPendingChange && dl.isSlaved?.value == "Yes") {
                slavedPendingChanges << newPendingChange
            }
        }

        slavedPendingChanges.each { spc ->
            log.debug('autoAccept! performing: ' + spc)
            def user = null
            pendingChangeService.performAccept(spc.getId(), user)
        }
    }

    def getNonDeletedDerivedLicenses() {
        License.where{ instanceOf == this }
    }

    def getCalculatedPropDefGroups(Org contextOrg) {
        def result = [ 'global':[], 'local':[], 'member':[], 'fallback': true, 'orphanedProperties':[]]

        // ALL type depending groups without checking tenants or bindings
        def groups = PropertyDefinitionGroup.findAllByOwnerType(License.class.name)
        groups.each{ it ->

            // cons_members
            if (this.instanceOf && ! this.instanceOf.isTemplate()) {
                def binding = PropertyDefinitionGroupBinding.findByPropDefGroupAndLic(it, this.instanceOf)

                // global groups
                if (it.tenant == null) {
                    if (binding) {
                        result.member << [it, binding]
                    } else {
                        result.global << it
                    }
                }
                // consortium @ member; getting group by tenant and instanceOf.binding
                if (it.tenant?.id == contextOrg?.id) {
                    if (binding) {
                        result.member << [it, binding]
                    }
                }
                // licensee consortial; getting group by consortia and instanceOf.binding
                else if (it.tenant?.id == this.instanceOf.getLicensingConsortium()?.id) {
                    if (binding) {
                        result.member << [it, binding]
                    }
                }
            }
            // consortium or locals
            else {
                def binding = PropertyDefinitionGroupBinding.findByPropDefGroupAndLic(it, this)

                if (it.tenant == null || it.tenant?.id == contextOrg?.id) {
                    if (binding) {
                        result.local << [it, binding]
                    } else {
                        result.global << it
                    }
                }
            }
        }

        result.fallback = (result.global.size() == 0 && result.local.size() == 0 && result.member.size() == 0)

        // storing properties without groups

        def orph = customProperties.id

        result.global.each{ gl -> orph.removeAll(gl.getCurrentProperties(this).id) }
        result.local.each{ lc  -> orph.removeAll(lc[0].getCurrentProperties(this).id) }
        result.member.each{ m  -> orph.removeAll(m[0].getCurrentProperties(this).id) }

        result.orphanedProperties = LicenseCustomProperty.findAllByIdInList(orph)

        result
    }

    @Override
    def beforeInsert() {
         if ( reference != null && !sortableReference) {
            sortableReference = generateSortableReference(reference)
        }
        if (impId == null) {
            impId = java.util.UUID.randomUUID().toString();
        }
        super.beforeInsert()
    }

    @Override
    def beforeUpdate() {
        if ( reference != null && !sortableReference) {
            sortableReference = generateSortableReference(reference)
        }
        if (impId == null) {
            impId = java.util.UUID.randomUUID().toString();
        }
        super.beforeUpdate()
    }


  public static String generateSortableReference(String input_title) {
    def result=null
    if ( input_title ) {
      def s1 = Normalizer.normalize(input_title, Normalizer.Form.NFKD).trim().toLowerCase()
      s1 = s1.replaceFirst('^copy of ','')
      s1 = s1.replaceFirst('^the ','')
      s1 = s1.replaceFirst('^a ','')
      s1 = s1.replaceFirst('^der ','')
      result = s1.trim()
    }
    result
  }

  /*
    Following getter methods were introduced to avoid making too many changes when custom properties 
    were introduced.
  */
  @Transient
  def getConcurrentUserCount(){
    log.error("called cust prop with deprecated method.Call should be replaced")
    return getCustomPropByName("Concurrent Users")
  }
  
  @Transient
  def setConcurrentUserCount(newVal){
    log.error("called cust prop with deprecated method.Call should be replaced")
    setReferencePropertyAsCustProp("Concurrent Users",newVal)
  }

  @Transient
  def getConcurrentUsers(){
    log.error("called cust prop with deprecated method.Call should be replaced")
    return getCustomPropByName("Concurrent Access")
  }  
    @Transient
  def setConcurrentUsers(newVal){
    log.error("called cust prop with deprecated method.Call should be replaced")
    setReferencePropertyAsCustProp("Concurrent Access",newVal)
  }
  
  @Transient
  def getRemoteAccess(){
    log.error("called cust prop with deprecated method.Call should be replaced")
    return getCustomPropByName("Remote Access")
  }
  
  @Transient
  def setRemoteAccess(newVal){
    log.error("called cust prop with deprecated method.Call should be replaced")
    setReferencePropertyAsCustProp("Remote Access",newVal)
  }
  
  @Transient
  def getWalkinAccess(){
    log.error("called cust prop with deprecated method.Call should be replaced")
    return getCustomPropByName("Walk In Access")
  }
  
  @Transient
  def setWalkinAccess(newVal){
    log.error("called cust prop with deprecated method.Call should be replaced")
    setReferencePropertyAsCustProp("Walk In Access",newVal)
  }
  
  @Transient
  def getMultisiteAccess(){
    log.error("called cust prop with deprecated method.Call should be replaced")
    return getCustomPropByName("Multi Site Access")
  }
  
  @Transient
  def setMultisiteAccess(newVal){
    log.error("called cust prop with deprecated method.Call should be replaced")
    setReferencePropertyAsCustProp("Multi Site Access",newVal)
  }
  
  @Transient
  def getPartnersAccess(){
    log.error("called cust prop with deprecated method.Call should be replaced")
    return getCustomPropByName("Partners Access")
  }
  
  @Transient
  def setPartnersAccess(newVal){
    log.error("called cust prop with deprecated method.Call should be replaced")
    setReferencePropertyAsCustProp("Partners Access",newVal)
  }
 
  @Transient
  def getAlumniAccess(){
    log.error("called cust prop with deprecated method.Call should be replaced")
    return getCustomPropByName("Alumni Access")
  }
 
  @Transient
  def setAlumniAccess(newVal){
    log.error("called cust prop with deprecated method.Call should be replaced")
    setReferencePropertyAsCustProp("Alumni Access",newVal)
  }
  @Transient
  def getIll(){
    log.error("called cust prop with deprecated method.Call should be replaced")
    return getCustomPropByName("ILL - InterLibraryLoans")
  }

  @Transient
  def setIll(newVal){
    log.error("called cust prop with deprecated method.Call should be replaced")
    setReferencePropertyAsCustProp("ILL - InterLibraryLoans",newVal)
  }
  @Transient
  def getCoursepack(){
    log.error("called cust prop with deprecated method.Call should be replaced")
    return getCustomPropByName("Include In Coursepacks")
  }

  @Transient
  def setCoursepack(newVal){
    log.error("called cust prop with deprecated method.Call should be replaced")
    setReferencePropertyAsCustProp("Include In Coursepacks",newVal)
  }
  
  @Transient
  def getVle(){
    log.error("called cust prop with deprecated method.Call should be replaced")
    return getCustomPropByName("Include in VLE")
  }
  
  @Transient
  def setVle(newVal){
    log.error("called cust prop with deprecated method.Call should be replaced")
    setReferencePropertyAsCustProp("Include in VLE",newVal)
  }

  @Transient
  def getEnterprise(){
    log.error("called cust prop with deprecated method.Call should be replaced")
    return getCustomPropByName("Enterprise Access")
  }
  @Transient
  def setEnterprise(newVal){
    log.error("called cust prop with deprecated method.Call should be replaced")
    setReferencePropertyAsCustProp("Enterprise Access",newVal)

  }

  @Transient
  def getPca(){
    log.error("called cust prop with deprecated method.Call should be replaced")
    return getCustomPropByName("Post Cancellation Access Entitlement")
  }

  @Transient
  def setPca(newVal){
    log.error("called cust prop with deprecated method.Call should be replaced")
    setReferencePropertyAsCustProp("Post Cancellation Access Entitlement",newVal)
  }

  @Transient
  def setReferencePropertyAsCustProp(custPropName, newVal) {
    def custProp = getCustomPropByName(custPropName)
    if(custProp == null){
      def type = PropertyDefinition.findWhere(name: custPropName, tenant: null)
      custProp = PropertyDefinition.createGenericProperty(PropertyDefinition.CUSTOM_PROPERTY, this, type)
    }

    if ( newVal != null ) {
      custProp.refValue = genericOIDService.resolveOID(newVal)
    }
    else {
      custProp.refValue = null;
    }

    custProp.save()
   
  }

  
  @Transient
  def getCustomPropByName(name){
    return customProperties.find{it.type.name == name}    
  }

  static def refdataFind(params) {

      String INSTITUTIONAL_LICENSES_QUERY = """
 FROM License AS l WHERE
( exists ( SELECT ol FROM OrgRole AS ol WHERE ol.lic = l AND ol.org.id =(:orgId) AND ol.roleType.id IN (:orgRoles)) OR l.isPublic.id=(:publicS))
AND lower(l.reference) LIKE (:ref)
"""
      def result = []
      def ql

        // TODO: ugly select2 fallback
      def roleTypes = []
      if (params.'roleTypes[]') {
          params.'roleTypes[]'.each{ x -> roleTypes << x.toLong() }
      } else {
          roleTypes << params.roleType?.toLong()
      }

      ql = License.executeQuery("select l ${INSTITUTIONAL_LICENSES_QUERY}",
        [orgId: params.inst?.toLong(), orgRoles: roleTypes, publicS: params.isPublic?.toLong(), ref: "${params.q.toLowerCase()}"])


      if ( ql ) {
          ql.each { lic ->
              def type = lic.type?.value ?"(${lic.type.value})":""
              result.add([id:"${lic.reference}||${lic.id}",text:"${lic.reference}${type}"])
          }
      }
      result
  }

    def getBaseCopy() {

        def copy = new License(
                //globalUID: globalUID,
                status: status, // fk
                type: type, // fk
                reference: reference,
                sortableReference: sortableReference,
                licenseCategory: licenseCategory, // fk
                isPublic: isPublic,
                noticePeriod: noticePeriod,
                licenseUrl: licenseUrl,
                licenseType: licenseType,
                licenseStatus: licenseStatus,
                //impId: impId,
                //lastmod: lastmod,
                startDate: startDate,
                endDate: endDate,
                dateCreated: dateCreated,
                lastUpdated: lastUpdated,
                onixplLicense: onixplLicense // fk
        )

        copy
    }
}
