package com.k_int.kbplus

import au.com.bytecode.opencsv.CSVReader
import com.k_int.kbplus.auth.Role
import com.k_int.kbplus.auth.User
import com.k_int.kbplus.auth.UserOrg
import com.k_int.kbplus.auth.UserRole
import com.k_int.properties.PropertyDefinition
import com.k_int.properties.PropertyDefinitionGroup
import com.k_int.properties.PropertyDefinitionGroupItem
import de.laser.SystemEvent
import de.laser.api.v0.ApiToolkit
import de.laser.controller.AbstractDebugController
import de.laser.helper.DebugAnnotation
import de.laser.helper.RDStore
import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured

@Secured(['IS_AUTHENTICATED_FULLY'])
class AdminController extends AbstractDebugController {

  def springSecurityService
  def dataloadService
  def statsSyncService
  def globalSourceSyncService
  def messageService
  def changeNotificationService
  def enrichmentService
  def sessionFactory
    def genericOIDService
    def deletionService

    def contextService
    def refdataService
    def propertyService
    def dataConsistencyService
    def organisationService

  def docstoreService
  def propertyInstanceMap = org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin.PROPERTY_INSTANCE_MAP
  def executorService
  def ESSearchService
  def GOKbService

  @Secured(['ROLE_ADMIN'])
  def index() { }

    @DebugAnnotation(test = 'hasRole("ROLE_ADMIN") || hasAffiliation("INST_ADM")')
    @Secured(closure = {
        ctx.springSecurityService.getCurrentUser()?.hasRole('ROLE_ADMIN') ||
            ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_ADM")
    })
    def manageAffiliationRequests() {

        def result = [:]
        result.user = User.get(springSecurityService.principal.id)
        result << organisationService.getPendingRequests(result.user, contextService.getOrg())

        result
    }

  @Secured(['ROLE_ADMIN'])
  def updatePendingChanges() {
  //Find all pending changes with license FK and timestamp after summer 14
  // For those with changeType: CustomPropertyChange, change it to PropertyChange
  // on changeDoc add value propertyOID with the value of OID
    String theDate = "01/05/2014 00:00:00";
    def summer_date = new Date().parse("d/M/yyyy H:m:s", theDate)
    def criteria = PendingChange.createCriteria()
    def changes = criteria.list{
      isNotNull("license")
      ge("ts",summer_date)
      like("changeDoc","%changeType\":\"CustomPropertyChange\",%")
    }
    log.debug("Starting PendingChange Update. Found:${changes.size()}")

    changes.each{
        def parsed_change_info = JSON.parse(it.changeDoc)
        parsed_change_info.changeType = "PropertyChange"
        //parsed_change_info.changeDoc.propertyOID = parsed_change_info.changeDoc.OID
        it.changeDoc = parsed_change_info
        it.save(failOnError:true)
    }
    log.debug("Pending Change Update Complete.")
    redirect(controller:'home')

  }

  @Secured(['ROLE_ADMIN'])
  def actionAffiliationRequest() {
    log.debug("actionMembershipRequest");
    def req = UserOrg.get(params.req);
    def user = User.get(springSecurityService.principal.id)
    if ( req != null ) {
      switch(params.act) {
        case 'approve':
          req.status = UserOrg.STATUS_APPROVED
          break;
        case 'deny':
          req.status = UserOrg.STATUS_REJECTED
          break;
        default:
          log.error("FLASH UNKNOWN CODE");
          break;
      }
      // req.actionedBy = user
      req.dateActioned = System.currentTimeMillis();
      req.save(flush:true);
    }
    else {
      log.error("FLASH");
    }
    redirect(action: "manageAffiliationRequests")
  }

  @Secured(['ROLE_ADMIN'])
  def hardDeletePkgs(){
    def result = [:]
    //If we make a search while paginating return to start
    if(params.search == "yes"){
        params.offset = 0
        params.search = null
    }
    result.user = User.get(springSecurityService.principal.id)
    result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
    result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

    if(params.id){
      def pkg = Package.get(params.id)
      def conflicts_list = []
      if(pkg.documents){
        def document_map = [:]
        document_map.name = "Documents"
        document_map.details = []
        pkg.documents.each{
          document_map.details += ['text':it.owner.title]
        }
        document_map.action = ['actionRequired':false,'text':"References will be deleted"]
        conflicts_list += document_map
      }
      if(pkg.subscriptions){
        def subscription_map = [:]
        subscription_map.name = "Subscriptions"
        subscription_map.details = []
        pkg.subscriptions.each{
          subscription_map.details += ['link':createLink(controller:'subscription', action: 'show', id:it.subscription.id), 'text': it.subscription.name]
        }
        subscription_map.action = ['actionRequired':true,'text':"Unlink subscriptions. (IEs will be removed as well)"]
        if(subscription_map.details){
          conflicts_list += subscription_map
        }
      }
      if(pkg.tipps){
        def tipp_map = [:]
        tipp_map.name = "TIPPs"
        def totalIE = 0
        pkg.tipps.each{
          totalIE += IssueEntitlement.countByTipp(it)
        }
        tipp_map.details = [['text':"Number of TIPPs: ${pkg.tipps.size()}"],
                ['text':"Number of IEs: ${totalIE}"]]
        tipp_map.action = ['actionRequired':false,'text':"TIPPs and IEs will be deleted"]
        conflicts_list += tipp_map
      }
      result.conflicts_list = conflicts_list
      result.pkg = pkg

      render(template: "hardDeleteDetails",model:result)
    }else{

      def criteria = Package.createCriteria()
      result.pkgs = criteria.list(max: result.max, offset:result.offset){
          if(params.pkg_name){
            ilike("name","${params.pkg_name}%")
          }
          order("name", params.order?:'asc')
      }
    }

    result
  }

