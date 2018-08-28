package com.k_int.kbplus

import com.k_int.goai.OaiClient
import com.k_int.kbplus.auth.User
import de.laser.oai.OaiClientLaser

import java.text.SimpleDateFormat
import org.springframework.transaction.annotation.*

/*
 *  Implementing new rectypes..
 *  the reconciler closure is responsible for reconciling the previous version of a record and the latest version
 *  the converter is responsible for creating the map structure passed to the reconciler. It needs to return a [:] sorted appropriate
 *  to the work the reconciler will need to do (Often this includes sorting lists)
 */

class GlobalSourceSyncService {


  def dataloadService
  public static boolean running = false;
  def genericOIDService
  def executorService
  def changeNotificationService
  boolean parallel_jobs = false
  def messageSource

  def titleReconcile = { grt ,oldtitle, newtitle ->
    log.debug("Reconcile grt: ${grt} oldtitle:${oldtitle} newtitle:${newtitle}");

    // DOes the remote title have a publisher (And is ours blank)
    def title_instance = genericOIDService.resolveOID(grt.localOid)

    if ( title_instance == null ) {
      log.debug("Failed to resolve ${grt.localOid} - Exiting");
      return
    }

    title_instance.status = RefdataValue.loc(RefdataCategory.TI_STATUS, [en: 'Deleted', de: 'Gelöscht'])

    if (newtitle.status == 'Current') {
      title_instance.status = RefdataValue.loc(RefdataCategory.TI_STATUS, [en: 'Current', de: 'Aktuell'])
    } else if (newtitle.status == 'Retired') {
      title_instance.status = RefdataValue.loc(RefdataCategory.TI_STATUS, [en: 'Retired', de: 'im Ruhestand'])
    }

    newtitle.identifiers.each {
      log.debug("Checking title has ${it.namespace}:${it.value}");
      title_instance.checkAndAddMissingIdentifier(it.namespace, it.value);
    }
    title_instance.save(flush: true);

    if ( newtitle.publishers != null ) {
      newtitle.publishers.each { pub ->
//         def publisher_identifiers = pub.identifiers
        def publisher_identifiers = []
        def orgSector = RefdataValue.loc('OrgSector',  [en: 'Publisher', de: 'Veröffentlicher']);
        def publisher = Org.lookupOrCreate(pub.name, orgSector, null, publisher_identifiers, null)
        def pub_role = RefdataValue.loc('Organisational Role',  [en: 'Publisher', de: 'Veröffentlicher']);
        def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        def start_date
        def end_date

        if(pub.startDate){
          start_date = sdf.parse(pub.startDate);
        }
        if(pub.endDate){
          end_date = sdf.parse(pub.endDate);
        }

        log.debug("Asserting ${publisher} ${title_instance} ${pub_role}");
        OrgRole.assertOrgTitleLink(publisher, title_instance, pub_role, ( pub.startDate ? start_date : null), ( pub.endDate ? end_date : null))
      }
    }

    // Title history!!
    newtitle.history.each { historyEvent ->
      log.debug("Processing title history event");
      // See if we already have a reference
      def fromset = []
      def toset = []

      historyEvent.from.each { he ->
        def participant = TitleInstance.lookupOrCreate(he.ids,he.title)
        fromset.add(participant)
      }

      historyEvent.to.each { he ->
        def participant = TitleInstance.lookupOrCreate(he.ids,he.title)
        toset.add(participant)
      }

      // Now - See if we can find a title history event for data and these particiapnts.
      // Title History Events are IMMUTABLE - so we delete them rather than updating them.
      def base_query = "select the from TitleHistoryEvent as the where the.eventDate = ? "
      // Need to parse date...
      def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
      def query_params = [(((historyEvent.date != null ) && ( historyEvent.date.trim().length() > 0 ) ) ? sdf.parse(historyEvent.date) : null)]

      fromset.each {
        base_query += "and exists ( select p from the.participants as p where p.participant = ? and p.participantRole = 'from' ) "
        query_params.add(it)
      }
      toset.each {
        base_query += "and exists ( select p from the.participants as p where p.participant = ? and p.participantRole = 'to' ) "
        query_params.add(it)
      }

      def existing_title_history_event = TitleHistoryEvent.executeQuery(base_query,query_params);
      log.debug("Result of title history event lookup : ${existing_title_history_event}");

      if ( existing_title_history_event.size() == 0  ) {
        log.debug("Create new history event");
        def he = new TitleHistoryEvent(eventDate:query_params[0]).save(flush:true)
        fromset.each {
          new TitleHistoryEventParticipant(event:he, participant:it, participantRole:'from').save(flush:true)
        }
        toset.each {
          new TitleHistoryEventParticipant(event:he, participant:it, participantRole:'to').save(flush:true)
        }
      }
    }
  }

