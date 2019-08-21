package com.k_int.kbplus

import com.k_int.kbplus.auth.User
import com.k_int.kbplus.auth.UserOrg
import com.k_int.properties.PropertyDefinition
import com.k_int.properties.PropertyDefinitionGroup
import com.k_int.properties.PropertyDefinitionGroupItem
import de.laser.DashboardDueDate
import de.laser.controller.AbstractDebugController
import de.laser.helper.*
import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import groovy.sql.Sql
import org.apache.commons.collections.BidiMap
import org.apache.commons.collections.bidimap.DualHashBidiMap
import org.apache.poi.POIXMLProperties
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.streaming.SXSSFSheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.mozilla.universalchardet.UniversalDetector
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.web.multipart.commons.CommonsMultipartFile

import javax.servlet.ServletOutputStream
import java.sql.Timestamp
import java.text.DateFormat
import java.text.RuleBasedCollator
import java.text.SimpleDateFormat

@Secured(['IS_AUTHENTICATED_FULLY'])
class MyInstitutionController extends AbstractDebugController {
    def dataSource
    def springSecurityService
    def ESSearchService
    def genericOIDService
    def factService
    def exportService
    def escapeService
    def transformerService
    def institutionsService
    def docstoreService
    def addressbookService
    def accessService
    def contextService
    def taskService
    def filterService
    def propertyService
    def queryService
    def dashboardDueDatesService
    def subscriptionsQueryService
    def orgTypeService
    def subscriptionService
    def organisationService
    def titleStreamService
    def financeService
    def surveyService

    // copied from
    static String INSTITUTIONAL_LICENSES_QUERY      =
            " from License as l where exists ( select ol from OrgRole as ol where ol.lic = l AND ol.org = :lic_org and ol.roleType IN (:org_roles) ) "

    // copied from
    static String INSTITUTIONAL_SUBSCRIPTION_QUERY  =
            " from Subscription as s where  ( ( exists ( select o from s.orgRelations as o where ( o.roleType IN (:roleTypes) AND o.org = :activeInst ) ) ) ) "

    // Map the parameter names we use in the webapp with the ES fields
    def renewals_reversemap = ['subject': 'subject', 'provider': 'provid', 'pkgname': 'tokname']
    def reversemap = ['subject': 'subject', 'provider': 'provid', 'studyMode': 'presentations.studyMode', 'qualification': 'qual.type', 'level': 'qual.level']

    def possible_date_formats = [
            new SimpleDateFormat('yyyy/MM/dd'),
            new SimpleDateFormat('dd.MM.yyyy'),
            new SimpleDateFormat('dd/MM/yyyy'),
            new SimpleDateFormat('dd/MM/yy'),
            new SimpleDateFormat('yyyy/MM'),
            new SimpleDateFormat('yyyy')
    ]

    @DebugAnnotation(test='hasAffiliation("INST_ADM")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_ADM") })
    def index() {
        // Work out what orgs this user has admin level access to
        def result = [:]
        result.institution  = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)
        def currentOrg = contextService.getOrg()
        log.debug("index for user with id ${springSecurityService.principal.id} :: ${result.user}");

        if ( result.user ) {
          if ((result.user.affiliations == null) || (result.user.affiliations.size() == 0)) {
              redirect controller: 'profile', action: 'index'
          }
        }
        else {
          log.error("Failed to find user in database");
        }

        result
    }

    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def tipview() {
        log.debug("admin::tipview ${params}")
        def result = [:]

        result.user = User.get(springSecurityService.principal.id)
        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;
        def current_inst = contextService.getOrg()
        //if(params.shortcode) current_inst = Org.findByShortcode(params.shortcode);
        //Parameters needed for criteria searching
        def (tip_property, property_field) = (params.sort ?: 'title-title').split("-")
        def list_order = params.order ?: 'asc'

        if (current_inst && ! accessService.checkUserIsMember(result.user, current_inst)) {
            flash.error = message(code:'myinst.error.noMember', args:[current_inst.name]);
            response.sendError(401)
            return;
        }

        def criteria = TitleInstitutionProvider.createCriteria();
        def results = criteria.list(max: result.max, offset:result.offset) {
              //if (params.shortcode){
              if (current_inst){
                institution{
                    idEq(current_inst.id)
                }
              }
              if (params.search_for == "institution") {
                institution {
                  ilike("name", "%${params.search_str}%")
                }
              }
             if (params.search_for == "provider") {
                provider {
                  ilike("name", "%${params.search_str}%")
                }
             }
             if (params.search_for == "title") {
                title {
                  ilike("title", "%${params.search_str}%")
                }
             }
             if(params.filter == "core" || !params.filter){
               isNotEmpty('coreDates')
             }else if(params.filter=='not'){
                isEmpty('coreDates')
             }
             "${tip_property}"{
                order(property_field,list_order)
             }
        }

        result.tips = results
        result.institution = current_inst
        result.editable = accessService.checkMinUserOrgRole(result.user, current_inst, 'INST_EDITOR')
        result
    }

    @Deprecated
    @DebugAnnotation(test='hasAffiliation("INST_ADM")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_ADM") })
    def manageAffiliationRequests() {
        redirect controller: 'organisation', action: 'users', id: contextService.getOrg().id

        def result = [:]
        result.institution        = contextService.getOrg()
        result.user               = User.get(springSecurityService.principal.id)
        result.editable           = true // inherit
        result.pendingRequestsOrg = UserOrg.findAllByStatusAndOrg(UserOrg.STATUS_PENDING, contextService.getOrg(), [sort:'dateRequested'])

        result
    }

    @Secured(['ROLE_USER'])
    def currentPlatforms() {
        long timestamp = System.currentTimeSeconds()

        def result = [:]
        result.user = User.get(springSecurityService.principal.id)
        result.max = params.max ?: result.user.getDefaultPageSizeTMP()
        result.offset = params.offset ?: 0

        List currentSubIds = orgTypeService.getCurrentSubscriptions(contextService.getOrg()).collect{ it.id }

        result.subscriptionMap = [:]

        List allLocals     = OrgRole.findAllWhere(org: contextService.getOrg(), roleType: RDStore.OR_SUBSCRIBER).collect{ it -> it.sub.id }
        List allSubscrCons = OrgRole.findAllWhere(org: contextService.getOrg(), roleType: RDStore.OR_SUBSCRIBER_CONS).collect{ it -> it.sub.id }
        List allSubscrColl = OrgRole.findAllWhere(org: contextService.getOrg(), roleType: RDStore.OR_SUBSCRIBER_COLLECTIVE).collect{ it -> it.sub.id }
        List allConsOnly   = OrgRole.findAllWhere(org: contextService.getOrg(), roleType: RDStore.OR_SUBSCRIPTION_CONSORTIA).collect{ it -> it.sub.id }
        List allCollOnly   = OrgRole.findAllWhere(org: contextService.getOrg(), roleType: RDStore.OR_SUBSCRIPTION_COLLECTIVE).collect{ it -> it.sub.id }

        if(currentSubIds) {
            /*
        String base_qry1 = "select distinct p from IssueEntitlement ie join ie.subscription s join ie.tipp tipp join tipp.platform p " +
                "where s.id in (:currentSubIds)"
        println base_qry1
        platforms.addAll(Subscription.executeQuery(base_qry1, [currentSubIds: currentSubIds]))
        */

            /*
        String base_qry2 = "select distinct p from TitleInstancePackagePlatform tipp join tipp.platform p join tipp.sub s " +
                "where s.id in (:currentSubIds)"
        println base_qry2
        platforms.addAll(Subscription.executeQuery(base_qry2, [currentSubIds: currentSubIds]))
        */

            String qry3 = "select distinct p, s from SubscriptionPackage subPkg join subPkg.subscription s join subPkg.pkg pkg, " +
                    "TitleInstancePackagePlatform tipp join tipp.platform p left join p.org o " +
                    "where tipp.pkg = pkg and s.id in (:currentSubIds) "

            qry3 += " and ((pkg.packageStatus is null) or (pkg.packageStatus != :pkgDeleted))"
            qry3 += " and ((p.status is null) or (p.status != :platformDeleted))"
            qry3 += " and ((tipp.status is null) or (tipp.status != :tippDeleted))"

            def qryParams3 = [
                    currentSubIds  : currentSubIds,
                    pkgDeleted     : RDStore.PACKAGE_DELETED,
                    platformDeleted: RDStore.PLATFORM_DELETED,
                    tippDeleted    : RDStore.TIPP_DELETED
            ]

            if (params.q?.length() > 0) {
                qry3 += "and ("
                qry3 += "   genfunc_filter_matcher(p.normname, :query) = true"
                qry3 += "   or genfunc_filter_matcher(p.primaryUrl, :query) = true"
                qry3 += "   or genfunc_filter_matcher(o.name, :query) = true"
                qry3 += "   or genfunc_filter_matcher(o.sortname, :query) = true"
                qry3 += "   or genfunc_filter_matcher(o.shortname, :query) = true "
                qry3 += ")"
                qryParams3.put('query', "${params.q}")
            } else {
                qry3 += "order by p.normname asc"
            }

            qry3 += " group by p, s"

            List platformSubscriptionList = Subscription.executeQuery(qry3, qryParams3)
            /*, [max:result.max, offset:result.offset])) */
            platformSubscriptionList.each { entry ->
                String key = 'platform_' + entry[0].id

                if (! result.subscriptionMap.containsKey(key)) {
                    result.subscriptionMap.put(key, [])
                }
                if (entry[1].status?.value == RDStore.SUBSCRIPTION_CURRENT.value) {

                    if (allLocals.contains(entry[1].id)) {
                        result.subscriptionMap.get(key).add(entry[1])
                    }
                    else if (allSubscrCons.contains(entry[1].id)) {
                        result.subscriptionMap.get(key).add(entry[1])
                    }
                    else if (allSubscrColl.contains(entry[1].id)) {
                        result.subscriptionMap.get(key).add(entry[1])
                    }
                    else if (allConsOnly.contains(entry[1].id) && entry[1].instanceOf == null) {
                        result.subscriptionMap.get(key).add(entry[1])
                    }
                    else if (allCollOnly.contains(entry[1].id) && entry[1].instanceOf == null) {
                        result.subscriptionMap.get(key).add(entry[1])
                    }
                }
            }

            result.platformInstanceList = (platformSubscriptionList.collect { it[0] }).unique()
        }
        else result.platformInstanceList = []
        result.platformInstanceTotal    = result.platformInstanceList.size()


        //println "platformSubscriptionList: " + platformSubscriptionList.size()
        //println "allLocals:                " + allLocals.size()
        //println "allSubscrCons:            " + allSubscrCons.size()
        //println "allConsOnly:              " + allConsOnly.size()

        //println "${System.currentTimeSeconds() - timestamp} Sekunden"

        result
    }

    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def actionLicenses() {
        log.debug("actionLicenses :: ${params}")
        if (params['copy-license']) {
            newLicense_DEPR(params)
        } /*else if (params['delete-license']) {
            deleteLicense(params)
        } */
    }

    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def currentLicenses() {
        def result = setResultGenerics()
        result.transforms = grailsApplication.config.licenseTransforms

        result.is_inst_admin = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_ADM')
        result.editable      = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        def date_restriction = null;
        def sdf = new DateUtil().getSimpleDateFormat_NoTime()

        if (params.validOn == null || params.validOn.trim() == '') {
            result.validOn = ""
        } else {
            result.validOn = params.validOn
            date_restriction = sdf.parse(params.validOn)
        }

        result.propList = PropertyDefinition.findAllPublicAndPrivateProp([PropertyDefinition.LIC_PROP], contextService.org)
        result.max      = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset   = params.offset ? Integer.parseInt(params.offset) : 0;
        result.max      = params.format ? 10000 : result.max
        result.offset   = params.format? 0 : result.offset

        def licensee_role           = RDStore.OR_LICENSEE
        def licensee_cons_role      = RDStore.OR_LICENSEE_CONS
        def lic_cons_role           = RDStore.OR_LICENSING_CONSORTIUM
        def template_license_type   = RDStore.LICENSE_TYPE_TEMPLATE

        def base_qry
        def qry_params

        @Deprecated
        def qry = INSTITUTIONAL_LICENSES_QUERY

        result.filterSet = params.filterSet ? true : false

        if (! params.orgRole) {
            if (accessService.checkPerm("ORG_CONSORTIUM")) {
                params.orgRole = 'Licensing Consortium'
            }
            else {
                params.orgRole = 'Licensee'
            }
        }

        if (params.orgRole == 'Licensee') {

            base_qry = """
from License as l where (
    exists ( select o from l.orgLinks as o where ( ( o.roleType = :roleType1 or o.roleType = :roleType2 ) AND o.org = :lic_org ) ) 
    AND ( l.type != :template )
)
"""
            qry_params = [roleType1:licensee_role, roleType2:licensee_cons_role, lic_org:result.institution, template: template_license_type]
        }

        if (params.orgRole == 'Licensing Consortium') {

            base_qry = """
from License as l where (
    exists ( select o from l.orgLinks as o where ( 
            ( o.roleType = :roleTypeC 
                AND o.org = :lic_org 
                AND NOT exists (
                    select o2 from l.orgLinks as o2 where o2.roleType = :roleTypeL
                )
            )
        )) 
    AND ( l.type != :template )
)
"""
            qry_params = [roleTypeC:lic_cons_role, roleTypeL:licensee_cons_role, lic_org:result.institution, template:template_license_type]
        }

        if ((params['keyword-search'] != null) && (params['keyword-search'].trim().length() > 0)) {
            base_qry += " and genfunc_filter_matcher(l.reference, :ref) = true "
            qry_params += [ref:"${params['keyword-search']}"]
            result.keyWord = params['keyword-search']
        }

        // eval property filter

        if (params.filterPropDef) {
            def psq = propertyService.evalFilterQuery(params, base_qry, 'l', qry_params)
            base_qry = psq.query
            qry_params = psq.queryParams
        }

        if (date_restriction) {
            base_qry += " and ( ( l.startDate <= :date_restr and l.endDate >= :date_restr ) OR l.startDate is null OR l.endDate is null OR l.startDate >= :date_restr  ) "
            qry_params += [date_restr: date_restriction]
            qry_params += [date_restr: date_restriction]
        }

        if(params.status) {
            base_qry += " and l.status = :status "
            qry_params += [status:RefdataValue.get(params.status)]
        }
        else if(params.status == '') {
            base_qry += " and l.status != :deleted "
            qry_params += [deleted: RDStore.LICENSE_DELETED]
            result.filterSet = false
        }
        else {
            base_qry += " and l.status = :status "
            qry_params += [status:RDStore.LICENSE_CURRENT]
            params.status = RDStore.LICENSE_CURRENT.id
            result.defaultSet = true
        }

        if ((params.sort != null) && (params.sort.length() > 0)) {
            base_qry += " order by l.${params.sort} ${params.order}"
        } else {
            base_qry += " order by lower(trim(l.reference)) asc"
        }

        //log.debug("query = ${base_qry}");
        //log.debug("params = ${qry_params}");

        List totalLicenses = License.executeQuery("select l ${base_qry}", qry_params)
        result.licenseCount = totalLicenses.size()
        result.licenses = totalLicenses.drop((int) result.offset).take((int) result.max)
        List orgRoles = OrgRole.findAllByOrgAndLicIsNotNull(result.institution)
        result.orgRoles = [:]
        orgRoles.each { oo ->
            result.orgRoles.put(oo.lic.id,oo.roleType)
        }

        def filename = "${g.message(code: 'export.my.currentLicenses')}_${sdf.format(new Date(System.currentTimeMillis()))}"
        if(params.exportXLS) {
            response.setHeader("Content-disposition", "attachment; filename=\"${filename}.xlsx\"")
            response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            List titles = [
                    g.message(code:'license.details.reference'),
                    g.message(code:'license.details.linked_subs'),
                    g.message(code:'consortium'),
                    g.message(code:'license.licensor.label'),
                    g.message(code:'license.startDate'),
                    g.message(code:'license.endDate'),
                    g.message(code:'license.properties'),
                    g.message(code:'license.properties.private')+" "+result.institution.name
            ]
            List rows = []
            totalLicenses.each { licObj ->
                License license = (License) licObj
                List row = [[field:license.reference.replaceAll(',',' '),style:null]]
                List linkedSubs = license.subscriptions.collect { sub ->
                    sub.name
                }
                row.add([field:linkedSubs.join(", "),style:null])
                row.add([field:license.licensingConsortium ? license.licensingConsortium.name : '',style:null])
                row.add([field:license.licensor ? license.licensor.name : '',style:null])
                row.add([field:license.startDate ? sdf.format(license.startDate) : '',style:null])
                row.add([field:license.endDate ? sdf.format(license.endDate) : '',style:null])
                List customProps = license.customProperties.collect { customProp ->
                    if(customProp.type.type == RefdataValue.toString() && customProp.refValue)
                        "${customProp.type.getI10n('name')}: ${customProp.refValue.getI10n('value')}"
                    else
                        "${customProp.type.getI10n('name')}: ${customProp.getValue()}"
                }
                row.add([field:customProps.join(", "),style:null])
                List privateProps = license.privateProperties.collect { privateProp ->
                    if(privateProp.type.type == RefdataValue.toString() && privateProp.refValue)
                        "${privateProp.type.getI10n('name')}: ${privateProp.refValue.getI10n('value')}"
                    else
                        "${privateProp.type.getI10n('name')}: ${privateProp.getValue()}"
                }
                row.add([field:privateProps.join(", "),style:null])
                rows.add(row)
            }
            Map sheetData = [:]
            sheetData[g.message(code:'menu.my.licenses')] = [titleRow:titles,columnData:rows]
            SXSSFWorkbook workbook = exportService.generateXLSXWorkbook(sheetData)
            workbook.write(response.outputStream)
            response.outputStream.flush()
            response.outputStream.close()
            workbook.dispose()
            return
        }
        withFormat {
            html result

            json {
                response.setHeader("Content-disposition", "attachment; filename=\"${filename}.json\"")
                response.contentType = "application/json"
                render (result as JSON)
            }
            csv {
                response.setHeader("Content-disposition", "attachment; filename=\"${filename}.csv\"")
                response.contentType = "text/csv"
                ServletOutputStream out = response.outputStream
                List titles = [
                        g.message(code:'license.details.reference'),
                        g.message(code:'license.details.linked_subs'),
                        g.message(code:'consortium'),
                        g.message(code:'license.licensor.label'),
                        g.message(code:'license.startDate'),
                        g.message(code:'license.endDate'),
                        g.message(code:'license.properties'),
                        g.message(code:'license.properties.private')+" "+result.institution.name
                ]
                List rows = []
                totalLicenses.each { licObj ->
                    License license = (License) licObj
                    List row = [license.reference.replaceAll(',',' ')]
                    List linkedSubs = license.subscriptions.collect { sub ->
                        sub.name.replaceAll(',',' ')
                    }
                    row.add(linkedSubs.join("; "))
                    row.add(license.licensingConsortium)
                    row.add(license.licensor)
                    row.add(license.startDate ? sdf.format(license.startDate) : '')
                    row.add(license.endDate ? sdf.format(license.endDate) : '')
                    List customProps = license.customProperties.collect { customProp ->
                        if(customProp.type.type == RefdataValue.toString() && customProp.refValue)
                            "${customProp.type.getI10n('name')}: ${customProp.refValue.getI10n('value')}"
                        else
                            "${customProp.type.getI10n('name')}: ${customProp.getValue()}"
                    }
                    row.add(customProps.join("; "))
                    List privateProps = license.privateProperties.collect { privateProp ->
                        if(privateProp.type.type == RefdataValue.toString() && privateProp.refValue)
                            "${privateProp.type.getI10n('name')}: ${privateProp.refValue.getI10n('value')}"
                        else
                            "${privateProp.type.getI10n('name')}: ${privateProp.getValue()}"
                    }
                    row.add(privateProps.join("; "))
                    rows.add(row)
                }
                out.withWriter { writer ->
                    writer.write(exportService.generateSeparatorTableString(titles,rows,','))
                }
                out.close()
            }
            xml {
                def doc = exportService.buildDocXML("Licences")

                if ((params.transformId) && (result.transforms[params.transformId] != null)) {
                    switch(params.transformId) {
                      case "sub_ie":
                        exportService.addLicenseSubPkgTitleXML(doc, doc.getDocumentElement(),result.licenses)
                      break;
                      case "sub_pkg":
                        exportService.addLicenseSubPkgXML(doc, doc.getDocumentElement(),result.licenses)
                        break;
                    }
                    String xml = exportService.streamOutXML(doc, new StringWriter()).getWriter().toString();
                    transformerService.triggerTransform(result.user, filename, result.transforms[params.transformId], xml, response)
                }else{
                    response.setHeader("Content-disposition", "attachment; filename=\"${filename}.xml\"")
                    response.contentType = "text/xml"
                    exportService.streamOutXML(doc, response.outputStream)
                }
            }
        }
    }

    private buildPropertySearchQuery(params,propDef) {
        def result = [:]

        def query = " and exists ( select cp from l.customProperties as cp where cp.type.name = :prop_filter_name and  "
        def queryParam = [prop_filter_name:params.propertyFilterType];
        switch (propDef.type){
            case Integer.toString():
                query += "cp.intValue = :filter_val "
                def value;
                try{
                 value =Integer.parseInt(params.propertyFilter)
                }catch(Exception e){
                    log.error("Exception parsing search value: ${e}")
                    value = 0
                }
                queryParam += [filter_val:value]
                break;
            case BigDecimal.toString():
                query += "cp.decValue = :filter_val "
                try{
                 value = new BigDecimal(params.propertyFilter)
                }catch(Exception e){
                    log.error("Exception parsing search value: ${e}")
                    value = 0.0
                }
                queryParam += [filter_val:value]
                break;
            case String.toString():
                query += "cp.stringValue like :filter_val "
                queryParam += [filter_val:params.propertyFilter]
                break;
            case RefdataValue.toString():
                query += "cp.refValue.value like :filter_val "
                queryParam += [filter_val:params.propertyFilter]
                break;
            default:
                log.error("Error executing buildPropertySearchQuery. Definition type ${propDef.type} case not found. ")
        }
        query += ")"

        result.query = query
        result.queryParam = queryParam
        result
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_EDITOR")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliation("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR")
    })
    def emptyLicense() {
        def result = setResultGenerics()

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

        if (! accessService.checkUserIsMember(result.user, result.institution)) {
            flash.error = message(code:'myinst.error.noMember', args:[result.institution.name]);
            response.sendError(401)
            return;
        }

        def cal = new java.util.GregorianCalendar()
        def sdf = new DateUtil().getSimpleDateFormat_NoTime()

        cal.setTimeInMillis(System.currentTimeMillis())
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        result.defaultStartYear = sdf.format(cal.getTime())

        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)