  @Secured(['ROLE_ADMIN'])
  def performPackageDelete(){
   if (request.method == 'POST'){
      def pkg = Package.get(params.id)
      Package.withTransaction { status ->
        log.info("Deleting Package ")
        log.info("${pkg.id}::${pkg}")
        pkg.pendingChanges.each{
          it.delete()
        }
        pkg.documents.each{
          it.delete()
        }
        pkg.orgs.each{
          it.delete()
        }

        pkg.subscriptions.each{
          it.delete()
        }
        pkg.tipps.each{
          it.delete()
        }
        pkg.delete()
      }
      log.info("Delete Complete.")
   }
   redirect controller: 'admin', action:'hardDeletePkgs'

  }

  @Secured(['ROLE_ADMIN'])
  def userMerge(){
     log.debug("AdminController :: userMerge :: ${params}");
     def usrMrgId = params.userToMerge == "null"?null:params.userToMerge
     def usrKeepId = params.userToKeep == "null"?null:params.userToKeep
     def result = [:]
     try {
       log.debug("Determine user merge operation : ${request.method}");
       switch (request.method) {
         case 'GET':
           if(usrMrgId && usrKeepId ){
             def usrMrg = User.get(usrMrgId)
             def usrKeep =  User.get(usrKeepId)
             log.debug("Selected users : ${usrMrg}, ${usrKeep}");
             result.userRoles = usrMrg.getAuthorities()
             result.userAffiliations =  usrMrg.getAuthorizedAffiliations()
             result.userMerge = usrMrg
             result.userKeep = usrKeep
           }else{
            log.error("Missing keep/merge userid ${params}");
            flash.error = "Please select'user to keep' and 'user to merge' from the dropdown."
           }
           log.debug("Get processing completed");
           break;
         case 'POST':
           log.debug("Post...");
           if(usrMrgId && usrKeepId){
             def usrMrg = User.get(usrMrgId)
             def usrKeep =  User.get(usrKeepId)
             def success = false
             try{
               log.debug("Copying user roles... from ${usrMrg} to ${usrKeep}");
               success = copyUserRoles(usrMrg, usrKeep)
               log.debug("Result of copyUserRoles : ${success}");
             }catch(Exception e){
              log.error("Exception while copying user roles.",e)
             }
             if(success){
               log.debug("Success");
               usrMrg.enabled = false
               log.debug("Save disable and save merged user");
               usrMrg.save(flush:true,failOnError:true)
               flash.message = "Rights copying successful. User '${usrMrg.displayName}' is now disabled."
             }else{
               flash.error = "An error occured before rights transfer was complete."
             }
           }else{
               flash.error = "Please select 'user to keep' and 'user to merge' from the dropdown."
           }
           break
         default:
           break;
       }

       log.debug("Get all users");
       result.usersAll = User.list(sort:"display", order:"asc")
       log.debug("Get active users");
       def activeHQL = " from User as usr where usr.enabled=true or usr.enabled=null order by display asc"
       result.usersActive = User.executeQuery(activeHQL)
    }
    catch ( Exception e ) {
      log.error("Problem in user merge",e);
    }

    log.debug("Returning ${result}");
    result
  }

    @Secured(['ROLE_ADMIN'])
  def copyUserRoles(usrMrg, usrKeep){
    def mergeRoles = usrMrg.getAuthorities()
    def mergeAffil = usrMrg.getAuthorizedAffiliations()
    def currentRoles = usrKeep.getAuthorities()
    def currentAffil = usrKeep.getAuthorizedAffiliations()

    mergeRoles.each{ role ->

        if (!currentRoles.contains(role) && role.authority != "ROLE_YODA") {
        UserRole.create(usrKeep,role)
      }
    }
    mergeAffil.each{affil ->
      if(!currentAffil.contains(affil)){

        // We should check that the new role does not already exist
        def existing_affil_check = UserOrg.findByOrgAndUserAndFormalRole(affil.org,usrKeep,affil.formalRole);

        if ( existing_affil_check == null ) {
          log.debug("No existing affiliation");
          def newAffil = new UserOrg(org:affil.org,user:usrKeep,formalRole:affil.formalRole,status:affil.status)
          if(!newAffil.save(flush:true,failOnError:true)){
            log.error("Probem saving user roles");
            newAffil.errors.each { e ->
              log.error(e);
            }
            return false
          }
        }
        else {
          if (affil.status != existing_affil_check.status) {
            existing_affil_check.status = affil.status
            existing_affil_check.save()
          }
          log.debug("Affiliation already present - skipping ${existing_affil_check}");
        }
      }
    }
    log.debug("copyUserRoles returning true");
    return true
  }