  def titleConv = { md, synctask ->
    log.debug("titleConv.... ${md}");
    def result = [:]
    result.parsed_rec = [:]
    result.parsed_rec.identifiers = []
    result.parsed_rec.history = []
    result.parsed_rec.publishers = []
    result.parsed_rec.status = md.gokb.title.status.text()

    result.title = md.gokb.title.name.text()
    result.parsed_rec.title = md.gokb.title.name.text()

    md.gokb.title.publishers?.publisher.each{ pub ->
      def publisher = [:]
      publisher.name = pub.name.text()
      publisher.status = pub.status.text()
//       if ( pub.identifiers)
//         publisher.identifiers = []
//
//         pub.identifiers.identifier.each { pub_id ->
//           publisher.identifiers.add(id.'@namespace'.text():id.'@value'.text())
//         }

      if(pub.startDate){
        publisher.startDate = pub.startDate.text()
      }

      if(pub.endDate){
        publisher.endDate = pub.endDate.text()
      }

      result.parsed_rec.publishers.add(publisher)
    }
    md.gokb.title.identifiers.identifier.each { id ->
      result.parsed_rec.identifiers.add([namespace:id.'@namespace'.text(), value:id.'@value'.text()])
    }
    result.parsed_rec.identifiers.add([namespace:'uri',value:md.gokb.title.'@id'.text()]);

    md.gokb.title.history?.historyEvent.each { he ->
      def history_statement = [:]
      history_statement.internalId = he.'@id'.text()
      history_statement.date = he.date.text()
      history_statement.from = []
      history_statement.to = []

      he.from.each { hef ->
        def new_history_statement = [:]
        new_history_statement.title=hef.title.text()
        new_history_statement.ids = []
        hef.identifiers.identifier.each { i ->
          new_history_statement.ids.add([namespace:i.'@namespace'.text(), value:i.'@value'.text()])
        }
        new_history_statement.ids.add([namespace:'uri',value:hef.internalId.text()]);
        history_statement.from.add(new_history_statement);
      }

      he.to.each { het ->
        def new_history_statement = [:]
        new_history_statement.title=het.title.text()
        new_history_statement.ids = []
        het.identifiers.identifier.each { i ->
          new_history_statement.ids.add([namespace:i.'@namespace'.text(), value:i.'@value'.text()])
        }
        new_history_statement.ids.add([namespace:'uri',value:het.internalId.text()]);
        history_statement.to.add(new_history_statement);
      }

      result.parsed_rec.history.add(history_statement)
    }

    log.debug(result);
    result
  }