        result.defaultEndYear = sdf.format(cal.getTime())

        result.is_inst_admin = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        def template_license_type = RefdataValue.getByValueAndCategory('Template', 'License Type')
        def qparams = [template_license_type]
        def public_flag = false

       // This query used to allow institutions to copy their own licenses - now users only want to copy template licenses
        // (OS License specs)
        // def qry = "from License as l where ( ( l.type = ? ) OR ( exists ( select ol from OrgRole as ol where ol.lic = l AND ol.org = ? and ol.roleType = ? ) ) OR ( l.isPublic=? ) ) AND l.status.value != 'Deleted'"

        def query = "from License as l where l.type = ? "

        if (params.filter) {
            query += " and lower(l.reference) like ?"
            qparams.add("%${params.filter.toLowerCase()}%")
        }

        //separately select all licenses that are not public or are null, to test access rights.
        // For some reason that I could track, l.isPublic != 'public-yes' returns different results.
        def non_public_query = query + " and ( l.isPublic = ? ) "

        if ((params.sort != null) && (params.sort.length() > 0)) {
            query += " order by l.${params.sort} ${params.order}"
        } else {
            query += " order by sortableReference asc"
        }

        result.numLicenses = License.executeQuery("select l.id ${query}", qparams).size()
        result.licenses = License.executeQuery("select l ${query}", qparams,[max: result.max, offset: result.offset])

        //We do the following to remove any licenses the user does not have access rights
        qparams += public_flag

        def nonPublic = License.executeQuery("select l ${non_public_query}", qparams)
        def no_access = nonPublic.findAll{ ! it.hasPerm("view", result.user)  }

        result.licenses = result.licenses - no_access
        result.numLicenses = result.numLicenses - no_access.size()

        if (params.sub) {
            result.sub         = params.sub
            result.subInstance = Subscription.get(params.sub)
        }