  @Secured(['ROLE_ADMIN'])
  def showAffiliations() {
    def result = [:]
    result.user = User.get(springSecurityService.principal.id)
    result.users = User.list()

    withFormat {
      html {
        render(view:'showAffiliations',model:result)
      }
      json {
        def r2 = []
        result.users.each { u ->
          def row = [:]
          row.username = u.username
          row.display = u.display
          row.email = u.email
          row.shibbScope = u.shibbScope
          row.enabled = u.enabled
          row.accountExpired = u.accountExpired
          row.accountLocked = u.accountLocked
          row.passwordExpired = u.passwordExpired
          row.affiliations = []
          u.affiliations.each { ua ->
            row.affiliations.add( [org: ua.org.shortcode, status: ua.status, formalRole:formalRole?.authority] )
          }
          r2.add(row)
        }
        render r2 as JSON
      }
    }
  }

  @Secured(['ROLE_ADMIN'])
  def eventLog() {
    def result = [:]

    params.sort = 'tstp'
    params.order = 'desc'

    result.eventlogs = EventLog.list(params)

    result
  }

    @Secured(['ROLE_ADMIN'])
    def systemEvents() {
        def result = [:]

        params.sort =   params.sort ?: 'created'
        params.order =  params.order ?: 'desc'
        params.max =    params.max ?: 1000

        result.events = SystemEvent.list(params)

        result
    }

  @Secured(['ROLE_YODA'])
  def dataCleanse() {
    // Sets nominal platform
    dataloadService.dataCleanse()
  }

  @Secured(['ROLE_YODA'])
  def titleAugment() {
    // Sets nominal platform
    dataloadService.titleAugment()
  }

  @Secured(['ROLE_ADMIN'])
  def licenseLink() {
    if ( ( params.sub_identifier ) && ( params.lic_reference.length() > 0 ) ) {
    }
  }

  @Secured(['ROLE_ADMIN'])
  def statsSync() {
    log.debug("statsSync()")
    statsSyncService.doSync()
    redirect(controller:'home')
  }

  @Secured(['ROLE_ADMIN'])
  def manageContentItems() {
    def result=[:]

    result.items = ContentItem.list()

    result
  }

    @Secured(['ROLE_ADMIN'])
    def dataConsistency() {
        Map result = [:]

        if (params.task) {
			List objIds = params.list('objId')

          	if (params.task == 'merge' && params.objType == 'Org') {
            	log.debug('dataConsistency( merge, ' + params.objType + ', ' + objIds + ' )')

                Org replacement = Org.get(objIds.first())
                for (def i = 1; i < objIds.size(); i++) {
                    deletionService.deleteOrganisation( Org.get(objIds[i]), replacement, false )
                }
          	}
          	if (params.task == 'delete' && params.objType == 'Org') {
            	log.debug('dataConsistency( delete, ' + params.objType + ', ' + objIds + ' )')

                for (def i = 0; i < objIds.size(); i++) {
                    deletionService.deleteOrganisation( Org.get(objIds[i]), null, false )
                }
          	}
            params.remove('task')
            params.remove('objType')
            params.remove('objId')

            redirect controller: 'admin', action: 'dataConsistency'
        }

        result.importIds = dataConsistencyService.checkImportIds()
        result.titles    = dataConsistencyService.checkTitles()

        result
    }

  @Secured(['ROLE_ADMIN'])
  def newContentItem() {
    def result=[:]
    if ( ( params.key != null ) && ( params.content != null ) && ( params.key.length() > 0 ) && ( params.content.length() > 0 ) ) {

      def locale = ( ( params.locale != null ) && ( params.locale.length() > 0 ) ) ? params.locale : ''

      if ( ContentItem.findByKeyAndLocale(params.key,locale) != null ) {
        flash.message = 'Content item already exists'
      }
      else {
        ContentItem.lookupOrCreate(params.key, locale, params.content)
      }
    }

    redirect(action:'manageContentItems')

    result
  }

  @Secured(['ROLE_ADMIN'])
  def editContentItem() {
    def result=[:]
    def idparts = params.id?.split(':')
    if ( idparts.length > 0 ) {
      def key = idparts[0]
      def locale = idparts.length > 1 ? idparts[1] : ''

      def contentItem = ContentItem.findByKeyAndLocale(key,locale)
      if ( contentItem != null ) {
        result.contentItem = contentItem
      }
      else {
        flash.message="Unable to locate content item for key ${idparts}"
        redirect(action:'manageContentItems');
      }
      if ( request.method.equalsIgnoreCase("post")) {
        contentItem.content = params.content
        contentItem.save(flush:true)
        messageService.update(key,locale)
        redirect(action:'manageContentItems');
      }
    }
    else {
      flash.message="Unable to parse content item id ${params.id} - ${idparts}"
      redirect(action:'manageContentItems');
    }

    result
  }

  @Secured(['ROLE_ADMIN'])
  def forceSendNotifications() {
    changeNotificationService.aggregateAndNotifyChanges()
    redirect(controller:'home')
  }

