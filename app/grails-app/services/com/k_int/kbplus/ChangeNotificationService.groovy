package com.k_int.kbplus

import com.k_int.kbplus.auth.User
import de.laser.helper.RDConstants
import de.laser.interfaces.AbstractLockableService
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONElement

import java.sql.Timestamp

class ChangeNotificationService extends AbstractLockableService {

    def executorService
    def genericOIDService
    def sessionFactory
    //def cacheService

    // N,B, This is critical for this service as it's called from domain object OnChange handlers
    static transactional = false

  void broadcastEvent(String contextObjectOID, changeDetailDocument) {
    // log.debug("broadcastEvent(${contextObjectOID},${changeDetailDocument})");
    def contextObject = genericOIDService.resolveOID(contextObjectOID);

    def jsonChangeDocument = changeDetailDocument as JSON
      ChangeNotificationQueueItem new_queue_item = new ChangeNotificationQueueItem(oid:contextObjectOID,
                                                         changeDocument:jsonChangeDocument.toString(),
                                                         ts:new Date())
    if ( new_queue_item.save() ) {
      // log.debug("Pending change saved ok");
    }
    else {
      log.error(new_queue_item.errors);
    }

  }


  // Gather together all the changes for a give context object, formate them into an aggregated document
  // notify any registered channels
  boolean aggregateAndNotifyChanges() {
      if(!running) {
          running = true
          def future = executorService.submit({
              internalAggregateAndNotifyChanges();
          } as java.util.concurrent.Callable)

          return true
      }
      else {
          log.warn("Not running, still one process active!")
          return false
      }
  }


    // Sum up all pending changes by OID and write a unified message
    def internalAggregateAndNotifyChanges() {

        boolean running = true

        while (running) {
            try {
                List<ChangeNotificationQueueItem> queueItems = ChangeNotificationQueueItem.executeQuery(
                    "select distinct c.oid from ChangeNotificationQueueItem as c order by c.oid", [max: 1]
                )

                innerInternalAggregateAndNotifyChanges(queueItems)
            }
            catch (Exception e) {
                running = false
                log.error("Problem", e)
            }
            finally {
                running = false
                this.running = false
            }
        }
    }

    def innerInternalAggregateAndNotifyChanges(List<ChangeNotificationQueueItem> queueItems) throws Exception {

        queueItems.each { poidc ->

            def contr = 0
            def contextObject = genericOIDService.resolveOID(poidc);
            log.debug("Got contextObject ${contextObject} for poidc ${poidc}")

            if ( contextObject == null ) {
              log.warn("Pending changes for a now deleted item.. nuke them!");
              ChangeNotificationQueueItem.executeUpdate("delete ChangeNotificationQueueItem c where c.oid = :oid", [oid:poidc])
            }

            List<ChangeNotificationQueueItem> pendingChanges = ChangeNotificationQueueItem.executeQuery(
                    "select c from ChangeNotificationQueueItem as c where c.oid = ? order by c.ts asc", [poidc]
            )
            StringWriter sw = new StringWriter();

            if ( contextObject ) {
              if ( contextObject.metaClass.respondsTo(contextObject, 'getURL') ) {
                  // pendingChange.message_1001
                sw.write("<p>Änderungen an <a href=\"${contextObject.getURL()}\">${contextObject.toString()}</a> ${new Date().toString()}</p><p><ul>");
              }
              else  {
                  // pendingChange.message_1002
                sw.write("<p>Änderungen an ${contextObject.toString()} ${new Date().toString()}</p><p><ul>");
              }
            }

            List pc_delete_list = []

            log.debug("TODO: Processing ${pendingChanges.size()} notifications for object ${poidc}")

        pendingChanges.each { pc ->
          // log.debug("Process pending change ${pc}");    
            JSONElement parsed_event_info = JSON.parse(pc.changeDocument)
          log.debug("Event Info: ${parsed_event_info}")

            ContentItem change_template = ContentItem.findByKey("ChangeNotification.${parsed_event_info.event}")
          if ( change_template != null ) {
            // log.debug("Found change template... ${change_template.content}");
            // groovy.util.Eval.x(r, 'x.' + rh.property)
            def event_props = [o:contextObject, evt:parsed_event_info]
            if ( parsed_event_info.OID != null && parsed_event_info.OID.length() > 0 ) {
              event_props.OID = genericOIDService.resolveOID(parsed_event_info.OID);
            }
            if( event_props.OID ) {

              // Use doStuff to cleverly render change_template with variable substitution 
              // log.debug("Make engine");
              def engine = new groovy.text.GStringTemplateEngine()
              // log.debug("createTemplate..");
              def tmpl = engine.createTemplate(change_template.content).make(event_props)
              // log.debug("Write to string writer");
              sw.write("<li>");
              sw.write(tmpl.toString());
              sw.write("</li>");
            }else{
              // pendingChange.message_1003
              sw.write("<li>Komponente ${parsed_event_info.OID} wurde gelöscht!</li>")
            }
          }
          else {
            // pendingChange.message_1004
            sw.write("<li>Template für das Ereignis \"ChangeNotification.${parsed_event_info.event}\" kann nicht gefunden werden. Infos zum Ereignis:\n\n${pc.changeDocument}</li>");
          }
          contr++;

          if(contr > 0 && contr % 100 == 0){
            log.debug("Processed ${contr} notifications for object ${poidc}")
          }
          pc_delete_list.add(pc.id)

        } // pendingChanges.each{}

        sw.write("</ul></p>");

        if ( contextObject != null ) {
          if ( contextObject.metaClass.respondsTo(contextObject, 'getNotificationEndpoints') ) {
            String announcement_content = sw.toString()
            // Does the objct have a zendesk URL, or any other comms URLs for that matter?
              // How do we decouple Same-As links? Only the object should know about what
            // notification services it's registered with? What about the case where we're adding
            // a new thing? Whats registered?
            contextObject.notificationEndpoints.each { ne ->
              // log.debug("  -> consider ${ne}");
              switch ( ne.service ) {

                case 'announcements':
                    RefdataValue announcement_type = RefdataValue.getByValueAndCategory('Announcement', RDConstants.DOCUMENT_TYPE)
                  // result.recentAnnouncements = Doc.findAllByType(announcement_type,[max:10,sort:'dateCreated',order:'desc'])
                    Doc newAnnouncement = new Doc(title:'Automated Announcement',
                                                type:announcement_type,
                                                content:announcement_content,
                                                dateCreated:new Date(),
                                                user:User.findByUsername('admin')).save(flush:true);

                  break;
                default:
                  break;
              }
            }
          }
        }


            if (pc_delete_list) {
                log.debug('Deleting ChangeNotificationQueueItems: ' + pc_delete_list)
                ChangeNotificationQueueItem.executeUpdate('DELETE FROM ChangeNotificationQueueItem WHERE id in (:idList)', [idList: pc_delete_list])
            }

        // log.debug("Delete reported changes...");
        // If we got this far, all is OK, delete any pending changes
        //pc_delete_list.each { pc ->
          // log.debug("Deleting reported change ${pc.id}");
          //pc.delete()
        //}
        cleanUpGorm()
      } // queueItems.each{}
  }