        result
    }

    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def currentProviders() {

        def result = setResultGenerics()

        result.orgRoles    = [RDStore.OR_PROVIDER, RDStore.OR_AGENCY]
        result.propList    = PropertyDefinition.findAllPublicAndPrivateOrgProp(contextService.getOrg())

        List<Org> providers = orgTypeService.getCurrentProviders( contextService.getOrg())
        List<Org> agencies   = orgTypeService.getCurrentAgencies( contextService.getOrg())

        providers.addAll(agencies)
        List orgIds = providers.unique().collect{ it2 -> it2.id }

//        result.user = User.get(springSecurityService.principal.id)
        params.sort = params.sort ?: " LOWER(o.shortname), LOWER(o.name)"
        result.max  = params.max ? Integer.parseInt(params.max) : result.user?.getDefaultPageSizeTMP()
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0

        def fsq  = filterService.getOrgQuery([constraint_orgIds: orgIds] << params)
        result.filterSet = params.filterSet ? true : false
        if (params.filterPropDef) {
            fsq = propertyService.evalFilterQuery(params, fsq.query, 'o', fsq.queryParams)
        }
        List orgListTotal = Org.findAll(fsq.query, fsq.queryParams)
        result.orgListTotal = orgListTotal.size()
        result.orgList = orgListTotal.drop((int) result.offset).take((int) result.max)

        def message = g.message(code: 'export.my.currentProviders')
        SimpleDateFormat sdf = new SimpleDateFormat(g.message(code:'default.date.format.notime', default:'yyyy-MM-dd'))
        String datetoday = sdf.format(new Date(System.currentTimeMillis()))
        String filename = message+"_${datetoday}"
        if ( params.exportXLS ) {
            try {
                SXSSFWorkbook wb = (SXSSFWorkbook) organisationService.exportOrg(orgListTotal, message, true, "xls")
                // Write the output to a file

                response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
                response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                wb.write(response.outputStream)
                response.outputStream.flush()
                response.outputStream.close()
                wb.dispose()

                return
            }
            catch (Exception e) {
                log.error("Problem",e);
                response.sendError(500)
            }
        }

        withFormat {
            html {
                result
            }
            csv {
                response.setHeader("Content-disposition", "attachment; filename=\"${filename}.csv\"")
                response.contentType = "text/csv"
                ServletOutputStream out = response.outputStream
                out.withWriter { writer ->
                    writer.write((String) organisationService.exportOrg(orgListTotal,message,true,"csv"))
                }
                out.close()
            }
        }
    }


    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def currentSubscriptions() {
        def result = setResultGenerics()

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP()
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0

        result.availableConsortia = Combo.executeQuery("select c.toOrg from Combo as c where c.fromOrg = ?", [result.institution])

        def viableOrgs = []

        if ( result.availableConsortia ){
          result.availableConsortia.each {
            viableOrgs.add(it)
          }
        }

        viableOrgs.add(result.institution)

        def date_restriction = null;
        def sdf = new DateUtil().getSimpleDateFormat_NoTime()

        if (params.validOn == null || params.validOn.trim() == '') {
            result.validOn = ""
        } else {
            result.validOn = params.validOn
            date_restriction = sdf.parse(params.validOn)
        }

        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        if (! params.status) {
            if (params.isSiteReloaded != "yes") {
                params.status = RDStore.SUBSCRIPTION_CURRENT.id
                result.defaultSet = true
            }
            else {
                params.status = 'FETCH_ALL'
            }
        }

        def tmpQ = subscriptionsQueryService.myInstitutionCurrentSubscriptionsBaseQuery(params, contextService.org)
        result.filterSet = tmpQ[2]
        List<Subscription> subscriptions = Subscription.executeQuery("select s ${tmpQ[0]}", tmpQ[1]) //,[max: result.max, offset: result.offset]
        if(!params.exportXLS)
        result.num_sub_rows = subscriptions.size()

        result.date_restriction = date_restriction;
        result.propList = PropertyDefinition.findAllPublicAndPrivateProp([PropertyDefinition.SUB_PROP], contextService.org)
        if (OrgSettings.get(result.institution, OrgSettings.KEYS.NATSTAT_SERVER_REQUESTOR_ID) instanceof OrgSettings){
            result.statsWibid = result.institution.getIdentifierByType('wibid')?.value
            result.usageMode = accessService.checkPerm("ORG_CONSORTIUM") ? 'package' : 'institution'
        }

        if(params.sort && params.sort.indexOf("§") >= 0) {
            switch(params.sort) {
                case "orgRole§provider":
                    subscriptions.sort { x,y ->
                        String a = x.getProviders().size() > 0 ? x.getProviders().first().name : ''
                        String b = y.getProviders().size() > 0 ? y.getProviders().first().name : ''
                        a.compareToIgnoreCase b
                    }
                    if(params.order.equals("desc"))
                        subscriptions.reverse(true)
                break
            }
        }

        result.subscriptions = subscriptions.drop((int) result.offset).take((int) result.max)

        // Write the output to a file
        sdf = new SimpleDateFormat(g.message(code: 'default.date.format.notimenopoint'));
        String datetoday = sdf.format(new Date(System.currentTimeMillis()))
        String filename = "${datetoday}_" + g.message(code: "export.my.currentSubscriptions")

        if ( params.exportXLS ) {

            //if(wb instanceof XSSFWorkbook) file += "x";
            response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
            response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            SXSSFWorkbook wb = (SXSSFWorkbook) exportcurrentSubscription(subscriptions, "xls", result.institution)
            wb.write(response.outputStream)
            response.outputStream.flush()
            response.outputStream.close()
            wb.dispose()

            return
        }

        withFormat {
            html {
                result
            }
            csv {
                response.setHeader("Content-disposition", "attachment; filename=\"${filename}.csv\"")
                response.contentType = "text/csv"
                ServletOutputStream out = response.outputStream
                out.withWriter { writer ->
                    writer.write((String) exportcurrentSubscription(subscriptions,"csv", result.institution))
                }
                out.close()
            }
        }
    }


    private def exportcurrentSubscription(List<Subscription> subscriptions, String format,contextOrg) {
        SimpleDateFormat sdf = new SimpleDateFormat(g.message(code:'default.date.format.notime'))
        List titles = ['Name',
                       g.message(code: 'subscription.owner.label'),
                       g.message(code: 'subscription.packages.label'),
                       g.message(code: 'consortium.label'),
                       g.message(code: 'default.provider.label'),
                       g.message(code: 'default.agency.label'),
                       g.message(code: 'subscription.startDate.label'),
                       g.message(code: 'subscription.endDate.label'),
                       g.message(code: 'subscription.manualCancellationDate.label'),
                       g.message(code: 'default.identifiers.label'),
                       g.message(code: 'subscription.details.status'),
                       g.message(code: 'subscription.details.type'),
                       g.message(code: 'subscription.form.label'),
                       g.message(code: 'subscription.resource.label')]
        boolean asCons = false
        if(accessService.checkPerm('ORG_CONSORTIUM')) {
            asCons = true
            titles.addAll([g.message(code: 'subscription.memberCount.label'),g.message(code: 'subscription.memberCostItemsCount.label')])
        }
        Map<Subscription,Set> providers = [:]
        Map<Subscription,Set> agencies = [:]
        Map<Subscription,Set> identifiers = [:]
        Map costItemCounts = [:]
        List allProviders = OrgRole.findAllByRoleTypeAndSubIsNotNull(RDStore.OR_PROVIDER)
        List allAgencies = OrgRole.findAllByRoleTypeAndSubIsNotNull(RDStore.OR_AGENCY)
        List allIdentifiers = IdentifierOccurrence.findAllBySubIsNotNull()
        List allCostItems = CostItem.executeQuery('select count(ci.id),s.instanceOf.id from CostItem ci join ci.sub s where s.instanceOf != null and ci.costItemStatus != :ciDeleted and ci.owner = :owner group by s.instanceOf.id',[ciDeleted:RDStore.COST_ITEM_DELETED,owner:contextOrg])
        allProviders.each { provider ->
            Set subProviders
            if(providers.get(provider.sub)) {
                subProviders = providers.get(provider.sub)
            }
            else subProviders = new TreeSet()
            subProviders.add(provider.org.name)
            providers.put(provider.sub,subProviders)
        }
        allAgencies.each { agency ->
            Set subAgencies
            if(agencies.get(agency.sub)) {
                subAgencies = agencies.get(agency.sub)
            }
            else subAgencies = new TreeSet()
            subAgencies.add(agency.org.name)
            agencies.put(agency.sub,subAgencies)
        }
        allIdentifiers.each { identifier ->
            Set subIdentifiers
            if(identifiers.get(identifier.sub))
                subIdentifiers = identifiers.get(identifier.sub)
            else subIdentifiers = new TreeSet()
            subIdentifiers.add("(${identifier.identifier.ns.ns}) ${identifier.identifier.value}")
            identifiers.put(identifier.sub,subIdentifiers)
        }
        allCostItems.each { row ->
            costItemCounts.put(row[1],row[0])
        }
        List membershipCounts = Subscription.executeQuery('select count(s.id),s.instanceOf.id from Subscription s where s.instanceOf != null group by s.instanceOf.id')
        Map subscriptionMembers = [:]
        membershipCounts.each { row ->
            subscriptionMembers.put(row[1],row[0])
        }
        List subscriptionData = []
        subscriptions.each { sub ->
            List row = []
            switch (format) {
                case "xls":
                case "xlsx":
                    row.add([field: sub.name ?: "", style: null])
                    List ownerReferences = sub.owner?.collect {
                        it.reference
                    }
                    row.add([field: ownerReferences ? ownerReferences.join(", ") : '', style: null])
                    List packageNames = sub.packages?.collect {
                        it.pkg.name
                    }
                    row.add([field: packageNames ? packageNames.join(", ") : '', style: null])
                    row.add([field: sub.getConsortia()?.name ?: '', style: null])
                    row.add([field: providers.get(sub) ? providers.get(sub).join(", ") : '', style: null])
                    row.add([field: agencies.get(sub) ? agencies.get(sub).join(", ") : '', style: null])
                    row.add([field: sub.startDate ? sdf.format(sub.startDate) : '', style: null])
                    row.add([field: sub.endDate ? sdf.format(sub.endDate) : '', style: null])
                    row.add([field: sub.manualCancellationDate ? sdf.format(sub.manualCancellationDate) : '', style: null])
                    row.add([field: identifiers.get(sub) ? identifiers.get(sub).join(", ") : '',style: null])
                    row.add([field: sub.status?.getI10n("value"), style: null])
                    row.add([field: sub.type?.getI10n("value"), style: null])
                    row.add([field: sub.form?.getI10n("value") ?: '', style: null])
                    row.add([field: sub.resource?.getI10n("value") ?: '', style: null])
                    if(asCons) {
                        row.add([field: subscriptionMembers.get(sub.id) ?: 0, style: null])
                        row.add([field: costItemCounts.get(sub.id) ?: 0, style: null])
                    }
                    subscriptionData.add(row)
                    break
                case "csv":
                    row.add(sub.name ? sub.name.replaceAll(',',' ') : "")
                    List ownerReferences = sub.owner?.collect {
                        it.reference
                    }
                    row.add(ownerReferences ? ownerReferences.join("; ") : '')
                    List packageNames = sub.packages?.collect {
                        it.pkg.name
                    }
                    row.add(packageNames ? packageNames.join("; ") : '')
                    row.add(sub.getConsortia()?.name ?: '')
                    row.add(providers.get(sub) ? providers.get(sub).join("; ") : '')
                    row.add(agencies.get(sub) ? agencies.get(sub).join("; ") : '')
                    row.add(sub.startDate ? sdf.format(sub.startDate) : '')
                    row.add(sub.endDate ? sdf.format(sub.endDate) : '')
                    row.add(sub.manualCancellationDate ? sdf.format(sub.manualCancellationDate) : '')
                    row.add(identifiers.get(sub) ? identifiers.get(sub).join("; ") : '')
                    row.add(sub.status?.getI10n("value"))
                    row.add(sub.type?.getI10n("value"))
                    row.add(sub.form?.getI10n("value"))
                    row.add(sub.resource?.getI10n("value"))
                    if(asCons) {
                        row.add(subscriptionMembers.get(sub.id))
                        row.add(costItemCounts.get(sub.id))
                    }
                    subscriptionData.add(row)
                    break
            }
        }
        switch(format) {
            case 'xls':
            case 'xlsx':
                Map sheetData = [:]
                sheetData[message(code: 'menu.my.subscriptions')] = [titleRow: titles, columnData: subscriptionData]
                return exportService.generateXLSXWorkbook(sheetData)
            case 'csv': return exportService.generateSeparatorTableString(titles, subscriptionData, ',')
        }
    }

    private def exportSurveyInfo(List<SurveyResult> results, String format, Org org) {
        SimpleDateFormat sdf = new SimpleDateFormat(g.message(code:'default.date.format.notime'))
        List titles = [g.message(code: 'surveyInfo.owner.label'),

                       g.message(code: 'surveyConfigsInfo.comment'),

                       g.message(code: 'surveyProperty.subName'),
                       g.message(code: 'surveyProperty.subProvider'),
                       g.message(code: 'surveyProperty.subAgency'),
                       g.message(code: 'subscription.owner.label'),
                       g.message(code: 'subscription.packages.label'),
                       g.message(code: 'subscription.details.status'),
                       g.message(code: 'subscription.details.type'),
                       g.message(code: 'subscription.form.label'),
                       g.message(code: 'subscription.resource.label'),

                       g.message(code: 'surveyConfigsInfo.newPrice'),
                       g.message(code: 'surveyConfigsInfo.newPrice.comment'),

                       g.message(code: 'surveyProperty.label'),
                       g.message(code: 'surveyProperty.type.label'),
                       g.message(code: 'surveyResult.result'),
                       g.message(code: 'surveyResult.comment'),
                        g.message(code: 'surveyResult.finishDate')]

        List surveyData = []
        results.findAll{it.surveyConfig.type == 'Subscription'}.each { result ->
            List row = []
            switch (format) {
                case "xls":
                case "xlsx":

                    def sub = result.surveyConfig.subscription.getDerivedSubscriptionBySubscribers(org)

                    def surveyCostItem = CostItem.findBySurveyOrg(SurveyOrg.findBySurveyConfigAndOrg(result?.surveyConfig, org))

                    row.add([field: result?.owner?.name ?: '', style: null])
                    row.add([field: result?.surveyConfig?.comment ?: '', style: null])
                    row.add([field: sub?.name ?: "", style: null])


                    row.add([field: sub?.providers ? sub?.providers?.join(", "): '', style: null])
                    row.add([field: sub?.agencies ? sub?.agencies?.join(", "): '', style: null])

                    row.add([field: sub?.owner?.reference ?: '', style: null])
                    List packageNames = sub?.packages?.collect {
                        it.pkg.name
                    }
                    row.add([field: packageNames ? packageNames.join(", ") : '', style: null])
                    row.add([field: sub?.status?.getI10n("value") ?: '', style: null])
                    row.add([field: sub?.type?.getI10n("value") ?: '', style: null])
                    row.add([field: sub?.form?.getI10n("value") ?: '', style: null])
                    row.add([field: sub?.resource?.getI10n("value") ?: '', style: null])

                    row.add([field: surveyCostItem?.costInBillingCurrencyAfterTax ? g.formatNumber(number: surveyCostItem?.costInBillingCurrencyAfterTax, minFractionDigits:"2", maxFractionDigits:"2", type:"number") : '', style: null])

                    row.add([field: surveyCostItem?.costDescription ?: '', style: null])

                    row.add([field: result.type?.getI10n('name') ?: '', style: null])
                    row.add([field: result.type?.getLocalizedType() ?: '', style: null])

                    def value = ""

                    if(result?.type?.type == Integer.toString())
                    {
                        value = result?.intValue ? result?.intValue.toString() : ""
                    }
                    else if (result?.type?.type == String.toString())
                    {
                        value = result?.stringValue ?: ""
                    }
                    else if (result?.type?.type ==  BigDecimal.toString())
                    {
                        value = result?.decValue ? result?.decValue.toString() : ""
                    }
                    else if (result?.type?.type == Date.toString())
                    {
                        value = result?.dateValue ? sdf.format(result?.dateValue) : ""
                    }
                    else if (result?.type?.type == URL.toString())
                    {
                        value = result?.urlValue ? result?.urlValue.toString() : ""
                    }
                    else if (result?.type?.type == RefdataValue.toString())
                    {
                        value = result?.refValue ? result?.refValue.getI10n('value') : ""
                    }

                    row.add([field: value ?: '', style: null])
                    row.add([field: result.comment ?: '', style: null])
                    row.add([field: result.finishDate ? sdf.format(result?.finishDate) : '', style: null])

                    surveyData.add(row)
                    break
            }
        }
        switch(format) {
            case 'xls':
            case 'xlsx':
                Map sheetData = [:]
                sheetData[message(code: 'menu.my.subscriptions')] = [titleRow: titles, columnData: surveyData]
                return exportService.generateXLSXWorkbook(sheetData)
        }
    }

    @Deprecated
    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def addSubscription() {
        def result = setResultGenerics()

        def date_restriction = null;
        def sdf = new DateUtil().getSimpleDateFormat_NoTime()

        if (params.validOn == null) {
            result.validOn = sdf.format(new Date(System.currentTimeMillis()))
            date_restriction = sdf.parse(result.validOn)
        } else if (params.validOn.trim() == '') {
            result.validOn = "" 
        } else {
            result.validOn = params.validOn
            date_restriction = sdf.parse(params.validOn)
        }

        // if ( ! permissionHelperService.hasUserWithRole(result.user, result.institution, 'INST_ADM') ) {
        if (! accessService.checkUserIsMember(result.user, result.institution)) {
            flash.error = message(code:'myinst.currentSubscriptions.no_permission', args:[result.institution.name]);
            response.sendError(401)
            result.is_inst_admin = false;
            // render(status: '401', text:"You do not have permission to add subscriptions to ${result.institution.name}. Please request editor access on the profile page");
            return;
        }

        result.is_inst_admin = result.user?.hasAffiliation('INST_ADM')

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

        // def base_qry = " from Subscription as s where s.type.value = 'Subscription Offered' and s.isPublic=?"
        def qry_params = []
        def base_qry = " from Package as p where lower(p.name) like ?"

        if (params.q == null) {
            qry_params.add("%");
        } else {
            qry_params.add("%${params.q.trim().toLowerCase()}%");
        }

        if (date_restriction) {
            base_qry += " and p.startDate <= ? and p.endDate >= ? "
            qry_params.add(date_restriction)
            qry_params.add(date_restriction)
        }

        // Only list subscriptions where the user has view perms against the org
        // base_qry += "and ( ( exists select or from OrgRole where or.org =? and or.user = ? and or.perms.contains'view' ) "

        // Or the user is a member of an org who has a consortial membership that has view perms
        // base_qry += " or ( 2==2 ) )"

        if ((params.sort != null) && (params.sort.length() > 0)) {
            base_qry += " order by ${params.sort} ${params.order}"
        } else {
            base_qry += " order by p.name asc"
        }

        result.num_pkg_rows = Package.executeQuery("select p.id " + base_qry, qry_params).size()
        result.packages = Package.executeQuery("select p ${base_qry}", qry_params, [max: result.max, offset: result.offset]);

        result
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_EDITOR")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliation("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR")
    })
    def emptySubscription() {
        def result = setResultGenerics()
        
        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        if (result.editable) {
            def cal = new java.util.GregorianCalendar()
            def sdf = new DateUtil().getSimpleDateFormat_NoTime()

            cal.setTimeInMillis(System.currentTimeMillis())
            cal.set(Calendar.MONTH, Calendar.JANUARY)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            result.defaultStartYear = sdf.format(cal.getTime())
            cal.set(Calendar.MONTH, Calendar.DECEMBER)
            cal.set(Calendar.DAY_OF_MONTH, 31)
            result.defaultEndYear = sdf.format(cal.getTime())
            result.defaultSubIdentifier = java.util.UUID.randomUUID().toString()

            if(accessService.checkPerm("ORG_CONSORTIUM,ORG_INST_COLLECTIVE")) {
                if(accessService.checkPerm("ORG_CONSORTIUM")) {
                    params.comboType = RDStore.COMBO_TYPE_CONSORTIUM.value
                    result.consortialView = true
                }
                else if(accessService.checkPerm("ORG_INST_COLLECTIVE")) {
                    params.comboType = RDStore.COMBO_TYPE_DEPARTMENT.value
                    result.departmentalView = true
                }
                def fsq = filterService.getOrgComboQuery(params, result.institution)
                result.members = Org.executeQuery(fsq.query, fsq.queryParams, params)
            }

            result
        } else {
            flash.message = "${message(code: 'default.notAutorized.message')}"
            redirect action: 'currentSubscriptions'
        }
    }

    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def processEmptySubscription() {
        log.debug(params)
        def result = setResultGenerics()

        RefdataValue role_sub = RDStore.OR_SUBSCRIBER
        RefdataValue role_sub_cons = RDStore.OR_SUBSCRIBER_CONS
        RefdataValue role_sub_cons_hidden = RDStore.OR_SUBSCRIBER_CONS_HIDDEN
        RefdataValue role_sub_coll = RDStore.OR_SUBSCRIBER_COLLECTIVE
        RefdataValue role_cons = RDStore.OR_SUBSCRIPTION_CONSORTIA
        RefdataValue role_coll = RDStore.OR_SUBSCRIPTION_COLLECTIVE

        RefdataValue orgRole
        RefdataValue memberRole
        RefdataValue subType = RefdataValue.get(params.type)

        switch(subType) {
            case RDStore.SUBSCRIPTION_TYPE_CONSORTIAL:
            case RDStore.SUBSCRIPTION_TYPE_ADMINISTRATIVE:
            case RDStore.SUBSCRIPTION_TYPE_NATIONAL:
            case RDStore.SUBSCRIPTION_TYPE_ALLIANCE:
				orgRole = role_cons
                memberRole = role_sub_cons
                break
            default:
                if (result.institution.getCustomerType() == 'ORG_INST_COLLECTIVE') {
                    orgRole = role_coll
                    memberRole = role_sub_coll
                }
                else {
                    orgRole = role_sub
                    if (! subType)
                        subType = RDStore.SUBSCRIPTION_TYPE_LOCAL
                }
                break
        }

        if (accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')) {

            SimpleDateFormat sdf = new DateUtil().getSimpleDateFormat_NoTime()
            Date startDate = params.valid_from ? sdf.parse(params.valid_from) : null
            Date endDate = params.valid_to ? sdf.parse(params.valid_to) : null
            RefdataValue status = RefdataValue.get(params.status)

            //beware: at this place, we cannot calculate the subscription type because essential data for the calculation is not persisted/available yet!
            boolean administrative = false
            if(subType == RDStore.SUBSCRIPTION_TYPE_ADMINISTRATIVE)
                administrative = true

            def new_sub = new Subscription(
                    type: subType,
                    name: params.newEmptySubName,
                    startDate: startDate,
                    endDate: endDate,
                    status: status,
                    administrative: administrative,
                    identifier: params.newEmptySubId,
                    isPublic: false,
                    impId: java.util.UUID.randomUUID().toString())

            if (new_sub.save()) {
                def new_sub_link = new OrgRole(org: result.institution,
                        sub: new_sub,
                        roleType: orgRole).save();
                        
                // if((com.k_int.kbplus.RefdataValue.getByValueAndCategory('Consortium', 'OrgRoleType')?.id in result.orgType) && params.linkToAll == "Y"){ // old code

                if (accessService.checkPerm('ORG_INST_COLLECTIVE') ||
                        (accessService.checkPerm('ORG_CONSORTIUM') && subType != RDStore.SUBSCRIPTION_TYPE_LOCAL)
                ){
                    
                    def cons_members = []

                    params.list('selectedOrgs').each{ it ->
                        def fo =  Org.findById(Long.valueOf(it))
                        cons_members << Combo.executeQuery(
                                "select c.fromOrg from Combo as c where c.toOrg = ? and c.fromOrg = ?",
                                [result.institution, fo] )
                    }

                    //def cons_members = Combo.executeQuery("select c.fromOrg from Combo as c where c.toOrg = ?", [result.institution])

                    cons_members.each { cm ->

                    if (params.generateSlavedSubs == "Y") {
                        log.debug("Generating seperate slaved instances for consortia members")
                        def postfix = cm.get(0).shortname ?: cm.get(0).name

                        def cons_sub = new Subscription(
                                            // type: RefdataValue.findByValue("Subscription Taken"),
                                          type: subType,
                                          name: params.newEmptySubName,
                                          // name: params.newEmptySubName + " (${postfix})",
                                          startDate: startDate,
                                          endDate: endDate,
                                          identifier: java.util.UUID.randomUUID().toString(),
                                          status: status,
                                          administrative: administrative,
                                          instanceOf: new_sub,
                                          isSlaved: true,
                                          isPublic: false,
                                          impId: java.util.UUID.randomUUID().toString()).save()
                        if(new_sub.administrative) {
                            new OrgRole(org: cm,
                                    sub: cons_sub,
                                    roleType: role_sub_cons_hidden).save()
                        }
                        else {
                            new OrgRole(org: cm,
                                    sub: cons_sub,
                                    roleType: memberRole).save()
                        }


                        new OrgRole(org: result.institution,
                            sub: cons_sub,
                            roleType: orgRole).save()
                    }
                    else {
                        if(new_sub.administrative) {
                            new OrgRole(org: cm,
                                    sub: new_sub,
                                    roleType: role_sub_cons_hidden).save()
                        }
                        else {
                            new OrgRole(org: cm,
                                    sub: new_sub,
                                    roleType: memberRole).save()
                        }
                    }
                  }
                }


                if (params.newEmptySubId) {
                  def sub_id_components = params.newEmptySubId.split(':');
                  if ( sub_id_components.length == 2 ) {
                    def sub_identifier = Identifier.lookupOrCreateCanonicalIdentifier(sub_id_components[0],sub_id_components[1]);
                      new IdentifierOccurrence(sub: new_sub, identifier: sub_identifier).save()
                  }
                  else {
                    def sub_identifier = Identifier.lookupOrCreateCanonicalIdentifier('Unknown', params.newEmptySubId);
                      new IdentifierOccurrence(sub: new_sub, identifier: sub_identifier).save()
                  }
                }

                redirect controller: 'subscription', action: 'show', id: new_sub.id
            } else {
                new_sub.errors.each { e ->
                    log.debug("Problem creating new sub: ${e}");
                }
                flash.error = new_sub.errors
                redirect action: 'emptySubscription'
            }
        } else {
            redirect action: 'currentSubscriptions'
        }
    }

    @Deprecated
    def buildQuery(params) {
        log.debug("BuildQuery...");

        StringWriter sw = new StringWriter()

        if (params?.q?.length() > 0)
            sw.write(params.q)
        else
            sw.write("*:*")

        reversemap.each { mapping ->

            // log.debug("testing ${mapping.key}");

            if (params[mapping.key] != null) {
                if (params[mapping.key].class == java.util.ArrayList) {
                    params[mapping.key].each { p ->
                        sw.write(" AND ")
                        sw.write(mapping.value)
                        sw.write(":")
                        sw.write("\"${p}\"")
                    }
                } else {
                    // Only add the param if it's length is > 0 or we end up with really ugly URLs
                    // II : Changed to only do this if the value is NOT an *
                    if (params[mapping.key].length() > 0 && !(params[mapping.key].equalsIgnoreCase('*'))) {
                        sw.write(" AND ")
                        sw.write(mapping.value)
                        sw.write(":")
                        sw.write("\"${params[mapping.key]}\"")
                    }
                }
            }
        }

        sw.write(" AND type:\"Subscription Offered\"");
        def result = sw.toString();
        result;
    }

    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def processEmptyLicense() {
        def user = User.get(springSecurityService.principal.id)
        def org = contextService.getOrg()

        params.asOrgType = params.asOrgType ? [params.asOrgType] : [RDStore.OT_INSTITUTION.id.toString()]


        if (! accessService.checkMinUserOrgRole(user, org, 'INST_EDITOR')) {
            flash.error = message(code:'myinst.error.noAdmin', args:[org.name])
            response.sendError(401)
            // render(status: '401', text:"You do not have permission to access ${org.name}. Please request access on the profile page");
            return;
        }

        def baseLicense = params.baselicense ? License.get(params.baselicense) : null;
        //Nur wenn von Vorlage ist
        if (baseLicense) {
            if (!baseLicense?.hasPerm("view", user)) {
                log.debug("return 401....");
                flash.error = message(code: 'myinst.newLicense.error', default: 'You do not have permission to view the selected license. Please request access on the profile page');
                response.sendError(401)
            }
            else {
                def copyLicense = institutionsService.copyLicense(
                        baseLicense, params, InstitutionsService.CUSTOM_PROPERTIES_COPY_HARD)

                if (copyLicense.hasErrors()) {
                    log.error("Problem saving license ${copyLicense.errors}");
                    render view: 'editLicense', model: [licenseInstance: copyLicense]
                } else {
                    copyLicense.reference = params.licenseName
                    copyLicense.startDate = parseDate(params.licenseStartDate,possible_date_formats)
                    copyLicense.endDate = parseDate(params.licenseEndDate,possible_date_formats)
                    copyLicense.status = RefdataValue.get(params.status)

                    if (copyLicense.save(flush: true)) {
                        flash.message = message(code: 'license.createdfromTemplate.message')
                    }

                    if( params.sub) {
                        def subInstance = Subscription.get(params.sub)
                        subInstance.owner = copyLicense
                        subInstance.save(flush: true)
                    }

                    redirect controller: 'license', action: 'show', params: params, id: copyLicense.id
                    return
                }
            }
        }

        def license_type = RefdataValue.getByValueAndCategory('Actual', 'License Type')

        def licenseInstance = new License(type: license_type, reference: params.licenseName,
                startDate:params.licenseStartDate ? parseDate(params.licenseStartDate,possible_date_formats) : null,
                endDate: params.licenseEndDate ? parseDate(params.licenseEndDate,possible_date_formats) : null,
                status: RefdataValue.get(params.status)
        )

        if (!licenseInstance.save(flush: true)) {
            log.error(licenseInstance.errors)
            flash.error = message(code:'license.create.error')
            redirect action: 'emptyLicense'
        }
        else {
            log.debug("Save ok");
            def licensee_role = RDStore.OR_LICENSEE
            def lic_cons_role = RDStore.OR_LICENSING_CONSORTIUM

            log.debug("adding org link to new license");


            OrgRole orgRole
            if (params.asOrgType && (RDStore.OT_CONSORTIUM.id.toString() in params.asOrgType)) {
                orgRole = new OrgRole(lic: licenseInstance,org:org,roleType: lic_cons_role)
            } else {
                orgRole = new OrgRole(lic: licenseInstance,org:org,roleType: licensee_role)
            }

            if (orgRole.save(flush: true)) {
            } else {
                log.error("Problem saving org links to license ${org.errors}");
            }
            if(params.sub) {
                def subInstance = Subscription.get(params.sub)
                subInstance.owner = licenseInstance
                subInstance.save(flush: true)
            }

            flash.message = message(code: 'license.created.message')
            redirect controller: 'license', action: 'show', params: params, id: licenseInstance.id
        }
    }

    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    @Deprecated
    def newLicense_DEPR(params) {
        def user = User.get(springSecurityService.principal.id)
        def org = contextService.getOrg()

        if (! accessService.checkMinUserOrgRole(user, org, 'INST_EDITOR')) {
            flash.error = message(code:'myinst.error.noAdmin', args:[org.name]);
            response.sendError(401)
            // render(status: '401', text:"You do not have permission to access ${org.name}. Please request access on the profile page");
            return;
        }

        def baseLicense = params.baselicense ? License.get(params.baselicense) : null;

        if (! baseLicense?.hasPerm("view", user)) {
            log.debug("return 401....");
            flash.error = message(code:'myinst.newLicense.error', default:'You do not have permission to view the selected license. Please request access on the profile page');
            response.sendError(401)

        }
        else {
            //Moe fragen
            /*def copyLicense = institutionsService.copyLicense(baseLicense, params)
            if (copyLicense.hasErrors() ){ */
                log.error("Problem saving license ${copyLicense.errors}");
                render view: 'editLicense', model: [licenseInstance: copyLicense]
           /* }else{
                flash.message = message(code: 'license.created.message')
                redirect controller: 'license', action: 'show', params: params, id: copyLicense.id
            }*/
        }
    }

    /*
    @Deprecated
    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def deleteLicense(params) {
        log.debug("deleteLicense ${params}");
        def result = setResultGenerics()

        if (! accessService.checkUserIsMember(result.user, result.institution)) {
            flash.error = message(code:'myinst.error.noMember', args:[result.institution.name]);
            response.sendError(401)
            // render(status: '401', text:"You do not have permission to access ${result.institution.name}. Please request access on the profile page");
            return;
        }

        def license = License.get(params.baselicense)


        if (license?.hasPerm("edit", result.user)) {
            def current_subscription_status = RefdataValue.getByValueAndCategory('Current', 'Subscription Status')

            def subs_using_this_license = Subscription.findAllByOwnerAndStatus(license, current_subscription_status)

            if (subs_using_this_license.size() == 0) {
                license.status = RefdataValue.getByValueAndCategory('Deleted', 'License Status')
                license.save(flush: true);
            } else {
                flash.error = message(code:'myinst.deleteLicense.error', default:'Unable to delete - The selected license has attached subscriptions marked as Current')
                redirect(url: request.getHeader('referer'))
                return
            }
        } else {
            log.warn("Attempt by ${result.user} to delete license ${result.license} without perms")
            flash.message = message(code: 'license.delete.norights', default: 'You do not have edit permission for the selected license.')
            redirect(url: request.getHeader('referer'))
            return
        }

        redirect action: 'currentLicenses'
    }
    */

    @DebugAnnotation(test = 'hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    Map documents() {
        Map result = setResultGenerics()
        result
    }

    @DebugAnnotation(test='hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def deleteDocuments() {
        def ctxlist = []

        log.debug("deleteDocuments ${params}");

        docstoreService.unifiedDeleteDocuments(params)

        redirect controller: 'myInstitution', action: 'documents' /*, fragment: 'docstab' */
    }

    @Deprecated
    @Secured(['ROLE_ADMIN'])
    def processAddSubscription() {

        def user = User.get(springSecurityService.principal.id)
        def institution = contextService.getOrg()

        if (! accessService.checkUserIsMember(user, institution)) {
            flash.error = message(code:'myinst.error.noMember', args:[result.institution.name]);
            response.sendError(401)
            // render(status: '401', text:"You do not have permission to access ${institution.name}. Please request access on the profile page");
            return;
        }

        log.debug("processAddSubscription ${params}");

        def basePackage = Package.get(params.packageId);

        if (basePackage) {
            //
            def add_entitlements = (params.createSubAction == 'copy' ? true : false)

            def new_sub = basePackage.createSubscription("Subscription Taken",
                    "A New subscription....",
                    "A New subscription identifier.....",
                    basePackage.startDate,
                    basePackage.endDate,
                    basePackage.getConsortia(),
                    add_entitlements)

            def new_sub_link = new OrgRole(
                    org: institution,
                    sub: new_sub,
                    roleType: RDStore.OR_SUBSCRIBER
            ).save();

            // This is done by basePackage.createSubscription
            // def new_sub_package = new SubscriptionPackage(subscription: new_sub, pkg: basePackage).save();

            flash.message = message(code: 'subscription.created.message', args: [message(code: 'subscription.label', default: 'Package'), basePackage.id])
            redirect controller: 'subscription', action: 'index', params: params, id: new_sub.id
        } else {
            flash.message = message(code: 'subscription.unknown.message')
            redirect action: 'addSubscription', params: params
        }
    }

    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def currentTitles() {
        // define if we're dealing with a HTML request or an Export (i.e. XML or HTML)
        boolean isHtmlOutput = !params.format || params.format.equals("html")

        def result = setResultGenerics()
        result.transforms = grailsApplication.config.titlelistTransforms
        
        result.availableConsortia = Combo.executeQuery("select c.toOrg from Combo as c where c.fromOrg = ?", [result.institution])
        
        def viableOrgs = []
        
        if(result.availableConsortia){
          result.availableConsortia.each {
            viableOrgs.add(it.id)
          }
        }
        
        viableOrgs.add(result.institution.id)
        
        log.debug("Viable Org-IDs are: ${viableOrgs}, active Inst is: ${result.institution.id}")

        // Set Date Restriction
        def date_restriction = null;

        def sdf = new DateUtil().getSimpleDateFormat_NoTime()
        boolean defaultSet = false
        if (params.validOn == null) {
            result.validOn = sdf.format(new Date(System.currentTimeMillis()))
            date_restriction = sdf.parse(result.validOn)
            defaultSet = true
            log.debug("Getting titles as of ${date_restriction} (current)")
        } else if (params.validOn.trim() == '') {
            result.validOn = ""
        } else {
            result.validOn = params.validOn
            date_restriction = sdf.parse(params.validOn)
            log.debug("Getting titles as of ${date_restriction} (given)")
        }

        // Set is_inst_admin
        result.is_inst_admin = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_ADM')

        // Set offset and max
        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

        def filterSub = params.list("filterSub")
        if (filterSub.contains("all")) filterSub = null
        def filterPvd = params.list("filterPvd")
        if (filterPvd.contains("all")) filterPvd = null
        def filterHostPlat = params.list("filterHostPlat")
        if (filterHostPlat.contains("all")) filterHostPlat = null
        def filterOtherPlat = params.list("filterOtherPlat")
        if (filterOtherPlat.contains("all")) filterOtherPlat = null

        def limits = (isHtmlOutput) ? [readOnly:true,max: result.max, offset: result.offset] : [offset: 0]
        RefdataValue del_ie =  RDStore.TIPP_DELETED

        RefdataValue role_sub        = RDStore.OR_SUBSCRIBER
        RefdataValue role_sub_cons   = RDStore.OR_SUBSCRIBER_CONS

        RefdataValue role_sub_consortia = RDStore.OR_SUBSCRIPTION_CONSORTIA
        RefdataValue role_pkg_consortia = RefdataValue.getByValueAndCategory('Package Consortia', 'Organisational Role')
        def roles = [role_sub.id,role_sub_consortia.id,role_pkg_consortia.id]
        
        log.debug("viable roles are: ${roles}")
        log.debug("Using params: ${params}")
        
        def qry_params = [
                institution: result.institution.id,
                del_ie: del_ie.id,
                role_sub: role_sub.id,
                role_sub_cons: role_sub_cons.id,
                role_cons: role_sub_consortia.id]


        def sub_qry = "from issue_entitlement ie INNER JOIN subscription sub on ie.ie_subscription_fk=sub.sub_id inner join org_role orole on sub.sub_id=orole.or_sub_fk, title_instance_package_platform tipp inner join title_instance ti  on tipp.tipp_ti_fk=ti.ti_id cross join title_instance ti2 "
        if (filterOtherPlat) {
            sub_qry += "INNER JOIN platformtipp ap on ap.tipp_id = tipp.tipp_id "
        }
        if (filterPvd) {
            sub_qry += "INNER JOIN org_role orgrole on orgrole.or_pkg_fk=tipp.tipp_pkg_fk "
        }
        sub_qry += "WHERE ie.ie_tipp_fk=tipp.tipp_id  and tipp.tipp_ti_fk=ti2.ti_id and ( orole.or_roletype_fk = :role_sub or orole.or_roletype_fk = :role_sub_cons or orole.or_roletype_fk = :role_cons ) "
        sub_qry += "AND orole.or_org_fk = :institution "
        sub_qry += "AND (ie.ie_status_rv_fk is null or ie.ie_status_rv_fk != :del_ie ) "

        if (date_restriction) {
            sub_qry += " AND ( "
            sub_qry += "( ie.ie_start_date <= :date_restriction OR (ie.ie_start_date is null AND (sub.sub_start_date <= :date_restriction OR sub.sub_start_date is null) ) ) AND "
            sub_qry += "( ie.ie_end_date >= :date_restriction OR (ie.ie_end_date is null AND (sub.sub_end_date >= :date_restriction OR sub.sub_end_date is null) ) ) "
            sub_qry += ") "
            result.date_restriction = date_restriction
            qry_params.date_restriction = new Timestamp(date_restriction.getTime())
        }

        if ((params.filter) && (params.filter.length() > 0)) {
            log.debug("Adding title filter ${params.filter}");
            sub_qry += " AND genfunc_filter_matcher(ti.ti_title, :titlestr) = true "
            qry_params.titlestr = params.get('filter').toString()
        }

        if (filterSub) {
            sub_qry += " AND sub.sub_id in (" + filterSub.join(", ") + ")"
        }

        if (filterOtherPlat) {
            sub_qry += " AND ap.id in (" + filterOtherPlat.join(", ") + ")"
        }

        if (filterHostPlat) {
            sub_qry += " AND tipp.tipp_plat_fk in (" + filterHostPlat.join(", ") + ")"
        }

        if (filterPvd) {
            def cp = RefdataValue.getByValueAndCategory('Content Provider', 'Organisational Role')?.id
            sub_qry += " AND orgrole.or_roletype_fk = :cprole  AND orgrole.or_org_fk IN (" + filterPvd.join(", ") + ")"
            qry_params.cprole = cp
        }

        String having_clause = params.filterMultiIE ? 'having count(ie.ie_id) > 1' : ''
        String limits_clause = limits ? " limit :max offset :offset " : ""
        
        def order_by_clause = ''
        if (params.order == 'desc') {
            order_by_clause = 'order by ti.sort_title desc'
        } else {
            order_by_clause = 'order by ti.sort_title asc'
        }

        def sql = new Sql(dataSource)

        String queryStr = "tipp.tipp_ti_fk, count(ie.ie_id) ${sub_qry} group by ti.sort_title, tipp.tipp_ti_fk ${having_clause} ".toString()

        log.debug(" SELECT ${queryStr} ${order_by_clause} ${limits_clause} ")

        if(params.format || params.exportKBart) {
            //double run until ERMS-1188
            String filterString = ""
            Map queryParams = [ieDeleted:RDStore.TIPP_DELETED,org:result.institution,orgRoles:[RDStore.OR_SUBSCRIBER,RDStore.OR_SUBSCRIBER_CONS,RDStore.OR_SUBSCRIPTION_CONSORTIA]]
            if (date_restriction) {
                filterString += " and ((ie.startDate <= :dateRestriction or (ie.startDate = null and (ie.subscription.startDate <= :dateRestriction or ie.subscription.startDate = null))) and (ie.endDate >= :dateRestriction or (ie.endDate = null and (ie.subscription.endDate >= :dateRestriction or ie.subscription.endDate = null))))"
                queryParams.dateRestriction = date_restriction
            }

            if ((params.filter) && (params.filter.length() > 0)) {
                filterString += " and genfunc_filter_matcher(ie.tipp.title.title, :title) = true "
                queryParams.title = "${params.filter}"
            }

            if (filterSub) {
                List subs = []
                filterSub.each {fs ->
                    subs.add(Subscription.get(Long.parseLong((String) fs)))
                }
                filterString += " AND sub in (:subs)"
                queryParams.subs = subs
            }

            if (filterHostPlat) {
                List hostPlatforms = []
                filterHostPlat.each { plat ->
                    hostPlatforms.add(Platform.get(Long.parseLong((String) plat)))
                }
                filterString += " AND ie.tipp.platform in (:hostPlatforms)"
                queryParams.hostPlatforms = hostPlatforms
            }

            if (filterPvd) {
                filterString += " and pkgOrgRoles.roleType in (:contentProvider) "
                queryParams.contentProvider = filterPvd
            }
            log.debug("select ie from IssueEntitlement ie join ie.subscription.orgRelations as oo join ie.tipp.pkg.orgs pkgOrgRoles where oo.org = :org and oo.roleType in (:orgRoles) and ie.status != :ieDeleted ${filterString} order by ie.tipp.title.title asc")
            log.debug(queryParams)
            result.titles = IssueEntitlement.executeQuery("select ie from IssueEntitlement ie join ie.subscription.orgRelations as oo join ie.tipp.pkg.orgs pkgOrgRoles where oo.org = :org and oo.roleType in (:orgRoles) and ie.status != :ieDeleted ${filterString} order by ie.tipp.title.title asc",queryParams)
        }
        else {

            qry_params.max = result.max
            qry_params.offset = result.offset

            log.debug( "SELECT ${queryStr} ${order_by_clause} ${limits_clause}" )
            log.debug( qry_params )

            result.titles = sql.rows("SELECT ${queryStr} ${order_by_clause} ${limits_clause} ".toString(), qry_params).collect {
                TitleInstance.get(it.tipp_ti_fk)
            }
        }
        def queryCnt = "SELECT count(*) as count from (SELECT ${queryStr}) as ras".toString()
        result.num_ti_rows = sql.firstRow(queryCnt,qry_params)['count']
        result = setFiltersLists(result, date_restriction)

        result.filterSet = params.filterSet || defaultSet
        String filename = "titles_listing_${result.institution.shortcode}"
        if(params.exportKBart) {
            response.setHeader("Content-disposition", "attachment; filename=${filename}.tsv")
            response.contentType = "text/tsv"
            ServletOutputStream out = response.outputStream
            Map<String,List> tableData = titleStreamService.generateTitleExportList(result.titles)
            out.withWriter { writer ->
                writer.write(exportService.generateSeparatorTableString(tableData.titleRow,tableData.columnData,'\t'))
            }
            out.flush()
            out.close()
        }
        else {
            withFormat {
                html {
                    result
                }
                csv {
                    response.setHeader("Content-disposition", "attachment; filename=${filename}.csv")
                    response.contentType = "text/csv"

                    def out = response.outputStream
                    exportService.StreamOutTitlesCSV(out, result.titles)       RefdataValue del_sub = RDStore.SUBSCRIPTION_DELETED
                    out.close()
                }
                /*json {
                    def map = [:]
                    exportService.addTitlesToMap(map, result.titles)
                    def content = map as JSON

                    response.setHeader("Content-disposition", "attachment; filename=\"${filename}.json\"")
                    response.contentType = "application/json"

                    render content
                }*/
                xml {
                    def doc = exportService.buildDocXML("TitleList")
                    exportService.addTitleListXML(doc, doc.getDocumentElement(), result.titles)

                    if ((params.transformId) && (result.transforms[params.transformId] != null)) {
                        String xml = exportService.streamOutXML(doc, new StringWriter()).getWriter().toString();
                        transformerService.triggerTransform(result.user, filename, result.transforms[params.transformId], xml, response)
                    } else { // send the XML to the user
                        response.setHeader("Content-disposition", "attachment; filename=\"${filename}.xml\"")
                        response.contentType = "text/xml"
                        exportService.streamOutXML(doc, response.outputStream)
                    }
                }
            }
        }
    }

    /**
     * Add to the given result Map the list of values available for each filters.
     *
     * @param result - result Map which will be sent to the view page
     * @param date_restriction - 'Subscription valid on' date restriction as a String
     * @return the result Map with the added filter lists
     */
    private setFiltersLists(result, date_restriction) {
        // Query the list of Subscriptions
        def del_ie =  RDStore.TIPP_DELETED

        def role_sub            = RDStore.OR_SUBSCRIBER
        def role_sub_cons       = RDStore.OR_SUBSCRIBER_CONS
        def role_sub_consortia  = RDStore.OR_SUBSCRIPTION_CONSORTIA

        def cp = RefdataValue.getByValueAndCategory('Content Provider', 'Organisational Role')
        def role_consortia = RefdataValue.getByValueAndCategory('Package Consortia', 'Organisational Role')

        def roles = [role_sub, role_sub_cons, role_sub_consortia]

        def sub_params = [institution: result.institution,roles:roles]
        def sub_qry = """
Subscription AS s INNER JOIN s.orgRelations AS o
WHERE o.roleType IN (:roles)
AND o.org = :institution """
        if (date_restriction) {
            sub_qry += "\nAND s.startDate <= :date_restriction AND s.endDate >= :date_restriction "
            sub_params.date_restriction = date_restriction
        }
        result.subscriptions = Subscription.executeQuery("SELECT s FROM ${sub_qry} ORDER BY s.name", sub_params);
        result.test = Subscription.executeQuery("""
SELECT Distinct(role.org), role.org.name FROM SubscriptionPackage sp INNER JOIN sp.pkg.orgs AS role ORDER BY role.org.name """);

        // Query the list of Providers
        result.providers = Subscription.executeQuery("""
SELECT Distinct(role.org), role.org.name FROM SubscriptionPackage sp INNER JOIN sp.pkg.orgs AS role 
WHERE EXISTS ( FROM ${sub_qry} AND sp.subscription = s ) 
AND role.roleType=:role_cp 
ORDER BY role.org.name""", sub_params+[role_cp:cp]);

        // Query the list of Host Platforms
        result.hostplatforms = IssueEntitlement.executeQuery("""
SELECT distinct(ie.tipp.platform), ie.tipp.platform.name
FROM IssueEntitlement AS ie, ${sub_qry}
AND s = ie.subscription
ORDER BY ie.tipp.platform.name""", sub_params);

        // Query the list of Other Platforms
        result.otherplatforms = IssueEntitlement.executeQuery("""
SELECT distinct(p.platform), p.platform.name
FROM IssueEntitlement AS ie
  INNER JOIN ie.tipp.additionalPlatforms as p,
  ${sub_qry}
AND s = ie.subscription
ORDER BY p.platform.name""", sub_params);

        return result
    }

    /**
     * This function will gather the different filters from the request parameters and
     * will build the base of the query to gather all the information needed for the view page
     * according to the requested filtering.
     *
     * @param institution - the {@link Org} object representing the institution we're looking at
     * @param date_restriction - 'Subscription valid on' date restriction as a String
     * @return a Map containing the base query as a String and a Map containing the parameters to run the query
     */
    private buildCurrentTitlesQuery(institution, date_restriction) {
        def qry_map = [:]

        // Put multi parameters for filtering into Lists
        // Set the variables to null if filter is equal to 'all'
        def filterSub = params.list("filterSub")
        if (filterSub.contains("all")) filterSub = null
        def filterPvd = params.list("filterPvd")
        if (filterPvd.contains("all")) filterPvd = null
        def filterHostPlat = params.list("filterHostPlat")
        if (filterHostPlat.contains("all")) filterHostPlat = null
        def filterOtherPlat = params.list("filterOtherPlat")
        if (filterOtherPlat.contains("all")) filterOtherPlat = null

        def qry_params = [:]

        StringBuilder title_query = new StringBuilder()
        title_query.append("FROM IssueEntitlement AS ie ")
        // Join with Org table if there are any Provider filters
        if (filterPvd) title_query.append("INNER JOIN ie.tipp.pkg.orgs AS role ")
        // Join with the Platform table if there are any Host Platform filters
        if (filterHostPlat) title_query.append("INNER JOIN ie.tipp.platform AS hplat ")
        title_query.append(", Subscription AS s INNER JOIN s.orgRelations AS o ")

        // Main query part
        title_query.append("\
  WHERE ( o.roleType.value = 'Subscriber' or o.roleType.value = 'Subscriber_Consortial' ) \
  AND o.org = :institution \
  AND s = ie.subscription ")
        qry_params.institution = institution

        // Subscription filtering
        if (filterSub) {
            title_query.append("\
  AND ( \
  ie.subscription.id IN (:subscriptions) \
  OR ( EXISTS ( FROM IssueEntitlement AS ie2 \
  WHERE ie2.tipp.title = ie.tipp.title \
  AND ie2.subscription.id IN (:subscriptions) \
  )))")
            qry_params.subscriptions = filterSub.collect(new ArrayList<Long>()) { Long.valueOf(it) }
        }

        // Title name filtering
        // Copied from SubscriptionDetailsController
        if (params.filter) {
            title_query.append("\
  AND ( ( Lower(ie.tipp.title.title) like :filterTrim ) \
  OR ( EXISTS ( FROM IdentifierOccurrence io \
  WHERE io.ti.id = ie.tipp.title.id \
  AND io.identifier.value like :filter ) ) )")
            qry_params.filterTrim = "%${params.filter.trim().toLowerCase()}%"
            qry_params.filter = "%${params.filter}%"
        }

        // Provider filtering
        if (filterPvd) {
            title_query.append("\
  AND role.roleType.value = 'Content Provider' \
  AND role.org.id IN (:provider) ")
            qry_params.provider = filterPvd.collect(new ArrayList<Long>()) { Long.valueOf(it) }
            //Long.valueOf(params.filterPvd)
        }
        // Host Platform filtering
        if (filterHostPlat) {
            title_query.append("AND hplat.id IN (:hostPlatform) ")
            qry_params.hostPlatform = filterHostPlat.collect(new ArrayList<Long>()) { Long.valueOf(it) }
            //Long.valueOf(params.filterHostPlat)
        }
        // Host Other filtering
        if (filterOtherPlat) {
            title_query.append("""
AND EXISTS (
  FROM IssueEntitlement ie2
  WHERE EXISTS (
    FROM ie2.tipp.additionalPlatforms AS ap
    WHERE ap.platform.id IN (:otherPlatform)
  )
  AND ie2.tipp.title = ie.tipp.title
) """)
            qry_params.otherPlatform = filterOtherPlat.collect(new ArrayList<Long>()) { Long.valueOf(it) }
            //Long.valueOf(params.filterOtherPlat)
        }
        // 'Subscription valid on' filtering
        if (date_restriction) {
            title_query.append(" AND ie.subscription.startDate <= :date_restriction AND ie.subscription.endDate >= :date_restriction ")
            qry_params.date_restriction = date_restriction
        }

        title_query.append("AND ( ie.status.value != 'Deleted' ) ")

        qry_map.query = title_query.toString()
        qry_map.parameters = qry_params
        return qry_map
    }

    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def availableLicenses() {
        // def sub = resolveOID(params.elementid);
        // OrgRole.findAllByOrgAndRoleType(result.institution, licensee_role).collect { it.lic }


        def user = User.get(springSecurityService.principal.id)
        def institution = contextService.getOrg()

        if (! accessService.checkUserIsMember(user, institution)) {
            flash.error = message(code:'myinst.error.noMember', args:[institution.name]);
            response.sendError(401)
            // render(status: '401', text:"You do not have permission to access ${institution.name}. Please request access on the profile page");
            return;
        }

        def licensee_role =  RDStore.OR_LICENSEE
        def licensee_cons_role = RDStore.OR_LICENSEE_CONS

        // Find all licenses for this institution...
        def result = [:]
        OrgRole.findAllByOrg(institution).each { it ->
            if (it.roleType in [licensee_role, licensee_cons_role]) {
                result["License:${it.lic?.id}"] = it.lic?.reference
            }
        }

        //log.debug("returning ${result} as available licenses");
        render result as JSON
    }

    def resolveOID(oid_components) {
        def result = null;
        def domain_class = grailsApplication.getArtefact('Domain', "com.k_int.kbplus.${oid_components[0]}")
        if (domain_class) {
            result = domain_class.getClazz().get(oid_components[1])
        }
        result
    }

    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def actionCurrentSubscriptions() {
        def result = [:]
        result.user = User.get(springSecurityService.principal.id)
        def subscription = Subscription.get(params.basesubscription)
        def inst = Org.get(params.curInst)
        def deletedStatus = RefdataValue.getByValueAndCategory('Deleted', 'Subscription Status')

        if (subscription.hasPerm("edit", result.user)) {
            def derived_subs = Subscription.findByInstanceOf(subscription)

            if (CostItem.findBySub(subscription)) {
                flash.error = message(code: 'subscription.delete.existingCostItems')

            }
            else if (! derived_subs) {
              log.debug("Current Institution is ${inst}, sub has consortium ${subscription.consortia}")
              if( subscription.consortia && subscription.consortia != inst ) {
                OrgRole.executeUpdate("delete from OrgRole where sub = ? and org = ?",[subscription, inst])
              } else {
                subscription.status = deletedStatus
                
                if(subscription.save(flush: true)) {
                    //delete eventual links, bugfix for ERMS-800 (ERMS-892)
                    Links.executeQuery('select l from Links as l where l.objectType = :objType and :subscription in (l.source,l.destination)',[objType:Subscription.class.name,subscription:subscription]).each { l ->
                        DocContext comment = DocContext.findByLink(l)
                        if(comment) {
                            Doc commentContent = comment.owner
                            comment.delete()
                            commentContent.delete()
                        }
                        l.delete()
                    }
                }
              }
            } else {
                flash.error = message(code:'myinst.actionCurrentSubscriptions.error', default:'Unable to delete - The selected subscriptions has attached subscriptions')
            }
        } else {
            log.warn("${result.user} attempted to delete subscription ${result.subscription} without perms")
            flash.message = message(code: 'subscription.delete.norights')
        }

        redirect action: 'currentSubscriptions'
    }

    def parseDate(datestr, possible_formats) {
        def parsed_date = null;
        if (datestr && (datestr.toString().trim().length() > 0)) {
            for (Iterator i = possible_formats.iterator(); (i.hasNext() && (parsed_date == null));) {
                try {
                    parsed_date = i.next().parse(datestr.toString());
                }
                catch (Exception e) {
                }
            }
        }
        parsed_date
    }

    @DebugAnnotation(test = 'hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def dashboard() {

        def result = setResultGenerics()

        if (! accessService.checkUserIsMember(result.user, result.institution)) {
            flash.error = "You do not have permission to access ${result.institution.name} pages. Please request access on the profile page";
            response.sendError(401)
            return;
        }

        result.is_inst_admin = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_ADM')
        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;
        result.announcementOffset = 0
        result.dashboardDueDatesOffset = 0
        switch(params.view) {
            case 'announcementsView': result.announcementOffset = Integer.parseInt(params.offset)
            break
            case 'dueDatesView': result.dashboardDueDatesOffset = Integer.parseInt(params.offset)
            break
        }

        // changes

        def periodInDays = contextService.getUser().getSettingsValue(UserSettings.KEYS.DASHBOARD_ITEMS_TIME_WINDOW, 14)

        getTodoForInst(result, periodInDays)

        // announcements

        def dcCheck = (new Date()).minus(periodInDays)

        /*
        result.recentAnnouncements = Doc.executeQuery(
                "select d from Doc d where d.type.value = :type and d.dateCreated >= :dcCheck",
                [type: 'Announcement', dcCheck: dcCheck],
                [max: result.max,offset: result.announcementOffset, sort: 'dateCreated', order: 'asc']
        )
        result.recentAnnouncementsCount = Doc.executeQuery(
                "select d from Doc d where d.type.value = :type and d.dateCreated >= :dcCheck",
                [type: 'Announcement', dcCheck: dcCheck]).size()
        */

        // tasks

        DateFormat sdFormat    = new DateUtil().getSimpleDateFormat_NoTime()
        params.taskStatus = 'not done'
        def query       = filterService.getTaskQuery(params, sdFormat)
        def contextOrg  = contextService.getOrg()
        result.tasks    = taskService.getTasksByResponsibles(springSecurityService.getCurrentUser(), contextOrg, query)
        result.tasksCount    = result.tasks.size()
        def preCon      = taskService.getPreconditions(contextOrg)
        result.enableMyInstFormFields = true // enable special form fields
        result << preCon

        def announcement_type = RefdataValue.getByValueAndCategory('Announcement', 'Document Type')
        result.recentAnnouncements = Doc.findAllByType(announcement_type, [max: result.max,offset:result.announcementOffset, sort: 'dateCreated', order: 'desc'])
        result.recentAnnouncementsCount = Doc.findAllByType(announcement_type).size()
        result.dueDates = DashboardDueDate.findAllByResponsibleUserAndResponsibleOrg(contextService.user, contextService.org, [max: result.max, offset: result.dashboardDueDatesOffset])
        result.dueDatesCount = DashboardDueDate.findAllByResponsibleUserAndResponsibleOrg(contextService.user, contextService.org).size()

        def currentDate = new Date(System.currentTimeMillis())

        params.currentDate = currentDate

        def fsq = filterService.getParticipantSurveyQuery(params, sdFormat, result.institution)

        result.surveys  = SurveyInfo.findAllByIdInList(SurveyResult.findAll(fsq.query, fsq.queryParams, params).surveyConfig.surveyInfo.id)
        result.countSurvey = SurveyInfo.findAllByIdInList(SurveyResult.findAll(fsq.query, fsq.queryParams, params).surveyConfig.surveyInfo.id).size()

        result.surveysConsortia = []

                /*SurveyResult.findAll("from SurveyResult where " +
                " owner = :contextOrg and surveyConfig.surveyInfo.status != :status and " +
                " and ((startDate >= :startDate and endDate <= :endDate)" +
                " or (startDate <= :startDate and endDate is null) " +
                " or (startDate is null and endDate is null))",
                [contextOrg: contextService.org,
                 status:  RefdataValue.getByValueAndCategory('In Processing', 'Survey Status'),
                 startDate: new Date(System.currentTimeMillis()),
                 endDate: new Date(System.currentTimeMillis())])*/

        result
    }

    private getTodoForInst(result, Integer periodInDays){

        result.changes = []

        def tsCheck = (new Date()).minus(periodInDays)

        def baseParams = [owner: result.institution, tsCheck: tsCheck, stats: ['Accepted']]

        def baseQuery1 = "select distinct sub, count(sub.id) from PendingChange as pc join pc.subscription as sub where pc.owner = :owner and pc.ts >= :tsCheck " +
                " and pc.subscription is not NULL and pc.status.value in (:stats) group by sub.id"

        def result1 = PendingChange.executeQuery(
                baseQuery1,
                baseParams,
                [max: result.max, offset: result.offset]
        )
        result.changes.addAll(result1)

        def baseQuery2 = "select distinct lic, count(lic.id) from PendingChange as pc join pc.license as lic where pc.owner = :owner and pc.ts >= :tsCheck" +
                " and pc.license is not NULL and pc.status.value in (:stats) group by lic.id"

        def result2 = PendingChange.executeQuery(
                baseQuery2,
                baseParams,
                [max: result.max, offset: result.offset]
        )


        result.changes.addAll(result2)

        List result3 = PendingChange.executeQuery("select pc from PendingChange pc join pc.costItem ci where pc.owner = :owner and pc.ts >= :tsCheck and pc.costItem is not null",[owner:result.institution,tsCheck:tsCheck],[max:result.max,offset:result.offset])

        //println result.changes
        result3.each { row ->
            result.changes.add([row,1])
        }
    }

    @DebugAnnotation(test = 'hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def changes() {
        def result = setResultGenerics()

        if (! accessService.checkUserIsMember(result.user, result.institution)) {
            flash.error = "You do not have permission to view ${result.institution.name}. Please request access on the profile page"
            response.sendError(401)
            return;
        }

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP()
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0

        result.itemsTimeWindow = 365
        getTodoForInst(result, result.itemsTimeWindow)

        result
    }

    @DebugAnnotation(test = 'hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def announcements() {
        def result = setResultGenerics()

        result.itemsTimeWindow = 365
        result.recentAnnouncements = Doc.executeQuery(
                'select d from Doc d where d.type = :type and d.dateCreated >= :tsCheck order by d.dateCreated desc',
                [type: RefdataValue.getByValueAndCategory('Announcement', 'Document Type'), tsCheck: (new Date()).minus(365)]
        )
        result.num_announcements = result.recentAnnouncements.size()

        result
    }

    @Secured(['ROLE_YODA'])
    def changeLog() {
        def result = setResultGenerics()

        def exporting = ( params.format == 'csv' ? true : false )

        result.institutional_objects = []

        if ( exporting ) {
          result.max = 1000000;
          result.offset = 0;
        }
        else {
          result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
          result.offset = params.offset ? Integer.parseInt(params.offset) : 0;
        }

        PendingChange.executeQuery('select distinct(pc.license) from PendingChange as pc where pc.owner = ?',[result.institution]).each {
          result.institutional_objects.add(['com.k_int.kbplus.License:'+it.id,"${message(code:'license')}: "+it.reference]);
        }
        PendingChange.executeQuery('select distinct(pc.subscription) from PendingChange as pc where pc.owner = ?',[result.institution]).each {
          result.institutional_objects.add(['com.k_int.kbplus.Subscription:'+it.id,"${message(code:'subscription')}: "+it.name]);
        }

        if ( params.restrict == 'ALL' )
          params.restrict=null

        def base_query = " from PendingChange as pc where owner = ?";
        def qry_params = [result.institution]
        if ( ( params.restrict != null ) && ( params.restrict.trim().length() > 0 ) ) {
          def o =  genericOIDService.resolveOID(params.restrict)
          if ( o != null ) {
            if ( o instanceof License ) {
              base_query += ' and license = ?'
            }
            else {
              base_query += ' and subscription = ?'
            }
            qry_params.add(o)
          }
        }

        result.num_changes = PendingChange.executeQuery("select pc.id "+base_query, qry_params).size()


        withFormat {
            html {
            result.changes = PendingChange.executeQuery("select pc "+base_query+"  order by ts desc", qry_params, [max: result.max, offset:result.offset])
                result
            }
            csv {
                def dateFormat = new DateUtil().getSimpleDateFormat_NoTime()
                def changes = PendingChange.executeQuery("select pc "+base_query+"  order by ts desc", qry_params)
                response.setHeader("Content-disposition", "attachment; filename=\"${escapeService.escapeString(result.institution.name)}_changes.csv\"")
                response.contentType = "text/csv"

                def out = response.outputStream
                out.withWriter { w ->
                  w.write('Date,ChangeId,Actor, SubscriptionId,LicenseId,Description\n')
                  changes.each { c ->
                    def line = "\"${dateFormat.format(c.ts)}\",\"${c.id}\",\"${c.user?.displayName?:''}\",\"${c.subscription?.id ?:''}\",\"${c.license?.id?:''}\",\"${c.desc}\"\n".toString()
                    w.write(line)
                  }
                }
                out.close()
            }

        }
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_EDITOR", specRole="ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def financeImport() {
        def result = setResultGenerics()
        result.mappingCols = ["title","element","elementSign","referenceCodes","budgetCode","status","invoiceTotal",
                              "currency","exchangeRate","taxType","taxRate","value","subscription","package",
                              "issueEntitlement","datePaid","financialYear","dateFrom","dateTo","invoiceDate",
                              "description","invoiceNumber","orderNumber","institution"]
        result
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_EDITOR", specRole="ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processFinanceImport() {
        def result = setResultGenerics()
        CommonsMultipartFile tsvFile = params.tsvFile
        if(tsvFile && tsvFile.size > 0) {
            String encoding = UniversalDetector.detectCharset(tsvFile.getInputStream())
            if(encoding == "UTF-8") {
                result.filename = tsvFile.originalFilename
                Map<String,Map> financialData = financeService.financeImport(tsvFile)
                result.candidates = financialData.candidates
                result.budgetCodes = financialData.budgetCodes
                result.criticalErrors = ['ownerMismatchError','noValidSubscription','multipleSubError','packageWithoutSubscription','noValidPackage','multipleSubPkgError',
                                         'packageNotInSubscription','entitlementWithoutPackageOrSubscription','noValidTitle','multipleTitleError','noValidEntitlement','multipleEntitlementError',
                                         'entitlementNotInSubscriptionPackage','multipleOrderError','multipleInvoiceError','invalidCurrencyError','invoiceTotalInvalid','valueInvalid','exchangeRateInvalid',
                                         'invalidTaxType','invalidYearFormat','noValidStatus','noValidElement','noValidSign']
                render view: 'postProcessingFinanceImport', model: result
            }
            else {
                flash.error = message(code:'default.import.error.wrongCharset',args:[encoding])
                redirect(url: request.getHeader('referer'))
            }
        }
        else {
            flash.error = message(code:'default.import.error.noFileProvided')
            redirect(url: request.getHeader('referer'))
        }
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_EDITOR", specRole="ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def subscriptionImport() {
        def result = setResultGenerics()
        result.mappingCols = ["name","owner","status","type","form","resource","startDate","endDate","instanceOf",
                              "manualCancellationDate","member","customProperties","privateProperties"]
        result
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_EDITOR", specRole="ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processSubscriptionImport() {
        def result = setResultGenerics()
        CommonsMultipartFile tsvFile = params.tsvFile
        if(tsvFile && tsvFile.size > 0) {
            String encoding = UniversalDetector.detectCharset(tsvFile.getInputStream())
            if(encoding == "UTF-8") {
                result.filename = tsvFile.originalFilename
                Map subscriptionData = subscriptionService.subscriptionImport(tsvFile)
                if(subscriptionData.globalErrors) {
                    flash.error = "<h3>${message([code:'myinst.subscriptionImport.post.globalErrors.header'])}</h3><p>${subscriptionData.globalErrors.join('</p><p>')}</p>"
                    redirect(action: 'subscriptionImport')
                }
                result.candidates = subscriptionData.candidates
                result.parentSubType = subscriptionData.parentSubType
                result.criticalErrors = ['multipleOrgsError','noValidOrg','noValidSubscription']
                render view: 'postProcessingSubscriptionImport', model: result
            }
            else {
                flash.error = message(code:'default.import.error.wrongCharset',args:[encoding])
                redirect(url: request.getHeader('referer'))
            }
        }
        else {
            flash.error = message(code:'default.import.error.noFileProvided')
            redirect(url: request.getHeader('referer'))
        }
    }

    @DebugAnnotation(perm="ORG_BASIC_MEMBER", affil="INST_EDITOR", specRole="ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_BASIC_MEMBER", "INST_EDITOR", "ROLE_ADMIN")
    })
    def currentSurveys() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;


        DateFormat sdFormat = new DateUtil().getSimpleDateFormat_NoTime()
        def fsq = filterService.getParticipantSurveyQuery(params, sdFormat, result.institution)

        result.surveys  = SurveyInfo.findAllByIdInList(SurveyResult.findAll(fsq.query, fsq.queryParams).surveyConfig.surveyInfo.id, result)
        result.countSurvey = SurveyInfo.findAllByIdInList(SurveyResult.findAll(fsq.query, fsq.queryParams).surveyConfig.surveyInfo.id).size()

        result
    }

    @DebugAnnotation(perm="ORG_BASIC_MEMBER", affil="INST_EDITOR", specRole="ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_BASIC_MEMBER", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyInfos() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        result.surveyInfo = SurveyInfo.get(params.id) ?: null

        def surveyResults = SurveyResult.findAllByParticipantAndSurveyConfigInList(result.institution, result.surveyInfo.surveyConfigs).sort { it?.surveyConfig?.configOrder }
        result.editable = surveyService.isEditableSurvey(result.institution, result.surveyInfo)

        result.surveyResults = surveyResults.groupBy {it?.surveyConfig?.id}

        def tmpList = SurveyResult.findAllByParticipantAndSurveyConfigInList(result.institution, result.surveyInfo.surveyConfigs)
        if (tmpList) {
            result.ownerId = tmpList[0].owner?.id
        }

        if ( params.exportXLS ) {
            def sdf = new SimpleDateFormat(g.message(code: 'default.date.format.notimenopoint'));
            String datetoday = sdf.format(new Date(System.currentTimeMillis()))
            String filename = "${datetoday}_" + g.message(code: "survey.label")
            //if(wb instanceof XSSFWorkbook) file += "x";
            response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
            response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            SXSSFWorkbook wb = (SXSSFWorkbook) exportSurveyInfo(surveyResults, "xls", result.institution)
            wb.write(response.outputStream)
            response.outputStream.flush()
            response.outputStream.close()
            wb.dispose()

            return
        }else {
            withFormat {
                html {
                    result
                }
            }
        }

    }

    @DebugAnnotation(perm="ORG_BASIC_MEMBER", affil="INST_EDITOR", specRole="ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_BASIC_MEMBER", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyConfigsInfo() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        result.surveyInfo = SurveyInfo.get(params.id) ?: null

        result.surveyConfig = SurveyConfig.get(params.surveyConfigID)

        result.subscriptionInstance = result.surveyConfig?.subscription?.getDerivedSubscriptionBySubscribers(result.institution)

        result.surveyResults = SurveyResult.findAllByParticipantAndSurveyConfig(result.institution, result.surveyConfig).sort { it?.surveyConfig?.configOrder }

        result.ownerId = result.surveyResults[0]?.owner?.id

        result.navigation = surveyService.getParticipantConfigNavigation(result.institution, result.surveyInfo, result.surveyConfig)

        if(result.surveyConfig?.type == 'Subscription') {
            result.authorizedOrgs = result.user?.authorizedOrgs
            result.contextOrg = contextService.getOrg()
            // restrict visible for templates/links/orgLinksAsList
            result.visibleOrgRelations = []
            result.subscriptionInstance?.orgRelations?.each { or ->
                if (!(or.org?.id == contextService.getOrg()?.id) && !(or.roleType.value in ['Subscriber', 'Subscriber_Consortial'])) {
                    result.visibleOrgRelations << or
                }
            }
            result.visibleOrgRelations.sort { it.org.sortname }
        }

        result.editable = surveyService.isEditableSurvey(result.institution, result.surveyInfo)
        result.consCostTransfer = true

        result
    }

    @DebugAnnotation(perm="ORG_BASIC_MEMBER", affil="INST_EDITOR", specRole="ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_BASIC_MEMBER", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyFinishConfig() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        def surveyResults = SurveyResult.findAllByParticipantAndSurveyConfig(result.institution, SurveyConfig.get(params.surveyConfigID))

        def allResultHaveValue = true
        surveyResults.each {
            def value = null
            if (it?.type?.type == Integer.toString()) {
                value = it.intValue.toString()
            } else if (it?.type?.type == String.toString()) {
                value = it.stringValue ?: ''
            } else if (it?.type?.type == BigDecimal.toString()) {
                value = it.decValue.toString()
            } else if (it?.type?.type == Date.toString()) {
                value = it.dateValue.toString()
            } else if (it?.type?.type == RefdataValue.toString()) {
                value = it.refValue?.getI10n('value') ?: ''
            }

            if(value == null || value == "")
            {
                allResultHaveValue = false
            }

        }
        if(allResultHaveValue) {
            surveyResults.each {
                it.finishDate = new Date()
                it.save(flush: true)
            }
            flash.message = message(code: "surveyResult.finish.info")
        }else {
            flash.error = message(code: "surveyResult.finish.info")
        }

        redirect(url: request.getHeader('referer'))
    }

    @DebugAnnotation(perm="ORG_BASIC_MEMBER", affil="INST_EDITOR", specRole="ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_BASIC_MEMBER", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyInfoFinish() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        List<SurveyResult> surveyResults = SurveyResult.findAllByParticipantAndSurveyConfigInList(result.institution, SurveyInfo.get(params.id).surveyConfigs)

        boolean allResultHaveValue = true
        surveyResults.each { surre ->
            SurveyOrg surorg = SurveyOrg.findBySurveyConfigAndOrg(surre.surveyConfig,result.institution)
            if(!surre.getFinish() && !surorg.checkPerennialTerm())
                allResultHaveValue = false
        }
        if(allResultHaveValue) {
            surveyResults.each {
                it.finishDate = new Date()
                it.save()
            }
            // flash.message = message(code: "surveyResult.finish.info")
        }else {
            flash.error = message(code: "surveyResult.finish.error")
        }

        redirect(url: request.getHeader('referer'))
    }


    @DebugAnnotation(perm="ORG_BASIC_MEMBER", affil="INST_EDITOR", specRole="ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_BASIC_MEMBER", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyResultFinish() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        result.surveyInfo = SurveyInfo.get(params.id) ?: null

        result.surveyResults = SurveyResult.findAllByParticipantAndSurveyConfigInList(result.institution, result.surveyInfo.surveyConfigs).sort { it?.surveyConfig?.configOrder }

        result.surveyResults.each{

           if(it.participant == result.institution) {
               it.finishDate = new Date(System.currentTimeMillis())
               it.save(flush: true)

               flash.message = g.message(code: "default.notAutorized.message")
           }
        }


        redirect action: 'surveyResult', id: result.surveyInfo.id
    }


    @DebugAnnotation(test = 'hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def tip() {
      def result = setResultGenerics()

      log.debug("tip :: ${params}")
      result.tip = TitleInstitutionProvider.get(params.id)

      if (request.method == 'POST' && result.tip ){
        log.debug("Add usage ${params}")
        def sdf = new DateUtil().getSimpleDateFormat_NoTime()
        def usageDate = sdf.parse(params.usageDate);
        def cal = new GregorianCalendar()
        cal.setTime(usageDate)
        def fact = new Fact(
          relatedTitle:result.tip.title,
          supplier:result.tip.provider,
          inst:result.tip.institution,
          juspio:result.tip.title.getIdentifierValue('jusp'),
          factFrom:usageDate,
          factTo:usageDate,
          factValue:params.usageValue,
          factUid:java.util.UUID.randomUUID().toString(),
          reportingYear:cal.get(Calendar.YEAR),
          reportingMonth:cal.get(Calendar.MONTH),
          factType:RefdataValue.get(params.factType)
        ).save(flush:true, failOnError:true);

      }

      if ( result.tip ) {
        result.usage = Fact.findAllByRelatedTitleAndSupplierAndInst(result.tip.title,result.tip.provider,result.tip.institution)
      }
      result
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_USER")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliation("ORG_INST,ORG_CONSORTIUM", "INST_USER")
    })
    def addressbook() {

        def result = setResultGenerics()

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP() as Integer
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0

        List visiblePersons = addressbookService.getVisiblePersons("addressbook",params)

        result.editable = accessService.checkMinUserOrgRole(result.user, contextService.getOrg(), 'INST_EDITOR') || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')

        result.propList =
                PropertyDefinition.findAllWhere(
                        descr: PropertyDefinition.PRS_PROP,
                        tenant: contextService.getOrg() // private properties
                )

        result.num_visiblePersons = visiblePersons.size()
        result.visiblePersons = visiblePersons.drop(result.offset).take(result.max)

        result
      }

    @DebugAnnotation(test = 'hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def myPublicContacts() {

        def result = setResultGenerics()

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP() as Integer
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0

        params.org = result.institution
        List visiblePersons = addressbookService.getVisiblePersons("myPublicContacts",params)

        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR') || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')

        result.propList =
                PropertyDefinition.findAllWhere(
                        descr: PropertyDefinition.PRS_PROP,
                        tenant: result.institution // private properties
                )

        result.num_visiblePersons = visiblePersons.size()
        result.visiblePersons = visiblePersons.drop(result.offset).take(result.max)

        result
      }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_EDITOR", specRole="ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def budgetCodes() {
        def result = setResultGenerics()

        result.editable = accessService.checkMinUserOrgRole(result.user, contextService.getOrg(), 'INST_EDITOR') || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')

        if (result.editable) {

            flash.message = null
            flash.error = null

            if (params.cmd == "newBudgetCode") {
                if (params.bc) {
                    def bc = new BudgetCode(
                            owner: result.institution,
                            value: params.bc,
                            descr: params.descr
                    )
                    if (bc.save()) {
                        flash.message = "Neuer Budgetcode wurde angelegt."
                    }
                    else {
                        flash.error = "Der neue Budgetcode konnte nicht angelegt werden."
                    }

                }
            } else if (params.cmd == "deleteBudgetCode") {
                def bc = genericOIDService.resolveOID(params.bc)
                if (bc && bc.owner.id == result.institution.id) {
                    bc.delete()
                }
            }

        }
        result.budgetCodes = BudgetCode.findAllByOwner(result.institution, [sort: 'value'])

        if (params.redirect) {
            redirect(url: request.getHeader('referer'), params: params)
        }

        result
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_USER")
    @Secured(closure = { ctx.accessService.checkPermAffiliation("ORG_INST,ORG_CONSORTIUM", "INST_USER") })
    def tasks() {
        def result = setResultGenerics()

        if (params.deleteId) {
            def dTask = Task.get(params.deleteId)
            if (dTask && (dTask.creator.id == result.user.id || contextService.getUser().hasAffiliation("INST_ADM"))) {
                try {
                    dTask.delete(flush: true)
                    flash.message = message(code: 'default.deleted.message', args: [message(code: 'task.label', default: 'Task'), params.deleteId])
                }
                catch (Exception e) {
                    flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'task.label', default: 'Task'), params.deleteId])
                }
            } else {
                flash.message = message(code: 'default.not.deleted.notAutorized.message', args: [message(code: 'task.label', default: 'Task'), params.deleteId])
            }
        }

        DateFormat sdFormat = new DateUtil().getSimpleDateFormat_NoTime()
        def query = filterService.getTaskQuery(params, sdFormat)
        int offset = params.offset ? Integer.parseInt(params.offset) : 0
        result.taskInstanceList = taskService.getTasksByResponsibles(result.user, result.institution, query)
        result.taskInstanceCount = result.taskInstanceList.size()
        //chop everything off beyond the user's pagination limit
        if (result.taskInstanceCount > result.user.getDefaultPageSizeTMP()) {
            try {
                result.taskInstanceList = result.taskInstanceList.subList(offset, offset + Math.toIntExact(result.user.getDefaultPageSizeTMP()))
            }
            catch (IndexOutOfBoundsException e) {
                result.taskInstanceList = result.taskInstanceList.subList(offset, result.taskInstanceCount)
            }
        }
        result.myTaskInstanceList = taskService.getTasksByCreator(result.user, null)
        result.myTaskInstanceCount = result.myTaskInstanceList.size()
        //chop everything off beyond the user's pagination limit
        if (result.myTaskInstanceCount > result.user.getDefaultPageSizeTMP()) {
            try {
                result.myTaskInstanceList = result.myTaskInstanceList.subList(offset, offset + Math.toIntExact(result.user.getDefaultPageSizeTMP()))
            }
            catch (IndexOutOfBoundsException e) {
                result.myTaskInstanceList = result.myTaskInstanceList.subList(offset, result.myTaskInstanceCount)
            }
        }
        result.editable = accessService.checkMinUserOrgRole(result.user, contextService.getOrg(), 'INST_EDITOR') || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')

        def preCon = taskService.getPreconditions(contextService.getOrg())
        result << preCon

        log.debug(result.taskInstanceList)
        result
    }

    @DebugAnnotation(perm="ORG_INST_COLLECTIVE, ORG_CONSORTIUM", affil="INST_ADM",specRole="ROLE_ADMIN, ROLE_ORG_EDITOR")
    @Secured(closure = { ctx.accessService.checkPermAffiliationX("ORG_INST_COLLECTIVE, ORG_CONSORTIUM","INST_ADM","ROLE_ADMIN, ROLE_ORG_EDITOR") })
    def addMembers() {
        def result = setResultGenerics()

        // new: filter preset
        if(accessService.checkPerm('ORG_CONSORTIUM')) {
            result.comboType = 'Consortium'
            params.orgType   = RDStore.OT_INSTITUTION.id?.toString()
        }
        else if(accessService.checkPerm('ORG_INST_COLLECTIVE')) {
            result.comboType = 'Department'
            params.orgType   = RDStore.OT_DEPARTMENT.id?.toString()
        }
        params.orgSector = RDStore.O_SECTOR_HIGHER_EDU.id?.toString()

        if (params.selectedOrgs) {
            log.debug('adding orgs to consortia/institution')

            params.list('selectedOrgs').each { soId ->
                Map map = [
                        toOrg: result.institution,
                        fromOrg: Org.findById( Long.parseLong(soId)),
                        type: RefdataValue.getByValueAndCategory(result.comboType,'Combo Type')
                ]
                if (! Combo.findWhere(map)) {
                    def cmb = new Combo(map)
                    cmb.save()
                }
            }

            redirect action: 'manageMembers'
        }
        result.filterSet = params.filterSet ? true : false
        def fsq = filterService.getOrgQuery(params)
        result.availableOrgs = Org.executeQuery(fsq.query, fsq.queryParams, params)

            result.memberIds = []
            Combo.findAllWhere(
                    toOrg: result.institution,
                    type:    RefdataValue.getByValueAndCategory(result.comboType,'Combo Type')
            ).each { cmb ->
                result.memberIds << cmb.fromOrg.id
            }

        SimpleDateFormat sdf = new SimpleDateFormat(message(code:'default.date.format.notimenopoint'))

        def tableHeader
        if(result.comboType == 'Consortium')
            tableHeader = message(code: 'menu.public.all_orgs')
        else if(result.comboType == 'Department')
            tableHeader = message(code: 'menu.my.departments')
        String filename = tableHeader+"_"+sdf.format(new Date(System.currentTimeMillis()))
        if ( params.exportXLS ) {
            List orgs = (List) result.availableOrgs
            SXSSFWorkbook workbook = (SXSSFWorkbook) organisationService.exportOrg(orgs, tableHeader, true,'xls')

            response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
            response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            workbook.write(response.outputStream)
            response.outputStream.flush()
            response.outputStream.close()
            workbook.dispose()
        }
        else {
            withFormat {
                html {
                    result
                }
                csv {
                    response.setHeader("Content-disposition", "attachment; filename=\"${filename}.csv\"")
                    response.contentType = "text/csv"
                    ServletOutputStream out = response.outputStream
                    out.withWriter { writer ->
                        writer.write((String) organisationService.exportOrg(orgListTotal,tableHeader,true,"csv"))
                    }
                    out.close()
                }
            }
        }
    }

    @DebugAnnotation(perm="ORG_INST_COLLECTIVE,ORG_CONSORTIUM", affil="INST_USER", specRole="ROLE_ADMIN,ROLE_ORG_EDITOR")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_INST_COLLECTIVE,ORG_CONSORTIUM","INST_USER","ROLE_ADMIN,ROLE_ORG_EDITOR")
    })
    def manageMembers() {
        def result = setResultGenerics()

        // new: filter preset
        if(accessService.checkPerm('ORG_CONSORTIUM')) {
            params.orgType  = RDStore.OT_INSTITUTION?.id?.toString()
            result.comboType = RDStore.COMBO_TYPE_CONSORTIUM
            if (params.selectedOrgs) {
                log.debug('remove orgs from consortia')

                params.list('selectedOrgs').each { soId ->
                    def cmb = Combo.findWhere(
                            toOrg: result.institution,
                            fromOrg: Org.get(Long.parseLong(soId)),
                            type: RDStore.COMBO_TYPE_CONSORTIUM
                    )
                    cmb.delete()
                }
            }
        }
        else if(accessService.checkPerm('ORG_INST_COLLECTIVE')) {
            params.orgType  = RDStore.OT_DEPARTMENT?.id?.toString()
            result.comboType = RDStore.COMBO_TYPE_DEPARTMENT
            if (params.selectedOrgs) {
                log.debug('remove orgs from department')
                params.list('selectedOrgs').each { soId ->
                    Org department = Org.get(soId)
                    if(!organisationService.removeDepartment(department)) {
                        flash.error(message(code:'default.not.deleted.message',args:[message(code:'org.department.label'),department.name]))
                        redirect(url: request.getHeader('referer'))
                    }
                }
            }
        }
        //params.orgSector    = RDStore.O_SECTOR_HIGHER_EDU?.id?.toString()

        result.max          = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP()
        result.offset       = params.offset ? Integer.parseInt(params.offset) : 0
        result.propList     = PropertyDefinition.findAllPublicAndPrivateOrgProp(contextService.org)
        result.filterSet    = params.filterSet ? true : false

        params.comboType = result.comboType.value
        def fsq = filterService.getOrgComboQuery(params, result.institution)
        def tmpQuery = "select o.id " + fsq.query.minus("select o ")
        def memberIds = Org.executeQuery(tmpQuery, fsq.queryParams)

        if (params.filterPropDef && memberIds) {
            fsq                      = propertyService.evalFilterQuery(params, "select o FROM Org o WHERE o.id IN (:oids) order by o.sortname asc", 'o', [oids: memberIds])
        }

        List totalMembers      = Org.executeQuery(fsq.query, fsq.queryParams)
        result.totalMembers    = totalMembers.clone()
        result.membersCount    = totalMembers.size()
        result.members         = totalMembers.drop((int) result.offset).take((int) result.max)
        String header
        String exportHeader
        if(result.comboType == RDStore.COMBO_TYPE_CONSORTIUM) {
            header = message(code: 'menu.my.consortia')
            exportHeader = message(code: 'export.my.consortia')
        }
        else if(result.comboType == RDStore.COMBO_TYPE_DEPARTMENT) {
            header = g.message(code: 'menu.my.departments')
            exportHeader = message(code: 'export.my.departments')
        }
        SimpleDateFormat sdf = new SimpleDateFormat(message(code:'default.date.format.notimenopoint'))
        // Write the output to a file
        String file = "${sdf.format(new Date(System.currentTimeMillis()))}_"+exportHeader
        if ( params.exportXLS ) {

            SXSSFWorkbook wb = (SXSSFWorkbook) organisationService.exportOrg(totalMembers, header, true, 'xls')
            response.setHeader "Content-disposition", "attachment; filename=\"${file}.xlsx\""
            response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            wb.write(response.outputStream)
            response.outputStream.flush()
            response.outputStream.close()
            wb.dispose()
        }
        else {
            withFormat {
                html {
                    result
                }
                csv {
                    response.setHeader("Content-disposition", "attachment; filename=\"${file}.csv\"")
                    response.contentType = "text/csv"
                    ServletOutputStream out = response.outputStream
                    out.withWriter { writer ->
                        writer.write((String) organisationService.exportOrg(totalMembers,header,true,"csv"))
                    }
                    out.close()
                }
            }
        }
    }

    @DebugAnnotation(perm="ORG_INST_COLLECTIVE", affil="INST_ADM", specRole="ROLE_ADMIN")
    @Secured(closure = { ctx.accessService.checkPermAffiliationX("ORG_INST_COLLECTIVE", "INST_ADM", "ROLE_ADMIN") })
    def removeDepartment() {
        Org department = Org.get(params.dept)
        if(organisationService.removeDepartment(department))
            redirect action: 'manageMembers'
        else {
            flash.error(message(code:'default.not.deleted.message',args:[message(code:'org.department.label'),department.name]))
            redirect(url: request.getHeader('referer'))
        }
    }

    @DebugAnnotation(perm="ORG_CONSORTIUM", affil="INST_EDITOR", specRole="ROLE_ADMIN")
    @Secured(closure = { ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN") })
    def manageConsortiaSubscriptions() {
        def result = setResultGenerics()

        DebugUtil du = new DebugUtil()
        du.setBenchMark('filterService')

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP()
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0

        Map fsq = filterService.getOrgComboQuery([comboType:RDStore.COMBO_TYPE_CONSORTIUM.value,sort: 'o.sortname'], contextService.getOrg())
        result.filterConsortiaMembers = Org.executeQuery(fsq.query, fsq.queryParams)

        du.setBenchMark('filterSubTypes & filterPropList')

        if(params.filterSet)
            result.filterSet = params.filterSet

        result.filterSubTypes = RefdataCategory.getAllRefdataValues('Subscription Type').minus(
                RefdataValue.getByValueAndCategory('Local Licence', 'Subscription Type')
        )
        result.filterPropList =
                PropertyDefinition.findAllWhere(
                        descr: PropertyDefinition.SUB_PROP,
                        tenant: null // public properties
                ) +
                        PropertyDefinition.findAllWhere(
                                descr: PropertyDefinition.SUB_PROP,
                                tenant: contextService.getOrg() // private properties
                        )

        /*
        String query = "select ci, subT, roleT.org from CostItem ci join ci.owner orgK join ci.sub subT join subT.instanceOf subK " +
                "join subK.orgRelations roleK join subT.orgRelations roleTK join subT.orgRelations roleT " +
                "where orgK = :org and orgK = roleK.org and roleK.roleType = :rdvCons " +
                "and orgK = roleTK.org and roleTK.roleType = :rdvCons " +
                "and roleT.roleType = :rdvSubscr "
        */

        // CostItem ci

        du.setBenchMark('filter query')

        String query = "select ci, subT, roleT.org " +
                " from CostItem ci right outer join ci.sub subT join subT.instanceOf subK " +
                " join subK.orgRelations roleK join subT.orgRelations roleTK join subT.orgRelations roleT " +
                " where roleK.org = :org and roleK.roleType = :rdvCons " +
                " and roleTK.org = :org and roleTK.roleType = :rdvCons " +
                " and roleT.roleType = :rdvSubscr " +
                " and ( ci is null or ci.owner = :org )"


        Map qarams = [org      : result.institution,
                      rdvCons  : RDStore.OR_SUBSCRIPTION_CONSORTIA,
                      rdvSubscr: RDStore.OR_SUBSCRIBER_CONS]

        if (params.member?.size() > 0) {
            query += " and roleT.org.id = :member "
            qarams.put('member', params.long('member'))
        }

        if (params.validOn?.size() > 0) {
            result.validOn = params.validOn

            query += " and ( "
            query += "( ci.startDate <= :validOn OR (ci.startDate is null AND (subT.startDate <= :validOn OR subT.startDate is null) ) ) and "
            query += "( ci.endDate >= :validOn OR (ci.endDate is null AND (subT.endDate >= :validOn OR subT.endDate is null) ) ) "
            query += ") "

            DateFormat sdf = new DateUtil().getSimpleDateFormat_NoTime()
            qarams.put('validOn', new Timestamp(sdf.parse(params.validOn).getTime()))
        }

        if (params.status?.size() > 0) {
            query += " and subT.status.id = :status "
            qarams.put('status', params.long('status'))
        } else if(!params.filterSet) {
            query += " and subT.status.id = :status "
            qarams.put('status', RDStore.SUBSCRIPTION_CURRENT.id)
            params.status = RDStore.SUBSCRIPTION_CURRENT.id
            result.defaultSet = true
        } else {
            query += " and subT.status.id != :deleted "
            qarams.put('deleted', RDStore.SUBSCRIPTION_DELETED.id)
        }

        if (params.filterPropDef?.size() > 0) {
            def psq = propertyService.evalFilterQuery(params, query, 'subT', qarams)
            query = psq.query
            qarams = psq.queryParams
        }

        if (params.form?.size() > 0) {
            query += " and subT.form.id = :form "
            qarams.put('form', params.long('form'))
        }
        if (params.resource?.size() > 0) {
            query += " and subT.resource.id = :resource "
            qarams.put('resource', params.long('resource'))
        }
        if (params.subTypes?.size() > 0) {
            query += " and subT.type.id in (:subTypes) "
            qarams.put('subTypes', params.list('subTypes').collect { it -> Long.parseLong(it) })
        }

        String orderQuery = " order by roleT.org.sortname, subT.name"
        if (params.sort?.size() > 0) {
            orderQuery = " order by " + params.sort + " " + params.order
        }

        if(params.filterSet && !params.member && !params.validOn && !params.status && !params.filterPropDef && !params.filterProp && !params.form && !params.resource && !params.subTypes)
            result.filterSet = false

        log.debug( query + " " + orderQuery )
        // log.debug( qarams )

        du.setBenchMark('costs')

        List costs = CostItem.executeQuery(
                query + " " + orderQuery, qarams
        )
        result.countCostItems = costs.size()
        if(params.exportXLS || params.format)
            result.costItems = costs
        else result.costItems = costs.drop((int) result.offset).take((int) result.max)

        result.finances = {
            Map entries = [:]
            result.costItems.each { obj ->
                if (obj[0]) {
                    CostItem ci = obj[0]
                    if (!entries."${ci.billingCurrency}") {
                        entries."${ci.billingCurrency}" = 0.0
                    }

                    if (ci.costItemElementConfiguration == RDStore.CIEC_POSITIVE) {
                        entries."${ci.billingCurrency}" += ci.costInBillingCurrencyAfterTax
                    }
                    else if (ci.costItemElementConfiguration == RDStore.CIEC_NEGATIVE) {
                        entries."${ci.billingCurrency}" -= ci.costInBillingCurrencyAfterTax
                    }
                }
            }
            entries
        }()

        List bm = du.stopBenchMark()
        result.benchMark = bm

        BidiMap subLinks = new DualHashBidiMap()
        Links.findAllByLinkTypeAndObjectType(RDStore.LINKTYPE_FOLLOWS,Subscription.class.name).each { link ->
            subLinks.put(link.source,link.destination)
        }
        LinkedHashMap<Subscription,List<Org>> providers = [:]
        OrgRole.findAllByRoleType(RDStore.OR_PROVIDER).each { it ->
            List<Org> orgs = providers.get(it.sub)
            if(orgs == null)
                orgs = [it.org]
            else orgs.add(it.org)
            providers.put(it.sub,orgs)
        }
        SimpleDateFormat sdf = new SimpleDateFormat(message(code:'default.date.format.notime'))
        if(params.exportXLS) {
            XSSFWorkbook wb = new XSSFWorkbook()
            POIXMLProperties xmlProps = wb.getProperties()
            POIXMLProperties.CoreProperties coreProps = xmlProps.getCoreProperties()
            coreProps.setCreator(message(code:'laser'))
            XSSFCellStyle lineBreaks = wb.createCellStyle()
            lineBreaks.setWrapText(true)
            XSSFCellStyle csPositive = wb.createCellStyle()
            csPositive.setFillForegroundColor(new XSSFColor(new java.awt.Color(198,239,206)))
            csPositive.setFillPattern(FillPatternType.SOLID_FOREGROUND)
            XSSFCellStyle csNegative = wb.createCellStyle()
            csNegative.setFillForegroundColor(new XSSFColor(new java.awt.Color(255,199,206)))
            csNegative.setFillPattern(FillPatternType.SOLID_FOREGROUND)
            XSSFCellStyle csNeutral = wb.createCellStyle()
            csNeutral.setFillForegroundColor(new XSSFColor(new java.awt.Color(255,235,156)))
            csNeutral.setFillPattern(FillPatternType.SOLID_FOREGROUND)
            SXSSFWorkbook workbook = new SXSSFWorkbook(wb,50)
            workbook.setCompressTempFiles(true)
            SXSSFSheet sheet = workbook.createSheet(message(code:'menu.my.consortiaSubscriptions'))
            sheet.flushRows(10)
            sheet.setAutobreaks(true)
            Row headerRow = sheet.createRow(0)
            headerRow.setHeightInPoints(16.75f)
            List titles = [message(code:'sidewide.number'),message(code:'myinst.consortiaSubscriptions.member'),message(code:'myinst.consortiaSubscriptions.subscription'),message(code:'myinst.consortiaSubscriptions.license'),
                           message(code:'myinst.consortiaSubscriptions.packages'),message(code:'myinst.consortiaSubscriptions.provider'),message(code:'myinst.consortiaSubscriptions.runningTimes'),
                           message(code:'financials.amountFinal'),"${message(code:'financials.isVisibleForSubscriber')} / ${message(code:'financials.costItemConfiguration')}"]
            titles.eachWithIndex{ titleName, int i ->
                Cell cell = headerRow.createCell(i)
                cell.setCellValue(titleName)
            }
            sheet.createFreezePane(0,1)
            Row row
            Cell cell
            int rownum = 1
            int sumcell = 7
            int sumTitleCell = 6
            result.costItems.eachWithIndex { entry, int sidewideNumber ->
                log.debug("processing entry ${sidewideNumber} ...")
                CostItem ci = (CostItem) entry[0] ?: new CostItem()
                Subscription subCons = (Subscription) entry[1]
                Org subscr = (Org) entry[2]
                int cellnum = 0
                row = sheet.createRow(rownum)
                //sidewide number
                log.debug("insert sidewide number")
                cell = row.createCell(cellnum++)
                cell.setCellValue(rownum)
                //sortname
                log.debug("insert sortname")
                cell = row.createCell(cellnum++)
                String subscrName = ""
                if(subscr.sortname) subscrName += subscr.sortname
                subscrName += "(${subscr.name})"
                cell.setCellValue(subscrName)
                //subscription name
                log.debug("insert subscription name")
                cell = row.createCell(cellnum++)
                String subscriptionString = subCons.name
                //if(subCons.getCalculatedPrevious()) //avoid! Makes 5846 queries!!!!!
                if(subLinks.getKey(subCons.id))
                    subscriptionString += " (${message(code:'subscription.hasPreviousSubscription')})"
                cell.setCellValue(subscriptionString)
                //license name
                log.debug("insert license name")
                cell = row.createCell(cellnum++)
                if(subCons.owner)
                    cell.setCellValue(subCons.owner.reference)
                //packages
                log.debug("insert package name")
                cell = row.createCell(cellnum++)
                cell.setCellStyle(lineBreaks)
                String packagesString = ""
                subCons.packages.each { subPkg ->
                    packagesString += "${subPkg.pkg.name}\n"
                }
                cell.setCellValue(packagesString)
                //provider
                log.debug("insert provider name")
                cell = row.createCell(cellnum++)
                cell.setCellStyle(lineBreaks)
                String providersString = ""
                providers.get(subCons).each { p ->
                    log.debug("Getting provider ${p}")
                    providersString += "${p.name}\n"
                }
                cell.setCellValue(providersString)
                //running time from / to
                log.debug("insert running times")
                cell = row.createCell(cellnum++)
                String dateString = ""
                if(ci.id) {
                    if(ci.getDerivedStartDate()) dateString += sdf.format(ci.getDerivedStartDate())
                    if(ci.getDerivedEndDate()) dateString += " - ${sdf.format(ci.getDerivedEndDate())}"
                }
                cell.setCellValue(dateString)
                //final sum
                log.debug("insert final sum")
                cell = row.createCell(cellnum++)
                if(ci.id && ci.costItemElementConfiguration) {
                    switch(ci.costItemElementConfiguration) {
                        case RDStore.CIEC_POSITIVE: cell.setCellStyle(csPositive)
                            break
                        case RDStore.CIEC_NEGATIVE: cell.setCellStyle(csNegative)
                            break
                        case RDStore.CIEC_NEUTRAL: cell.setCellStyle(csNeutral)
                            break
                    }
                    cell.setCellValue(formatNumber([number:ci.costInBillingCurrencyAfterTax ?: 0.0,type:'currency',currencySymbol:ci.billingCurrency ?: 'EUR']))
                }
                //cost item sign and visibility
                log.debug("insert cost sign and visiblity")
                cell = row.createCell(cellnum++)
                String costSignAndVisibility = ""
                if(ci.id) {
                    if(ci.isVisibleForSubscriber) {
                        costSignAndVisibility += message(code:'financials.isVisibleForSubscriber')+" / "
                    }
                    if(ci.costItemElementConfiguration) {
                        costSignAndVisibility += ci.costItemElementConfiguration.getI10n("value")
                    }
                    else
                        costSignAndVisibility += message(code:'financials.costItemConfiguration.notSet')
                }
                cell.setCellValue(costSignAndVisibility)
                rownum++
            }
            rownum++
            sheet.createRow(rownum)
            rownum++
            Row sumRow = sheet.createRow(rownum)
            cell = sumRow.createCell(sumTitleCell)
            cell.setCellValue(message(code:'financials.export.sums'))
            rownum++
            result.finances.each { entry ->
                sumRow = sheet.createRow(rownum)
                cell = sumRow.createCell(sumTitleCell)
                cell.setCellValue("${message(code:'financials.sum.billing')} ${entry.key}")
                cell = sumRow.createCell(sumcell)
                cell.setCellValue(formatNumber([number:entry.value,type:'currency',currencySymbol: entry.key]))
                rownum++
            }
            for(int i = 0;i < titles.size();i++) {
                try {
                    sheet.autoSizeColumn(i)
                }
                catch (NullPointerException e) {
                    log.error("Null value in column ${i}")
                }
            }
            String filename = "${g.message(code:'export.my.consortiaSubscriptions')}_${sdf.format(new Date(System.currentTimeMillis()))}.xlsx"
            response.setHeader("Content-disposition","attachment; filename=\"${filename}\"")
            response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            workbook.write(response.outputStream)
            response.outputStream.flush()
            response.outputStream.close()
            workbook.dispose()
        }
        else
            withFormat {
                html {
                    result
                }
                csv {
                    List titles = [message(code:'sidewide.number'),message(code:'myinst.consortiaSubscriptions.member'),message(code:'myinst.consortiaSubscriptions.subscription'),message(code:'myinst.consortiaSubscriptions.license'),
                                   message(code:'myinst.consortiaSubscriptions.packages'),message(code:'myinst.consortiaSubscriptions.provider'),message(code:'myinst.consortiaSubscriptions.runningTimes'),
                                   message(code:'financials.amountFinal'),"${message(code:'financials.isVisibleForSubscriber')} / ${message(code:'financials.costItemConfiguration')}"]
                    List columnData = []
                    List row
                    result.costItems.eachWithIndex { entry, int sidewideNumber ->
                        row = []
                        log.debug("processing entry ${sidewideNumber} ...")
                        CostItem ci = (CostItem) entry[0] ?: new CostItem()
                        Subscription subCons = (Subscription) entry[1]
                        Org subscr = (Org) entry[2]
                        int cellnum = 0
                        //sidewide number
                        log.debug("insert sidewide number")
                        cellnum++
                        row.add(sidewideNumber)
                        //sortname
                        log.debug("insert sortname")
                        cellnum++
                        String subscrName = ""
                        if(subscr.sortname) subscrName += subscr.sortname
                        subscrName += "(${subscr.name})"
                        row.add(subscrName.replaceAll(',',' '))
                        //subscription name
                        log.debug("insert subscription name")
                        cellnum++
                        String subscriptionString = subCons.name
                        //if(subCons.getCalculatedPrevious()) //avoid! Makes 5846 queries!!!!!
                        if(subLinks.getKey(subCons.id))
                            subscriptionString += " (${message(code:'subscription.hasPreviousSubscription')})"
                        row.add(subscriptionString.replaceAll(',',' '))
                        //license name
                        log.debug("insert license name")
                        cellnum++
                        if(subCons.owner)
                            row.add(subCons.owner.reference.replaceAll(',',' '))
                        else row.add(' ')
                        //packages
                        log.debug("insert package name")
                        cellnum++
                        String packagesString = " "
                        subCons.packages.each { subPkg ->
                            packagesString += "${subPkg.pkg.name} "
                        }
                        row.add(packagesString.replaceAll(',',' '))
                        //provider
                        log.debug("insert provider name")
                        cellnum++
                        String providersString = " "
                        providers.get(subCons).each { p ->
                            log.debug("Getting provider ${p}")
                            providersString += "${p.name} "
                        }
                        row.add(providersString.replaceAll(',',' '))
                        //running time from / to
                        log.debug("insert running times")
                        cellnum++
                        String dateString = " "
                        if(ci.id) {
                            if(ci.getDerivedStartDate()) dateString += sdf.format(ci.getDerivedStartDate())
                            if(ci.getDerivedEndDate()) dateString += " - ${sdf.format(ci.getDerivedEndDate())}"
                        }
                        row.add(dateString)
                        //final sum
                        log.debug("insert final sum")
                        cellnum++
                        if(ci.id && ci.costItemElementConfiguration) {
                            row.add("${ci.costInBillingCurrencyAfterTax ?: 0.0} ${ci.billingCurrency ?: 'EUR'}")
                        }
                        else row.add(" ")
                        //cost item sign and visibility
                        log.debug("insert cost sign and visiblity")
                        cellnum++
                        String costSignAndVisibility = " "
                        if(ci.id) {
                            if(ci.isVisibleForSubscriber) {
                                costSignAndVisibility += message(code:'financials.isVisibleForSubscriber')+" / "
                            }
                            if(ci.costItemElementConfiguration) {
                                costSignAndVisibility += ci.costItemElementConfiguration.getI10n("value")
                            }
                            else
                                costSignAndVisibility += message(code:'financials.costItemConfiguration.notSet')
                        }
                        row.add(costSignAndVisibility)
                        columnData.add(row)
                    }
                    columnData.add([])
                    columnData.add([])
                    row = []
                    //sumcell = 7
                    //sumTitleCell = 6
                    for(int h = 0;h < 6;h++) {
                        row.add(" ")
                    }
                    row.add(message(code:'financials.export.sums'))
                    columnData.add(row)
                    columnData.add([])
                    result.finances.each { entry ->
                        row = []
                        for(int h = 0;h < 6;h++) {
                            row.add(" ")
                        }
                        row.add("${message(code:'financials.sum.billing')} ${entry.key}")
                        row.add("${entry.value} ${entry.key}")
                        columnData.add(row)
                    }
                    String filename = "${g.message(code:'export.my.consortiaSubscriptions')}_${sdf.format(new Date(System.currentTimeMillis()))}.csv"
                    response.setHeader("Content-disposition","attachment; filename=\"${filename}\"")
                    response.contentType = "text/csv"
                    response.outputStream.withWriter { writer ->
                        writer.write(exportService.generateSeparatorTableString(titles,columnData,','))
                    }
                    response.outputStream.flush()
                    response.outputStream.close()
                }
            }
    }

    @DebugAnnotation(perm="ORG_CONSORTIUM", affil="INST_EDITOR", specRole="ROLE_ADMIN")
    @Secured(closure = { ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN") })
    def manageConsortiaSurveys() {
        def result = setResultGenerics()

        DebugUtil du = new DebugUtil()
        du.setBenchMark('filterService')

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP()
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0

        DateFormat sdFormat = new DateUtil().getSimpleDateFormat_NoTime()

        result.participant = Org.get(Long.parseLong(params.participant))

        //For Filter
        params.participant = params.participant ? Org.get(Long.parseLong(params.participant)) : null

        def fsq = filterService.getSurveyQueryConsortia(params, sdFormat, result.institution)

        result.surveys = SurveyInfo.findAll(fsq.query, fsq.queryParams, params)
        result.countSurvey = SurveyInfo.executeQuery("select si.id ${fsq.query}", fsq.queryParams).size()

        result

    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_EDITOR")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliation("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR")
    })
    def managePropertyGroups() {
        def result = setResultGenerics()
        result.editable = true // true, because action is protected

        if (params.cmd == 'new') {
            result.formUrl = g.createLink([controller: 'myInstitution', action: 'managePropertyGroups'])

            render template: '/templates/properties/propertyGroupModal', model: result
            return
        }
        else if (params.cmd == 'edit') {
            result.pdGroup = genericOIDService.resolveOID(params.oid)
            result.formUrl = g.createLink([controller: 'myInstitution', action: 'managePropertyGroups'])

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
                            tenant: result.institution,
                            ownerType: ownerType,
                            visible: true
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

        result.propDefGroups = PropertyDefinitionGroup.findAllByTenant(result.institution, [sort: 'name'])
        result
    }

    /**
     * Display and manage PrivateProperties for this institution
     */
    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_EDITOR")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliation("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR")
    })
    def managePrivateProperties() {
        def result = setResultGenerics()

        result.editable = true // true, because action is protected
        result.privatePropertyDefinitions = PropertyDefinition.findAllWhere([tenant: result.institution])

        if('add' == params.cmd) {
            flash.message = addPrivatePropertyDefinition(params)
            result.privatePropertyDefinitions = PropertyDefinition.findAllWhere([tenant: result.institution])
        }
        else if('delete' == params.cmd) {
            flash.message = deletePrivatePropertyDefinition(params)
            result.privatePropertyDefinitions = PropertyDefinition.findAllWhere([tenant: result.institution])
        }

        RuleBasedCollator clt = SortUtil.getCollator()
        result.privatePropertyDefinitions.sort{a, b -> clt.compare(
                message(code: "propertyDefinition.${a.descr}.label", args:[]) + '|' + a.name,
                message(code: "propertyDefinition.${b.descr}.label", args:[]) + '|' + b.name
        )}

        result.language = LocaleContextHolder.getLocale().toString()
        result
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_EDITOR")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliation("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR")
    })
    def managePropertyDefinitions() {
        def result = setResultGenerics()

        def propDefs = [:]
        PropertyDefinition.AVAILABLE_CUSTOM_DESCR.each { it ->
            def itResult = PropertyDefinition.findAllByDescrAndTenant(it, null, [sort: 'name']) // NO private properties!
            propDefs << ["${it}": itResult]
        }

        def (usedPdList, attrMap) = propertyService.getUsageDetails()
        result.editable = false
        result.propertyDefinitions = propDefs
        result.attrMap = attrMap
        result.usedPdList = usedPdList

        result.language = LocaleContextHolder.getLocale().toString()
        result
    }

    @Secured(['ROLE_USER'])
    def switchContext() {
        def user = User.get(springSecurityService.principal.id)
        def org  = genericOIDService.resolveOID(params.oid)

        if (user && org && org.id in user.getAuthorizedOrgsIds()) {
            log.debug('switched context to: ' + org)
            contextService.setOrg(org)
        }
        redirect action:'dashboard', params:params.remove('oid')
    }

    /**
     * Adding new PrivateProperty for this institution if not existing
     *
     * @param params
     * @return
     */

    private addPrivatePropertyDefinition(params) {
        log.debug("adding private property definition for institution: " + params)

        def tenant = GrailsHibernateUtil.unwrapIfProxy(contextService.getOrg())

        def privatePropDef = PropertyDefinition.findWhere(
                name:   params.pd_name,
                descr:  params.pd_descr,
               // type:   params.pd_type,
                tenant: tenant,
        )

        if(! privatePropDef){
            def rdc

            if (params.refdatacategory) {
                rdc = RefdataCategory.findById( Long.parseLong(params.refdatacategory) )
            }
            privatePropDef = PropertyDefinition.loc(
                    params.pd_name,
                    params.pd_descr,
                    params.pd_type,
                    rdc,
                    params.pd_expl,
                    (params.pd_multiple_occurrence ? true : false),
                    (params.pd_mandatory ? true : false),
                    tenant
            )

            if (privatePropDef.save(flush: true)) {
                return message(code: 'default.created.message', args:[privatePropDef.descr, privatePropDef.name])
            }
            else {
                return message(code: 'default.not.created.message', args:[privatePropDef.descr, privatePropDef.name])
            }
        }
    }

    /**
     * Delete existing PrivateProperty for this institution
     *
     * @param params
     * @return
     */

    private deletePrivatePropertyDefinition(params) {
        log.debug("delete private property definition for institution: " + params)

        def messages  = []
        def tenant    = contextService.getOrg()
        def deleteIds = params.list('deleteIds')

        deleteIds.each { did ->
            def id = Long.parseLong(did)
            def privatePropDef = PropertyDefinition.findWhere(id: id, tenant: tenant)
            if (privatePropDef) {

                try {
                    if (privatePropDef.mandatory) {
                        privatePropDef.mandatory = false
                        privatePropDef.save()

                        // delete inbetween created mandatories
                        Class.forName(
                                privatePropDef.getImplClass('private')
                        )?.findAllByType(privatePropDef)?.each { it ->
                            it.delete()
                        }
                    }
                } catch(Exception e) {
                    log.error(e)
                }

                privatePropDef.delete()
                messages << message(code: 'default.deleted.message', args:[privatePropDef.descr, privatePropDef.name])
            }
        }
        messages
    }

    private setResultGenerics() {

        def result          = [:]
        result.user         = User.get(springSecurityService.principal.id)
        //result.institution  = Org.findByShortcode(params.shortcode)
        result.institution  = contextService.getOrg()
        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR') || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN,ROLE_YODA')
        result
    }

    @DebugAnnotation(test='hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def ajaxEmptySubscription() {

        def result = setResultGenerics()

        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')
        if (result.editable) {

            if(accessService.checkPerm("ORG_INST_COLLECTIVE,ORG_CONSORTIUM")) {
                if(accessService.checkPerm("ORG_CONSORTIUM"))
                    params.comboType = RDStore.COMBO_TYPE_CONSORTIUM.value
                else(accessService.checkPerm("ORG_INST_COLLECTIVE"))
                    params.comboType = RDStore.COMBO_TYPE_DEPARTMENT.value
                def fsq = filterService.getOrgComboQuery(params, result.institution)
                result.members = Org.executeQuery(fsq.query, fsq.queryParams, params)
            }

            result
        }
        render (template: "../templates/filter/orgFilterTable", model: [orgList: result.members, tmplShowCheckbox: true, tmplConfigShow: ['sortname', 'name']])
    }

    @Deprecated
    @DebugAnnotation(test='hasAffiliation("INST_ADM")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_ADM") })
    def actionAffiliationRequestOrg() {
        log.debug("actionMembershipRequestOrg");
        def req = UserOrg.get(params.req);
        def user = User.get(springSecurityService.principal.id)
        def currentOrg = contextService.getOrg()
        if ( req != null && req.org == currentOrg) {
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
            req.dateActioned = System.currentTimeMillis();
            req.save(flush:true);
        }
        else {
            log.error("FLASH");
        }
        redirect(action: "manageAffiliationRequests")
    }

    @DebugAnnotation(test='hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_USER") })
    def copyLicense() {
        def result = setResultGenerics()

        if(params.id)
        {
            def license = License.get(params.id)
            def isEditable = license.isEditableBy(result.user)

            if (! (accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR'))) {
                flash.error = message(code:'license.permissionInfo.noPerms', default: 'No User Permissions');
                response.sendError(401)
                return;
            }

            if(isEditable){
                redirect controller: 'license', action: 'processcopyLicense', params: ['baseLicense'                 : license.id,
                                                                                       'license.copyAnnouncements'   : 'on',
                                                                                       'license.copyCustomProperties': 'on',
                                                                                       'license.copyDates'           : 'on',
                                                                                       'license.copyDocs'             : 'on',
                                                                                       'license.copyLinks'            : 'on',
                                                                                       'license.copyPrivateProperties': 'on',
                                                                                       'license.copyTasks'            : 'on']
            }else {
                flash.error = message(code:'license.permissionInfo.noPerms', default: 'No User Permissions');
                response.sendError(401)
                return;
            }
        }

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyParticipantConsortia() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        result.surveyInfo = SurveyInfo.get(params.id) ?: null

        result.participant = Org.get(params.participant)

        result.surveyResult = SurveyResult.findAllByOwnerAndParticipantAndSurveyConfigInList(result.institution, result.participant, result.surveyInfo.surveyConfigs).sort{it?.surveyConfig?.configOrder}.groupBy {it?.surveyConfig?.id}

        result

    }
}