  @Secured(['ROLE_ADMIN'])
  def tippTransfer(){
    log.debug("tippTransfer :: ${params}")
    def result = [:]
    result.error = []

    if(params.sourceTIPP && params.targetTI){
      def ti = TitleInstance.get(params.long("targetTI"))
      def tipp = TitleInstancePackagePlatform.get(params.long("sourceTIPP"))
      if(ti && tipp){
        tipp.title = ti
        try{
          tipp.save(flush:true,failOnError:true)
          result.success = true
        }catch(Exception e){
          log.error(e)
          result.error += "An error occured while saving the changes."
        }
      }else{
        if(!ti) result.error += "No TitleInstance found with identifier: ${params.targetTI}."
        if(!tipp) result.error += "No TIPP found with identifier: ${params.sourceTIPP}" 
      }
    }

    result
  }

  @Secured(['ROLE_ADMIN'])
  def ieTransfer(){
    log.debug(params)
    def result = [:]
    if(params.sourceTIPP && params.targetTIPP){
      result.sourceTIPPObj = TitleInstancePackagePlatform.get(params.sourceTIPP)
      result.targetTIPPObj = TitleInstancePackagePlatform.get(params.targetTIPP)
    }

    if(params.transfer == "Go" && result.sourceTIPPObj && result.targetTIPPObj){
      log.debug("Tranfering ${IssueEntitlement.countByTipp(result.sourceTIPPObj)} IEs from ${result.sourceTIPPObj} to ${result.targetTIPPObj}")
      def sourceIEs = IssueEntitlement.findAllByTipp(result.sourceTIPPObj)
      sourceIEs.each{
        it.setTipp(result.targetTIPPObj)
        it.save()
      }
    }

    result
  }

  @Secured(['ROLE_ADMIN'])
  def titleMerge() {

    log.debug(params)

    def result=[:]

    if ( ( params.titleIdToDeprecate != null ) &&
         ( params.titleIdToDeprecate.length() > 0 ) &&
         ( params.correctTitleId != null ) &&
         ( params.correctTitleId.length() > 0 ) ) {
      result.title_to_deprecate = TitleInstance.get(params.titleIdToDeprecate)
      result.correct_title = TitleInstance.get(params.correctTitleId)

      if ( params.MergeButton=='Go' ) {
        log.debug("Execute title merge....");
        result.title_to_deprecate.tipps.each { tipp ->
          log.debug("Update tipp... ${tipp.id}");
          tipp.title = result.correct_title
          tipp.save()
        }
        redirect(action:'titleMerge',params:[titleIdToDeprecate:params.titleIdToDeprecate, correctTitleId:params.correctTitleId])
      }

      result.title_to_deprecate.status = RefdataValue.loc(RefdataCategory.TI_STATUS, [en: 'Deleted', de: 'Gelöscht'])
      result.title_to_deprecate.save(flush:true);
    }
    result
  }

  @Secured(['ROLE_ADMIN'])
  def orgsExport() {
    response.setHeader("Content-disposition", "attachment; filename=\"orgsExport.csv\"")
    response.contentType = "text/csv"
    def out = response.outputStream
    out << "org.name,sector,consortia,id.jusplogin,id.JC,id.Ringold,id.UKAMF,id.ISIL,iprange\n"
    Org.list().each { org ->
      def consortium = org.outgoingCombos.find{it.type.value=='Consortium'}.collect{it.toOrg.name}.join(':')

      out << "\"${org.name}\",\"${org.sector?:''}\",\"${consortium}\",\"${org.getIdentifierByType('jusplogin')?.value?:''}\",\"${org.getIdentifierByType('JC')?.value?:''}\",\"${org.getIdentifierByType('Ringold')?.value?:''}\",\"${org.getIdentifierByType('UKAMF')?.value?:''}\",\"${org.getIdentifierByType('ISIL')?.value?:''}\",\"${org.ipRange?:''}\"\n"
    }
    out.close()
  }

  @Secured(['ROLE_ADMIN'])
  def orgsImport() {

    if ( request.method=="POST" ) {
      def upload_mime_type = request.getFile("orgs_file")?.contentType
      def upload_filename = request.getFile("orgs_file")?.getOriginalFilename()
      def input_stream = request.getFile("orgs_file")?.inputStream

      CSVReader r = new CSVReader( new InputStreamReader(input_stream, java.nio.charset.Charset.forName('UTF-8') ) )
      String[] nl;
      def first = true
      while ((nl = r.readNext()) != null) {
        if ( first ) {
          first = false; // Skip header
        }
        else {
          
          def candidate_identifiers = [
            'jusplogin':nl[3],
            'JC':nl[4],
            'Ringold':nl[5],
            'UKAMF':nl[6],
            'ISIL':nl[7]
          ]
          log.debug("Load ${nl[0]}, ${nl[1]}, ${nl[2]} ${candidate_identifiers} ${nl[8]}");
          Org.lookupOrCreate(nl[0],
                             nl[1],
                             nl[2],
                             candidate_identifiers,
                             nl[8].replace('-', ','))
        }
      }
    }
  }

  @Secured(['ROLE_ADMIN'])
  @Deprecated
  def docstoreMigrate() {
    docstoreService.migrateToDb()
    redirect(controller:'home')
  }

  @Secured(['ROLE_YODA'])
  def triggerHousekeeping() {
    log.debug("trigggerHousekeeping()")
    enrichmentService.initiateHousekeeping()
    redirect(controller:'home')
  }