    /**
    *  An object has changed. Because we don't want to do heavy work of calculating dependent objects in the thread doing the DB
    *  commit, responsibility for handling the change is delegated to this method. However, the source object is the seat of
    *  knowledge for what dependencies there are (For example, a title change should propagate to all packages using that title).
    *  Therefore, we get a new handle to the object.
    */
    def fireEvent(Map<String, Object> changeDocument) {
        log.debug("fireEvent(${changeDocument})")

        //store changeDoc in cache
        //EhcacheWrapper cache = cacheService.getTTL1800Cache("/pendingChanges/")
        //cache.put(changeDocument.OID,changeDocument)

        // TODO [ticket=1807] should not be done in extra thread but collected and saved afterwards
        def submit = executorService.submit({
            Thread.currentThread().setName("PendingChangeSubmission")
            try {
                log.debug("inside executor task submission .. ${changeDocument.OID}")
                def contextObject = genericOIDService.resolveOID(changeDocument.OID)

                log.debug("Context object: ${contextObject}")
                contextObject?.notifyDependencies_trait(changeDocument)
            }
            catch (Exception e) {
                log.error("Problem with event transmission for ${changeDocument.OID}" ,e)
            }
        } as java.util.concurrent.Callable)

    }

    def cleanUpGorm() {
        log.debug("Clean up GORM")
        def session = sessionFactory.currentSession
        session.flush()
        session.clear()
    }


    @Deprecated
    def registerPendingChange(prop, target, desc, objowner, changeMap) {

        def msgToken = null
        def msgParams = null
        def legacyDesc = desc

        registerPendingChange(prop, target, objowner, changeMap, msgToken, msgParams, legacyDesc)
    }

    //def registerPendingChange(prop, target, desc, objowner, changeMap) << legacy
    PendingChange registerPendingChange(String prop, def target, def objowner, def changeMap, String msgToken, def msgParams, String legacyDesc) {
        log.debug("Register pending change ${prop} ${target.class.name}:${target.id}")

        def desc = legacyDesc?.toString() // freeze string before altering referenced values

        // WTF !?

        // JSON converts in UTC,
        // we now add timezone delta to dates
        // so that changedoc entries can be interpreted as local timezone entries

        TimeZone currentTz = Calendar.getInstance().getTimeZone()
        TimeZone utcTz = TimeZone.getTimeZone('UTC')

        // WTF !?
        def deltaTz = 0
        // (currentTz.getRawOffset() + currentTz.getDSTSavings()) - (utcTz.getRawOffset() + utcTz.getDSTSavings())

        changeMap.changeDoc.each { k, v ->
            if (k in ['old', 'new']) {
                if (v instanceof Date || v instanceof Timestamp) {
                    v.setTime(v.getTime() + deltaTz)
                }
            }
        }
        def existsPendingChange = null
        //IF PENDING Change for PKG exists
        if (prop == PendingChange.PROP_PKG) {
            def payload = changeMap as JSON
            def changeDocNew = payload.toString()

            existsPendingChange = PendingChange.findWhere(
                    desc: desc,
                    oid: "${target.class.name}:${target.id}",
                    owner: objowner,
                    msgToken: msgToken,
                    msgParams: null,
                    pkg: target,
                    payload: changeDocNew,
            )
        }
        if (!existsPendingChange) {
            def new_pending_change = new PendingChange(
                    desc: desc,
                    oid: "${target.class.name}:${target.id}",
                    owner: objowner,
                    msgToken: msgToken,
                    ts: new Date()
            )

            new_pending_change[prop] = target;

            def payload = changeMap as JSON
            new_pending_change.payload = payload.toString()

            def jsonMsgParams = msgParams as JSON
            new_pending_change.msgParams = msgParams ? jsonMsgParams.toString() : null

            new_pending_change.workaroundForDatamigrate() // ERMS-2184

            if (new_pending_change.save(failOnError: true)) {
                return new_pending_change
            } else {
                log.error("Problem saving pending change: ${new_pending_change.errors}")
            }
            return null
        }
    }

}
