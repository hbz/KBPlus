package com.k_int.kbplus

import com.k_int.kbplus.auth.*
import de.laser.domain.BaseDomainComponent
import de.laser.domain.Permissions
import de.laser.domain.TemplateSupport

import javax.persistence.Transient

class Subscription extends BaseDomainComponent implements TemplateSupport, Permissions {

  static auditable = [ignore:['version','lastUpdated','pendingChanges']]

    @Transient
    def grailsApplication
    @Transient
    def contextService

  RefdataValue status
  RefdataValue type         // RefdataCatagory 'Subscription Type'

  String name
  String identifier
  String impId
  Date startDate
  Date endDate
  Date manualRenewalDate
  Date manualCancellationDate
  String cancellationAllowances

  Subscription instanceOf
  Subscription previousSubscription

  // If a subscription is slaved then any changes to instanceOf will automatically be applied to this subscription
  RefdataValue isSlaved // RefdataCategory 'YN'

  String noticePeriod
  Date dateCreated
  Date lastUpdated
  // Org vendor

  License owner
  SortedSet issueEntitlements
  RefdataValue isPublic     // RefdataCategory 'YN'
  Set ids = []

  static transients = [ 'subscriber', 'provider', 'consortia' ]


  static hasMany = [
                     ids: IdentifierOccurrence,
                     packages : SubscriptionPackage,
                     issueEntitlements: IssueEntitlement,
                     documents: DocContext,
                     orgRelations: OrgRole,
                     prsLinks: PersonRole,
                     derivedSubscriptions: Subscription,
                     pendingChanges: PendingChange,
                     customProperties: SubscriptionCustomProperty,
                     privateProperties: SubscriptionPrivateProperty,
                     costItems: CostItem]

  static mappedBy = [
                      ids: 'sub',
                      packages: 'subscription',
                      issueEntitlements: 'subscription',
                      documents: 'subscription',
                      orgRelations: 'sub',
                      prsLinks: 'sub',
                      derivedSubscriptions: 'instanceOf',
                      pendingChanges: 'subscription',
                      costItems: 'sub',
                      privateProperties: 'owner'
                      ]

    static mapping = {
        sort name: 'asc'
        id          column:'sub_id'
        version     column:'sub_version'
        globalUID   column:'sub_guid'
        status      column:'sub_status_rv_fk'
        type        column:'sub_type_rv_fk'
        owner       column:'sub_owner_license_fk'
        name        column:'sub_name'
        identifier  column:'sub_identifier'
        impId       column:'sub_imp_id', index:'sub_imp_id_idx'
        startDate   column:'sub_start_date'
        endDate     column:'sub_end_date'
        manualRenewalDate       column:'sub_manual_renewal_date'
        manualCancellationDate  column:'sub_manual_cancellation_date'
        instanceOf              column:'sub_parent_sub_fk'
        previousSubscription    column:'sub_previous_subscription_fk'
        isSlaved        column:'sub_is_slaved'
        noticePeriod    column:'sub_notice_period'
        isPublic        column:'sub_is_public'
        pendingChanges  sort: 'ts', order: 'asc'
    }

    static constraints = {
        globalUID(nullable:true, blank:false, unique:true, maxSize:255)
        status(nullable:true, blank:false)
        type(nullable:true, blank:false)
        owner(nullable:true, blank:false)
        impId(nullable:true, blank:false)
        startDate(nullable:true, blank:false)
        endDate(nullable:true, blank:false)
        manualRenewalDate(nullable:true, blank:false)
        manualCancellationDate(nullable:true, blank:false)
        instanceOf(nullable:true, blank:false)
        previousSubscription(nullable:true, blank:false)
        isSlaved(nullable:true, blank:false)
        noticePeriod(nullable:true, blank:true)
        isPublic(nullable:true, blank:true)
        customProperties(nullable:true)
        privateProperties(nullable:true)
        cancellationAllowances(nullable:true, blank:true)
        lastUpdated(nullable: true, blank: true)
        // vendor(nullable:true, blank:false)
    }

    // TODO: implement
    @Override
    def isTemplate() {
        return false
    }

    // TODO: implement
    @Override
    def hasTemplate() {
        return false
    }


  def getIsSlavedAsString() {
    isSlaved?.value == "Yes" ? "Yes" : "No"
  }

  def getSubscriber() {
    def result = null;
    def cons = null;
    
    orgRelations.each { or ->
      if ( or?.roleType?.value in ['Subscriber', 'Subscriber_Consortial'] )
        result = or.org;
        
      if ( or?.roleType?.value=='Subscription Consortia' )
        cons = or.org;
    }
    
    if ( !result && cons ) {
      result = cons
    }
    
    result
  }

  def getAllSubscribers() {
    def result = [];
    orgRelations.each { or ->
      if ( or?.roleType?.value in ['Subscriber', 'Subscriber_Consortial'] )
        result.add(or.org)
    }
    result
  }
  