  @Secured(['ROLE_YODA'])
  def initiateCoreMigration() {
    log.debug("initiateCoreMigration...");
    enrichmentService.initiateCoreMigration()
    redirect(controller:'home')
  }

  @Secured(['ROLE_ADMIN'])
  def titlesImport() {

    if ( request.method=="POST" ) {
      def upload_mime_type = request.getFile("titles_file")?.contentType
      def upload_filename = request.getFile("titles_file")?.getOriginalFilename()
      def input_stream = request.getFile("titles_file")?.inputStream

      CSVReader r = new CSVReader( new InputStreamReader(input_stream, java.nio.charset.Charset.forName('UTF-8') ) )
      String[] nl;
      String[] cols;
      def first = true
      while ((nl = r.readNext()) != null) {
        if ( first ) {
          first = false; // Skip header
          cols=nl;

          // Make sure that there is at least one valid identifier column
        }
        else {
          def title = null;
          def bindvars = []
          // Set up base_query
          def q = "Select distinct(t) from TitleInstance as t "
          def joinclause = ''
          def whereclause = ' where ';
          def i = 0;
          def disjunction_ctr = 0;
          cols.each { cn ->
            if ( cn == 'title.id' ) {
              if ( disjunction_ctr++ > 0 ) { whereclause += ' OR ' }
              whereclause += 't.id = ?'
              bindvars.add(new Long(nl[i]));
            }
            else if ( cn == 'title.title' ) {
              title = nl[i]
            }
            else if ( cn.startsWith('title.id.' ) ) {
              // Namespace and value
              if ( nl[i].trim().length() > 0 ) {
                if ( disjunction_ctr++ > 0 ) { whereclause += ' OR ' }
                joinclause = " join t.ids as id "
                whereclause += " ( id.identifier.ns.ns = ? AND id.identifier.value = ? ) "
                bindvars.add(cn.substring(9))
                bindvars.add(nl[i])
              }
            }
            i++;
          }

          log.debug("\n\n");
          log.debug(q);
          log.debug(joinclause);
          log.debug(whereclause);
          log.debug(bindvars);

          def title_search = TitleInstance.executeQuery(q+joinclause+whereclause,bindvars);
          log.debug("Search returned ${title_search.size()} titles");

          if ( title_search.size() == 0 ) {
            if ( title != null ) {
              log.debug("New title - create identifiers and title ${title}");
            }
            else {
              log.debug("NO match - no title - skip row");
            }
          }
          else if ( title_search.size() == 1 ) {
            log.debug("Matched one - see if any of the supplied identifiers are missing");
            def title_obj = title_search[0]
            def c = 0;
            cols.each { cn ->
              if ( cn.startsWith('title.id.' ) ) {
                def ns = cn.substring(9)
                def val = nl[c]
                log.debug("validate ${title_obj.title} has identifier with ${ns} ${val}");
                title_obj.checkAndAddMissingIdentifier(ns,val);
              }
              c++
            }
          }
          else {
            log.debug("Unable to continue - matched multiple titles");
          }
        }
      }
    }
  }
  
    @Secured(['ROLE_YODA'])
    def uploadIssnL() {
        def result=[:]
        boolean hasStarted = false

        if (request.method == 'POST'){
            def input_stream = request.getFile("sameasfile")?.inputStream
            CSVReader reader = new CSVReader( new InputStreamReader(input_stream, java.nio.charset.Charset.forName('UTF-8') ), '\t' as char )
            def future = executorService.submit({
                performUploadISSNL(reader)
            } as java.util.concurrent.Callable)
            log.debug("Uploading ISSNL is returning");
            hasStarted = true
        }

        [hasStarted: hasStarted]
    }

    @Secured(['ROLE_ADMIN'])
    def performUploadISSNL(r) {
        def ctr = 0;
        def start_time = System.currentTimeMillis()
        String[] nl;
        String[] types;
        def first = true
        while ((nl = r.readNext()) != null) {
            def elapsed = System.currentTimeMillis() - start_time
            def avg = 0;
            if ( ctr > 0 ) {
                avg = elapsed / 1000 / ctr
            }

            if ( nl.length == 2 ) {
                if ( first ) {
                    first = false; // Skip header
                    log.debug('Header :'+nl);
                    types=nl
                }
                else {
                    Identifier.withNewTransaction {
                        log.debug("[seq ${ctr++} - avg=${avg}] ${types[0]}:${nl[0]} == ${types[1]}:${nl[1]}");
                        def id1 = Identifier.lookupOrCreateCanonicalIdentifier(types[0],nl[0]);
                        def id2 = Identifier.lookupOrCreateCanonicalIdentifier(types[1],nl[1]);


                        // Do either of our identifiers have a group set
                        if ( id1.ig == id2.ig ) {
                            if ( id1.ig == null ) {
                                log.debug("Both identifiers have a group of null - so create a new group and relate them")
                                def identifier_group = new IdentifierGroup().save(flush:true);
                                id1.ig = identifier_group
                                id2.ig = identifier_group
                                id1.save(flush:true);
                                id2.save(flush:true);
                            }
                            else {
                                log.debug("Identifiers already belong to same identifier group");
                            }
                        }
                        else {
                            if ( ( id1.ig != null ) && ( id2.ig != null ) ) {
                                log.error("Conflicting identifier group for same as ${id1} ${id2}");
                            }
                            else if ( id1.ig != null ) {
                                log.debug("Adding identifier ${id2} to same group (${id1.ig}) as ${id1}");
                                id2.ig = id1.ig
                                id2.save(flush:true);
                            }
                            else {
                                log.debug("Adding identifier ${id1} to same group (${id1.ig}) as ${id2}");
                                id1.ig = id2.ig
                                id1.save(flush:true);
                            }
                        }
                    }
                }
            }
            else {
                log.error("uploadIssnL expected 2 values");
            }

            if ( ctr % 200 == 0 ) {
                cleanUpGorm()
            }
        }
        return true
    }

