package com.k_int.kbplus

import com.k_int.kbplus.abstract_domain.AbstractPropertyWithCalculatedLastUpdated
import com.k_int.kbplus.auth.Role
import com.k_int.kbplus.auth.User
import com.k_int.properties.PropertyDefinition
import com.k_int.properties.PropertyDefinitionGroup
import com.k_int.properties.PropertyDefinitionGroupBinding
import de.laser.AuditConfig
import de.laser.DashboardDueDate
import de.laser.DashboardDueDatesService
import de.laser.DueDateObject
import de.laser.base.AbstractI10nOverride
import de.laser.base.AbstractI10nTranslatable
import de.laser.I10nTranslation
import de.laser.SystemProfiler
import de.laser.helper.*
import de.laser.interfaces.ShareSupport
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.support.RequestContextUtils

import java.text.SimpleDateFormat

//import org.grails.orm.hibernate.cfg.GrailsHibernateUtil

import java.util.regex.Matcher
import java.util.regex.Pattern

@Secured(['permitAll']) // TODO
class AjaxController {

    def genericOIDService
    def contextService
    def taskService
    def controlledListService
    def dataConsistencyService
    def accessService
    def debugService

    def refdata_config = [
    "ContentProvider" : [
      domain:'Org',
      countQry:"select count(o) from Org as o where exists (select roletype from o.orgType as roletype where roletype.value = 'Provider' ) and lower(o.name) like ? and (o.status is null or o.status != ?)",
      rowQry:"select o from Org as o where exists (select roletype from o.orgType as roletype where roletype.value = 'Provider' ) and lower(o.name) like ? and (o.status is null or o.status != ?) order by o.name asc",
      qryParams:[
              [
                param:'sSearch',
                clos:{ value ->
                    def result = '%'
                    if ( value && ( value.length() > 0 ) )
                        result = "%${value.trim().toLowerCase()}%"
                    result
                }
              ]
      ],
      cols:['name'],
      format:'map'
    ],
    "Licenses" : [
      domain:'License',
      countQry:"select count(l) from License as l",
      rowQry:"select l from License as l",
      qryParams:[],
      cols:['reference'],
      format:'simple'
    ],
    'Currency' : [
      domain:'RefdataValue',
      countQry:"select count(rdv) from RefdataValue as rdv where rdv.owner.desc='${RDConstants.CURRENCY}'",
      rowQry:"select rdv from RefdataValue as rdv where rdv.owner.desc='${RDConstants.CURRENCY}'",
      qryParams:[
                   [
                      param:'iDisplayLength',
                      value: 200
                   ]
      ],
      cols:['value'],
      format:'simple'
    ],
    "allOrgs" : [
            domain:'Org',
            countQry:"select count(o) from Org as o where lower(o.name) like ? and (o.status is null or o.status != ?)",
            rowQry:"select o from Org as o where lower(o.name) like ? and (o.status is null or o.status != ?) order by o.name asc",
            qryParams:[
                    [
                            param:'sSearch',
                            clos:{ value ->
                                def result = '%'
                                if ( value && ( value.length() > 0 ) )
                                    result = "%${value.trim().toLowerCase()}%"
                                result
                            }
                    ]
            ],
            cols:['name'],
            format:'map'
    ],
    "CommercialOrgs" : [
            domain:'Org',
            countQry:"select count(o) from Org as o where (o.sector.value = 'Publisher') and lower(o.name) like ? and (o.status is null or o.status != ?)",
            rowQry:"select o from Org as o where (o.sector.value = 'Publisher') and lower(o.name) like ? and (o.status is null or o.status != ?) order by o.name asc",
            qryParams:[
                    [
                            param:'sSearch',
                            clos:{ value ->
                                def result = '%'
                                if ( value && ( value.length() > 0 ) )
                                    result = "%${value.trim().toLowerCase()}%"
                                result
                            }
                    ]
            ],
            cols:['name'],
            format:'map'
    ]
  ]

    def genericDialogMessage() {

        if (params.template) {
            render template: "/templates/ajax/${params.template}", model: [a: 1, b: 2, c: 3]
        }
        else {
            render '<p>invalid call</p>'
        }
    }


    def notifyProfiler() {
        Map<String, Object> result = [status:'failed']

        SessionCacheWrapper cache = contextService.getSessionCache()
        DebugUtil debugUtil = (DebugUtil) cache.get(DebugUtil.SYSPROFILER_SESSION)

        if (debugUtil) {
            long delta = debugUtil.stopSimpleBench(params.uri)

            SystemProfiler.update(delta, params.uri)

            result.uri = params.uri
            result.delta = delta
            result.status = 'ok'
        }

        render result as JSON
    }

    def updateSessionCache() {
        if (contextService.getUser()) {
            SessionCacheWrapper cache = contextService.getSessionCache()

            if (params.key == UserSettings.KEYS.SHOW_EXTENDED_FILTER.toString()) {

                if (params.uri) {
                    cache.put("${params.key}/${params.uri}", params.value)
                    log.debug("update session based user setting: [${params.key}/${params.uri} -> ${params.value}]")
                }
            }
        }

        if (params.redirect) {
            redirect(url: request.getHeader('referer'))
        }
        Map<String, Object> result = [:]
        render result as JSON
    }

  @Secured(['ROLE_USER'])
  def setFieldNote() {
    def domain_class=grailsApplication.getArtefact('Domain',"com.k_int.kbplus.${params.type}")
    if ( domain_class ) {
      def instance = domain_class.getClazz().get(params.id)
      if ( instance ) {
        if ( params.elementid?.startsWith('__fieldNote_') ) {
          def note_domain = params.elementid.substring(12)
          instance.setNote(note_domain, params.value);
          instance.save(flush:true)
        }
      }
    }
    else {
      log.error("no type");
    }

    response.setContentType('text/plain')
    def outs = response.outputStream
    outs << params.value
    outs.flush()
    outs.close()
  }

   @Secured(['ROLE_USER'])
  def setFieldTableNote() {
    // log.debug("setFieldTableNote(${params})")
    def domain_class=grailsApplication.getArtefact('Domain',"com.k_int.kbplus.${params.type}")
    if ( domain_class ) {
      def instance = domain_class.getClazz().get(params.id)
       
      if ( instance ) {
        String temp = '__fieldNote_'+params.name
        if ( temp?.startsWith('__fieldNote_') ) {
          def note_domain = temp.substring(12)
          // log.debug("note_domain: " + note_domain +" : \""+ params.value+"\"")
          instance.setNote(note_domain, params.value);
          instance.save(flush:true)
        }
      }
    }
    else {
      log.error("no type");
    }

    response.setContentType('text/plain')
    def outs = response.outputStream
    outs << params.value
    outs.flush()
    outs.close()
  }

    @Secured(['ROLE_USER'])
    def genericSetValue() {
    // [id:1, value:JISC_Collections_NESLi2_Lic_IOP_Institute_of_Physics_NESLi2_2011-2012_01012011-31122012.., type:License, action:inPlaceSave, controller:ajax
    // def clazz=grailsApplication.domainClasses.findByFullName(params.type)
    // log.debug("genericSetValue:${params}");
        def result = params.value

        try {


    // params.elementid (The id from the html element)  must be formed as domain:pk:property:otherstuff
    String[] oid_components = params.elementid.split(":");

    def domain_class=grailsApplication.getArtefact('Domain',"com.k_int.kbplus.${oid_components[0]}")

    if ( domain_class ) {
      def instance = domain_class.getClazz().get(oid_components[1])
      if ( instance ) {

        def value = params.value;
        if ( value == '__NULL__' ) {
           value=null;
           result='';
        }
        else {
          if ( params.dt == 'date' ) {
            // log.debug("Special date processing, idf=${params.idf}");
              SimpleDateFormat formatter = new SimpleDateFormat(params.idf)
            value = formatter.parse(params.value)
            if ( params.odf ) {
                SimpleDateFormat of = new SimpleDateFormat(params.odf)
              result=of.format(value);
            }
            else {
                SimpleDateFormat of = DateUtil.getSDF_NoTime()
              result=of.format(value)
            }
          }
        }
        // log.debug("Got instance ${instance}");
        def binding_properties = [ "${oid_components[2]}":value ]
        // log.debug("Merge: ${binding_properties}");
        // see http://grails.org/doc/latest/ref/Controllers/bindData.html
        bindData(instance, binding_properties)
        instance.save()
      }
      else {
        log.debug("no instance");
      }
    }
    else {
      log.debug("no type");
    }

        } catch (Exception e) {
            log.error("@ genericSetValue()")
            log.error(e)
        }

        log.debug("genericSetValue() returns ${result}")
        response.setContentType('text/plain')

        def outs = response.outputStream
        outs << result
        outs.flush()
        outs.close()
    }

    @Secured(['ROLE_USER'])
    def genericSetRel() {
        def result = ''

        try {

    String[] target_components = params.pk.split(":");

    def target = genericOIDService.resolveOID(params.pk);
    if ( target ) {
      if ( params.value == '' ) {
        // Allow user to set a rel to null be calling set rel ''
        target[params.name] = null
          if ( ! target.save()){
              Map r = [status:"error", msg: message(code: 'default.save.error.general.message')]
              render r as JSON
              return
          }
      }
      else {
        String[] value_components = params.value.split(":");
        def value = genericOIDService.resolveOID(params.value);

        if ( target && value ) {

            if (target instanceof UserSettings) {
                target.setValue(value)
            } else {
                def binding_properties = ["${params.name}": value]
                bindData(target, binding_properties)
                //if (target.hasProperty(params.name)) {
                //    target."${params.name}" = value
                //}
                //if (target.hasProperty('owner')) {
                //    target.owner?.save()  // avoid .. not processed by flush
                //}
            }

            if ( ! target.save()){
                Map r = [status:"error", msg: message(code: 'default.save.error.general.message')]
                render r as JSON
                return
            }
            if (target instanceof SurveyResult) {

                Org org = contextService.getOrg()
                //If Survey Owner set Value then set FinishDate
                if (org?.id == target?.owner?.id && target?.finishDate == null) {
                    String property = ""
                    if (target?.type?.type == Integer.toString()) {
                        property = "intValue"
                    } else if (target?.type?.type == String.toString()) {
                        property = "stringValue"
                    } else if (target?.type?.type == BigDecimal.toString()) {
                        property = "decValue"
                    } else if (target?.type?.type == Date.toString()) {
                        property = "dateValue"
                    } else if (target?.type?.type == URL.toString()) {
                        property = "urlValue"
                    } else if (target?.type?.type == RefdataValue.toString()) {
                        property = "refValue"
                    }

                    if (target[property] != null) {
                        log.debug("Set/Save FinishDate of SurveyResult (${target.id})")
                        target.finishDate = new Date()
                        target.save()
                    }
            }
        }

          // We should clear the session values for a user if this is a user to force reload of the,
          // parameters.
          if (target instanceof User) {
            session.userPereferences = null
          }

            if (target instanceof UserSettings) {
                if (target.key.toString() == 'LANGUAGE') {
                    Locale newLocale = new Locale(value.value, value.value.toUpperCase())
                    log.debug("UserSettings: LANGUAGE changed to: " + newLocale)

                    LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request)
                    localeResolver.setLocale(request, response, newLocale)
                }
            }

          if ( params.resultProp ) {
            result = value[params.resultProp]
          }
          else {
            if ( value ) {
              result = renderObjectValue(value);
              // result = value.toString()
            }
          }
        }
        else {
          log.debug("no value (target=${target_components}, value=${value_components}");
        }
      }
    }
    else {
      log.error("no target (target=${target_components}");
    }

        } catch (Exception e) {
            log.error("@ genericSetRel()")
            log.error(e)
        }