  def getProvider() {
    def result = null;
    orgRelations.each { or ->
      if ( or?.roleType?.value=='Content Provider' )
        result = or.org;
    }
    result
  }

    def getConsortia() {
        def result = null;
        orgRelations.each { or ->
            if ( or?.roleType?.value=='Subscription Consortia' )
                result = or.org
        }
        result
    }

    def getDerivedSubscribers() {
        def result = []

        Subscription.findAllByInstanceOf(this).each { s ->
            def ors = OrgRole.findAllWhere( sub: s )
            ors.each { or ->
                if (or.roleType?.value in ['Subscriber', 'Subscriber_Consortial']) {
                    result << or.org
                }
            }
        }
        result = result.sort {it.name}
    }

    def isEditableBy(user) {
        hasPerm('edit', user)
    }

    def isVisibleBy(user) {
        hasPerm('view', user)
    }

    def hasPerm(perm, user) {
        if (perm == 'view' && this.isPublic?.value == 'Yes') {
            return true
        }
        def adm = Role.findByAuthority('ROLE_ADMIN')
        def yda = Role.findByAuthority('ROLE_YODA')

        if (user.getAuthorities().contains(adm) || user.getAuthorities().contains(yda)) {
            return true
        }

        return checkPermissionsNew(perm, user)
    }

    def checkPermissionsNew(perm, user) {

        def principles = user.listPrincipalsGrantingPermission(perm) // UserOrgs with perm

        log.debug("The target list of principles : ${principles}")

        Set object_orgs = new HashSet()

        OrgRole.findAllBySubAndOrg(this, contextService.getOrg()).each { or ->
            def perm_exists = false
            or.roleType?.sharedPermissions.each { sp ->
                if (sp.perm.code == perm)
                    perm_exists = true
            }
            if (perm_exists) {
                log.debug("Looks like org ${or.org} has perm ${perm} shared with it .. so add to list")
                object_orgs.add("${or.org.id}:${perm}")
            }
        }

        log.debug("After analysis, the following relevant org_permissions were located ${object_orgs}, user has the following orgs for that perm ${principles}")

        def intersection = principles.retainAll(object_orgs)
        log.debug("intersection is ${principles}")

        return (principles.size() > 0)
    }

  def checkPermissions(perm, user) {
    def result = false
    def principles = user.listPrincipalsGrantingPermission(perm);   // This will list all the orgs and people granted the given perm
    log.debug("The target list of principles : ${principles}");

    // Now we need to see if we can find a path from this object to any of those resources... Any of these orgs can edit

    // If this is a concrete license, the owner is the
    // If it's a template, the owner is the consortia that negoited
    // def owning org list
    // We're looking for all org links that grant a role with the corresponding edit property.
    Set object_orgs = new HashSet();
    orgRelations.each { ol ->
      def perm_exists=false
      ol.roleType?.sharedPermissions.each { sp ->
        if ( sp.perm.code==perm )
          perm_exists=true;
      }
      if ( perm_exists ) {
        log.debug("Looks like org ${ol.org} has perm ${perm} shared with it.. so add to list")
        object_orgs.add("${ol.org.id}:${perm}")
      }
    }

    log.debug("After analysis, the following relevant org_permissions were located ${object_orgs}, user has the following orgs for that perm ${principles}")

    // Now find the intersection
    def intersection = principles.retainAll(object_orgs)

    log.debug("intersection is ${principles}")

    if ( principles.size() > 0 )
      result = true

    result
  }

  def onChange = { oldMap,newMap ->

    log.debug("onChange")

    def changeNotificationService = grailsApplication.mainContext.getBean("changeNotificationService")
    def controlledProperties = ['name','startDate','endDate']

    controlledProperties.each { cp ->
      if ( oldMap[cp] != newMap[cp] ) {
        //changeNotificationService.notifySubscriptionChange(this.id, cp, oldMap[cp], newMap[cp], null, 'S');
        changeNotificationService.notifyChangeEvent([
                                                     OID:"${this.class.name}:${this.id}",
                                                     event:'Subscription.updated',
                                                     prop:cp,
                                                     old:oldMap[cp],
                                                     new:newMap[cp]
                                                    ])
      }
    }
  }

  @Override
  def beforeInsert() {
    if (impId == null) {
      impId = java.util.UUID.randomUUID().toString();
    }
    super.beforeInsert()
  }

  @Transient
  def notifyDependencies(changeDocument) {
    log.debug("notifyDependencies(${changeDocument})");
  }

  public String toString() {
      name ? "${name}" : "Subscription ${id}"
  }