  def cleanUpGorm() {
    log.debug("Clean up GORM");
    def session = sessionFactory.currentSession
    session.flush()
    session.clear()
    propertyInstanceMap.get().clear()
  }

    @Secured(['ROLE_ADMIN'])
  def financeImport() {
    def result = [:];
    if (request.method == 'POST'){
      def input_stream = request.getFile("tsvfile")?.inputStream
      //result.loaderResult = tsvSuperlifterService.load(input_stream,grailsApplication.config.financialImportTSVLoaderMappings,params.dryRun=='Y'?true:false)
    }
    result
  }

    @Secured(['ROLE_ADMIN'])
    def manageOrganisations() {
        Map<String, Object> result = [:]

        if (params.cmd == 'changeApiLevel') {
            Org target = genericOIDService.resolveOID(params.target)

            if (ApiToolkit.getAllApiLevels().contains(params.apiLevel)) {
                ApiToolkit.setApiLevel(target, params.apiLevel)
            }
            else if (params.apiLevel == 'Kein Zugriff') {
                ApiToolkit.removeApiLevel(target)
            }
        }
        else if (params.cmd == 'changeCustomerType') {
            Org target = genericOIDService.resolveOID(params.target)
            Role customerType = Role.get(params.customerType)

            if (customerType.authority == 'FAKE') {
                OrgSettings.delete(target, OrgSettings.KEYS.CUSTOMER_TYPE)
            }
            else {
                def oss = OrgSettings.get(target, OrgSettings.KEYS.CUSTOMER_TYPE)

                if (oss != OrgSettings.SETTING_NOT_FOUND) {

                    // ERMS-1615
                    if (oss.roleValue.authority in ['ORG_INST', 'ORG_BASIC_MEMBER'] && customerType.authority == 'ORG_INST_COLLECTIVE') {
                        log.debug('changing ' + oss.roleValue.authority + ' to ' + customerType.authority)

						// orgRole = subscriber
						List<OrgRole> subscriberRoles = OrgRole.executeQuery(
								'select ro from OrgRole ro ' +
								'where ro.org = :org and ro.sub is not null and ro.roleType.value like \'Subscriber\'',
								[ org: target ]
						)

						List<OrgRole> conSubscriberRoles = OrgRole.executeQuery(
								'select ro from OrgRole ro ' +
								'where ro.org = :org and ro.sub is not null and ro.roleType.value in (:roleTypes)',
								[ org: target, roleTypes: ['Subscriber_Consortial', 'Subscriber_Consortial_Hidden'] ]
						)


                        subscriberRoles.each{ role ->
                            if (role.sub.getCalculatedType() == Subscription.CALCULATED_TYPE_LOCAL) {
                                role.setRoleType(RDStore.OR_SUBSCRIPTION_COLLECTIVE)
                                role.save()

                                role.sub.type = RDStore.SUBSCRIPTION_TYPE_LOCAL
                                role.sub.save()
                            }
                        }
                        conSubscriberRoles.each{ role ->
                            if (role.sub.getCalculatedType() == Subscription.CALCULATED_TYPE_PARTICIPATION) {
                                OrgRole newRole = new OrgRole(
                                      org: role.org,
                                      sub: role.sub,
                                      roleType: RDStore.OR_SUBSCRIPTION_COLLECTIVE
                                )
                                newRole.save()

                                // keep consortia type
                                //role.sub.type = RDStore.SUBSCRIPTION_TYPE_LOCAL
                                //role.sub.save()
                            }
                        }
                    }

                    oss.roleValue = customerType
                    oss.save(flush:true)
                }
                else {
                    OrgSettings.add(target, OrgSettings.KEYS.CUSTOMER_TYPE, customerType)
                }
            }
        }
        else if (params.cmd == 'changeGascoEntry') {
          Org target = genericOIDService.resolveOID(params.target)
          RefdataValue option = genericOIDService.resolveOID(params.gascoEntry)

          if (target && option) {
            def oss = OrgSettings.get(target, OrgSettings.KEYS.GASCO_ENTRY)

            if (oss != OrgSettings.SETTING_NOT_FOUND) {
              oss.rdValue = option
              oss.save(flush: true)
            } else {
              OrgSettings.add(target, OrgSettings.KEYS.GASCO_ENTRY, option)
            }
          }
        }

        result.orgList = Org.findAll()
        result.orgListTotal = result.orgList.size()

        result
    }