  def packageReconcile = { grt ,oldpkg, newpkg ->
    def pkg = null;
    boolean auto_accept_flag = false

    log.debug("Reconciling new Package!")

    def scope = RefdataValue.loc(RefdataCategory.PKG_SCOPE,      [en: (newpkg?.scope)?:'Unknown']);
    def listStatus = RefdataValue.loc(RefdataCategory.PKG_LIST_STAT, [en: (newpkg?.listStatus)?:'Unknown']);
    def breakable = RefdataValue.loc(RefdataCategory.PKG_BREAKABLE, [en: (newpkg?.breakable)?:'Unknown']);
    def consistent = RefdataValue.loc(RefdataCategory.PKG_CONSISTENT, [en: (newpkg?.consistent)?:'Unknown']);
    def fixed = RefdataValue.loc(RefdataCategory.PKG_FIXED, [en: (newpkg?.fixed)?:'Unknown']);
    def paymentType = RefdataValue.loc(RefdataCategory.PKG_PAYMENTTYPE,[en: (newpkg?.paymentType)?:'Unknown']);
    def global = RefdataValue.loc(RefdataCategory.PKG_GLOBAL,[en: (newpkg?.global)?:'Unknown']);
    def isPublic = RefdataValue.loc('YN',   [en: 'Yes', de: 'Ja'])
    def ref_pprovider = RefdataValue.loc('Organisational Role',  [en: 'Content Provider', de: 'Anbieter']);

    // Firstly, make sure that there is a package for this record
    if ( grt.localOid != null ) {
      pkg = genericOIDService.resolveOID(grt.localOid)

      if( pkg && newpkg.status != 'Current' ){
        def pkg_del_status = RefdataValue.loc('Package Status', [en: 'Deleted', de: 'Gelöscht'])
        if (newpkg.status == 'Retired') {
          pkg_del_status = RefdataValue.loc('Package Status', [en: 'Retired', de: 'im Ruhestand'])
        }

        pkg.packageStatus = pkg_del_status
      }

      if( pkg ) {
        newpkg.identifiers.each {
          log.debug("Checking package has ${it.namespace}:${it.value}");
          pkg.checkAndAddMissingIdentifier(it.namespace, it.value);
        }
      }
      pkg.save()
    }
    else {
      // create a new package
      log.debug("Creating new Package..")

      def packageStatus = RefdataValue.loc('Package Status', [en: 'Deleted', de: 'Gelöscht'])

      if (newpkg.status == 'Current') {
        packageStatus = RefdataValue.loc('Package Status', [en: 'Current', de: 'Aktuell'])
      } else if (newpkg.status == 'Retired') {
        packageStatus = RefdataValue.loc('Package Status', [en: 'Retired', de: 'im Ruhestand'])
      }

      // Auto accept everything whilst we load the package initially
      auto_accept_flag = true;

      pkg = new Package(
              identifier: grt.identifier,
              name: grt.name,
              impId: grt.owner.identifier,
              autoAccept: false,
              packageType: null,
              packageStatus: packageStatus,
              packageListStatus: listStatus,
              breakable: breakable,
              consistent: consistent,
              fixed: fixed,
              isPublic: isPublic,
              packageScope: scope
      )


      if ( pkg.save() ) {
        log.debug("Saved Package as com.k_int.kbplus.Package:${pkg.id}!")

        newpkg.identifiers.each {
          log.debug("Checking package has ${it.namespace}:${it.value}");
          pkg.checkAndAddMissingIdentifier(it.namespace, it.value);
        }

        if ( newpkg.packageProvider ) {

          def orgSector = RefdataValue.getByValueAndCategory('Publisher','OrgSector')
          def orgType = RefdataValue.getByValueAndCategory('Provider','OrgType')
          def orgRole = RefdataValue.loc('Organisational Role',  [en: 'Content Provider', de: 'Anbieter']);
          def provider = Org.lookupOrCreate2(newpkg.packageProvider , orgSector , null, [:], null, orgType)

          OrgRole.assertOrgPackageLink(provider, pkg, orgRole)
        }

        grt.localOid = "com.k_int.kbplus.Package:${pkg.id}"
        grt.save()


      }
    }

    def onNewTipp = { ctx, tipp, auto_accept ->
      def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
      println("new tipp: ${tipp}");
      println("identifiers: ${tipp.title.identifiers}");

      def title_instance = TitleInstance.lookupOrCreate(tipp.title.identifiers,tipp.title.name, tipp.title.titleType)
      println("Result of lookup or create for ${tipp.title.name} with identifiers ${tipp.title.identifiers} is ${title_instance}");
      def origin_uri = null
      tipp.title.identifiers.each { i ->
        if (i.namespace.toLowerCase() == 'uri') {
          origin_uri = i.value
        }
      }
      updatedTitleafterPackageReconcile(grt, origin_uri, title_instance)

      def plat_instance = Platform.lookupOrCreatePlatform([name:tipp.platform]);
      def tipp_status_str = tipp.status ? tipp.status.capitalize():'Current'
      def tipp_status = RefdataCategory.lookupOrCreate(RefdataCategory.TIPP_STATUS,tipp_status_str);

      if ( auto_accept ) {
        def new_tipp = new TitleInstancePackagePlatform()
        new_tipp.pkg = ctx;
        new_tipp.platform = plat_instance;
        new_tipp.title = title_instance;
        new_tipp.status = tipp_status;
        new_tipp.impId = tipp.tippId;

        // We rely upon there only being 1 coverage statement for now, it seems likely this will need
        // to change in the future.
        // tipp.coverage.each { cov ->
        def cov = tipp.coverage[0]
        new_tipp.startDate = ((cov.startDate != null) && (cov.startDate.length() > 0)) ? sdf.parse(cov.startDate) : null;
        new_tipp.startVolume = cov.startVolume;
        new_tipp.startIssue = cov.startIssue;
        new_tipp.endDate = ((cov.endDate != null) && (cov.endDate.length() > 0)) ? sdf.parse(cov.endDate) : null;
        new_tipp.endVolume = cov.endVolume;
        new_tipp.endIssue = cov.endIssue;
        new_tipp.embargo = cov.embargo;
        new_tipp.coverageDepth = cov.coverageDepth;
        new_tipp.coverageNote = cov.coverageNote;
        // }
        new_tipp.hostPlatformURL=tipp.url;

        new_tipp.save(flush:true, failOnError:true);

        if (tipp.tippId){
          def tipp_id = Identifier.lookupOrCreateCanonicalIdentifier('uri', tipp.tippId)

          if(tipp_id){
            def tipp_io = new IdentifierOccurrence(identifier:tipp_id, tipp:new_tipp).save(flush:true)
          }else{
            log.error("Error creating identifier instance for new TIPP!")
          }
        }
      }
      else {
        println("Register new tipp event for user to accept or reject");

        def cov = tipp.coverage[0]
        def change_doc = [
                pkg          : [id: ctx.id],
                platform     : [id: plat_instance.id],
                title        : [id: title_instance.id],
                impId        : tipp.tippId,
                status       : [id: tipp_status.id],
                startDate    : ((cov.startDate != null) && (cov.startDate.length() > 0)) ? sdf.parse(cov.startDate) : null,
                startVolume  : cov.startVolume,
                startIssue   : cov.startIssue,
                endDate      : ((cov.endDate != null) && (cov.endDate.length() > 0)) ? sdf.parse(cov.endDate) : null,
                endVolume    : cov.endVolume,
                endIssue     : cov.endIssue,
                embargo      : cov.embargo,
                coverageDepth: cov.coverageDepth,
                coverageNote : cov.coverageNote];

        changeNotificationService.registerPendingChange('pkg',
                ctx,
                "Eine neue Verknüpfung (TIPP) für den Titel ${title_instance.title} mit der Plattform ${plat_instance.name}",
                null,
                [
                        newObjectClass: "com.k_int.kbplus.TitleInstancePackagePlatform",
                        changeType    : 'New Object',
                        changeDoc     : change_doc
                ])

      }
    }

    def onUpdatedTipp = { ctx, tipp, oldtipp, changes, auto_accept ->
      println("updated tipp, ctx = ${ctx.toString()}");

      // Find title with ID tipp... in package ctx
      def title_of_tipp_to_update = TitleInstance.lookupOrCreate(tipp.title.identifiers,tipp.title.name)

      def db_tipp = ctx.tipps.find { it.impId == tipp.tippId }

      if ( db_tipp != null) {

        def TippStatus = RefdataValue.loc(RefdataCategory.TIPP_STATUS, [en: 'Deleted', de: 'Gelöscht'])

        if (tipp.status == 'Current') {
          TippStatus = RefdataValue.loc(RefdataCategory.TIPP_STATUS, [en: 'Current', de: 'Aktuell'])
        } else if (tipp.status == 'Retired') {
          TippStatus = RefdataValue.loc(RefdataCategory.TIPP_STATUS, [en: 'Retired', de: 'im Ruhestand'])
        }

        def changetext
        def change_doc = [:]
        changes.each { chg ->

          if ("${chg.field}" == "accessStart") {
            changetext = changetext ? changetext + ", accessStartDate: ${tipp.accessStart}" : "accessStartDate: ${tipp.accessStart}"
            change_doc.put("accessStartDate", tipp.accessStart)
          }
          if ("${chg.field}" == "accessEnd") {
            changetext = changetext ? changetext + ", accessEndDate: ${tipp.accessEnd}" : "accessEndDate: ${tipp.accessEnd}"
            change_doc.put("accessEndDate", tipp.accessEnd)

          }
          if ("${chg.field}" == "coverage") {
            changetext = changetext ? changetext + ", Coverage: (Start Date:${tipp.coverage[0].startDate}, Start Volume:${tipp.coverage[0].startVolume}, Start Issue:${tipp.coverage[0].startIssue}, End Date:${tipp.coverage[0].endDate} , End Volume:${tipp.coverage[0].endVolume}, End Issue:${tipp.coverage[0].endIssue}, Embargo:${tipp.coverage[0].embargo}, Coverage Depth:${tipp.coverage[0].coverageDepth}, Coverage Note:${tipp.coverage[0].coverageNote})" : "Coverage: (Start Date:${tipp.coverage[0].startDate}, Start Volume:${tipp.coverage[0].startVolume}, Start Issue:${tipp.coverage[0].startIssue}, End Date:${tipp.coverage[0].endDate} , End Volume:${tipp.coverage[0].endVolume}, End Issue:${tipp.coverage[0].endIssue}, Embargo:${tipp.coverage[0].embargo}, Coverage Depth:${tipp.coverage[0].coverageDepth}, Coverage Note:${tipp.coverage[0].coverageNote})"
            change_doc.put("startDate", tipp.coverage[0].startDate)
            change_doc.put("startVolume", tipp.coverage[0].startVolume)
            change_doc.put("startIssue", tipp.coverage[0].startIssue)
            change_doc.put("endDate", tipp.coverage[0].endDate)
            change_doc.put("endVolume", tipp.coverage[0].endVolume)
            change_doc.put("endIssue", tipp.coverage[0].endIssue)
            change_doc.put("embargo", tipp.coverage[0].embargo)
            change_doc.put("coverageDepth", tipp.coverage[0].coverageDepth)
            change_doc.put("coverageNote", tipp.coverage[0].coverageNote)
          }
          if ("${chg.field}" == "hostPlatformURL") {
            changetext = changetext ? changetext + ", Url: ${tipp.url}" : "Url: ${tipp.url}"
            change_doc.put("hostPlatformURL", tipp.url)

          }

        }
        if (change_doc) {
          changeNotificationService.registerPendingChange('pkg',
                  ctx,
                  "Eine TIPP/Coverage Änderung für den Titel \"${title_of_tipp_to_update.title}\", ${changetext}, Status: ${TippStatus}",
                  null,
                  [
                          changeTarget: "com.k_int.kbplus.TitleInstancePackagePlatform:${db_tipp.id}",
                          changeType  : 'Update Object',
                          changeDoc   : change_doc
                  ])
        } else {
          throw new RuntimeException("changeDoc is empty. ctx:${ctx}, tipp:${tipp}");
        }
      }
      else {
        throw new RuntimeException("Unable to locate TIPP for update. ctx:${ctx}, tipp:${tipp}");
      }
    }

    def onDeletedTipp = { ctx, tipp, auto_accept ->

      // Find title with ID tipp... in package ctx
      def title_of_tipp_to_update = TitleInstance.lookupOrCreate(tipp.title.identifiers, tipp.title.name)

      def TippStatus = RefdataValue.loc(RefdataCategory.TIPP_STATUS, [en: 'Deleted', de: 'Gelöscht'])
      if (tipp.status == 'Retired') {
        TippStatus = RefdataValue.loc(RefdataCategory.TIPP_STATUS, [en: 'Retired', de: 'im Ruhestand'])
      }
      def db_tipp = ctx.tipps.find { it.impId == tipp.tippId }

      if (db_tipp != null && !(db_tipp.status.equals(TippStatus))) {

        def change_doc = [status: TippStatus]

        changeNotificationService.registerPendingChange('pkg',
                ctx,
                "Eine Änderung des Status der Verknüpfung (TIPP) für den Titel \"${title_of_tipp_to_update.title}\", Status: ${TippStatus}",
                null,
                [
                        changeTarget: "com.k_int.kbplus.TitleInstancePackagePlatform:${db_tipp.id}",
                        changeType  : 'Update Object',
                        changeDoc   : change_doc
                ])
        println("deleted tipp");
      }

    }

    def onPkgPropChange = { ctx, propname, value, auto_accept ->
      def oldvalue
      def announcement_content
      switch (propname) {
        case 'title':
          def contextObject = genericOIDService.resolveOID("Package:${ctx.id}");
          oldvalue = ctx.name
          ctx.name = value
          def locale = org.springframework.context.i18n.LocaleContextHolder.getLocale()
          announcement_content = "<p>${messageSource.getMessage('announcement.package.ChangeTitle', null, "Change Package Title on ", locale)}  ${contextObject.getURL() ? "<a href=\"${contextObject.getURL()}\">${ctx.name}</a>" : "${ctx.name}"} ${new Date().toString()}</p>"
          announcement_content += "<p><ul><li>${messageSource.getMessage("announcement.package.TitleChange", [oldvalue, value] as Object[],"Package Title was change from {0} to {1}.", locale)}</li></ul></p>"
          println("updated pkg prop");
          break;
        default:
          println("Not updated pkg prop");
          break;
      }

      if(auto_accept){
        ctx.save(flush:true)

        def announcement_type = RefdataCategory.lookupOrCreate('Document Type','Announcement')
        def newAnnouncement = new Doc(title:'Automated Announcement',
              type:announcement_type,
              content:announcement_content,
              dateCreated:new Date(),
              user: User.findByUsername('admin')).save(flush:true);
      }

    }