  // JSON definition of the subscription object
  // see http://manbuildswebsite.com/2010/02/15/rendering-json-in-grails-part-3-customise-your-json-with-object-marshallers/
  // Also http://jwicz.wordpress.com/2011/07/11/grails-custom-xml-marshaller/
  // Also http://lucy-michael.klenk.ch/index.php/informatik/grails/c/
  static {
    grails.converters.JSON.registerObjectMarshaller(User) {
      // you can filter here the key-value pairs to output:
      return it.properties.findAll {k,v -> k != 'passwd'}
    }
  }

  // XML.registerObjectMarshaller Facility, { facility, xml ->
  //    xml.attribute 'id', facility.id
  //               xml.build {
  //      name(facility.name)
  //    }
  //  }

  public Date getDerivedAccessStartDate() {
    startDate ? startDate : null
  }

  public Date getDerivedAccessEndDate() {
    endDate ? endDate : null
  }

  public Date getRenewalDate() {
    manualRenewalDate ? manualRenewalDate : null
  }
  /**
  * OPTIONS: startDate, endDate, hideIdent, inclSubStartDate, hideDeleted, accessibleToUser,inst_shortcode
  **/
  @Transient
  static def refdataFind(params) {
    def result = [];

    def hqlString = "select sub from Subscription sub where lower(sub.name) like :name "
    def hqlParams = [name: ((params.q ? params.q.toLowerCase() : '' ) + "%")]
    def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd")
    def cons_role = RefdataCategory.lookupOrCreate('Organisational Role', 'Subscription Consortia');
    def subscr_role = RefdataCategory.lookupOrCreate('Organisational Role', 'Subscriber');
    def subscr_cons_role = RefdataCategory.lookupOrCreate('Organisational Role', 'Subscriber_Consortial');
    def viableRoles = [cons_role, subscr_role, subscr_cons_role]
    
    hqlParams.put('viableRoles', viableRoles)

    if(params.hasDate ){

      def startDate = params.startDate.length() > 1 ? sdf.parse(params.startDate) : null
      def endDate = params.endDate.length() > 1 ? sdf.parse(params.endDate)  : null

      if(startDate){
          hqlString += " AND sub.startDate >= :startDate "
          hqlParams.put('startDate', startDate)
      }
      if(endDate){
        hqlString += " AND sub.endDate <= :endDate "
        hqlParams.put('endDate', endDate)
        }
    }

    if(params.hideDeleted == 'true'){
      hqlString += " AND sub.status.value != 'Deleted' "
    }

    if(params.inst_shortcode && params.inst_shortcode.length() > 1){
      hqlString += " AND exists ( select orgs from sub.orgRelations orgs where orgs.org.shortcode = :inst AND orgs.roleType IN (:viableRoles) ) "
      hqlParams.put('inst', params.inst_shortcode)
    }


    def results = Subscription.executeQuery(hqlString,hqlParams)

    if(params.accessibleToUser){
      for(int i=0;i<results.size();i++){
        if(! results.get(i).checkPermissionsNew("view",User.get(params.accessibleToUser))){
          results.remove(i)
        }
      }
    }

    results?.each { t ->
      def resultText = t.name
      def date = t.startDate ? " (${sdf.format(t.startDate)})" : ""
      resultText = params.inclSubStartDate == "true"? resultText + date : resultText
      resultText = params.hideIdent == "true"? resultText : resultText + " (${t.identifier})"
      result.add([id:"${t.class.name}:${t.id}",text:resultText])
    }

    result
  }

  def setInstitution(inst) {
    println("Set institution ${inst}");
    def subrole = RefdataCategory.lookupOrCreate('Organisational Role', 'Subscriber');
    def or = new OrgRole(org:inst, roleType:subrole, sub:this)
    if ( this.orgRelations == null)
      this.orgRelations = []
    this.orgRelations.add(or)
  }

  def addNamespacedIdentifier(ns,value) {
    println("Add Namespaced identifier ${ns}:${value}");
    def canonical_id = Identifier.lookupOrCreateCanonicalIdentifier(ns, value);
    if ( this.ids == null)
      this.ids = []
    this.ids.add(new IdentifierOccurrence(sub:this,identifier:canonical_id))

  }

  def getCommaSeperatedPackagesIsilList() {
      def result = []
      packages.each { it ->
          def identifierValue = it.pkg.getIdentifierByType('isil')?.value ?: null
          if (identifierValue) {
              result += identifierValue
          }
      }
      result.join(',')
  }

  def hasOrgWithUsageSupplierId() {
      def hasUsageSupplier = false
      packages.each { it ->
          def hql = "select count(orole.id) from OrgRole as orole "+
                  "join orole.pkg as pa "+
                  "join orole.roleType as roletype "+
                  "where pa.id = :package_id and roletype.value='Content Provider' "+
                  "and exists (select oid from orole.org.ids as oid where oid.identifier.ns.ns='statssid')"
          def queryResult = OrgRole.executeQuery(hql, ['package_id':it.pkg.id])
          if (queryResult[0] > 0){
              hasUsageSupplier = true
          }
      }
      return hasUsageSupplier
  }

}