    @Secured(['ROLE_ADMIN'])
    def manageNamespaces() {
        def identifierNamespaceInstance = new IdentifierNamespace(params)

        switch (request.method) {
            case 'GET':
                if (params.cmd == 'deleteNamespace') {
                    def idns = genericOIDService.resolveOID(params.oid)
                    if (idns && Identifier.countByNs(idns) == 0) {
                        try {
                            idns.delete()
                            flash.message = "Namensraum ${idns.ns} wurde gelöscht."
                        } catch (Exception e) {
                            flash.message = "Namensraum ${idns.ns} konnte nicht gelöscht werden."
                        }
                    }
                }
                break

            case 'POST':
                if (IdentifierNamespace.findByNsIlike(params.ns) || !identifierNamespaceInstance.save(flush: true)) {

                    if(IdentifierNamespace.findByNsIlike(params.ns)) {
                        flash.error = message(code: 'identifier.namespace.exist', default: 'IdentifierNamespace exist', args:[params.ns])
                        break
                    }
                    return
                }
                else {
                    flash.message = message(code: 'default.created.message', args: [message(code: 'identifier.namespace.label', default: 'IdentifierNamespace'), identifierNamespaceInstance.ns])
                }
                break
        }
        render view: 'manageNamespaces', model: [
            editable: true, // TODO check role and editable !!!
            identifierNamespaceInstance: identifierNamespaceInstance,
            identifierNamespaces: IdentifierNamespace.where{}.sort('ns')
        ]
    }

    @Secured(['ROLE_ADMIN'])
    def managePropertyDefinitions() {

        if (params.cmd == 'deletePropertyDefinition') {
            def pd = genericOIDService.resolveOID(params.pd)

            if (pd) {
                if (! pd.isHardData) {
                    try {
                        pd.delete(flush:true)
                        flash.message = "${params.pd} wurde gelöscht."
                    }
                    catch(Exception e) {
                        flash.error = "${params.pd} konnte nicht gelöscht werden."
                    }
                }
            }
        }
        else if (params.cmd == 'replacePropertyDefinition') {
            if (SpringSecurityUtils.ifAnyGranted('ROLE_YODA')) {
                def pdFrom = genericOIDService.resolveOID(params.xcgPdFrom)
                def pdTo = genericOIDService.resolveOID(params.xcgPdTo)

                if (pdFrom && pdTo && (pdFrom.tenant?.id == pdTo.tenant?.id)) {

                    try {
                        def count = propertyService.replacePropertyDefinitions(pdFrom, pdTo)

                        flash.message = "${count} Vorkommen von ${params.xcgPdFrom} wurden durch ${params.xcgPdTo} ersetzt."
                    }
                    catch (Exception e) {
                        log.error(e)
                        flash.error = "${params.xcgPdFrom} konnte nicht durch ${params.xcgPdTo} ersetzt werden."
                    }

                }
            } else {
                flash.error = "Keine ausreichenden Rechte!"
            }
        }

        def propDefs = [:]
        PropertyDefinition.AVAILABLE_CUSTOM_DESCR.each { it ->
            def itResult = PropertyDefinition.findAllByDescrAndTenant(it, null, [sort: 'name']) // NO private properties!
            propDefs << ["${it}": itResult]
        }

        def (usedPdList, attrMap) = propertyService.getUsageDetails()

        render view: 'managePropertyDefinitions', model: [
              editable    : true,
              propertyDefinitions: propDefs,
              attrMap     : attrMap,
              usedPdList  : usedPdList
            ]
    }

  @Secured(['ROLE_ADMIN'])
  def manageSurveyPropertyDefinitions() {

    if (params.cmd == 'deletePropertyDefinition') {
      def pd = genericOIDService.resolveOID(params.pd)

      if (pd) {
        if (! pd.isHardData) {
          try {
            pd.delete(flush:true)
            flash.message = "${params.pd} wurde gelöscht."
          }
          catch(Exception e) {
            flash.error = "${params.pd} konnte nicht gelöscht werden."
          }
        }
      }
    }
    else if (params.cmd == 'replacePropertyDefinition') {
      if (SpringSecurityUtils.ifAnyGranted('ROLE_YODA')) {
        def pdFrom = genericOIDService.resolveOID(params.xcgPdFrom)
        def pdTo = genericOIDService.resolveOID(params.xcgPdTo)

        if (pdFrom && pdTo && (pdFrom.tenant?.id == pdTo.tenant?.id)) {

          try {
            def count = propertyService.replacePropertyDefinitions(pdFrom, pdTo)

            flash.message = "${count} Vorkommen von ${params.xcgPdFrom} wurden durch ${params.xcgPdTo} ersetzt."
          }
          catch (Exception e) {
            log.error(e)
            flash.error = "${params.xcgPdFrom} konnte nicht durch ${params.xcgPdTo} ersetzt werden."
          }

        }
      } else {
        flash.error = "Keine ausreichenden Rechte!"
      }
    }

    def propDefs = []
    SurveyProperty.findAllByOwnerIsNull().each { it ->
      propDefs << it

    }

    propDefs.sort { a, b -> a.getI10n('name').compareToIgnoreCase b.getI10n('name') }

    render view: 'manageSurveyPropertyDefinitions', model: [
            editable    : true,
            surveyPropertyDefinitions: propDefs
    ]
  }