    def onTippUnchanged = {ctx, tippa ->
    }

    com.k_int.kbplus.GokbDiffEngine.diff(pkg, oldpkg, newpkg, onNewTipp, onUpdatedTipp, onDeletedTipp, onPkgPropChange, onTippUnchanged, auto_accept_flag)

  }

  def testTitleCompliance = { json_record ->
    log.debug("testTitleCompliance:: ${json_record}");

    def result = RefdataValue.loc('YNO',  [en: 'No', de: 'Nein'])

    if ( json_record.identifiers?.size() > 0 ) {
      result = RefdataValue.loc('YNO',  [en: 'Yes', de: 'Ja'])
    }

    result
  }

  // def testKBPlusCompliance = { json_record ->
  def testPackageCompliance = { json_record ->
    // Iterate through all titles..
    def error = false
    def result = null
    def problem_titles = []

    log.debug(json_record.packageName);
    log.debug(json_record.packageId);

    // GOkb records containing titles with no identifiers are not valid in KB+ land
    json_record?.tipps.each { tipp ->
      log.debug(tipp.title.name);
      // tipp.title.identifiers
      if ( tipp.title?.identifiers?.size() > 0 ) {
        // No problem
      }
      else {
        problem_titles.add(tipp.title.titleId)
        error = true
      }

      // tipp.titleid
      // tipp.platform
      // tipp.platformId
      // tipp.coverage
      // tipp.url
      // tipp.identifiers
    }

    if ( error ) {
      result = RefdataValue.loc('YNO',  [en: 'No', de: 'Nein'])
    }
    else {
      result = RefdataValue.loc('YNO',  [en: 'Yes', de: 'Ja'])
    }

    result
  }
  def packageConv = { md, synctask ->
    log.debug("Package conv...");
    // Convert XML to internal structure and return
    def result = [:]
    // result.parsed_rec = xml.text().getBytes();
    result.title = md.gokb.package.name.text()

    result.parsed_rec = [:]
    result.parsed_rec.packageName = md.gokb.package.name.text()
    result.parsed_rec.packageId = md.gokb.package.'@id'.text()
    result.parsed_rec.packageProvider = md.gokb.package.nominalProvider.text()
    result.parsed_rec.tipps = []
    result.parsed_rec.identifiers = []
    result.parsed_rec.status = md.gokb.package.status.text()
    result.parsed_rec.scope = md.gokb.package.scope.text()
    result.parsed_rec.listStatus = md.gokb.package.listStatus.text()
    result.parsed_rec.breakable = md.gokb.package.breakable.text()
    result.parsed_rec.consistent = md.gokb.package.consistent.text()
    result.parsed_rec.fixed = md.gokb.package.fixed.text()
    result.parsed_rec.global = md.gokb.package.global.text()
    result.parsed_rec.paymentType = md.gokb.package.paymentType.text()
    result.parsed_rec.nominalPlatform = md.gokb.package.nominalPlatform.text()

    md.gokb.package.identifiers.identifier.each { id ->
      result.parsed_rec.identifiers.add([namespace:id.'@namespace'.text(), value:id.'@value'.text()])
    }

    int ctr=0
    md.gokb.package.TIPPs.TIPP.each { tip ->
      log.debug("Processing tipp ${ctr++} from package ${result.parsed_rec.packageId} - ${result.title} (source:${synctask.uri})");
      def newtip = [
              title      : [
                      name       : tip.title.name.text(),
                      identifiers: [],
                      titleType: tip.title.mediumByTypClass.text()
              ],
              status     : tip.status?.text() ?: 'Current',
              titleId    : tip.title.'@id'.text(),
              platform   : tip.platform.name.text(),
              platformId : tip.platform.'@id'.text(),
              coverage   : [],
              url        : tip.url.text(),
              identifiers: [],
              tippId     : tip.'@id'.text(),
              accessStart: tip.access.'@start'.text(),
              accessEnd  : tip.access.'@end'.text(),
              medium     : tip.medium.text()
      ];

      tip.coverage.each { cov ->
        newtip.coverage.add([
                startDate    : cov.'@startDate'.text(),
                endDate      : cov.'@endDate'.text(),
                startVolume  : cov.'@startVolume'.text(),
                endVolume    : cov.'@endVolume'.text(),
                startIssue   : cov.'@startIssue'.text(),
                endIssue     : cov.'@endIssue'.text(),
                coverageDepth: cov.'@coverageDepth'.text(),
                coverageNote : cov.'@coverageNote'.text(),
                embargo      : cov.'@embargo'.text()
        ]);
      }

      tip.title.identifiers.identifier.each { id ->
        newtip.title.identifiers.add([namespace:id.'@namespace'.text(), value:id.'@value'.text()]);
      }
      newtip.title.identifiers.add([namespace:'uri',value:newtip.titleId]);

      newtip.identifiers.add([namespace:'uri',value:newtip.tippId]);

      //log.debug("Harmonise identifiers");
      //harmoniseTitleIdentifiers(newtip);

      result.parsed_rec.tipps.add(newtip)
    }

    result.parsed_rec.tipps.sort{it.tippId}
    log.debug("Rec conversion for package returns object with title ${result.parsed_rec.title} and ${result.parsed_rec.tipps?.size()} tipps");
    return result
  }


  // We always match a remote title against a local one, or create a local one to mirror the remote
  // definition. Having created the remote title, we synchronize the other details (Title History for example)
  // using the standard reconciler with the new info and null as the old info - essentially a full update the first time.
  def onNewTitle = { global_record_info, newtitle ->

    log.debug("onNewTitle.... ${global_record_info} ${newtitle} ");

    // We need to create a new global record tracker. If there is already a local title for this remote title, link to it,
    // otherwise create a new title and link to it. See if we can locate a title.
    def title_instance = TitleInstance.lookupOrCreate(newtitle.identifiers,newtitle.title)

    if ( title_instance != null ) {

      title_instance.refresh()

      // merge in any new identifiers we have
      newtitle.identifiers.each {
        log.debug("Checking title has ${it.namespace}:${it.value}");
        title_instance.checkAndAddMissingIdentifier(it.namespace, it.value);
      }
      title_instance.save(flush: true)


      log.debug("Creating new global record tracker... for title ${title_instance}");


      def grt = new GlobalRecordTracker(
              owner: global_record_info,
              localOid: title_instance.class.name + ':' + title_instance.id,
              identifier: java.util.UUID.randomUUID().toString(),
              name: newtitle.title
      ).save(flush:true)

      log.debug("call title reconcile");
      titleReconcile(grt, null, newtitle)
    }
    else {
      log.error("Unable to lookup or create title... ids:${newtitle.identifiers}, title:${newtitle.title}");
    }
  }


  // Main configuration map
  def rectypes = [
          [name: 'Package', converter: packageConv, reconciler: packageReconcile, newRemoteRecordHandler: null, complianceCheck: testPackageCompliance],
          [name: 'Title', converter: titleConv, reconciler: titleReconcile, newRemoteRecordHandler: onNewTitle, complianceCheck: testTitleCompliance],
  ]

  def runAllActiveSyncTasks() {

    if ( running == false ) {
      def future = executorService.submit({ internalRunAllActiveSyncTasks() } as java.util.concurrent.Callable)
    }
    else {
      log.warn("Not starting duplicate OAI thread");
    }
  }

  def internalRunAllActiveSyncTasks() {

    running = true;

    def jobs = GlobalRecordSource.findAll()

    jobs.each { sync_job ->
      log.debug(sync_job);
      // String identifier
      // String name
      // String type
      // Date haveUpTo
      // String uri
      // String listPrefix
      // String fullPrefix
      // String principal
      // String credentials
      switch (sync_job.type) {
        case 'OAI':
          log.debug("start internal sync");
          this.doOAISync(sync_job)
          log.debug("this.doOAISync has returned...");
          break;
        default:
          log.error("Unhandled sync job type: ${sync_job.type}");
          break;
      }
    }
    running = false
  }

  def private doOAISync(sync_job) {
    log.debug("doOAISync");
    if ( parallel_jobs ) {
      def future = executorService.submit({ intOAI(sync_job.id) } as java.util.concurrent.Callable)
    }
    else {
      intOAI(sync_job.id)
    }
    log.debug("doneOAISync");
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  def intOAI(sync_job_id) {

    log.debug("internalOAI processing ${sync_job_id}");
    new EventLog(event:'kbplus.doOAISync', message:"internalOAI processing ${sync_job_id}", tstp:new Date(System.currentTimeMillis())).save(flush:true)

    def sync_job = GlobalRecordSource.get(sync_job_id)
    int rectype = sync_job.rectype.longValue()
    def cfg = rectypes[rectype]
    def olddate = sync_job.haveUpTo

    Thread.currentThread().setName("GlobalDataSync");

    try {

      log.debug("Rectype: ${rectype} == config ${cfg}");

      log.debug("internalOAISync records from [job ${sync_job_id}] ${sync_job.uri} since ${sync_job.haveUpTo} using ${sync_job.fullPrefix}");

      if (cfg == null) {
        throw new RuntimeException("Unable to resolve config for ID ${sync_job.rectype}");
      }

      def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

      def date = sync_job.haveUpTo

      log.debug("upto: ${date} uri:${sync_job.uri} prefix:${sync_job.fullPrefix}");

      def oai_client = new OaiClient(host: sync_job.uri)
      def max_timestamp = 0

      log.debug("Collect ${cfg.name} changes since ${date}");

      oai_client.getChangesSince(date, sync_job.fullPrefix) { rec ->

        log.debug("Got OAI Record ${rec.header.identifier} datestamp: ${rec.header.datestamp} job:${sync_job.id} url:${sync_job.uri} cfg:${cfg.name}")

        def qryparams = [sync_job.id, rec.header.identifier.text()]
        def record_timestamp = sdf.parse(rec.header.datestamp.text())
        def existing_record_info = GlobalRecordInfo.executeQuery('select r from GlobalRecordInfo as r where r.source.id = ? and r.identifier = ?', qryparams);
        if (existing_record_info.size() == 1) {
          log.debug("convert xml into json - config is ${cfg} ");
          def parsed_rec = cfg.converter.call(rec.metadata, sync_job)

          // Deserialize
          def bais = new ByteArrayInputStream((byte[]) (existing_record_info[0].record))
          def ins = new ObjectInputStream(bais);
          def old_rec_info = ins.readObject()
          ins.close()
          def new_record_info = parsed_rec.parsed_rec

          // For each tracker we need to update the local object which reflects that remote record
          existing_record_info[0].trackers.each { tracker ->
            cfg.reconciler.call(tracker, old_rec_info, new_record_info)
          }

          log.debug("Calling compliance check, cfg name is ${cfg.name}");
          existing_record_info[0].kbplusCompliant = cfg.complianceCheck.call(parsed_rec.parsed_rec)
          log.debug("Result of compliance check: ${existing_record_info[0].kbplusCompliant}");

          // Finally, update our local copy of the remote object
          def baos = new ByteArrayOutputStream()
          def out = new ObjectOutputStream(baos)
          out.writeObject(new_record_info)
          out.close()
          existing_record_info[0].record = baos.toByteArray();
          existing_record_info[0].desc = "Package ${parsed_rec.title} consisting of ${parsed_rec.parsed_rec.tipps?.size()} titles"


          def status = RefdataValue.loc("${cfg.name} Status", [en: 'Deleted', de: 'Gelöscht'])

          if (parsed_rec.parsed_rec.status == 'Current') {
            status = RefdataValue.loc("${cfg.name} Status", [en: 'Current', de: 'Aktuell'])
          } else if (parsed_rec.parsed_rec.status == 'Retired') {
            status = RefdataValue.loc("${cfg.name} Status", [en: 'Retired', de: 'im Ruhestand'])
          }

          existing_record_info[0].globalRecordInfoStatus = status
          existing_record_info[0].save()
        } else {
          log.debug("First time we have seen this record - converting ${cfg.name}");
          def parsed_rec = cfg.converter.call(rec.metadata, sync_job)
          log.debug("Converter thinks this rec has title :: ${parsed_rec.title}");

          // Evaluate the incoming record to see if it meets KB+ stringent data quality standards
          log.debug("Calling compliance check, cfg name is ${cfg.name}");
          def kbplus_compliant = cfg.complianceCheck.call(parsed_rec.parsed_rec)
          // RefdataCategory.lookupOrCreate("YNO","No")
          log.debug("Result of compliance [new] check: ${kbplus_compliant}");

          def baos = new ByteArrayOutputStream()
          def out = new ObjectOutputStream(baos)
          log.debug("write object ${parsed_rec.parsed_rec}");
          out.writeObject(parsed_rec.parsed_rec)

          log.debug("written, closed...");

          out.close()

          log.debug("Create new GlobalRecordInfo");

          def status = RefdataValue.loc("${cfg.name} Status", [en: 'Deleted', de: 'Gelöscht'])
          if (parsed_rec.parsed_rec.status == 'Current') {
            status = RefdataValue.loc("${cfg.name} Status", [en: 'Current', de: 'Aktuell'])
          } else if (parsed_rec.parsed_rec.status == 'Retired') {
            status = RefdataValue.loc("${cfg.name} Status", [en: 'Retired', de: 'im Ruhestand'])
          }

          // Because we don't know about this record, we can't possibly be already tracking it. Just create a local tracking record.
          existing_record_info = new GlobalRecordInfo(
                  ts: record_timestamp,
                  name: parsed_rec.title,
                  identifier: rec.header.identifier.text(),
                  desc: "${parsed_rec.title}",
                  source: sync_job,
                  rectype: sync_job.rectype,
                  record: baos.toByteArray(),
                  kbplusCompliant: kbplus_compliant,
                  globalRecordInfoStatus: status);

          if (existing_record_info.save(flush: true)) {
            log.debug("existing_record_info created ok");
          } else {
            log.error("Problem saving record info: ${existing_record_info.errors}");
          }

          if (kbplus_compliant?.value == 'Yes') {
            if (cfg.newRemoteRecordHandler != null) {
              log.debug("Calling new remote record handler...");
              cfg.newRemoteRecordHandler.call(existing_record_info, parsed_rec.parsed_rec)
              log.debug("Call completed");
            } else {
              log.debug("No new record handler");
            }
          } else {
            log.debug("Skip record - not KBPlus compliant");
          }
        }

        if (record_timestamp.getTime() > max_timestamp) {
          max_timestamp = record_timestamp.getTime()
          log.debug("Max timestamp is now ${record_timestamp}");
        }

        log.debug("Updating sync job max timestamp");
        sync_job.haveUpTo = new Date(max_timestamp)
        sync_job.save(flush: true);
        sleep(3000);
      }
    }
    catch ( Exception e ) {
      log.error("Problem",e);
      log.error("Problem running job ${sync_job_id}, conf=${cfg}",e);
      new EventLog(event:'kbplus.doOAISync', message:"Problem running job ${sync_job_id}, conf=${cfg}", tstp:new Date(System.currentTimeMillis())).save(flush:true)
      log.debug("Reset sync job haveUpTo");
      sync_job.haveUpTo = olddate
      sync_job.save(flush: true);
    }
    finally {
      log.debug("internalOAISync completed for job ${sync_job_id}");
      new EventLog(event:'kbplus.doOAISync', message:"internalOAISync completed for job ${sync_job_id}", tstp:new Date(System.currentTimeMillis())).save(flush:true)
    }
  }

  def parseDate(datestr, possible_formats) {
    def parsed_date = null;
    if ( datestr && ( datestr.toString().trim().length() > 0 ) ) {
      for(Iterator i = possible_formats.iterator(); ( i.hasNext() && ( parsed_date == null ) ); ) {
        try {
          parsed_date = i.next().parse(datestr.toString());
        }
        catch ( Exception e ) {
        }
      }
    }
    parsed_date
  }

  def dumpPkgRec(pr) {
    log.debug(pr);
  }

  def initialiseTracker(grt) {

    def newrecord = reloadAndSaveRecordofPackage(grt)

    int rectype = grt.owner.rectype.longValue()
    def cfg = rectypes[rectype]

    def oldrec = [:]
    oldrec.tipps=[]
    def bais = new ByteArrayInputStream((byte[])(grt.owner.record))
    def ins = new ObjectInputStream(bais);
    def newrec = ins.readObject()
    ins.close()

    def record = newrecord.parsed_rec ?: newrec

    cfg.reconciler.call(grt,oldrec,record)
  }

  def initialiseTracker(grt, localPkgOID) {
    int rectype = grt.owner.rectype.longValue()
    def cfg = rectypes[rectype]
    def localPkg = genericOIDService.resolveOID(localPkgOID)

    def oldrec = localPkg.toComparablePackage()

    def bais = new ByteArrayInputStream((byte[])(grt.owner.record))
    def ins = new ObjectInputStream(bais);
    def newrec = ins.readObject()
    ins.close()

    cfg.reconciler.call(grt,oldrec,newrec)
  }

  /**
   *  When this system sees a title from a remote source, we need to try and find a common canonical identifier. We will use the
   *  GoKB TitleID for this. Each time a title is seen we make sure that we locally know what the GoKB Title ID is for that remote
   *  record.
   */
  def harmoniseTitleIdentifiers(titleinfo) {
    // println("harmoniseTitleIdentifiers");
    // println("Remote Title ID: ${titleinfo.titleId}");
    // println("Identifiers: ${titleinfo.title.identifiers}");
    //def title_instance = TitleInstance.lookupOrCreate(titleinfo.title.identifiers,titleinfo.title.name, true)
  }

  def diff(localPackage, globalRecordInfo) {

    def result = []

    def oldpkg = localPackage ? localPackage.toComparablePackage() : [tipps:[]];

    def bais = new ByteArrayInputStream((byte[])(globalRecordInfo.record))
    def ins = new ObjectInputStream(bais);
    def newpkg = ins.readObject()
    ins.close()

    def onNewTipp = { ctx, tipp, auto_accept -> ctx.add([tipp:tipp, action:'i']); }
    def onUpdatedTipp = { ctx, tipp, oldtipp, changes, auto_accept -> ctx.add([tipp:tipp, action:'u', changes:changes, oldtipp:oldtipp]); }
    def onDeletedTipp = { ctx, tipp, auto_accept  -> ctx.add([oldtipp:tipp, action:'d']); }
    def onPkgPropChange = { ctx, propname, value, auto_accept -> null; }
    def onTippUnchanged = { ctx, tipp -> ctx.add([tipp:tipp, action:'-']);  }

    com.k_int.kbplus.GokbDiffEngine.diff(result, oldpkg, newpkg, onNewTipp, onUpdatedTipp, onDeletedTipp, onPkgPropChange, onTippUnchanged, false)

    return result
  }

  def updatedTitleafterPackageReconcile = { grt, title_id, local_id ->
    //rectype = 2 = Title
    def cfg = rectypes[2]

    def uri = GlobalRecordSource.get(GlobalRecordInfo.get(grt.owner.id).source.id).uri

    uri = uri.replaceAll("packages", "")

    if(title_id == null)
    {
      return
    }

    def oai = new OaiClientLaser()
    def titlerecord = oai.getRecord(uri, 'titles', 'org.gokb.cred.TitleInstance:'+title_id)

    if(titlerecord == null)
    {
      return
    }
    def titleinfo = titleConv(titlerecord.metadata, null)

    println("TitleRecord:" + titleinfo)

    def kbplus_compliant = testTitleCompliance(titleinfo.parsed_rec)

    if (kbplus_compliant?.value == 'No') {
      log.debug("Skip record - not KBPlus compliant");
    } else {

              def title_instance = genericOIDService.resolveOID(local_id)

              if (title_instance == null) {
                log.debug("Failed to resolve ${local_id} - Exiting");
                return
              }

              title_instance.status = RefdataValue.loc(RefdataCategory.TI_STATUS, [en: 'Deleted', de: 'Gelöscht'])

              if (titleinfo.status == 'Current') {
                title_instance.status = RefdataValue.loc(RefdataCategory.TI_STATUS, [en: 'Current', de: 'Aktuell'])
              } else if (titleinfo.status == 'Retired') {
                title_instance.status = RefdataValue.loc(RefdataCategory.TI_STATUS, [en: 'Retired', de: 'im Ruhestand'])
              }

              titleinfo.identifiers.each {
                log.debug("Checking title has ${it.namespace}:${it.value}");
                title_instance.checkAndAddMissingIdentifier(it.namespace, it.value);
              }
              title_instance.save(flush: true);

              if (titleinfo.publishers != null) {
                titleinfo.publishers.each { pub ->

                  def publisher_identifiers = []
                  def orgSector = RefdataValue.loc('OrgSector', [en: 'Publisher', de: 'Veröffentlicher']);
                  def publisher = Org.lookupOrCreate(pub.name, orgSector, null, publisher_identifiers, null)
                  def pub_role = RefdataValue.loc('Organisational Role', [en: 'Publisher', de: 'Veröffentlicher']);
                  def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                  def start_date
                  def end_date

                  if (pub.startDate) {
                    start_date = sdf.parse(pub.startDate);
                  }
                  if (pub.endDate) {
                    end_date = sdf.parse(pub.endDate);
                  }

                  log.debug("Asserting ${publisher} ${title_instance} ${pub_role}");
                  OrgRole.assertOrgTitleLink(publisher, title_instance, pub_role, (pub.startDate ? start_date : null), (pub.endDate ? end_date : null))
                }
              }

              // Title history!!
              titleinfo.history.each { historyEvent ->
                log.debug("Processing title history event");
                // See if we already have a reference
                def fromset = []
                def toset = []

                historyEvent.from.each { he ->
                  def participant = TitleInstance.lookupOrCreate(he.ids, he.title)
                  fromset.add(participant)
                }

                historyEvent.to.each { he ->
                  def participant = TitleInstance.lookupOrCreate(he.ids, he.title)
                  toset.add(participant)
                }

                // Now - See if we can find a title history event for data and these particiapnts.
                // Title History Events are IMMUTABLE - so we delete them rather than updating them.
                def base_query = "select the from TitleHistoryEvent as the where the.eventDate = ? "
                // Need to parse date...
                def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                def query_params = [(((historyEvent.date != null) && (historyEvent.date.trim().length() > 0)) ? sdf.parse(historyEvent.date) : null)]

                fromset.each {
                  base_query += "and exists ( select p from the.participants as p where p.participant = ? and p.participantRole = 'from' ) "
                  query_params.add(it)
                }
                toset.each {
                  base_query += "and exists ( select p from the.participants as p where p.participant = ? and p.participantRole = 'to' ) "
                  query_params.add(it)
                }

                def existing_title_history_event = TitleHistoryEvent.executeQuery(base_query, query_params);
                log.debug("Result of title history event lookup : ${existing_title_history_event}");

                if (existing_title_history_event.size() == 0) {
                  log.debug("Create new history event");
                  def he = new TitleHistoryEvent(eventDate: query_params[0]).save(flush: true)
                  fromset.each {
                    new TitleHistoryEventParticipant(event: he, participant: it, participantRole: 'from').save(flush: true)
                  }
                  toset.each {
                    new TitleHistoryEventParticipant(event: he, participant: it, participantRole: 'to').save(flush: true)
                  }
                }
              }
            }
  }

  def reloadAndSaveRecordofPackage(grt)
  {
    def gli = GlobalRecordInfo.get(grt.owner.id)
    def grs = GlobalRecordSource.get(gli.source.id)
    def uri = grs.uri.replaceAll("packages", "")
    def oai = new OaiClientLaser()
    def record = oai.getRecord(uri, 'packages', grt.owner.identifier)

    def newrecord = record ? packageConv(record.metadata, grs) : null

    if(newrecord) {
      def baos = new ByteArrayOutputStream()
      def out = new ObjectOutputStream(baos)
      log.debug("write object ${newrecord?.parsed_rec}");
      out.writeObject(newrecord?.parsed_rec)
      out.close()

      gli.record = baos.toByteArray()
      gli.save()
    }

    return newrecord

  }

  def initialiseTrackerNew(grt) {

    def newrecord = reloadAndSaveRecordofPackage(grt)

    int rectype = grt.owner.rectype.longValue()
    def cfg = rectypes[rectype]

    def oldrec = [:]
    oldrec.tipps=[]
    def bais = new ByteArrayInputStream((byte[])(grt.owner.record))
    def ins = new ObjectInputStream(bais);
    def newrec = ins.readObject()
    ins.close()

    def record = newrecord.parsed_rec ?: newrec

    cfg.reconciler.call(grt,oldrec,record)
  }



}