        def resp = [ newValue: result ]
        log.debug("genericSetRel() returns ${resp as JSON}")
        render resp as JSON
    }

  def orgs() {
    // log.debug("Orgs: ${params}");

    def result = [
      options:[]
    ]

    def query_params = ["%${params.query.trim().toLowerCase()}%"];

    // log.debug("q params: ${query_params}");

    // result.options = Org.executeQuery("select o.name from Org as o where lower(o.name) like ? order by o.name desc",["%${params.query.trim().toLowerCase()}%"],[max:10]);
    def ol = Org.executeQuery("select o from Org as o where lower(o.name) like ? order by o.name asc",query_params,[max:10,offset:0]);

    ol.each {
      result.options.add(it.name);
    }

    render result as JSON
  }

  def validatePackageId() {
    Map<String, Object> result = [:]
    result.response = false;
    if( params.id ) {
        Package p = Package.findByIdentifier(params.id)
      if ( !p ) {
        result.response = true
      }
    }

    render result as JSON
  }

  def generateBoolean() {
    def result = [
        [value: 1, text: RDStore.YN_YES.getI10n('value')],
        [value: 0, text: RDStore.YN_NO.getI10n('value')]
    ]
    render result as JSON
  }

  @Deprecated
  def refdataSearch() {
      // TODO: refactoring - only used by /templates/_orgLinksModal.gsp

    //log.debug("refdataSearch params: ${params}");
    
    Map<String, Object> result = [:]
    //we call toString in case we got a GString
    def config = refdata_config.get(params.id?.toString())

    if ( config == null ) {
      // If we werent able to locate a specific config override, assume the ID is just a refdata key
      config = [
        domain:'RefdataValue',
        countQry:"select count(rdv) from RefdataValue as rdv where rdv.owner.desc='${params.id}'",
        rowQry:"select rdv from RefdataValue as rdv where rdv.owner.desc='${params.id}'",
        qryParams:[],
        cols:['value'],
        format:'simple'
      ]
    }

    if ( config ) {

      // result.config = config

      def query_params = []
      config.qryParams.each { qp ->
        log.debug("Processing query param ${qp} value will be ${params[qp.param]}");
        if ( qp.clos ) {
          query_params.add(qp.clos(params[qp.param]?:''));
        }
        else {
          query_params.add(params[qp.param]);
        }
      }

        if (config.domain == 'Org') {
            // new added param for org queries in this->refdata_config
            query_params.add(RefdataValue.getByValueAndCategory('Deleted', RDConstants.ORG_STATUS))
        }

        //log.debug("Row qry: ${config.rowQry}");
        //log.debug("Params: ${query_params}");
        //log.debug("Count qry: ${config.countQry}");

      def cq = Org.executeQuery(config.countQry,query_params);    

      def rq = Org.executeQuery(config.rowQry,
                                query_params,
                                [max:params.iDisplayLength?:1000,offset:params.iDisplayStart?:0]);

      if ( config.format=='map' ) {
        result.aaData = []
        result.sEcho = params.sEcho
        result.iTotalRecords = cq[0]
        result.iTotalDisplayRecords = cq[0]
    
        rq.each { it ->
          def rowobj = GrailsHibernateUtil.unwrapIfProxy(it)
          int ctr = 0;
          def row = [:]
          config.cols.each { cd ->
            // log.debug("Processing result col ${cd} pos ${ctr}");
            row["${ctr++}"] = rowobj[cd]
          }
          row["DT_RowId"] = "${rowobj.class.name}:${rowobj.id}"
          result.aaData.add(row)
        }
      }
      else {
        rq.each { it ->
          def rowobj = GrailsHibernateUtil.unwrapIfProxy(it)
          result["${rowobj.class.name}:${rowobj.id}"] = rowobj[config.cols[0]];
        }
      }
    }

    // log.debug("refdataSearch returning ${result as JSON}");
    withFormat {
      html {
        result
      }
      json {
        render result as JSON
      }
    }
  }

    def propertyAlternativesSearchByOID() {
        def result = []
        def pd = genericOIDService.resolveOID(params.oid)

        def queryResult = PropertyDefinition.findAllWhere(
                descr: pd.descr,
                refdataCategory: pd.refdataCategory,
                type: pd.type,
                multipleOccurrence: pd.multipleOccurrence,
                tenant: pd.tenant
        )//.minus(pd)

        queryResult.each { it ->
            def rowobj = GrailsHibernateUtil.unwrapIfProxy(it)
            if (pd.isUsedForLogic) {
                if (it.isUsedForLogic) {
                    result.add([value: "${rowobj.class.name}:${rowobj.id}", text: "${it.getI10n('name')}"])
                }
            }
            else {
                if (! it.isUsedForLogic) {
                    result.add([value: "${rowobj.class.name}:${rowobj.id}", text: "${it.getI10n('name')}"])
                }
            }
        }

        if (result.size() > 1) {
           result.sort{ x,y -> x.text.compareToIgnoreCase y.text }
        }

        withFormat {
            html {
                result
            }
            json {
                render result as JSON
            }
        }
    }

    /**
     * Copied legacy sel2RefdataSearch(), but uses OID.
     *
     * @return
     */
    def refdataSearchByOID() {
        def result = []
        def rdc = genericOIDService.resolveOID(params.oid)

        def config = [
                domain:'RefdataValue',
                countQry:"select count(rdv) from RefdataValue as rdv where rdv.owner.id='${rdc?.id}'",
                rowQry:"select rdv from RefdataValue as rdv where rdv.owner.id='${rdc?.id}'",
                qryParams:[],
                cols:['value'],
                format:'simple'
        ]

        def query_params = []
        config.qryParams.each { qp ->
            if (qp?.clos) {
                query_params.add(qp.clos(params[qp.param]?:''));
            }
            else if(qp?.value) {
                params."${qp.param}" = qp?.value
            }
            else {
                query_params.add(params[qp.param]);
            }
        }

        def cq = RefdataValue.executeQuery(config.countQry, query_params);
        def rq = RefdataValue.executeQuery(config.rowQry,
                query_params,
                [max:params.iDisplayLength?:1000, offset:params.iDisplayStart?:0]);

        rq.each { it ->
            def rowobj = GrailsHibernateUtil.unwrapIfProxy(it)

            if ( it instanceof AbstractI10nTranslatable) {
                result.add([value:"${rowobj.class.name}:${rowobj.id}", text:"${it.getI10n(config.cols[0])}"])
            }
            else if ( it instanceof AbstractI10nOverride) {
                result.add([value:"${rowobj.class.name}:${rowobj.id}", text:"${it.getI10n(config.cols[0])}"])
            }
            else {
                def objTest = rowobj[config.cols[0]]
                if (objTest) {
                    def no_ws = objTest.replaceAll(' ','');
                    def local_text = message(code:"refdata.${no_ws}", default:"${objTest}");
                    result.add([value:"${rowobj.class.name}:${rowobj.id}", text:"${local_text}"])
                }
            }
        }

        if(result) {
            RefdataValue notSet = RDStore.GENERIC_NULL_VALUE
            result.add([value:"${notSet.class.name}:${notSet.id}",text:notSet.getI10n("value")])
            result.sort{ x,y -> x.text.compareToIgnoreCase y.text  }
        }

        withFormat {
            html {
                result
            }
            json {
                render result as JSON
            }
        }
    }

    def getPropValues() {
        Set result = []
        if(params.oid != "undefined") {
            PropertyDefinition propDef = (PropertyDefinition) genericOIDService.resolveOID(params.oid)
            if(propDef) {
                List<AbstractPropertyWithCalculatedLastUpdated> values
                if(propDef.tenant) {
                    switch(params.domain) {
                        case 'currentSubscriptions':
                        case 'manageConsortiaSubscriptions': values = SubscriptionProperty.findAllByTypeAndTenantAndIsPublic(propDef,contextService.org,false)
                            break
                        case 'currentLicenses': values = LicenseProperty.findAllByTypeAndTenantAndIsPublic(propDef,contextService.org,false)
                            break
                        case 'listProvider':
                        case 'currentProviders':
                        case 'manageMembers': values = OrgProperty.findAllByTypeAndTenantAndIsPublic(propDef,contextService.org,false)
                            break
                        case 'addressbook': values = PersonProperty.findAllByType(propDef)
                            break
                    }
                }
                else {
                    switch(params.domain) {
                        case 'currentSubscriptions':
                        case 'manageConsortiaSubscriptions': values = SubscriptionProperty.executeQuery('select sp from SubscriptionProperty sp where sp.type = :propDef and sp.tenant = :tenant and sp.isPublic = true',[propDef:propDef, tenant:contextService.org])
                            break
                        case 'currentLicenses': values = LicenseProperty.executeQuery('select lcp from LicenseProperty lcp join lcp.owner l join l.orgLinks oo where lcp.type = :propDef and oo.org = :tenant',[propDef:propDef, tenant:contextService.org])
                            break
                        case 'listProvider':
                        case 'currentProviders':
                        case 'manageMembers': values = OrgProperty.executeQuery('select op from OrgProperty op where op.type = :propDef and op.tenant = :tenant and op.isPublic = true',[propDef:propDef,tenant:contextService.org])
                            break
                    }
                }

                if(values) {
                    if(propDef.type == Integer.toString()) {
                        values.each { AbstractPropertyWithCalculatedLastUpdated v ->
                            if(v.intValue != null)
                                result.add([value:v.intValue.toInteger(),text:v.intValue.toInteger()])
                        }
                        result = result.sort { x, y -> x.text.compareTo y.text}
                    }
                    else {
                        values.each { AbstractPropertyWithCalculatedLastUpdated v ->
                            if(v.getValue() != null)
                                result.add([value:v.getValue(),text:v.getValue()])
                        }
                        result = result.sort { x, y -> x.text.compareToIgnoreCase y.text}
                    }
                }
            }
        }
        //excepted structure: [[value:,text:],[value:,text:]]
        withFormat {
            json {
                render result as JSON
            }
        }
    }

    def sel2RefdataSearch() {

        log.debug("sel2RefdataSearch params: ${params}");
    
        List result = []
        Map<String, Object> config = refdata_config.get(params.id?.toString()) //we call toString in case we got a GString
        boolean defaultOrder = true

        if (config == null) {
            String locale = I10nTranslation.decodeLocale(LocaleContextHolder.getLocale().toString())
            defaultOrder = false
            // If we werent able to locate a specific config override, assume the ID is just a refdata key
            config = [
                domain      :'RefdataValue',
                countQry    :"select count(rdv) from RefdataValue as rdv where rdv.owner.desc='${params.id}'",
                rowQry      :"select rdv from RefdataValue as rdv where rdv.owner.desc='${params.id}' order by rdv.value_${locale}",
                qryParams   :[],
                cols        :['value'],
                format      :'simple'
            ]
        }

    if ( config ) {

      List query_params = []
      config.qryParams.each { qp ->
        if ( qp?.clos) {
          query_params.add(qp.clos(params[qp.param]?:''));
        }
        else if(qp?.value) {
            params."${qp.param}" = qp?.value
        }
        else {
          query_params.add(params[qp.param]);
        }
      }

      def cq = RefdataValue.executeQuery(config.countQry,query_params);
      def rq = RefdataValue.executeQuery(config.rowQry,
                                query_params,
                                [max:params.iDisplayLength?:1000,offset:params.iDisplayStart?:0]);

      rq.each { it ->
        def rowobj = GrailsHibernateUtil.unwrapIfProxy(it)

          // handle custom constraint(s) ..
          if (it.value.equalsIgnoreCase('deleted') && params.constraint?.contains('removeValue_deleted')) {
              log.debug('ignored value "' + it + '" from result because of constraint: '+ params.constraint)
          }
          if (it.value.equalsIgnoreCase('administrative subscription') && params.constraint?.contains('removeValue_administrativeSubscription')) {
              log.debug('ignored value "' + it + '" from result because of constraint: '+ params.constraint)
          }
          //value is correct incorrectly translated!
          if (it.value.equalsIgnoreCase('local licence') && accessService.checkPerm("ORG_CONSORTIUM") && params.constraint?.contains('removeValue_localSubscription')) {
              log.debug('ignored value "' + it + '" from result because of constraint: '+ params.constraint)
          }
          // default ..
          else {
              if (it instanceof AbstractI10nTranslatable) {
                  result.add([value: "${rowobj.class.name}:${rowobj.id}", text: "${it.getI10n(config.cols[0])}"])
              }
              else if (it instanceof AbstractI10nOverride) {
                  result.add([value: "${rowobj.class.name}:${rowobj.id}", text: "${it.getI10n(config.cols[0])}"])
              }
              else {
                  def objTest = rowobj[config.cols[0]]
                  if (objTest) {
                      def no_ws = objTest.replaceAll(' ', '');
                      def local_text = message(code: "refdata.${no_ws}", default: "${objTest}");
                      result.add([value: "${rowobj.class.name}:${rowobj.id}", text: "${local_text}"])
                  }
              }
          }
      }
    }
    else {
      log.error("No config for refdata search ${params.id}");
    }

      if (result && defaultOrder) {
          result.sort{ x,y -> x.text.compareToIgnoreCase y.text  }
      }

    withFormat {
      html {
        result
      }
      json {
        render result as JSON
      }
    }
  }

  @Secured(['ROLE_USER'])
  def lookupIssueEntitlements() {
    params.checkView = true
    if(params.sub != "undefined")
        render controlledListService.getIssueEntitlements(params) as JSON
    else {
        Map entry = ["results": []]
        render entry as JSON
    }
  }

  @Secured(['ROLE_USER'])
  def lookupTitleGroups() {
     params.checkView = true
     if(params.sub != "undefined")
        render controlledListService.getTitleGroups(params) as JSON
      else {
         Map empty = [results: []]
         render empty as JSON
     }
    }

  @Secured(['ROLE_USER'])
  def lookupSubscriptions() {
    render controlledListService.getSubscriptions(params) as JSON
  }

  @Secured(['ROLE_USER'])
  def lookupSubscriptionsLicenses() {
    Map result = [results:[]]
    result.results.addAll(controlledListService.getSubscriptions(params).results)
    result.results.addAll(controlledListService.getLicenses(params).results)
    render result as JSON
  }

  @Secured(['ROLE_USER'])
  def lookupSubscriptions_IndendedAndCurrent() {
      params.status = [RDStore.SUBSCRIPTION_INTENDED, RDStore.SUBSCRIPTION_CURRENT]
      render controlledListService.getSubscriptions(params) as JSON
  }

  @Secured(['ROLE_USER'])
  def lookupSubscriptionPackages() {
      if(params.ctx != "undefined")
        render controlledListService.getSubscriptionPackages(params) as JSON
      else render [:] as JSON
  }

  @Secured(['ROLE_USER'])
  def lookupLicenses() {
    render controlledListService.getLicenses(params) as JSON
  }

    @Secured(['ROLE_USER'])
    def lookupProviderAndPlatforms() {
        def result = []

        List<Org> provider = Org.executeQuery('SELECT o FROM Org o JOIN o.orgType ot WHERE ot = :ot', [ot: RDStore.OT_PROVIDER])
        provider.each{ prov ->
            Map<String, Object> pp = [name: prov.name, value: prov.class.name + ":" + prov.id, platforms:[]]

            Platform.findAllByOrg(prov).each { plt ->
                pp.platforms.add([name: plt.name, value: plt.class.name + ":" + plt.id])
            }
            result.add(pp)
        }
        render result as JSON
    }

    @Secured(['ROLE_USER'])
    def lookupProvidersAgencies() {
        render controlledListService.getProvidersAgencies(params) as JSON
    }

  @Secured(['ROLE_USER'])
  def lookupBudgetCodes() {
      render controlledListService.getBudgetCodes(params) as JSON
  }

  @Secured(['ROLE_USER'])
  def lookupInvoiceNumbers() {
      render controlledListService.getInvoiceNumbers(params) as JSON
  }

  @Secured(['ROLE_USER'])
  def lookupOrderNumbers() {
      render controlledListService.getOrderNumbers(params) as JSON
  }

  @Secured(['ROLE_USER'])
  def lookupReferences() {
      render controlledListService.getReferences(params) as JSON
  }

  @Secured(['ROLE_USER'])
  def lookupCombined() {
      render controlledListService.getElements(params) as JSON
  }

  @Secured(['ROLE_USER'])
  def checkCascade() {
      Map result = [sub:true,subPkg:true,ie:true]
      if(!params.subscription && ((params.package && params.issueEntitlement) || params.issueEntitlement)) {
          result.sub = false
          result.subPkg = false
          result.ie = false
      }
      else if(params.subscription) {
          Subscription sub = genericOIDService.resolveOID(params.subscription)
          if(!sub) {
              result.sub = false
              result.subPkg = false
              result.ie = false
          }
          else if(params.issueEntitlement) {
              if(!params.package || params.package.contains('null')) {
                  result.subPkg = false
                  result.ie = false
              }
              else if(params.package && !params.package.contains('null')) {
                  SubscriptionPackage subPkg = genericOIDService.resolveOID(params.package)
                  if(!subPkg || subPkg.subscription != sub) {
                      result.subPkg = false
                      result.ie = false
                  }
                  else {
                      IssueEntitlement ie = genericOIDService.resolveOID(params.issueEntitlement)
                      if(!ie || ie.subscription != subPkg.subscription || ie.tipp.pkg != subPkg.pkg) {
                          result.ie = false
                      }
                  }
              }
          }
      }
      //Map result = [sub: params.subscription ? true : false,subPkg: params.package && !params.package.contains(":null"),ie: params.issueEntitlement ? true : false]
      render result as JSON
  }

  @DebugAnnotation(test = 'hasRole("ROLE_ADMIN") || hasAffiliation("INST_ADM")')
  @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasRole('ROLE_ADMIN') || ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_ADM") })
  def verifyUserInput() {
      Map result = [result:false]
      if(params.input) {
          List<User> checkList = User.executeQuery("select u from User u where u.username = lower(:searchTerm)",[searchTerm:params.input])
          result.result = checkList.size() > 0
      }
      render result as JSON
  }

  @Secured(['ROLE_USER'])
  def updateChecked() {
      Map success = [success:false]
      EhcacheWrapper cache = contextService.getCache("/subscription/${params.referer}/${params.sub}", contextService.USER_SCOPE)
      Map checked = cache.get('checked')
      if(params.index == 'all') {
		  def newChecked = [:]
		  checked.eachWithIndex { e, int idx ->
			  newChecked[e.key] = params.checked == 'true' ? 'checked' : null
			  cache.put('checked',newChecked)
		  }
	  }
	  else {
		  checked[params.index] = params.checked == 'true' ? 'checked' : null
		  if(cache.put('checked',checked))
			  success.success = true
	  }

      render success as JSON
  }

  @Secured(['ROLE_USER'])
  def updateIssueEntitlementOverwrite() {
      Map success = [success:false]
      EhcacheWrapper cache = contextService.getCache("/subscription/${params.referer}/${params.sub}", contextService.USER_SCOPE)
      Map issueEntitlementCandidates = cache.get('issueEntitlementCandidates')
      def ieCandidate = issueEntitlementCandidates.get(params.key)
      if(!ieCandidate)
          ieCandidate = [:]
      if(params.coverage) {
          def ieCoverage
          Pattern pattern = Pattern.compile("(\\w+)(\\d+)")
          Matcher matcher = pattern.matcher(params.prop)
          if(matcher.find()) {
              String prop = matcher.group(1)
              int covStmtKey = Integer.parseInt(matcher.group(2))
              if(!ieCandidate.coverages){
                  ieCandidate.coverages = []
                  ieCoverage = [:]
              }
              else
                  ieCoverage = ieCandidate.coverages[covStmtKey]
              if(prop in ['startDate','endDate']) {
                  SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
                  ieCoverage[prop] = sdf.parse(params.propValue)
              }
              else {
                  ieCoverage[prop] = params.propValue
              }
              ieCandidate.coverages[covStmtKey] = ieCoverage
          }
          else {
              log.error("something wrong with the regex matching ...")
          }
      }
      else {
          ieCandidate[params.prop] = params.propValue
      }
      issueEntitlementCandidates.put(params.key,ieCandidate)
      if(cache.put('issueEntitlementCandidates',issueEntitlementCandidates))
          success.success = true
      render success as JSON
  }

    @Secured(['ROLE_USER'])
    def addOrgRole() {
        def owner  = genericOIDService.resolveOID(params.parent)
        def rel    = RefdataValue.get(params.orm_orgRole)

        def orgIds = params.list('orm_orgOid')
        orgIds.each{ oid ->
            def org_to_link = genericOIDService.resolveOID(oid)
            boolean duplicateOrgRole = false

            if(params.recip_prop == 'sub') {
                duplicateOrgRole = OrgRole.findAllBySubAndRoleTypeAndOrg(owner, rel, org_to_link) ? true : false
            }
            else if(params.recip_prop == 'pkg') {
                duplicateOrgRole = OrgRole.findAllByPkgAndRoleTypeAndOrg(owner, rel, org_to_link) ? true : false
            }
            else if(params.recip_prop == 'lic') {
                duplicateOrgRole = OrgRole.findAllByLicAndRoleTypeAndOrg(owner, rel, org_to_link) ? true : false
            }
            else if(params.recip_prop == 'title') {
                duplicateOrgRole = OrgRole.findAllByTitleAndRoleTypeAndOrg(owner, rel, org_to_link) ? true : false
            }

            if(! duplicateOrgRole) {
                def new_link = new OrgRole(org: org_to_link, roleType: rel)
                new_link[params.recip_prop] = owner

                if (new_link.save(flush: true)) {
                    // log.debug("Org link added")
                    if (owner instanceof ShareSupport && owner.checkSharePreconditions(new_link)) {
                        new_link.isShared = true
                        new_link.save(flush:true)

                        owner.updateShare(new_link)
                    }
                } else {
                    log.error("Problem saving new org link ..")
                    new_link.errors.each { e ->
                        log.error(e)
                    }
                    //flash.error = message(code: 'default.error')
                }
            }
        }
        redirect(url: request.getHeader('referer'))
    }

    @Secured(['ROLE_USER'])
    def delOrgRole() {
        def or = OrgRole.get(params.id)

        def owner = or.getOwner()
        if (owner instanceof ShareSupport && or.isShared) {
            or.isShared = false
            owner.updateShare(or)
        }
        or.delete(flush:true)

        redirect(url: request.getHeader('referer'))
    }

    @Secured(['ROLE_USER'])
    def addPrsRole() {
        def org     = genericOIDService.resolveOID(params.org)
        def parent  = genericOIDService.resolveOID(params.parent)
        def person  = genericOIDService.resolveOID(params.person)
        def role    = genericOIDService.resolveOID(params.role)

        def newPrsRole
        def existingPrsRole

        if (org && person && role) {
            newPrsRole = new PersonRole(prs: person, org: org)
            if (parent) {
                newPrsRole.responsibilityType = role
                newPrsRole.setReference(parent)

                def ref = newPrsRole.getReference().split(":")
                existingPrsRole = PersonRole.findWhere(prs:person, org: org, responsibilityType: role, "${ref[0]}": parent)
            }
            else {
                newPrsRole.functionType = role
                existingPrsRole = PersonRole.findWhere(prs:person, org: org, functionType: role)
            }
        }

        if (! existingPrsRole && newPrsRole && newPrsRole.save(flush:true)) {
            //flash.message = message(code: 'default.success')
        }
        else {
            log.error("Problem saving new person role ..")
            //flash.error = message(code: 'default.error')
        }

        redirect(url: request.getHeader('referer'))
    }

    @Secured(['ROLE_USER'])
    def delPrsRole() {
        def prsRole = PersonRole.get(params.id)

        if (prsRole && prsRole.delete(flush: true)) {
        }
        else {
            log.error("Problem deleting person role ..")
            //flash.error = message(code: 'default.error')
        }
        redirect(url: request.getHeader('referer'))
    }

    @Secured(['ROLE_USER'])
    def addRefdataValue() {

        def newRefdataValue
        def error
        def msg

        def rdc = RefdataCategory.findById(params.refdata_category_id)

        if (RefdataValue.getByValueAndCategory(params.refdata_value, rdc.desc)) {
            error = message(code: "refdataValue.create_new.unique")
            log.debug(error)
        }
        else {
            Map<String, Object> map = [
                    token   : params.refdata_value,
                    rdc     : rdc.desc,
                    hardData: false,
                    i10n    : [value_de: params.refdata_value, value_en: params.refdata_value]
            ]

            newRefdataValue = RefdataValue.construct(map)

            if (newRefdataValue?.hasErrors()) {
                log.error(newRefdataValue.errors)
                error = message(code: 'default.error')
            }
            else {
                msg = message(code: 'refdataValue.created', args: [newRefdataValue.value])
            }
        }

        if (params.reloadReferer) {
            flash.newRefdataValue = newRefdataValue
            flash.error   = error
            flash.message = msg
            redirect(url: params.reloadReferer)
        }
    }

    @Secured(['ROLE_USER'])
    def addRefdataCategory() {

        def newRefdataCategory
        def error
        def msg

        def rdc = RefdataCategory.getByDesc(params.refdata_category)
        if (rdc) {
            error = message(code: 'refdataCategory.create_new.unique')
            log.debug(error)
        }
        else {
            Map<String, Object> map = [
                    token   : params.refdata_category,
                    hardData: false,
                    i10n    : [desc_de: params.refdata_category, desc_en: params.refdata_category]
            ]

            newRefdataCategory = RefdataCategory.construct(map)

            if (newRefdataCategory?.hasErrors()) {
                log.error(newRefdataCategory.errors)
                error = message(code: 'default.error')
            }
            else {
                msg = message(code: 'refdataCategory.created', args: [newRefdataCategory.desc])
            }
        }

        if (params.reloadReferer) {
            flash.newRefdataCategory = newRefdataCategory
            flash.error   = error
            flash.message = msg
            redirect(url: params.reloadReferer)
        }
    }

    @Secured(['ROLE_USER'])
    def addCustomPropertyType() {
        def newProp
        def error
        def msg
        def ownerClass = params.ownerClass // we might need this for addCustomPropertyValue
        def owner      = grailsApplication.getArtefact("Domain", ownerClass.replace("class ",""))?.getClazz()?.get(params.ownerId)

        // TODO ownerClass
        if (PropertyDefinition.findByNameAndDescrAndTenantIsNull(params.cust_prop_name, params.cust_prop_desc)) {
            error = message(code: 'propertyDefinition.name.unique')
        }
        else {
            if (params.cust_prop_type.equals(RefdataValue.toString())) {
                if (params.refdatacategory) {

                    Map<String, Object> map = [
                            token       : params.cust_prop_name,
                            category    : params.cust_prop_desc,
                            type        : params.cust_prop_type,
                            rdc         : RefdataCategory.get(params.refdatacategory)?.getDesc(),
                            multiple    : (params.cust_prop_multiple_occurence == 'on'),
                            i10n        : [
                                    name_de: params.cust_prop_name?.trim(),
                                    name_en: params.cust_prop_name?.trim(),
                                    expl_de: params.cust_prop_expl?.trim(),
                                    expl_en: params.cust_prop_expl?.trim()
                            ]
                    ]

                    newProp = PropertyDefinition.construct(map)
                }
                else {
                    error = message(code: 'ajax.addCustPropertyType.error')
                }
            }
            else {
                    Map<String, Object> map = [
                            token       : params.cust_prop_name,
                            category    : params.cust_prop_desc,
                            type        : params.cust_prop_type,
                            multiple    : (params.cust_prop_multiple_occurence == 'on'),
                            i10n        : [
                                    name_de: params.cust_prop_name?.trim(),
                                    name_en: params.cust_prop_name?.trim(),
                                    expl_de: params.cust_prop_expl?.trim(),
                                    expl_en: params.cust_prop_expl?.trim()
                            ]
                    ]

                    newProp = PropertyDefinition.construct(map)
            }

            if (newProp?.hasErrors()) {
                log.error(newProp.errors)
                error = message(code: 'default.error')
            }
            else {
                msg = message(code: 'ajax.addCustPropertyType.success')
                //newProp.softData = true
                newProp.save(flush: true)

                if (params.autoAdd == "on" && newProp) {
                    params.propIdent = newProp.id.toString()
                    chain(action: "addCustomPropertyValue", params: params)
                }
            }
        }

        request.setAttribute("editable", params.editable == "true")

        if (params.reloadReferer) {
            flash.newProp = newProp
            flash.error = error
            flash.message = msg
            redirect(url: params.reloadReferer)
        }
        else if (params.redirect) {
            flash.newProp = newProp
            flash.error = error
            flash.message = msg
            redirect(controller:"propertyDefinition", action:"create")
        }
        else {
            Map<String, Object> allPropDefGroups = owner.getCalculatedPropDefGroups(contextService.getOrg())

            render(template: "/templates/properties/custom", model: [
                    ownobj: owner,
                    customProperties: owner.customProperties,
                    newProp: newProp,
                    error: error,
                    message: msg,
                    orphanedProperties: allPropDefGroups.orphanedProperties
            ])
        }
    }

  @Secured(['ROLE_USER'])
  def addCustomPropertyValue(){
    if(params.propIdent.length() > 0) {
      def error
      def newProp
      def owner = grailsApplication.getArtefact("Domain", params.ownerClass.replace("class ", ""))?.getClazz()?.get(params.ownerId)
      def type = PropertyDefinition.get(params.propIdent.toLong())

      def existingProp = owner.customProperties.find { it.type.name == type.name }

      if (existingProp == null || type.multipleOccurrence) {
        newProp = PropertyDefinition.createGenericProperty(PropertyDefinition.CUSTOM_PROPERTY, owner, type, contextService.getOrg())
        if (newProp.hasErrors()) {
          log.error(newProp.errors)
        } else {
          log.debug("New custom property created: " + newProp.type.name)
        }
      } else {
        error = message(code: 'ajax.addCustomPropertyValue.error')
      }

      owner.refresh()

      request.setAttribute("editable", params.editable == "true")
      boolean showConsortiaFunctions = Boolean.parseBoolean(params.showConsortiaFunctions)
      boolean showCollectiveFunctions = Boolean.parseBoolean(params.showCollectiveFunctions)
      if (params.propDefGroup) {
        render(template: "/templates/properties/group", model: [
                ownobj          : owner,
                newProp         : newProp,
                error           : error,
                showConsortiaFunctions: showConsortiaFunctions,
                showCollectiveFunctions: showCollectiveFunctions,
                propDefGroup    : genericOIDService.resolveOID(params.propDefGroup),
                custom_props_div: "${params.custom_props_div}", // JS markup id
                prop_desc       : type.descr // form data
        ])
      }
      else {
          Map<String, Object> allPropDefGroups = owner.getCalculatedPropDefGroups(contextService.getOrg())

          Map<String, Object> modelMap =  [
                  ownobj                : owner,
                  newProp               : newProp,
                  showConsortiaFunctions: showConsortiaFunctions,
                  showCollectiveFunctions: showCollectiveFunctions,
                  error                 : error,
                  custom_props_div      : "${params.custom_props_div}", // JS markup id
                  prop_desc             : type.descr, // form data
                  orphanedProperties    : allPropDefGroups.orphanedProperties
          ]

          render(template: "/templates/properties/custom", model: modelMap)
      }
    }
    else {
      log.error("Form submitted with missing values")
    }
  }

    @Secured(['ROLE_USER'])
    def addCustomPropertyGroupBinding() {

        def ownobj              = genericOIDService.resolveOID(params.ownobj)
        def propDefGroup        = genericOIDService.resolveOID(params.propDefGroup)
        List<PropertyDefinitionGroup> availPropDefGroups  = PropertyDefinitionGroup.getAvailableGroups(contextService.getOrg(), ownobj.class.name)

        if (ownobj && propDefGroup) {
            if (params.isVisible in ['Yes', 'No']) {
                PropertyDefinitionGroupBinding gb = new PropertyDefinitionGroupBinding(
                        propDefGroup: propDefGroup,
                        isVisible: (params.isVisible == 'Yes')
                )
                if (ownobj.class.name == License.class.name) {
                    gb.lic = ownobj
                }
                else if (ownobj.class.name == Org.class.name) {
                    gb.org = ownobj
                }
                else if (ownobj.class.name == Subscription.class.name) {
                    gb.sub = ownobj
                }
                gb.save(flush:true)
            }
        }

        render(template: "/templates/properties/groupBindings", model:[
                propDefGroup: propDefGroup,
                ownobj: ownobj,
                availPropDefGroups: availPropDefGroups,
                editable: params.editable,
                showConsortiaFunctions: params.showConsortiaFunctions,
                showCollectiveFunctions: params.showCollectiveFunctions
        ])
    }


    @Secured(['ROLE_USER'])
    def deleteCustomPropertyGroupBinding() {
        def ownobj              = genericOIDService.resolveOID(params.ownobj)
        def propDefGroup        = genericOIDService.resolveOID(params.propDefGroup)
        def binding             = genericOIDService.resolveOID(params.propDefGroupBinding)
        List<PropertyDefinitionGroup> availPropDefGroups  = PropertyDefinitionGroup.getAvailableGroups(contextService.getOrg(), ownobj.class.name)

        if (ownobj && propDefGroup && binding) {
            binding.delete(flush:true)
        }

        render(template: "/templates/properties/groupBindings", model:[
                propDefGroup: propDefGroup,
                ownobj: ownobj,
                availPropDefGroups: availPropDefGroups,
                editable: params.editable,
                showConsortiaFunctions: params.showConsortiaFunctions
        ])
    }

    /**
    * Add domain specific private property
    * @return
    */
    @Secured(['ROLE_USER'])
    def addPrivatePropertyValue(){
      if(params.propIdent.length() > 0) {
        def error
        def newProp
        Org tenant = Org.get(params.tenantId)
        def owner  = grailsApplication.getArtefact("Domain", params.ownerClass.replace("class ",""))?.getClazz()?.get(params.ownerId)
          PropertyDefinition type   = PropertyDefinition.get(params.propIdent.toLong())

        if (! type) { // new property via select2; tmp deactivated
          error = message(code:'propertyDefinition.private.deactivated')
        }
        else {
            Set<AbstractPropertyWithCalculatedLastUpdated> existingProps
            if(owner.hasProperty("privateProperties")) {
                existingProps = owner.customProperties.findAll {
                    it.owner.id == owner.id && it.type.id == type.id // this sucks due lazy proxy problem
                }
            }
            else {
                existingProps = owner.customProperties.findAll { AbstractPropertyWithCalculatedLastUpdated prop ->
                    prop.owner.id == owner.id && prop.type.id == type.id && prop.tenant.id == tenant.id && prop.isPublic == false
                }
            }
          existingProps.removeAll { it.type.name != type.name } // dubious fix


          if (existingProps.size() == 0 || type.multipleOccurrence) {
            newProp = PropertyDefinition.createGenericProperty(PropertyDefinition.PRIVATE_PROPERTY, owner, type, contextService.getOrg())
            if (newProp.hasErrors()) {
              log.error(newProp.errors)
            } else {
              log.debug("New private property created: " + newProp.type.name)
            }
          } else {
            error = message(code: 'ajax.addCustomPropertyValue.error')
          }
        }

        owner.refresh()

        request.setAttribute("editable", params.editable == "true")
        render(template: "/templates/properties/private", model:[
                ownobj: owner,
                tenant: tenant,
                newProp: newProp,
                error: error,
                institution: contextService.org,
                custom_props_div: "custom_props_div_${tenant.id}", // JS markup id
                prop_desc: type?.descr // form data
        ])
      }
      else  {
        log.error("Form submitted with missing values")
      }
    }

    @Deprecated
    @Secured(['ROLE_USER'])
    def showAuditConfigManager() {

        def owner = genericOIDService.resolveOID(params.target)
        if (owner) {
            render(template: "/templates/audit/modal_config", model:[
                    ownobj: owner,
                    target: params.target,
                    properties: owner.getClass().controlledProperties
            ])
        }
    }

    @Deprecated
    @Secured(['ROLE_USER'])
    def processAuditConfigManager() {

        String referer = request.getHeader('referer')

        def owner = genericOIDService.resolveOID(params.target)
        if (owner) {
            def objProps = owner.getClass().controlledProperties
            def positiveList = params.list('properties')
            def negativeList = objProps.minus(positiveList)

            def members = owner.getClass().findAllByInstanceOf(owner)

            positiveList.each{ prop ->
                if (! AuditConfig.getConfig(owner, prop)) {
                    AuditConfig.addConfig(owner, prop)

                    members.each { m ->
                        m.setProperty(prop, owner.getProperty(prop))
                        m.save(flush:true)
                    }
                }
            }

            def keepProperties = params.list('keepProperties')

            negativeList.each{ prop ->
                if (AuditConfig.getConfig(owner, prop)) {
                    AuditConfig.removeConfig(owner, prop)

                    if (! keepProperties.contains(prop)) {
                        members.each { m ->
                            m.setProperty(prop, null)
                            m.save(flush:true)
                        }
                    }

                    // delete pending changes
                    // e.g. PendingChange.changeDoc = {changeTarget, changeType, changeDoc:{OID,  event}}
                    members.each { m ->
                        def openPD = PendingChange.executeQuery("select pc from PendingChange as pc where pc.status is null and pc.oid = :objectID",
                                [objectID: "${m.class.name}:${m.id}"])
                        openPD.each { pc ->
                            if (pc.payload) {
                                def payload = JSON.parse(pc.payload)
                                if (payload.changeDoc) {
                                    def eventObj = genericOIDService.resolveOID(payload.changeDoc.OID)
                                    def eventProp = payload.changeDoc.prop

                                    if (eventObj?.id == owner.id && eventProp.equalsIgnoreCase(prop)) {
                                        pc.delete(flush: true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        redirect(url: referer)
    }

    @Secured(['ROLE_USER'])
    def toggleShare() {
        def owner = genericOIDService.resolveOID( params.owner )
        def sharedObject = genericOIDService.resolveOID( params.sharedObject )

        if (! sharedObject.isShared) {
            sharedObject.isShared = true
        } else {
            sharedObject.isShared = false
        }
        sharedObject.save(flush:true)

        ((ShareSupport) owner).updateShare(sharedObject)

        if (params.tmpl) {
            if (params.tmpl == 'documents') {
                render(template: '/templates/documents/card', model: [ownobj: owner, editable: true]) // TODO editable from owner
            }
            else if (params.tmpl == 'notes') {
                render(template: '/templates/notes/card', model: [ownobj: owner, editable: true]) // TODO editable from owner
            }
        }
        else {
            redirect(url: request.getHeader('referer'))
        }
    }

    @Secured(['ROLE_USER'])
    def toggleOrgRole() {
        OrgRole oo = OrgRole.executeQuery('select oo from OrgRole oo where oo.sub = :sub and oo.roleType in :roleTypes',[sub:Subscription.get(params.id),roleTypes:[RDStore.OR_SUBSCRIBER_CONS,RDStore.OR_SUBSCRIBER_CONS_HIDDEN]])[0]
        if(oo) {
            if(oo.roleType == RDStore.OR_SUBSCRIBER_CONS)
                oo.roleType = RDStore.OR_SUBSCRIBER_CONS_HIDDEN
            else if(oo.roleType == RDStore.OR_SUBSCRIBER_CONS_HIDDEN)
                oo.roleType = RDStore.OR_SUBSCRIBER_CONS
        }
        oo.save()
        redirect(url: request.getHeader('referer'))
    }

    @Secured(['ROLE_USER'])
    def toggleAudit() {
        String referer = request.getHeader('referer')

        def owner = genericOIDService.resolveOID(params.owner)
        if (owner) {
            def members = owner.getClass().findAllByInstanceOf(owner)
            def objProps = owner.getClass().controlledProperties
            def prop = params.property

            if (prop in objProps) {
                if (! AuditConfig.getConfig(owner, prop)) {
                    AuditConfig.addConfig(owner, prop)

                    members.each { m ->
                        m.setProperty(prop, owner.getProperty(prop))
                        m.save(flush:true)
                    }
                }
                else {
                    AuditConfig.removeConfig(owner, prop)

                    if (! params.keep) {
                        members.each { m ->
                            if(m[prop] instanceof Boolean)
                                m.setProperty(prop, false)
                            else m.setProperty(prop, null)
                            m.save(flush: true)
                        }
                    }

                    // delete pending changes
                    // e.g. PendingChange.changeDoc = {changeTarget, changeType, changeDoc:{OID,  event}}
                    members.each { m ->
                        List<PendingChange> openPD = PendingChange.executeQuery("select pc from PendingChange as pc where pc.status is null and pc.costItem is null and pc.oid = :objectID",
                                [objectID: "${m.class.name}:${m.id}"])

                        openPD?.each { pc ->
                            def payload = JSON.parse(pc?.payload)
                            if (payload && payload?.changeDoc) {
                                def eventObj = genericOIDService.resolveOID(payload.changeDoc?.OID)
                                def eventProp = payload.changeDoc?.prop
                                if (eventObj?.id == owner?.id && eventProp.equalsIgnoreCase(prop)) {
                                    pc.delete(flush: true)
                                }
                            }
                        }
                    }
                }
            }

        }
        redirect(url: request.getHeader('referer'))
    }

    @Secured(['ROLE_USER'])
    def togglePropertyAuditConfig() {
        def className = params.propClass.split(" ")[1]
        def propClass = Class.forName(className)
        def owner     = grailsApplication.getArtefact("Domain", params.ownerClass.replace("class ", ""))?.getClazz()?.get(params.ownerId)
        def property  = propClass.get(params.id)
        def prop_desc = property.getType().getDescr()

        if (AuditConfig.getConfig(property, AuditConfig.COMPLETE_OBJECT)) {

            AuditConfig.removeAllConfigs(property)

            property.getClass().findAllByInstanceOf(property).each{ prop ->
                prop.delete(flush: true) //see ERMS-2049. Here, it is unavoidable because it affects the loading of orphaned properties - Hibernate tries to set up a list and encounters implicitely a SessionMismatch
            }


            // delete pending changes

            /*def openPD = PendingChange.executeQuery("select pc from PendingChange as pc where pc.status is null" )
            openPD.each { pc ->
                if (pc.payload) {
                    def payload = JSON.parse(pc.payload)
                    if (payload.changeDoc) {
                        def scp = genericOIDService.resolveOID(payload.changeDoc.OID)
                        if (scp?.id == property.id) {
                            pc.delete()
                        }
                    }
                }
            }*/
        }
        else {

            owner.getClass().findAllByInstanceOf(owner).each { member ->

                def existingProp = property.getClass().findByOwnerAndInstanceOf(member, property)
                if (! existingProp) {

                    // multi occurrence props; add one additional with backref
                    if (property.type.multipleOccurrence) {
                        def additionalProp = PropertyDefinition.createGenericProperty(PropertyDefinition.CUSTOM_PROPERTY, member, property.type, contextService.getOrg())
                        additionalProp = property.copyInto(additionalProp)
                        additionalProp.instanceOf = property
                        additionalProp.save(flush: true)
                    }
                    else {
                        def matchingProps = property.getClass().findByOwnerAndType(member, property.type)
                        // unbound prop found with matching type, set backref
                        if (matchingProps) {
                            matchingProps.each { memberProp ->
                                memberProp.instanceOf = property
                                memberProp.save(flush: true)
                            }
                        }
                        else {
                            // no match found, creating new prop with backref
                            def newProp = PropertyDefinition.createGenericProperty(PropertyDefinition.CUSTOM_PROPERTY, member, property.type)
                            newProp = property.copyInto(newProp)
                            newProp.instanceOf = property
                            newProp.save(flush: true)
                        }
                    }
                }
            }

            AuditConfig.addConfig(property, AuditConfig.COMPLETE_OBJECT)
        }

        request.setAttribute("editable", params.editable == "true")
        if(params.propDefGroup) {
          render(template: "/templates/properties/group", model: [
                  ownobj          : owner,
                  newProp         : property,
                  showConsortiaFunctions: params.showConsortiaFunctions,
                  propDefGroup    : genericOIDService.resolveOID(params.propDefGroup),
                  custom_props_div: "${params.custom_props_div}", // JS markup id
                  prop_desc       : prop_desc // form data
          ])
        }
        else {
            Map<String, Object>  allPropDefGroups = owner.getCalculatedPropDefGroups(contextService.getOrg())

            Map<String, Object> modelMap =  [
                    ownobj                : owner,
                    newProp               : property,
                    showConsortiaFunctions: params.showConsortiaFunctions,
                    custom_props_div      : "${params.custom_props_div}", // JS markup id
                    prop_desc             : prop_desc, // form data
                    orphanedProperties    : allPropDefGroups.orphanedProperties
            ]
            render(template: "/templates/properties/custom", model: modelMap)
        }
    }

    @Secured(['ROLE_USER'])
    def deleteCustomProperty() {
        def className = params.propClass.split(" ")[1]
        def propClass = Class.forName(className)
        def owner     = grailsApplication.getArtefact("Domain", params.ownerClass.replace("class ", ""))?.getClazz()?.get(params.ownerId)
        def property  = propClass.get(params.id)
        def prop_desc = property.getType().getDescr()

        AuditConfig.removeAllConfigs(property)

        //owner.customProperties.remove(property)

        try {
            property.delete(flush:true)
        } catch (Exception e) {
            log.error(" TODO: fix property.delete() when instanceOf ")
        }


        if(property.hasErrors()) {
            log.error(property.errors)
        }
        else {
            log.debug("Deleted custom property: " + property.type.name)
        }
        request.setAttribute("editable", params.editable == "true")
        boolean showConsortiaFunctions = Boolean.parseBoolean(params.showConsortiaFunctions)
        if(params.propDefGroup) {
          render(template: "/templates/properties/group", model: [
                  ownobj          : owner,
                  newProp         : property,
                  showConsortiaFunctions: showConsortiaFunctions,
                  propDefGroup    : genericOIDService.resolveOID(params.propDefGroup),
                  custom_props_div: "${params.custom_props_div}", // JS markup id
                  prop_desc       : prop_desc // form data
          ])
        }
        else {
            Map<String, Object> allPropDefGroups = owner.getCalculatedPropDefGroups(contextService.getOrg())
            Map<String, Object> modelMap =  [
                    ownobj                : owner,
                    newProp               : property,
                    showConsortiaFunctions: showConsortiaFunctions,
                    custom_props_div      : "${params.custom_props_div}", // JS markup id
                    prop_desc             : prop_desc, // form data
                    orphanedProperties    : allPropDefGroups.orphanedProperties
            ]

            render(template: "/templates/properties/custom", model: modelMap)
        }
    }

  /**
    * Delete domain specific private property
    *
    * @return
    */
  @Secured(['ROLE_USER'])
  def deletePrivateProperty(){
    def className = params.propClass.split(" ")[1]
    def propClass = Class.forName(className)
    def property  = propClass.get(params.id)
    def tenant    = property.type.tenant
    def owner     = grailsApplication.getArtefact("Domain", params.ownerClass.replace("class ",""))?.getClazz()?.get(params.ownerId)
    def prop_desc = property.getType().getDescr()

    owner.customProperties.remove(property)
    property.delete(flush:true)

    if(property.hasErrors()){
      log.error(property.errors)
    } else{
      log.debug("Deleted private property: " + property.type.name)
    }
    request.setAttribute("editable", params.editable == "true")
    render(template: "/templates/properties/private", model:[
            ownobj: owner,
            tenant: tenant,
            newProp: property,
            institution: contextService.org,
            custom_props_div: "custom_props_div_${tenant.id}",  // JS markup id
            prop_desc: prop_desc // form data
    ])
  }

    @Secured(['ROLE_USER'])
    def hideDashboardDueDate(){
        setDashboardDueDateIsHidden(true)
    }

    @Secured(['ROLE_USER'])
    def showDashboardDueDate(){
        setDashboardDueDateIsHidden(false)
    }

    @Secured(['ROLE_USER'])
    private setDashboardDueDateIsHidden(boolean isHidden){
        log.debug("Hide/Show Dashboard DueDate - isHidden="+isHidden)

        def result = [:]
        result.user = contextService.user
        result.institution = contextService.org
        flash.error = ''

        if (! accessService.checkUserIsMember(result.user, result.institution)) {
            flash.error = "You do not have permission to access ${contextService.org.name} pages. Please request access on the profile page"
            response.sendError(401)
            return;
        }

        if (params.owner) {
            DashboardDueDate dueDate = genericOIDService.resolveOID(params.owner)
            if (dueDate){
                dueDate.isHidden = isHidden
                dueDate.save(flush: true)
            } else {
                if (isHidden)   flash.error += message(code:'dashboardDueDate.err.toHide.doesNotExist')
                else            flash.error += message(code:'dashboardDueDate.err.toShow.doesNotExist')
            }
        } else {
            if (isHidden)   flash.error += message(code:'dashboardDueDate.err.toHide.doesNotExist')
            else            flash.error += message(code:'dashboardDueDate.err.toShow.doesNotExist')
        }

        result.is_inst_admin = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_ADM')
        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP()
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0
        result.dashboardDueDatesOffset = result.offset

        result.dueDates = DashboardDueDatesService.getDashboardDueDates(contextService.user, contextService.org, false, false, result.max, result.dashboardDueDatesOffset)
        result.dueDatesCount = DashboardDueDatesService.getDashboardDueDates(contextService.user, contextService.org, false, false).size()

        render (template: "/user/tableDueDates", model: [dueDates: result.dueDates, dueDatesCount: result.dueDatesCount, max: result.max, offset: result.offset])
    }

    @Secured(['ROLE_USER'])
    def dashboardDueDateSetIsDone() {
       setDashboardDueDateIsDone(true)
    }

    @Secured(['ROLE_USER'])
    def dashboardDueDateSetIsUndone() {
       setDashboardDueDateIsDone(false)
    }

    @Secured(['ROLE_USER'])
    private setDashboardDueDateIsDone(boolean isDone){
        log.debug("Done/Undone Dashboard DueDate - isDone="+isDone)

        def result = [:]
        result.user = contextService.user
        result.institution = contextService.org
        flash.error = ''

        if (! accessService.checkUserIsMember(result.user, result.institution)) {
            flash.error = "You do not have permission to access ${contextService.org.name} pages. Please request access on the profile page"
            response.sendError(401)
            return
        }


        if (params.owner) {
            DueDateObject dueDateObject = genericOIDService.resolveOID(params.owner)
            if (dueDateObject){
                Object obj = genericOIDService.resolveOID(dueDateObject.oid)
                if (obj instanceof Task && isDone){
                    Task dueTask = (Task)obj
                    dueTask.setStatus(RDStore.TASK_STATUS_DONE)
                    dueTask.save()
                }
                dueDateObject.isDone = isDone
                dueDateObject.save()
            } else {
                if (isDone)   flash.error += message(code:'dashboardDueDate.err.toSetDone.doesNotExist')
                else          flash.error += message(code:'dashboardDueDate.err.toSetUndone.doesNotExist')
            }
        } else {
            if (isDone)   flash.error += message(code:'dashboardDueDate.err.toSetDone.doesNotExist')
            else          flash.error += message(code:'dashboardDueDate.err.toSetUndone.doesNotExist')
        }

        result.is_inst_admin = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_ADM')
        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP()
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0
        result.dashboardDueDatesOffset = result.offset

        result.dueDates = DashboardDueDatesService.getDashboardDueDates(contextService.user, contextService.org, false, false, result.max, result.dashboardDueDatesOffset)
        result.dueDatesCount = DashboardDueDatesService.getDashboardDueDates(contextService.user, contextService.org, false, false).size()

        render (template: "/user/tableDueDates", model: [dueDates: result.dueDates, dueDatesCount: result.dueDatesCount, max: result.max, offset: result.offset])
    }

    /*
  @Deprecated
  def coreExtend(){
    log.debug("ajax::coreExtend:: ${params}")
    def tipID = params.tipID
    try{
        SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
      def startDate = sdf.parse(params.coreStartDate)
      def endDate = params.coreEndDate? sdf.parse(params.coreEndDate) : null
      if(tipID && startDate){
        def tip = TitleInstitutionProvider.get(tipID)
        log.debug("Extending tip ${tip.id} with start ${startDate} and end ${endDate}")
        tip.extendCoreExtent(startDate, endDate)
        params.message = message(code:'ajax.coreExtend.success')
      }
    }catch (Exception e){
        log.error("Error while extending core dates",e)
        params.message = message(code:'ajax.coreExtend.error')
    }
    redirect(action:'getTipCoreDates',controller:'ajax',params:params)
  }

  @Deprecated
  def getTipCoreDates(){
    log.debug("ajax::getTipCoreDates:: ${params}")
    def tipID = params.tipID ?:params.id
    def tip = null
    if(tipID) tip = TitleInstitutionProvider.get(tipID);
    if(tip){
      def dates = tip.coreDates
      log.debug("Returning ${dates}")
      request.setAttribute("editable",params.editable?:true)
      render(template:"/templates/coreAssertionsModal",model:[message:params.message,coreDates:dates,tipID:tip.id,tip:tip]);    
    }
  }
     */

    def delete() {
      switch(params.cmd) {
        case 'deletePersonRole': deletePersonRole()
        break
        default: def obj = genericOIDService.resolveOID(params.oid)
          if (obj) {
            obj.delete()
          }
        break
      }
      redirect(url: request.getHeader('referer'))
    }

    //TODO: Überprüfuen, ob die Berechtigung korrekt funktioniert.
    @Secured(['ROLE_ORG_EDITOR'])
    def deletePersonRole(){
        def obj = genericOIDService.resolveOID(params.oid)
        if (obj) {
                obj.delete()
        }
    }

    @Secured(['ROLE_USER'])
    def deleteCoreDate(){
    log.debug("ajax:: deleteCoreDate::${params}")
    def date = CoreAssertion.get(params.coreDateID)
    if(date) date.delete(flush:true)
    redirect(action:'getTipCoreDates',controller:'ajax',params:params)
  }

  def getProvidersWithPrivateContacts() {
    Map<String, Object> result = [:]
    def query_params = []
    String fuzzyString = '%'
    if(params.sSearch) {
      fuzzyString+params.sSearch.trim().toLowerCase()+'%'
    }
    query_params.add(fuzzyString)
    query_params.add(RefdataValue.getByValueAndCategory('Deleted', RDConstants.ORG_STATUS))
    String countQry = "select count(o) from Org as o where exists (select roletype from o.orgType as roletype where roletype.value = 'Provider' ) and lower(o.name) like ? and (o.status is null or o.status != ?)"
    String rowQry = "select o from Org as o where exists (select roletype from o.orgType as roletype where roletype.value = 'Provider' ) and lower(o.name) like ? and (o.status is null or o.status != ?) order by o.name asc"
    def cq = Org.executeQuery(countQry,query_params);

    def rq = Org.executeQuery(rowQry,
            query_params,
            [max:params.iDisplayLength?:1000,offset:params.iDisplayStart?:0]);

    result.aaData = []
    result.sEcho = params.sEcho
    result.iTotalRecords = cq[0]
    result.iTotalDisplayRecords = cq[0]
    def currOrg = genericOIDService.resolveOID(params.oid)
    List<Person> contacts = Person.findAllByContactTypeAndTenant(RDStore.CONTACT_TYPE_PERSONAL, currOrg)
    LinkedHashMap personRoles = [:]
    PersonRole.findAll().collect { prs ->
      personRoles.put(prs.org,prs.prs)
    }
      rq.each { it ->
        def rowobj = GrailsHibernateUtil.unwrapIfProxy(it)
        int ctr = 0;
        LinkedHashMap row = [:]
        String name = rowobj["name"]
        if(personRoles.get(rowobj) && contacts.indexOf(personRoles.get(rowobj)) > -1)
          name += '<span data-tooltip="Persönlicher Kontakt vorhanden"><i class="address book icon"></i></span>'
        row["${ctr++}"] = name
        row["DT_RowId"] = "${rowobj.class.name}:${rowobj.id}"
        result.aaData.add(row)
      }

    render result as JSON
  }

  def lookup() {
      // fallback for static refdataFind calls
      params.shortcode  = contextService.getOrg().shortcode

    // log.debug("AjaxController::lookup ${params}");
    Map<String, Object> result = [:]
    // params.max = params.max ?: 20;
    params.max = params.max ?: 40;
    def domain_class = grailsApplication.getArtefact('Domain',params.baseClass)
    if ( domain_class ) {
      result.values = domain_class.getClazz().refdataFind(params);
      result.values.sort{ x,y -> x.text.compareToIgnoreCase y.text  }
    }
    else {
      log.error("Unable to locate domain class ${params.baseClass}");
      result.values=[]
    }
    //result.values = [[id:'Person:45',text:'Fred'],
    //                 [id:'Person:23',text:'Jim'],
    //                 [id:'Person:22',text:'Jimmy'],
    //                 [id:'Person:3',text:'JimBob']]
    render result as JSON
  }

  // used only from IdentifierTabLib.formAddIdentifier
  def lookup2() {
      // fallback for static refdataFind calls
      params.shortcode  = contextService.getOrg().shortcode

    Map<String, Object> result = [:]
    def domain_class = grailsApplication.getArtefact('Domain', params.baseClass)
    if (domain_class) {
      result.values = domain_class.getClazz().refdataFind2(params);
      result.values.sort{ x,y -> x.text.compareToIgnoreCase y.text  }
    }
    else {
      log.error("Unable to locate domain class ${params.baseClass}");
      result.values=[]
    }
    render result as JSON
  }

    def toggleEditMode() {
        log.debug ('toggleEditMode()')

        User user = contextService.getUser()
        def show = params.showEditMode

        if (show) {
            def setting = user.getSetting(UserSettings.KEYS.SHOW_EDIT_MODE, RefdataValue.getByValueAndCategory('Yes', RDConstants.Y_N))

            if (show == 'true') {
                setting.setValue(RefdataValue.getByValueAndCategory('Yes', RDConstants.Y_N))
            }
            else if (show == 'false') {
                setting.setValue(RefdataValue.getByValueAndCategory('No', RDConstants.Y_N))
            }
        }
        render show
    }

    @Secured(['ROLE_USER'])
    def addIdentifier() {
        log.debug("AjaxController::addIdentifier ${params}")
        def owner = genericOIDService.resolveOID(params.owner)
        def namespace = genericOIDService.resolveOID(params.namespace)
        String value = params.value?.trim()

        if (owner && namespace && value) {
            FactoryResult fr = Identifier.constructWithFactoryResult([value: value, reference: owner, namespace: namespace])

            fr.setFlashScopeByStatus(flash)
        }
        redirect(url: request.getHeader('referer'))
    }

    @Secured(['ROLE_USER'])
    def deleteIdentifier() {
        log.debug("AjaxController::deleteIdentifier ${params}")
        def owner = genericOIDService.resolveOID(params.owner)
        def target = genericOIDService.resolveOID(params.target)

        log.debug('owner: ' + owner)
        log.debug('target: ' + target)

        if (owner && target) {
            if (target."${Identifier.getAttributeName(owner)}"?.id == owner.id) {
                log.debug("Identifier deleted: ${params}")
                target.delete()
            }
        }
        redirect(url: request.getHeader('referer'))
    }

    @Secured(['ROLE_USER'])
  def addToCollection() {
    log.debug("AjaxController::addToCollection ${params}");

    def contextObj = resolveOID2(params.__context)
    def domain_class = grailsApplication.getArtefact('Domain',params.__newObjectClass)
    if ( domain_class ) {

      if ( contextObj ) {
        // log.debug("Create a new instance of ${params.__newObjectClass}");

        def new_obj = domain_class.getClazz().newInstance();

        domain_class.getPersistentProperties().each { p -> // list of GrailsDomainClassProperty
          // log.debug("${p.name} (assoc=${p.isAssociation()}) (oneToMany=${p.isOneToMany()}) (ManyToOne=${p.isManyToOne()}) (OneToOne=${p.isOneToOne()})");
          if ( params[p.name] ) {
            if ( p.isAssociation() ) {
              if ( p.isManyToOne() || p.isOneToOne() ) {
                // Set ref property
                // log.debug("set assoc ${p.name} to lookup of OID ${params[p.name]}");
                // if ( key == __new__ then we need to create a new instance )
                def new_assoc = resolveOID2(params[p.name])
                if(new_assoc){
                  new_obj[p.name] = new_assoc               
                }
              }
              else {
                // Add to collection
                // log.debug("add to collection ${p.name} for OID ${params[p.name]}");
                new_obj[p.name].add(resolveOID2(params[p.name]))
              }
            }
            else {
              // log.debug("Set simple prop ${p.name} = ${params[p.name]}");
              new_obj[p.name] = params[p.name]
            }
          }
        }

        if ( params.__recip ) {
          // log.debug("Set reciprocal property ${params.__recip} to ${contextObj}");
          new_obj[params.__recip] = contextObj
        }

        // log.debug("Saving ${new_obj}");
        try{
          if ( new_obj.save() ) {
            log.debug("Saved OK");
          }
          else {
            flash.domainError = new_obj
            new_obj.errors.each { e ->
              log.debug("Problem ${e}");
            }
          }
        }catch(Exception ex){

            flash.domainError = new_obj
            new_obj.errors.each { e ->
            log.debug("Problem ${e}");
            }
        }
      }
      else {
        log.debug("Unable to locate instance of context class with oid ${params.__context}");
      }
    }
    else {
      log.error("Unable to lookup domain class ${params.__newObjectClass}");
    }
    redirect(url: request.getHeader('referer'))
  }

  def validateIdentifierUniqueness(){
    log.debug("validateIdentifierUniqueness - ${params}")
    Map<String, Object> result = [:]
    def owner = resolveOID2(params.owner)
    def identifier = resolveOID2(params.identifier)

    // TODO [ticket=1789]
    def owner_type = Identifier.getAttributeName(owner)
    if (!owner_type) {
      log.error("Unexpected Identifier Owner ${owner.class}")
      return null
    }

    // TODO: BUG !? multiple occurrences on the same object allowed
    //def duplicates = identifier?.occurrences.findAll{it."${owner_type}" != owner && it."${owner_type}" != null}?.collect{it."${owner_type}"}

    def duplicates = Identifier.executeQuery(
            "select ident from Identifier ident where ident.value = ? and ident.${owner_type} != ?", [identifier.value, owner]
    )

    if(duplicates){
      result.duplicates = duplicates.collect{ it."${owner_type}" }
    }
    else{
      result.unique=true
    }
    log.debug("validateIdentifierUniqueness - ${result}")
    render result as JSON
  }
    
  def resolveOID2(oid) {
    def oid_components = oid.split(':');
    def result = null;
    def domain_class=null;
    domain_class = grailsApplication.getArtefact('Domain',oid_components[0])
    if ( domain_class ) {
      if ( oid_components[1]=='__new__' ) {
        result = domain_class.getClazz().refdataCreate(oid_components)
        // log.debug("Result of create ${oid} is ${result?.id}");
      }
      else {
        result = domain_class.getClazz().get(oid_components[1])
      }
    }
    else {
      log.error("resolve OID failed to identify a domain class. Input was ${oid_components}");
    }
    result
  }

    @Secured(['ROLE_USER'])
  def deleteThrough() {
    // log.debug("deleteThrough(${params})");
    def context_object = resolveOID2(params.contextOid)
    def target_object = resolveOID2(params.targetOid)
    if ( context_object."${params.contextProperty}".contains(target_object) ) {
      def otr = context_object."${params.contextProperty}".remove(target_object)
      target_object.delete()
      context_object.save(flush:true);
    }
    redirect(url: request.getHeader('referer'))

  }

    @Secured(['ROLE_USER'])
  def deleteManyToMany() {
    // log.debug("deleteManyToMany(${params})");
    def context_object = resolveOID2(params.contextOid)
    def target_object = resolveOID2(params.targetOid)
    if ( context_object."${params.contextProperty}".contains(target_object) ) {
      context_object."${params.contextProperty}".remove(target_object)
      context_object.save(flush:true);
    }
    redirect(url: request.getHeader('referer'))    
  }

    @Secured(['ROLE_USER'])
    def getEmailAddresses() {
        Set result = []
        if (params.orgIdList){
            List<Long> orgIds = params.orgIdList.split ','
            List<Org> orgList = Org.findAllByIdInList(orgIds)
            boolean showPrivateContactEmails = Boolean.valueOf(params.isPrivate)
            boolean showPublicContactEmails = Boolean.valueOf(params.isPublic)

            List<RefdataValue> selectedRoleTypes = null
            if (params.selectedRoleTypIds) {
                List<Long> selectedRoleTypIds = params.selectedRoleTypIds.split ','
                selectedRoleTypes = RefdataValue.findAllByIdInList(selectedRoleTypIds)
            }

            String query = "select distinct p from Person as p inner join p.roleLinks pr where pr.org in (:orgs) "
            Map queryParams = [orgs: orgList]

            if (showPublicContactEmails && showPrivateContactEmails){
                query += "and ( (p.isPublic = false and p.tenant = :ctx) or (p.isPublic = true) ) "
                queryParams << [ctx: contextService.org]
            } else {
                if (showPublicContactEmails){
                    query += "and p.isPublic = true "
                } else if (showPrivateContactEmails){
                    query += "and (p.isPublic = false and p.tenant = :ctx) "
                    queryParams << [ctx: contextService.org]
                } else {
                    return [] as JSON
                }
            }

            if (selectedRoleTypes) {
                query += "and pr.functionType in (:selectedRoleTypes) "
                queryParams << [selectedRoleTypes: selectedRoleTypes]
//                selectedRoleTypes.eachWithIndex{ it, index ->
//                    query += "and pr.functionType = :r${index} "
//                    queryParams << ["r${index}": it]
//                }
            }

            List<Person> persons = Person.executeQuery(query, queryParams)

            if (persons){
                result = Contact.executeQuery("select c.content from Contact c where c.prs in (:persons) and c.contentType = :contentType",
                        [persons: persons, contentType: RDStore.CCT_EMAIL])
            }

        }
        render result as JSON
    }

  def validationException(final grails.validation.ValidationException exception){
    log.error(exception)
    response.status = 400
    response.setContentType('text/plain')
    def outs = response.outputStream
    outs << "Value validation failed"
  }

    @Secured(['ROLE_USER'])
    def editableSetValue() {
        log.debug("editableSetValue ${params}");
        def result = null

        try {

            def target_object = resolveOID2(params.pk)

            if (target_object) {
                if (params.type == 'date') {
                    SimpleDateFormat sdf = DateUtil.getSDF_NoTime()

                    def backup = target_object."${params.name}"
                    try {
                        if (params.value && params.value.size() > 0) {
                            // parse new date
                            def parsed_date = sdf.parse(params.value)
                            target_object."${params.name}" = parsed_date
                        } else {
                            // delete existing date
                            target_object."${params.name}" = null
                        }
                        //if (target_object.hasProperty('owner')) {
                        //    target_object.owner?.save() // avoid owner.xyz not processed by flush
                        //}
                        target_object.save(failOnError: true);
                    }
                    catch (Exception e) {
                        target_object."${params.name}" = backup
                        log.error(e)
                    }
                    finally {
                        if (target_object."${params.name}") {
                            result = (target_object."${params.name}").format(message(code: 'default.date.format.notime'))
                        }
                    }
                } else if (params.type == 'url') {

                    def backup = target_object."${params.name}"
                    try {
                        if (params.value && params.value.size() > 0) {
                            target_object."${params.name}" = new URL(params.value)
                        } else {
                            // delete existing url
                            target_object."${params.name}" = null
                        }
                        //if (target_object.hasProperty('owner')) {
                        //    target_object.owner?.save() // avoid owner.xyz not processed by flush
                        //}
                        target_object.save(failOnError: true)
                    }
                    catch (Exception e) {
                        target_object."${params.name}" = backup
                        log.error(e)
                    }
                    finally {
                        if (target_object."${params.name}") {
                            result = target_object."${params.name}"
                        }
                    }
                } else {
                    def binding_properties = [:]

                    if (target_object."${params.name}" instanceof Double) {
                        params.value = Double.parseDouble(params.value)
                    }
                    if (target_object."${params.name}" instanceof Boolean) {
                        params.value = params.value?.equals("1")
                    }
                    if(params.value instanceof String) {
                        String value = params.value.startsWith('www.') ? ('http://' + params.value) : params.value
                        binding_properties[params.name] = value
                    }
                    else binding_properties[params.name] = params.value
                    bindData(target_object, binding_properties)

                    target_object.save(failOnError: true)

                    result = target_object."${params.name}"
                }

                if (target_object instanceof SurveyResult) {

                    Org org = contextService.getOrg()
                    //If Survey Owner set Value then set FinishDate
                    if (org?.id == target_object?.owner?.id && target_object?.finishDate == null) {
                        String property = ""
                        if (target_object?.type?.type == Integer.toString()) {
                            property = "intValue"
                        } else if (target_object?.type?.type == String.toString()) {
                            property = "stringValue"
                        } else if (target_object?.type?.type == BigDecimal.toString()) {
                            property = "decValue"
                        } else if (target_object?.type?.type == Date.toString()) {
                            property = "dateValue"
                        } else if (target_object?.type?.type == URL.toString()) {
                            property = "urlValue"
                        } else if (target_object?.type?.type == RefdataValue.toString()) {
                            property = "refValue"
                        }

                        if (target_object[property] != null) {
                            log.debug("Set/Save FinishDate of SurveyResult (${target_object.id})")
                            target_object.finishDate = new Date()
                            target_object.save()
                        }
                    }
                }

            }

        } catch(Exception e) {
            log.error("@ editableSetValue()")
            log.error(e)
        }

        log.debug("editableSetValue() returns ${result}")

        response.setContentType('text/plain')

        def outs = response.outputStream
        outs << result
        outs.flush()
        outs.close()
    }

    @Secured(['ROLE_USER'])
    def removeUserRole() {
        User user = resolveOID2(params.user);
        Role role = resolveOID2(params.role);
        if (user && role) {
            com.k_int.kbplus.auth.UserRole.remove(user,role,true);
        }
        redirect(url: request.getHeader('referer'))
    }

  /**
   * ToDo: This function is a duplicate of the one found in InplaceTagLib, both should be moved to a shared static utility
   */
  def renderObjectValue(value) {
    def result=''
    def not_set = message(code:'refdata.notSet')

    if ( value ) {
      switch ( value.class ) {
        case com.k_int.kbplus.RefdataValue.class:

          if ( value.icon != null ) {
            result="<span class=\"select-icon ${value.icon}\"></span>";
            result += value.value ? value.getI10n('value') : not_set
          }
          else {
            result = value.value ? value.getI10n('value') : not_set
          }
          break;
        default:
          if(value instanceof String){

          }else{
            value = value.toString()
          }
          def no_ws = value.replaceAll(' ','')

          result = message(code:"refdata.${no_ws}", default:"${value ?: not_set}")
      }
    }
    // log.debug("Result of render: ${value} : ${result}");
    result;
  }
    @Secured(['ROLE_ADMIN'])
    def addCreatorToTitle() {

        if(params.role && params.creator && params.title) {
            def creatorTitleInstance = new CreatorTitle(role: params.role.id, creator: params.creator.id, title: params.title.id)
            if (!creatorTitleInstance.save(flush: true)) {
                redirect(url: request.getHeader('referer'))
                return
            }
        }
        redirect(url: request.getHeader('referer'))
    }

    @Secured(['ROLE_USER'])
    def TaskEdit() {
        Org contextOrg = contextService.getOrg()
        def result     = taskService.getPreconditionsWithoutTargets(contextOrg)
        result.params = params
        result.taskInstance = Task.get(params.id)
        if (result.taskInstance){
            render template:"../templates/tasks/modal_edit", model: result
//        } else {
//            flash.error = "Diese Aufgabe existiert nicht (mehr)."
//            redirect(url: request.getHeader('referer'))
        }

    }

    @Secured(['ROLE_USER'])
    def TaskCreate() {
        long backendStart = System.currentTimeMillis()
        Org contextOrg = contextService.getOrg()
        def result     = taskService.getPreconditions(contextOrg)
        result.backendStart = backendStart

        render template:"../templates/tasks/modal_create", model: result

    }

    @Secured(['ROLE_USER'])
    def AddressEdit() {
        Map model = [:]
        model.addressInstance = Address.get(params.id)
        if (model.addressInstance){
            model.modalId = 'addressFormModal'
            String messageCode = 'person.address.label'
            switch (model.addressInstance.type){
                case RDStore.ADRESS_TYPE_LEGAL_PATRON:
                    messageCode = 'addressFormModalLegalPatronAddress'
                    break
                case RDStore.ADRESS_TYPE_BILLING:
                    messageCode = 'addressFormModalBillingAddress'
                    break
                case RDStore.ADRESS_TYPE_POSTAL:
                    messageCode = 'addressFormModalPostalAddress'
                    break
                case RDStore.ADRESS_TYPE_DELIVERY:
                    messageCode = 'addressFormModalDeliveryAddress'
                    break
                case RDStore.ADRESS_TYPE_LIBRARY:
                    messageCode = 'addressFormModalLibraryAddress'
                    break
            }

            model.typeId = model.addressInstance.type.id
            model.modalText = message(code: 'default.edit.label', args: [message(code: messageCode)])
            model.modalMsgSave = message(code: 'default.button.save_changes')
            model.url = [controller: 'address', action: 'edit']
            render template:"../templates/addresses/formModal", model: model
        }
    }

    @Secured(['ROLE_USER'])
    def AddressCreate() {
        Map model = [:]
        model.orgId = params.orgId
        model.prsId = params.prsId
        model.redirect = params.redirect
        model.typeId = Long.valueOf(params.typeId)
        model.hideType = params.hideType
        if (model.orgId && model.typeId) {
            String messageCode = 'addressFormModalLibraryAddress'
            if (model.typeId == RDStore.ADRESS_TYPE_LEGAL_PATRON.id)  {messageCode = 'addressFormModalLegalPatronAddress'}
            else if (model.typeId == RDStore.ADRESS_TYPE_BILLING.id)  {messageCode = 'addressFormModalBillingAddress'}
            else if (model.typeId == RDStore.ADRESS_TYPE_POSTAL.id)   {messageCode = 'addressFormModalPostalAddress'}
            else if (model.typeId == RDStore.ADRESS_TYPE_DELIVERY.id) {messageCode = 'addressFormModalDeliveryAddress'}
            else if (model.typeId == RDStore.ADRESS_TYPE_LIBRARY.id)  {messageCode = 'addressFormModalLibraryAddress'}

            model.modalText = message(code: 'default.create.label', args: [message(code: messageCode)])
        } else {
            model.modalText = message(code: 'default.new.label', args: [message(code: 'person.address.label')])
        }
        model.modalMsgSave = message(code: 'default.button.create.label')
        model.url = [controller: 'address', action: 'create']

        render template:"../templates/addresses/formModal", model: model
    }

    @Secured(['ROLE_USER'])
    def NoteEdit() {
        Map<String, Object> result = [:]
        result.params = params
        result.noteInstance = Doc.get(params.id)

        render template:"/templates/notes/modal_edit", model: result
    }

    @Secured(['ROLE_USER'])
    def readNote() {
        Map<String, Object> result = [:]
        result.params = params
        result.noteInstance = Doc.get(params.id)

        render template:"/templates/notes/modal_read", model: result
    }

    @Secured(['ROLE_USER'])
    def consistencyCheck() {
        def result = dataConsistencyService.ajaxQuery(params.key, params.key2, params.value)
        render result as JSON
    }
}