    @Secured(['ROLE_ADMIN'])
    def managePropertyGroups() {
        //def result = setResultGenerics()
        def result = [:]
        result.editable = true // true, because action is protected

        if (params.cmd == 'new') {
            result.formUrl = g.createLink([controller: 'admin', action: 'managePropertyGroups'])
            render template: '/templates/properties/propertyGroupModal', model: result
            return
        }
        else if (params.cmd == 'edit') {
            result.pdGroup = genericOIDService.resolveOID(params.oid)
            result.formUrl = g.createLink([controller: 'admin', action: 'managePropertyGroups'])

            render template: '/templates/properties/propertyGroupModal', model: result
            return
        }
        else if (params.cmd == 'delete') {
            def pdg = genericOIDService.resolveOID(params.oid)
            try {
                pdg.delete()
                flash.message = "Die Gruppe ${pdg.name} wurde gelöscht."
            }
            catch (e) {
                flash.error = "Die Gruppe ${params.oid} konnte nicht gelöscht werden."
            }
        }
        else if (params.cmd == 'processing') {
            def valid
            def propDefGroup
            def ownerType = PropertyDefinition.getDescrClass(params.prop_descr)

            if (params.oid) {
                propDefGroup = genericOIDService.resolveOID(params.oid)
                propDefGroup.name = params.name ?: propDefGroup.name
                propDefGroup.description = params.description
                propDefGroup.ownerType = ownerType

                if (propDefGroup.save(flush:true)) {
                    valid = true
                }
            }
            else {
                if (params.name && ownerType) {
                    propDefGroup = new PropertyDefinitionGroup(
                        name: params.name,
                        description: params.description,
                        tenant: null,
                        ownerType: ownerType,
                        isVisible: true
                    )
                    if (propDefGroup.save(flush:true)) {
                        valid = true
                    }
                }
            }

            if (valid) {
                PropertyDefinitionGroupItem.executeUpdate(
                    "DELETE PropertyDefinitionGroupItem pdgi WHERE pdgi.propDefGroup = :pdg",
                    [pdg: propDefGroup]
                )

                params.list('propertyDefinition')?.each { pd ->

                    new PropertyDefinitionGroupItem(
                          propDef: pd,
                          propDefGroup: propDefGroup
                    ).save(flush: true)
                }
            }
        }

        result.propDefGroups = PropertyDefinitionGroup.findAllWhere(
                tenant: null
        )
        result
    }

    @Secured(['ROLE_ADMIN'])
    def manageRefdatas() {

        if (params.cmd == 'deleteRefdataValue') {
            def rdv = genericOIDService.resolveOID(params.rdv)

            if (rdv) {
                if (! rdv.isHardData) {
                    try {
                        rdv.delete(flush:true)
                        flash.message = "${params.rdv} wurde gelöscht."
                    }
                    catch(Exception e) {
                        flash.error = "${params.rdv} konnte nicht gelöscht werden."
                    }
                }
            }
        }
        else if (params.cmd == 'replaceRefdataValue') {
            if (SpringSecurityUtils.ifAnyGranted('ROLE_YODA')) {
                RefdataValue rdvFrom = genericOIDService.resolveOID(params.xcgRdvFrom)
                RefdataValue rdvTo = genericOIDService.resolveOID(params.xcgRdvTo)

                boolean check = false

                if (! rdvFrom) {
                    check = false
                }
                else if (rdvTo && rdvTo.owner == rdvFrom.owner) {
                    check = true
                }
                else if (! rdvTo && params.xcgRdvGlobalTo) {

                    List<String> pParts = params.xcgRdvGlobalTo.split(':')
                    if (pParts.size() == 2) {
                        RefdataCategory rdvToCat = RefdataCategory.findByDesc(pParts[0].trim())
                        RefdataValue rdvToRdv = RefdataValue.getByValueAndCategory(pParts[1].trim(), pParts[0].trim())

                        if (rdvToRdv && rdvToRdv.owner == rdvToCat ) {
                            rdvTo = rdvToRdv
                            check = true
                        }
                    }
                }

                if (check) {
                    try {
                        def count = refdataService.replaceRefdataValues(rdvFrom, rdvTo)

                        flash.message = "${count} Vorkommen von ${params.xcgRdvFrom} wurden durch ${params.xcgRdvTo}${params.xcgRdvGlobalTo} ersetzt."
                    }
                    catch (Exception e) {
                        log.error(e)
                        flash.error = "${params.xcgRdvFrom} konnte nicht durch ${params.xcgRdvTo}${params.xcgRdvGlobalTo} ersetzt werden."
                    }

                }
            }
            else {
                flash.error = "Keine ausreichenden Rechte!"
            }
        }

        def (usedRdvList, attrMap) = refdataService.getUsageDetails()

        def integrityCheckResult = refdataService.integrityCheck()

        render view: 'manageRefdatas', model: [
                editable    : true,
                rdCategories: RefdataCategory.where{}.sort('desc'),
                attrMap     : attrMap,
                usedRdvList : usedRdvList,
                integrityCheckResult : integrityCheckResult
        ]
  }



}
