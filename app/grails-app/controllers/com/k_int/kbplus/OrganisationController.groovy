package com.k_int.kbplus

import com.k_int.kbplus.auth.Role
import com.k_int.kbplus.auth.User
import com.k_int.kbplus.auth.UserOrg
import com.k_int.properties.PropertyDefinition
import de.laser.DeletionService
import de.laser.controller.AbstractDebugController
import de.laser.helper.DebugAnnotation
import de.laser.helper.DebugUtil
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import grails.plugin.springsecurity.annotation.Secured
import grails.plugin.springsecurity.SpringSecurityUtils
import de.laser.helper.RDStore
import static de.laser.helper.RDStore.*

import javax.servlet.ServletOutputStream
import java.text.SimpleDateFormat

@Secured(['IS_AUTHENTICATED_FULLY'])
class OrganisationController extends AbstractDebugController {

    def springSecurityService
    def accessService
    def contextService
    def addressbookService
    def filterService
    def genericOIDService
    def propertyService
    def docstoreService
    def instAdmService
    def organisationService
    def deletionService

    static allowedMethods = [create: ['GET', 'POST'], edit: ['GET', 'POST'], delete: 'POST']

    @Secured(['ROLE_ORG_EDITOR','ROLE_ADMIN'])
    def index() {
        redirect action: 'list', params: params
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_ADM", specRole="ROLE_ADMIN,ROLE_ORG_EDITOR")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_INST,ORG_CONSORTIUM", "INST_ADM", "ROLE_ADMIN,ROLE_ORG_EDITOR")
    })
    def settings() {

        User user = User.get(springSecurityService.principal.id)
        Org org   = Org.get(params.id)

        Map result = [
                user:           user,
                orgInstance:    org,
                editable:   	SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN,ROLE_ORG_EDITOR'),
                inContextOrg:   contextService.getOrg().id == params.int('id')
        ]

        result.editable = result.editable || (result.inContextOrg && accessService.checkMinUserOrgRole(user, org, 'INST_ADM'))

        // forbidden access
        if (! result.editable) {
            redirect controller: 'organisation', action: 'show', id: result.orgInstance.id
        }

        if (! result.orgInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'org.label', default: 'Org'), params.id])
            redirect action: 'list'
            return
        }

        // adding default settings
        if (OrgSettings.get(result.orgInstance, OrgSettings.KEYS.NATSTAT_SERVER_ACCESS) == OrgSettings.SETTING_NOT_FOUND) {
            OrgSettings.add(
                    result.orgInstance, OrgSettings.KEYS.NATSTAT_SERVER_ACCESS,
                    RefdataValue.getByValueAndCategory('No', 'YN')
            )
        }
        if (OrgSettings.get(result.orgInstance, OrgSettings.KEYS.NATSTAT_SERVER_API_KEY) == OrgSettings.SETTING_NOT_FOUND) {
            OrgSettings.add(
                    result.orgInstance, OrgSettings.KEYS.NATSTAT_SERVER_API_KEY,''
            )
        }
        if (OrgSettings.get(result.orgInstance, OrgSettings.KEYS.NATSTAT_SERVER_REQUESTOR_ID) == OrgSettings.SETTING_NOT_FOUND) {
            OrgSettings.add(
                    result.orgInstance, OrgSettings.KEYS.NATSTAT_SERVER_REQUESTOR_ID, ''
            )
        }
        if (OrgSettings.get(result.orgInstance, OrgSettings.KEYS.OAMONITOR_SERVER_ACCESS) == OrgSettings.SETTING_NOT_FOUND) {
            OrgSettings.add(
                    result.orgInstance, OrgSettings.KEYS.OAMONITOR_SERVER_ACCESS,
                    RefdataValue.getByValueAndCategory('No', 'YN')
            )
        }

        result.settings = OrgSettings.findAllByOrg(result.orgInstance)
        result
    }

    @Secured(['ROLE_ORG_EDITOR','ROLE_ADMIN'])
    def list() {

        def result = [:]
        result.user = User.get(springSecurityService.principal.id)
        result.max  = params.max ? Long.parseLong(params.max) : result.user?.getDefaultPageSizeTMP()
        result.offset = params.offset ? Long.parseLong(params.offset) : 0
        params.sort = params.sort ?: " LOWER(o.shortname), LOWER(o.name)"

        result.editable = SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN,ROLE_ORG_EDITOR')

        def fsq = filterService.getOrgQuery(params)
        result.filterSet = params.filterSet ? true : false

        List orgListTotal  = Org.findAll(fsq.query, fsq.queryParams)
        result.orgListTotal = orgListTotal.size()
        result.orgList = orgListTotal.drop((int) result.offset).take((int) result.max)

        SimpleDateFormat sdf = new SimpleDateFormat(g.message(code:'default.date.format.notimenopoint'))
        String datetoday = sdf.format(new Date(System.currentTimeMillis()))
        def message = message(code: 'export.all.orgs')
        // Write the output to a file
        String file = message+"_${datetoday}"
        if ( params.exportXLS ) {

            try {
                SXSSFWorkbook wb = (SXSSFWorkbook) organisationService.exportOrg(orgListTotal, message, true,'xls')

                response.setHeader "Content-disposition", "attachment; filename=\"${file}.xlsx\""
                response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                wb.write(response.outputStream)
                response.outputStream.flush()
                response.outputStream.close()
                wb.dispose()

            }
            catch (Exception e) {
                log.error("Problem",e);
                response.sendError(500)
            }
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
                        writer.write((String) organisationService.exportOrg(orgListTotal,message,true,"csv"))
                    }
                    out.close()
                }
            }
        }
    }

    @DebugAnnotation(perm="ORG_CONSORTIUM", type="Consortium", affil="INST_USER")
    @Secured(closure = {
        ctx.accessService.checkPermTypeAffiliation("ORG_CONSORTIUM", "Consortium", "INST_USER")
    })
    Map listInstitution() {
        Map result = setResultGenericsAndCheckAccess(params)
        params.orgType   = OT_INSTITUTION.id.toString()
        params.orgSector = O_SECTOR_HIGHER_EDU.id.toString()
        if(!params.sort)
            params.sort = " LOWER(o.sortname)"
        def fsq = filterService.getOrgQuery(params)
        result.availableOrgs = Org.executeQuery(fsq.query, fsq.queryParams, params)
        result.consortiaMemberIds = []
        Combo.findAllWhere(
                toOrg: result.institution,
                type:    RefdataValue.getByValueAndCategory('Consortium','Combo Type')
        ).each { cmb ->
            result.consortiaMemberIds << cmb.fromOrg.id
        }
        result
    }

    @Secured(['ROLE_USER'])
    def listProvider() {
        def result = [:]
        result.propList    = PropertyDefinition.findAllPublicAndPrivateOrgProp(contextService.getOrg())
        result.user        = User.get(springSecurityService.principal.id)
        result.editable    = SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN,ROLE_ORG_EDITOR') || accessService.checkConstraint_ORG_COM_EDITOR()

        params.orgSector   = O_SECTOR_PUBLISHER?.id?.toString()
        params.orgType = OT_PROVIDER?.id?.toString()
        params.sort        = params.sort ?: " LOWER(o.shortname), LOWER(o.name)"

        def fsq            = filterService.getOrgQuery(params)
        result.filterSet = params.filterSet ? true : false

        if (params.filterPropDef) {
            def orgIdList = Org.executeQuery("select o.id ${fsq.query}", fsq.queryParams)
            fsq = filterService.getOrgQuery([constraint_orgIds: orgIdList] << params)
            fsq = propertyService.evalFilterQuery(params, fsq.query, 'o', fsq.queryParams)
        }
        result.max          = params.max ? Integer.parseInt(params.max) : result.user?.getDefaultPageSizeTMP()
        result.offset       = params.offset ? Integer.parseInt(params.offset) : 0
        List orgListTotal   = Org.findAll(fsq.query, fsq.queryParams)
        result.orgListTotal = orgListTotal.size()
        result.orgList      = orgListTotal.drop((int) result.offset).take((int) result.max)

        def message = g.message(code: 'export.all.providers')
        SimpleDateFormat sdf = new SimpleDateFormat(g.message(code:'default.date.format.notime', default:'yyyy-MM-dd'))
        String datetoday = sdf.format(new Date(System.currentTimeMillis()))
        String filename = message+"_${datetoday}"

        if ( params.exportXLS) {
            params.remove('max')
            try {
                SXSSFWorkbook wb = (SXSSFWorkbook) organisationService.exportOrg(orgListTotal, message, false, "xls")
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

    @Secured(['ROLE_ADMIN','ROLE_ORG_EDITOR'])
    def create() {
        switch (request.method) {
            case 'POST':
                def orgInstance = new Org(params)

                if (params.name) {
                    if (orgInstance.save(flush: true)) {
                        orgInstance.setDefaultCustomerType()

                        flash.message = message(code: 'default.created.message', args: [message(code: 'org.label', default: 'Org'), orgInstance.name])
                        redirect action: 'show', id: orgInstance.id
                        return
                    }
                }

                render view: 'create', model: [orgInstance: orgInstance]
                break
        }
    }

    @Secured(['ROLE_ADMIN','ROLE_ORG_EDITOR'])
    def setupBasicTestData() {
        Org targetOrg = Org.get(params.id)
        if(organisationService.setupBasicTestData(targetOrg)) {
            flash.message = message(code:'org.setup.success')
        }
        else {
            flash.error = message(code:'org.setup.error',args: [organisationService.dumpErrors()])
        }
        redirect action: 'show', id: params.id
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_EDITOR", specRole="ROLE_ADMIN,ROLE_ORG_EDITOR")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN,ROLE_ORG_EDITOR")
    })
    def createProvider() {

        def orgSector = RefdataValue.getByValueAndCategory('Publisher','OrgSector')
        def orgType = RefdataValue.getByValueAndCategory('Provider','OrgRoleType')
        def orgType2 = RefdataValue.getByValueAndCategory('Agency','OrgRoleType')
        def orgInstance = new Org(name: params.provider, sector: orgSector.id)

        if ( orgInstance.save(flush:true) ) {

            orgInstance.addToOrgType(orgType)
            orgInstance.addToOrgType(orgType2)
            orgInstance.save(flush:true)

            flash.message = message(code: 'default.created.message', args: [message(code: 'org.label', default: 'Org'), orgInstance.name])
            redirect action: 'show', id: orgInstance.id
        }
        else {
            log.error("Problem creating title: ${orgInstance.errors}");
            flash.message = message(code:'org.error.createProviderError',args:[orgInstance.errors])
            redirect ( action:'findProviderMatches' )
        }
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_EDITOR", specRole="ROLE_ADMIN,ROLE_ORG_EDITOR")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN,ROLE_ORG_EDITOR")
    })
    def findProviderMatches() {

        def result=[:]
        if ( params.proposedProvider ) {

            result.providerMatches= Org.executeQuery("from Org as o where exists (select roletype from o.orgType as roletype where roletype = :provider ) and (lower(o.name) like :searchName or lower(o.shortname) like :searchName or lower(o.sortname) like :searchName ) ",
                    [provider: OT_PROVIDER, searchName: "%${params.proposedProvider.toLowerCase()}%"])
        }
        result
    }

    @DebugAnnotation(perm="ORG_INST_COLLECTIVE, ORG_CONSORTIUM", affil="INST_ADM",specRole="ROLE_ADMIN, ROLE_ORG_EDITOR")
    @Secured(closure = { ctx.accessService.checkPermAffiliationX("ORG_INST_COLLECTIVE, ORG_CONSORTIUM","INST_ADM","ROLE_ADMIN, ROLE_ORG_EDITOR") })
    def createMember() {
        Org contextOrg = contextService.org
        //new institution = consortia member, implies combo type consortium
        RefdataValue orgSector = O_SECTOR_HIGHER_EDU
        if(params.institution) {
            Org orgInstance = new Org(name: params.institution, sector: orgSector)
            orgInstance.addToOrgType(OT_INSTITUTION)
            try {
                if(accessService.checkPerm("ORG_CONSORTIUM")) {
                    orgInstance.save()
                    Combo newMember = new Combo(fromOrg:orgInstance,toOrg:contextOrg,type:COMBO_TYPE_CONSORTIUM)
                    newMember.save()
                }
                orgInstance.setDefaultCustomerType()

                flash.message = message(code: 'default.created.message', args: [message(code: 'org.institution.label'), orgInstance.name])
                redirect action: 'show', id: orgInstance.id, params: [fromCreate: true]
            }
            catch (Exception e) {
                log.error("Problem creating institution: ${orgInstance.errors}")
                log.error(e.printStackTrace())
                flash.message = message(code: "org.error.createInstitutionError",args:[orgInstance.errors])
                redirect ( action:'findOrganisationMatches', params: params )
            }
        }
        //new department = institution member, implies combo type department
        else if(params.department) {
            Org deptInstance = new Org(name: params.department, sector: orgSector)
            deptInstance.addToOrgType(OT_DEPARTMENT)
            try {
                if(accessService.checkPerm("ORG_INST_COLLECTIVE")) {
                    deptInstance.save()
                    Combo newMember = new Combo(fromOrg:deptInstance,toOrg:contextOrg,type:COMBO_TYPE_DEPARTMENT)
                    newMember.save()
                }
                deptInstance.setDefaultCustomerType()

                flash.message = message(code: 'default.created.message', args: [message(code: 'org.department.label'), deptInstance.name])
                redirect action: 'show', id: deptInstance.id, params: [fromCreate: true]
            }
            catch (Exception e) {
                log.error("Problem creating department: ${deptInstance.errors}")
                log.error(e.printStackTrace())
                flash.error = message(code: "org.error.createInstitutionError",args:[deptInstance.errors])
                redirect ( action:'findOrganisationMatches', params: params )
            }
        }
    }

    @DebugAnnotation(perm="ORG_INST_COLLECTIVE, ORG_CONSORTIUM", affil="INST_ADM",specRole="ROLE_ADMIN, ROLE_ORG_EDITOR")
    @Secured(closure = { ctx.accessService.checkPermAffiliationX("ORG_INST_COLLECTIVE, ORG_CONSORTIUM","INST_ADM","ROLE_ADMIN, ROLE_ORG_EDITOR") })
    Map findOrganisationMatches() {
        Map memberMap = [:]
        RefdataValue comboType
        if(accessService.checkPerm('ORG_CONSORTIUM'))
            comboType = COMBO_TYPE_CONSORTIUM
        else if(accessService.checkPerm('ORG_INST_COLLECTIVE'))
            comboType = COMBO_TYPE_DEPARTMENT
        Combo.findAllByType(comboType).each { lObj ->
            Combo link = (Combo) lObj
            List members = memberMap.get(link.fromOrg.id)
            if(!members)
                members = [link.toOrg.id]
            else members << link.toOrg.id
            memberMap.put(link.fromOrg.id,members)
        }
        Map result=[institution:contextService.org,organisationMatches:[],members:memberMap,comboType:comboType]
        //searching members for consortium, i.e. the context org is a consortium
        if(comboType == COMBO_TYPE_CONSORTIUM) {
            if ( params.proposedOrganisation ) {
                result.organisationMatches.addAll(Org.executeQuery("select o from Org as o where exists (select roletype from o.orgType as roletype where roletype = :institution ) and (lower(o.name) like :searchName or lower(o.shortname) like :searchName or lower(o.sortname) like :searchName) ",
                        [institution: OT_INSTITUTION, searchName: "%${params.proposedOrganisation.toLowerCase()}%"]))
            }
            if (params.proposedOrganisationID) {
                result.organisationMatches.addAll(Org.executeQuery("select id.org from IdentifierOccurrence id where lower(id.identifier.value) like :identifier and lower(id.identifier.ns.ns) in (:namespaces) ",
                        [identifier: "%${params.proposedOrganisationID.toLowerCase()}%",namespaces:["isil","wibid"]]))
            }
        }
        //searching departments of the institution, i.e. the context org is an institution
        else if(comboType == COMBO_TYPE_DEPARTMENT) {
            if(params.proposedOrganisation) {
                result.organisationMatches.addAll(Org.executeQuery("select c.fromOrg from Combo c join c.fromOrg o where c.toOrg = :contextOrg and c.type = :department and (lower(o.name) like :searchName or lower(o.shortname) like :searchName or lower(o.sortname) like :searchName)",
                        [department: comboType, contextOrg: contextService.org, searchName: "%${params.proposedOrganisation.toLowerCase()}%"]))
            }
        }
        result
    }

    @Secured(['ROLE_USER'])
    def show() {

        DebugUtil du = new DebugUtil()
        du.setBenchMark('this-n-that')

        Map result = setResultGenericsAndCheckAccess(params)
        if(!result) {
            response.sendError(401)
            return
        }

        //this is a flag to check whether the page has been called directly after creation
        result.fromCreate = params.fromCreate ? true : false

        //def link_vals = RefdataCategory.getAllRefdataValues("Organisational Role")
        //def sorted_links = [:]
        //def offsets = [:]

        du.setBenchMark('orgRoles')

        // TODO: experimental asynchronous task
        /*def task_orgRoles = task {

            if (SpringSecurityUtils.ifAnyGranted("ROLE_YODA") ||
                    (orgInstance.id == org.id && user.hasAffiliation('INST_ADM'))
            ) {

                link_vals.each { lv ->
                    def param_offset = 0

                    if (lv.id) {
                        def cur_param = "rdvl_${String.valueOf(lv.id)}"

                        if (params[cur_param]) {
                            param_offset = params[cur_param]
                            result[cur_param] = param_offset
                        }

                        def links = OrgRole.findAll {
                            org == orgInstance && roleType == lv
                        }
                        links = links.findAll { it2 -> it2.ownerStatus?.value != 'Deleted' }

                        def link_type_results = links.drop(param_offset.toInteger()).take(10) // drop from head, take 10

                        if (link_type_results) {
                            sorted_links["${String.valueOf(lv.id)}"] = [rdv: lv, rdvl: cur_param, links: link_type_results, total: links.size()]
                        }
                    } else {
                        log.debug("Could not read Refdata: ${lv}")
                    }
                }
            }
        }*/

        /*if (params.ajax) {
            render template: '/templates/links/orgRoleContainer', model: [listOfLinks: sorted_links, orgInstance: orgInstance]
            return
        }*/

        du.setBenchMark('editable')

        //result.sorted_links = sorted_links

        def orgSector = O_SECTOR_PUBLISHER
        def orgType = OT_PROVIDER

        //IF ORG is a Provider
        if(result.orgInstance.sector == orgSector || orgType?.id in result.orgInstance?.getallOrgTypeIds()) {
            du.setBenchMark('editable2')
            result.editable = accessService.checkMinUserOrgRole(result.user, result.orgInstance, 'INST_EDITOR') ||
                    accessService.checkPermAffiliationX("ORG_INST,ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN,ROLE_ORG_EDITOR")
        }
        else {
            du.setBenchMark('editable2')
            if(accessService.checkPerm("ORG_CONSORTIUM")) {
                List<Long> consortia = Combo.findAllByTypeAndFromOrg(RDStore.COMBO_TYPE_CONSORTIUM,result.orgInstance).collect { it ->
                    it.toOrg.id
                }
                if(consortia.size() == 1 && consortia.contains(result.institution.id) && accessService.checkMinUserOrgRole(result.user,result.institution,'INST_EDITOR'))
                    result.editable = true
            }
            else if(accessService.checkPerm("ORG_INST_COLLECTIVE")) {
                List<Long> department = Combo.findAllByTypeAndFromOrg(RDStore.COMBO_TYPE_DEPARTMENT,result.orgInstance).collect { it ->
                    it.toOrg.id
                }
                if (department.contains(result.institution.id) && accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR'))
                    result.editable = true
            }
            else
                result.editable = accessService.checkMinUserOrgRole(result.user, result.orgInstance, 'INST_EDITOR') || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN,ROLE_ORG_EDITOR')
        }
        
      if (!result.orgInstance) {
        flash.message = message(code: 'default.not.found.message', args: [message(code: 'org.label', default: 'Org'), params.id])
        redirect action: 'list'
        return
      }

        du.setBenchMark('properties')

        // TODO: experimental asynchronous task
        //def task_properties = task {

            // -- private properties

            result.authorizedOrgs = result.user?.authorizedOrgs

            // create mandatory OrgPrivateProperties if not existing

            def mandatories = PropertyDefinition.findAllByDescrAndMandatoryAndTenant("Organisation Property", true, result.institution)

            mandatories.each { pd ->
                if (!OrgPrivateProperty.findWhere(owner: result.orgInstance, type: pd)) {
                    def newProp = PropertyDefinition.createGenericProperty(PropertyDefinition.PRIVATE_PROPERTY, result.orgInstance, pd)


                    if (newProp.hasErrors()) {
                        log.error(newProp.errors)
                    } else {
                        log.debug("New org private property created via mandatory: " + newProp.type.name)
                    }
                }
            }

            // -- private properties
       //}

        //documents
        //du.setBenchMark('documents')

        List bm = du.stopBenchMark()
        result.benchMark = bm

        // TODO: experimental asynchronous task
        //waitAll(task_orgRoles, task_properties)

        if(result.orgInstance.hasPerm("ORG_INST,ORG_CONSORTIUM")){

            def foundIsil = false
            def foundWibid = false
            def foundEZB = false

            result.orgInstance.ids.each {io ->
                if(io?.identifier?.ns?.ns == 'ISIL')
                {
                    foundIsil = true
                }
                if(io?.identifier?.ns?.ns == 'wibid')
                {
                    foundWibid = true
                }
                if(io?.identifier?.ns?.ns == 'ezb')
                {
                    foundEZB = true
                }
            }

            if(!foundIsil) {
                result.orgInstance.addOnlySpecialIdentifiers('ISIL', 'Unknown')


            }
            if(!foundWibid) {
                result.orgInstance.addOnlySpecialIdentifiers('wibid', 'Unknown')


            }
            if(!foundEZB) {
                result.orgInstance.addOnlySpecialIdentifiers('ezb', 'Unknown')


            }
        }


        result
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_USER")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliation("ORG_INST,ORG_CONSORTIUM", "INST_USER")
    })
    def documents() {
        Map result = setResultGenericsAndCheckAccess(params)
        if(!result) {
            response.sendError(401)
            return
        }
        result
    }

    @DebugAnnotation(test='hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def editDocument() {
        Map result = setResultGenericsAndCheckAccess(params)
        if(!result) {
            response.sendError(401)
            return
        }
        result.ownobj = result.institution
        result.owntp = 'organisation'
        if(params.id) {
            result.docctx = DocContext.get(params.id)
            result.doc = result.docctx.owner
        }

        render template: "/templates/documents/modal", model: result
    }

    @DebugAnnotation(test='hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def deleteDocuments() {
        log.debug("deleteDocuments ${params}");

        docstoreService.unifiedDeleteDocuments(params)

        redirect controller: 'organisation', action: 'documents', id: params.instanceId /*, fragment: 'docstab' */
    }

    @Deprecated
    @Secured(['ROLE_ADMIN'])
    def properties() {
        def result = [:]
        result.user = User.get(springSecurityService.principal.id)
        def orgInstance = Org.get(params.id)

        result.editable = accessService.checkMinUserOrgRole(result.user, orgInstance, 'INST_EDITOR') || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')

        if (!orgInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'org.label', default: 'Org'), params.id])
            redirect action: 'list'
            return
        }

        // create mandatory OrgPrivateProperties if not existing

        def mandatories = []
        result.user?.authorizedOrgs?.each{ org ->
            def ppd = PropertyDefinition.findAllByDescrAndMandatoryAndTenant("Organisation Property", true, org)
            if(ppd){
                mandatories << ppd
            }
        }
        mandatories.flatten().each{ pd ->
            if (! OrgPrivateProperty.findWhere(owner: orgInstance, type: pd)) {
                def newProp = PropertyDefinition.createGenericProperty(PropertyDefinition.PRIVATE_PROPERTY, orgInstance, pd)

                if (newProp.hasErrors()) {
                    log.error(newProp.errors)
                } else {
                    log.debug("New org private property created via mandatory: " + newProp.type.name)
                }
            }
        }

        result.orgInstance = orgInstance
        result.authorizedOrgs = result.user?.authorizedOrgs
        result
    }

    @DebugAnnotation(test = 'hasAffiliation("INST_ADM")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_ADM") })
    def users() {
        def result = [:]
        result.user = User.get(springSecurityService.principal.id)
        def orgInstance = Org.get(params.id)

        result.editable = SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN') ||
                          instAdmService.hasInstAdmPivileges(result.user, orgInstance, [RDStore.COMBO_TYPE_DEPARTMENT, RDStore.COMBO_TYPE_CONSORTIUM])

        // forbidden access
        if (! result.editable && orgInstance.id != contextService.getOrg().id) {
            redirect controller: 'organisation', action: 'show', id: orgInstance.id

        }

      if (!orgInstance) {
        flash.message = message(code: 'default.not.found.message', args: [message(code: 'org.label', default: 'Org'), params.id])
        redirect action: 'list'
        return
      }

        result.pendingRequests = UserOrg.findAllByStatusAndOrg(UserOrg.STATUS_PENDING, orgInstance, [sort:'dateRequested', order:'desc'])
        //result.users = UserOrg.findAllByStatusAndOrg(UserOrg.STATUS_APPROVED, orgInstance, [sort:'user.username', order: 'asc'])

        List baseQuery  = ['select distinct uo from UserOrg uo, User u']
        List whereQuery = ['uo.user = u and uo.org = :org']
        Map queryParams = [org: orgInstance]

        whereQuery.add('uo.status = :status')
        queryParams.put('status', UserOrg.STATUS_APPROVED)

        if (params.authority) {
            whereQuery.add('uo.formalRole = :role')
            queryParams.put('role', Role.get(params.authority.toLong()))
        }

        if (params.name && params.name != '' ) {
            whereQuery.add('(genfunc_filter_matcher(u.username, :name) = true or genfunc_filter_matcher(u.display, :name) = true)')
            queryParams.put('name', "${params.name}")
        }

        result.users = User.executeQuery(
                baseQuery.join(', ') + (whereQuery ? ' where ' + whereQuery.join(' and ') : '') ,
                queryParams,
                [sort:'user.username', order: 'asc']
        )

        result.orgInstance = orgInstance
        result
    }

    @Secured(['ROLE_ADMIN','ROLE_ORG_EDITOR'])
    def edit() {
        redirect controller: 'organisation', action: 'show', params: params
        return
    }

    @Secured(['ROLE_ADMIN'])
    def _delete() {
        def result = [:]

        result.editable = SpringSecurityUtils.ifAnyGranted("ROLE_ORG_EDITOR,ROLE_ADMIN")
        result.orgInstance = Org.get(params.id)

        if (result.orgInstance) {
            if (params.process  && result.editable) {
                result.result = deletionService.deleteOrganisation(result.orgInstance, null, false)
            }
            else {
                result.dryRun = deletionService.deleteOrganisation(result.orgInstance, null, DeletionService.DRY_RUN)
            }

            if (contextService.getUser().isAdmin()) {
                result.substituteList = Org.executeQuery("select distinct o from Org o where o.status.value != 'Deleted'")
            }
            else {
                List<Org> orgList = [result.orgInstance]
                orgList.addAll(Org.executeQuery("select o from Combo cmb join cmb.fromOrg o where o.status.value != 'Deleted' and cmb.toOrg = :org", [org: result.orgInstance]))
                orgList.addAll(Org.executeQuery("select o from Combo cmb join cmb.toOrg o where o.status.value != 'Deleted' and cmb.fromOrg = :org", [org: result.orgInstance]))
                orgList.unique()

                result.substituteList = orgList
            }
        }

        render view: 'delete', model: result
    }

    @DebugAnnotation(test = 'hasAffiliation("INST_ADM")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_ADM") })
    def processAffiliation() {
      def result = [:]
      result.user = User.get(springSecurityService.principal.id)
      UserOrg uo = UserOrg.get(params.assoc)

      if (instAdmService.hasInstAdmPivileges(result.user, Org.get(params.id), [RDStore.COMBO_TYPE_DEPARTMENT, RDStore.COMBO_TYPE_CONSORTIUM])) {

          if (params.cmd == 'approve') {
              uo.status = UserOrg.STATUS_APPROVED
              uo.dateActioned = System.currentTimeMillis()
              uo.save(flush: true)
          }
          else if (params.cmd == 'reject') {
              uo.status = UserOrg.STATUS_REJECTED
              uo.dateActioned = System.currentTimeMillis()
              uo.save(flush: true)
          }
          else if (params.cmd == 'delete') {
              uo.delete(flush: true)
          }
      }
      redirect action: 'users', id: params.id
    }

    @Secured(['ROLE_USER'])
    def addOrgCombo(Org fromOrg, Org toOrg) {
      //def comboType = RefdataCategory.lookupOrCreate('Organisational Role', 'Package Consortia')
      def comboType = RefdataValue.get(params.comboTypeTo)
      log.debug("Processing combo creation between ${fromOrg} AND ${toOrg} with type ${comboType}")
      def dupe = Combo.executeQuery("from Combo as c where c.fromOrg = ? and c.toOrg = ?", [fromOrg, toOrg])
      
      if (! dupe) {
        def consLink = new Combo(fromOrg:fromOrg,
                                 toOrg:toOrg,
                                 status:null,
                                 type:comboType)
          consLink.save()
      }
      else {
        flash.message = "This Combo already exists!"
      }
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_USER")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliation("ORG_INST,ORG_CONSORTIUM", "INST_USER")
    })
    def addressbook() {
        Map result = setResultGenericsAndCheckAccess(params)
        if(!result) {
            response.sendError(401)
            return
        }

        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR') || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')

        if (! result.institution) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'org.label', default: 'Org'), params.id])
            redirect action: 'list'
            return
        }

        result.visiblePersons = addressbookService.getAllVisiblePersons(result.user, result.orgInstance)

        result
    }
    @DebugAnnotation(test = 'checkForeignOrgComboPermAffiliation()')
    @Secured(closure = {
        ctx.accessService.checkForeignOrgComboPermAffiliationX([
                org: Org.get(request.getRequestURI().split('/').last()),
                affiliation: "INST_USER",
                comboPerm: "ORG_CONSORTIUM",
                comboAffiliation: "INST_EDITOR",
                specRoles: "ROLE_ADMIN,ROLE_ORG_EDITOR"
                ])
    })
    def readerNumber() {
        Map result = setResultGenericsAndCheckAccess(params)
        if(!result) {
            response.sendError(401)
            return
        }
        result.editable = accessService.checkMinUserOrgRole(result.user, result.orgInstance, 'INST_EDITOR') || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN,ROLE_ORG_EDITOR')

        params.sort = params.sort ?: 'dueDate'
        params.order = params.order ?: 'desc'

        result.numbersInstanceList = ReaderNumber.findAllByOrg(result.orgInstance, params)

        result
    }

    @DebugAnnotation(test = 'checkForeignOrgComboPermAffiliation()')
    @Secured(closure = {
        ctx.accessService.checkForeignOrgComboPermAffiliationX([
                org: Org.get(request.getRequestURI().split('/').last()),
                affiliation: "INST_USER",
                comboPerm: "ORG_CONSORTIUM",
                comboAffiliation: "INST_EDITOR",
                specRoles: "ROLE_ADMIN"
        ])
    })
    def accessPoints() {
        Map result = setResultGenericsAndCheckAccess(params)
        if(!result) {
            response.sendError(401)
            return
        }

        result.editable = accessService.checkMinUserOrgRole(result.user, result.orgInstance, 'INST_EDITOR') || SpringSecurityUtils.ifAllGranted('ROLE_ADMIN')

        if (! result.orgInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'org.label', default: 'Org'), params.id])
            redirect action: 'list'
            return
        }

        def orgAccessPointList = OrgAccessPoint.findAllByOrg(result.orgInstance,  [sort: ["name": 'asc', "accessMethod" : 'asc']])
        result.orgAccessPointList = orgAccessPointList

        result
    }

    def addOrgType()
    {
        def result = [:]
        result.user = User.get(springSecurityService.principal.id)
        def orgInstance = Org.get(params.org)

        if (!orgInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'org.label', default: 'Org'), params.id])
            redirect action: 'list'
            return
        }

        if ( SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN,ROLE_ORG_EDITOR') ) {
            result.editable = true
        }
        else {
            result.editable = accessService.checkMinUserOrgRole(result.user, orgInstance, 'INST_ADM')
        }

        if(result.editable)
        {
            orgInstance.addToOrgType(RefdataValue.get(params.orgType))
            orgInstance.save(flush: true)
            flash.message = message(code: 'default.updated.message', args: [message(code: 'org.label', default: 'Org'), orgInstance.name])
            redirect action: 'show', id: orgInstance.id
        }
    }
    def deleteOrgType()
    {
        def result = [:]
        result.user = User.get(springSecurityService.principal.id)
        def orgInstance = Org.get(params.org)

        if (!orgInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'org.label', default: 'Org'), params.id])
            redirect action: 'list'
            return
        }

        if ( SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN,ROLE_ORG_EDITOR') ) {
            result.editable = true
        }
        else {
            result.editable = accessService.checkMinUserOrgRole(result.user, orgInstance, 'INST_ADM')
        }

        if(result.editable)
        {
            orgInstance.removeFromOrgType(RefdataValue.get(params.removeOrgType))
            orgInstance.save(flush: true)
            flash.message = message(code: 'default.updated.message', args: [message(code: 'org.label', default: 'Org'), orgInstance.name])
            redirect action: 'show', id: orgInstance.id
        }
    }

    private Map setResultGenericsAndCheckAccess(params) {
        User user = User.get(springSecurityService.principal.id)
        Org org = contextService.org
        Map result = [user:user,institution:org,editable:accessService.checkMinUserOrgRole(user,org,'INST_EDITOR'),inContextOrg:true]
        if(params.id) {
            result.orgInstance = Org.get(params.id)
            result.inContextOrg = result.orgInstance.id == org.id
            //this is a flag to check whether the page has been called for a consortia or inner-organisation member
            Combo checkCombo = Combo.findByFromOrgAndToOrg(result.orgInstance,org)
            if(checkCombo) {
                if(checkCombo.type == COMBO_TYPE_CONSORTIUM)
                    result.institutionalView = true
                else if(checkCombo.type == COMBO_TYPE_DEPARTMENT)
                    result.departmentalView = true
            }
            //restrictions hold if viewed org is not the context org
            if(!result.inContextOrg && !accessService.checkPerm("ORG_CONSORTIUM") && !SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN, ROLE_ORG_EDITOR")) {
                //restrictions further concern only singe users, not consortia
                if(accessService.checkPerm("ORG_INST")) {
                    if(OrgSettings.get(result.orgInstance,OrgSettings.KEYS.CUSTOMER_TYPE)?.getValue() in ["ORG_INST","ORG_INST_COLLECTIVE"]) {
                        return null
                    }
                    else if(accessService.checkPerm("ORG_INST_COLLECTIVE") && !result.departmentalView) {
                        return null
                    }
                }
            }
        }
        result
    }

    @DebugAnnotation(perm="ORG_CONSORTIUM", type="Consortium", affil="INST_EDITOR", specRole="ROLE_ORG_EDITOR")
    @Secured(closure = { ctx.accessService.checkPermTypeAffiliationX("ORG_CONSORTIUM", "Consortium", "INST_EDITOR", "ROLE_ORG_EDITOR") })
    def toggleCombo() {
        Map result = setResultGenericsAndCheckAccess(params)
        if(!result) {
            response.sendError(401)
            return
        }
        if(!params.direction) {
            flash.error(message(code:'org.error.noToggleDirection'))
            response.sendError(404)
            return
        }
        switch(params.direction) {
            case 'add':
                Map map = [toOrg: result.institution,
                        fromOrg: Org.get(params.fromOrg),
                        type: RefdataValue.getByValueAndCategory('Consortium','Combo Type')]
                if (! Combo.findWhere(map)) {
                    def cmb = new Combo(map)
                    cmb.save()
                }
                break
            case 'remove':
                Combo cmb = Combo.findWhere(toOrg: result.institution,
                    fromOrg: Org.get(params.fromOrg),
                    type: RefdataValue.getByValueAndCategory('Consortium','Combo Type'))
                cmb.delete()
                break
        }
        redirect action: 'listInstitution'
    }
}
