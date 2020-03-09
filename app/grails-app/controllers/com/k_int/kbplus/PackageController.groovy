package com.k_int.kbplus

import com.k_int.kbplus.auth.Role
import com.k_int.kbplus.auth.User
import de.laser.DeletionService
import de.laser.controller.AbstractDebugController
import de.laser.helper.DateUtil
import de.laser.helper.DebugAnnotation
import de.laser.helper.RDConstants
import de.laser.helper.RDStore
import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import org.apache.poi.hssf.usermodel.HSSFRow
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent

import java.text.SimpleDateFormat

@Secured(['IS_AUTHENTICATED_FULLY'])
class PackageController extends AbstractDebugController {

    def springSecurityService
    def genericOIDService
    def ESSearchService
    def exportService
    def institutionsService
    def executorWrapperService
    def accessService
    def contextService
    def taskService
    def addressbookService
    def docstoreService
    def GOKbService
    def globalSourceSyncService
    def dataloadService
    def escapeService
    def filterService

    static allowedMethods = [create: ['GET', 'POST'], edit: ['GET', 'POST'], delete: 'POST']

    @Secured(['ROLE_USER'])
    def index() {

        Map<String, Object> result = [:]
        result.user = springSecurityService.getCurrentUser()
        params.max = params.max ?: result.user.getDefaultPageSizeTMP()

        if (springSecurityService.isLoggedIn()) {
            if (params.q == "") params.remove('q');

            if (params.search.equals("yes")) {
                //when searching make sure results start from first page
                params.offset = 0
                params.remove("search")
            }

            def old_q = params.q
            def old_sort = params.sort

            if (!ApiSource.findAllByTypAndActive(ApiSource.ApiTyp.GOKBAPI, true)) {
                redirect controller: 'package', action: 'list'
                return
            }

            def gokbRecords = []

            ApiSource.findAllByTypAndActive(ApiSource.ApiTyp.GOKBAPI, true).each { api ->
                gokbRecords << GOKbService.getPackagesMap(api, params.q, false).records
            }

            params.sort = params.sort ?: 'name'
            params.order = params.order ?: 'asc'

            result.records = gokbRecords ? gokbRecords.flatten().sort() : null

            result.records?.sort { x, y ->
                if (params.order == 'desc') {
                    y."${params.sort}".toString().compareToIgnoreCase x."${params.sort}".toString()
                } else {
                    x."${params.sort}".toString().compareToIgnoreCase y."${params.sort}".toString()
                }
            }

            result.resultsTotal2 = result.records?.size()

            Integer start = params.offset ? params.int('offset') : 0
            Integer end = params.offset ? params.int('max') + params.int('offset') : params.int('max')
            end = (end > result.records?.size()) ? result.records?.size() : end

            result.records = result.records?.subList(start, end)

            //Double-Quoted search strings wont display without this
            params.q = old_q?.replace("\"", "&quot;")

            if (!old_q) {
                params.remove('q')
            }
            if (!old_sort) {
                params.remove('sort')
            }
        }
        result
    }

    @Secured(['ROLE_USER'])
    def list() {
        Map<String, Object> result = [:]
        result.user = User.get(springSecurityService.principal.id)
        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();

        result.editable = true

        def paginate_after = params.paginate_after ?: ((2 * result.max) - 1)
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

        def deleted_package_status = RefdataValue.getByValueAndCategory('Deleted', RDConstants.PACKAGE_STATUS)
        //def qry_params = [deleted_package_status]
        def qry_params = []

        // TODO: filter by status in frontend
        // TODO: use elastic search
        String base_qry = " from Package as p where ( (p.packageStatus is null ) OR ( p.packageStatus is not null ) ) "
        //def base_qry = " from Package as p where ( (p.packageStatus is null ) OR ( p.packageStatus = ? ) ) "

        if (params.q?.length() > 0) {
            base_qry += " and ( ( lower(p.name) like ? ) or ( lower(p.identifier) like ? ) )"
            qry_params.add("%${params.q.trim().toLowerCase()}%");
            qry_params.add("%${params.q.trim().toLowerCase()}%");
        }

        if (params.updateStartDate?.length() > 0) {
            base_qry += " and ( p.lastUpdated > ? )"
            qry_params.add(params.date('updateStartDate', message(code: 'default.date.format.notime')));
        }

        if (params.updateEndDate?.length() > 0) {
            base_qry += " and ( p.lastUpdated < ? )"
            qry_params.add(params.date('updateEndDate', message(code: 'default.date.format.notime')));
        }

        if (params.createStartDate?.length() > 0) {
            base_qry += " and ( p.dateCreated > ? )"
            qry_params.add(params.date('createStartDate', message(code: 'default.date.format.notime')));
        }

        if (params.createEndDate?.length() > 0) {
            base_qry += " and ( p.dateCreated < ? )"
            qry_params.add(params.date('createEndDate', message(code: 'default.date.format.notime')));
        }

        if ((params.sort != null) && (params.sort.length() > 0)) {
            base_qry += " order by p.${params.sort} ${params.order}"
        } else {
            base_qry += " order by lower(p.name) asc"
        }


        log.debug(base_qry + ' <<< ' + qry_params)
        result.packageInstanceTotal = Subscription.executeQuery("select p.id " + base_qry, qry_params).size()


        withFormat {
            html {
                result.packageInstanceList = Subscription.executeQuery("select p ${base_qry}", qry_params, [max: result.max, offset: result.offset]);
                result
            }
            csv {
                response.setHeader("Content-disposition", "attachment; filename=\"packages.csv\"")
                response.contentType = "text/csv"
                def packages = Subscription.executeQuery("select p ${base_qry}", qry_params)
                def out = response.outputStream
                log.debug('colheads');
                out.withWriter { writer ->
                    writer.write('Package Name, Creation Date, Last Modified, Identifier\n');
                    packages.each {
                        log.debug(it);
                        writer.write("${it.name},${it.dateCreated},${it.lastUpdated},${it.identifier}\n")
                    }
                    writer.write("END");
                    writer.flush();
                    writer.close();
                }
                out.close()
            }
        }
    }

    @Deprecated
    @Secured(['ROLE_YODA'])
    def consortia() {
        redirect controller: 'package', action: 'show', params: params
        return

        Map<String, Object> result = [:]
        result.user = User.get(springSecurityService.principal.id)
        result.packageInstance = Package.get(params.id)
        result.editable = isEditable()
        result.id = params.id

        def hasAccess
        def isAdmin
        if (result.user.getAuthorities().contains(Role.findByAuthority('ROLE_ADMIN'))) {
            isAdmin = true;
        } else {
            hasAccess = result.packageInstance.orgs.find {
                it.roleType?.value == 'Package Consortia' && accessService.checkMinUserOrgRole(result.user, it.org, 'INST_ADM')
            }
        }

        if (!isAdmin && hasAccess == null) {
            flash.error = "Consortia screen only available to institution administrators for Packages with Package Consortium link."
            response.sendError(401)
            return
        }
        def packageInstance = result.packageInstance

        RefdataValue type = RefdataValue.getByValueAndCategory('Consortium', 'Combo Type')

        def institutions_in_consortia_hql = "select c.fromOrg from Combo as c where c.type = ? and c.toOrg in ( select org_role.org from Package as p join p.orgs as org_role where org_role.roleType.value = 'Package Consortia' and p = ?) order by c.fromOrg.name"
        def consortiaInstitutions = Combo.executeQuery(institutions_in_consortia_hql, [type, packageInstance])

        def package_consortia = "select org_role.org from Package as p join p.orgs as org_role where org_role.roleType.value = 'Package Consortia' and p = ?"
        def consortia = Package.executeQuery(package_consortia, [packageInstance]);


        def consortiaInstsWithStatus = [:]

        def hql = "SELECT role.org FROM OrgRole as role WHERE role.org = ? AND (role.roleType.value = 'Subscriber') AND ( EXISTS ( select sp from role.sub.packages as sp where sp.pkg = ? ) )"
        consortiaInstitutions.each { org ->
            log.debug("looking up all orgs based on consortia org ${org} and package ${packageInstance}");
            def queryParams = [org, packageInstance]
            def hasPackage = OrgRole.executeQuery(hql, queryParams)
            if (hasPackage) {
                consortiaInstsWithStatus.put(org, RefdataValue.getByValueAndCategory("Yes", RDConstants.Y_N_O))
            } else {
                consortiaInstsWithStatus.put(org, RefdataValue.getByValueAndCategory("No", RDConstants.Y_N_O))
            }
        }
        result.consortia = consortia
        result.consortiaInstsWithStatus = consortiaInstsWithStatus

        // log.debug("institutions with status are ${consortiaInstsWithStatus}")

        result
    }

    @Deprecated
    @Secured(['ROLE_YODA'])
    def generateSlaveSubscriptions() {
        params.each { p ->
            if (p.key.startsWith("_create.")) {
                def orgID = p.key.substring(8)
                Org orgaisation = Org.get(orgID)
                if (orgaisation)
                    log.debug("Create slave subscription for ${orgaisation.name}")
                createNewSubscription(orgaisation, params.id, params.genSubName);
            }
        }
        redirect controller: 'package', action: 'consortia', params: [id: params.id]
    }


    @Deprecated
    private def createNewSubscription(org, packageId, genSubName) {
        //Initialize default subscription values
        log.debug("Create slave with org ${org} and packageID ${packageId}")

        def defaultSubIdentifier = java.util.UUID.randomUUID().toString()
        def pkg_to_link = Package.get(packageId)
        log.debug("Sub start Date ${pkg_to_link.startDate} and end date ${pkg_to_link.endDate}")
        pkg_to_link.createSubscription("Subscription Taken", genSubName ?: "Slave subscription for ${pkg_to_link.name}", defaultSubIdentifier,
                pkg_to_link.startDate, pkg_to_link.endDate, org, "Subscriber", true, true)
    }

    @Secured(['ROLE_ADMIN'])
    def create() {
        User user = User.get(springSecurityService.principal.id)

        switch (request.method) {
            case 'GET':
                [packageInstance: new Package(params), user: user]
                break
            case 'POST':
                def providerName = params.contentProviderName
                def packageName = params.packageName
                def identifier = params.identifier

                def contentProvider = Org.findByName(providerName);
                def existing_pkg = Package.findByIdentifier(identifier);

                if (contentProvider && existing_pkg == null) {
                    log.debug("Create new package, content provider = ${contentProvider}, identifier is ${identifier}");
                    Package new_pkg = new Package(identifier: identifier,
                            contentProvider: contentProvider,
                            name: packageName,
                            impId: java.util.UUID.randomUUID().toString());
                    if (new_pkg.save(flush: true)) {
                        redirect action: 'edit', id: new_pkg.id
                    } else {
                        new_pkg.errors.each { e ->
                            log.error("Problem: ${e}");
                        }
                        render view: 'create', model: [packageInstance: new_pkg, user: user]
                    }
                } else {
                    render view: 'create', model: [packageInstance: packageInstance, user: user]
                    return
                }

                // flash.message = message(code: 'default.created.message', args: [message(code: 'package.label'), packageInstance.id])
                // redirect action: 'show', id: packageInstance.id
                break
        }
    }

    @DebugAnnotation(perm="ORG_INST,ORG_CONSORTIUM", affil="INST_USER")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliation("ORG_INST,ORG_CONSORTIUM", "INST_USER")
    })
    def compare() {
        Map<String, Object> result = [:]
        result.unionList = []

        result.user = User.get(springSecurityService.principal.id)
        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

        if (params.pkgA?.length() > 0 && params.pkgB?.length() > 0) {

            result.pkgInsts = []
            result.pkgDates = []
            def listA
            def listB
            try {
                listA = createCompareList(params.pkgA, params.dateA, params, result)
                listB = createCompareList(params.pkgB, params.dateB, params, result)
                if (!params.countA) {
                    def countHQL = "select count(elements(pkg.tipps)) from Package pkg where pkg.id = ?"
                    params.countA = Package.executeQuery(countHQL, [result.pkgInsts.get(0).id])
                    log.debug("countA is ${params.countA}")
                    params.countB = Package.executeQuery(countHQL, [result.pkgInsts.get(1).id])
                    log.debug("countB is ${params.countB}")
                }
            } catch (IllegalArgumentException e) {
                request.message = e.getMessage()
                return
            }

            def groupedA = listA.groupBy({ it.title.title })
            def groupedB = listB.groupBy({ it.title.title })

            def mapA = listA.collectEntries { [it.title.title, it] }
            def mapB = listB.collectEntries { [it.title.title, it] }

            result.listACount = [tipps: listA.size(), titles: mapA.size()]
            result.listBCount = [tipps: listB.size(), titles: mapB.size()]

            log.debug("mapA: ${mapA.size()}, mapB: ${mapB.size()}")

            def unionList = groupedA.keySet().plus(groupedB.keySet()).toList() // heySet is hashSet
            unionList = unionList.unique()
            unionList.sort()

            log.debug("UnionList has ${unionList.size()} entries.")

            def filterRules = [params.insrt ? true : false, params.dlt ? true : false, params.updt ? true : false, params.nochng ? true : false]

            result.unionListSize = institutionsService.generateComparisonMap(unionList, mapA, mapB, 0, unionList.size(), filterRules).size()

            withFormat {
                html {
                    def toIndex = result.offset + result.max < unionList.size() ? result.offset + result.max : unionList.size()
                    result.comparisonMap =
                            institutionsService.generateComparisonMap(unionList, groupedA, groupedB, result.offset, toIndex.intValue(), filterRules)
                    result
                }
                csv {
                    try {

                        def comparisonMap =
                                institutionsService.generateComparisonMap(unionList, mapA, mapB, 0, unionList.size(), filterRules)
                        log.debug("Create CSV Response")
                        SimpleDateFormat dateFormatter = DateUtil.getSDF_NoTime()
                        response.setHeader("Content-disposition", "attachment; filename=\"packageComparison.csv\"")
                        response.contentType = "text/csv"
                        def out = response.outputStream
                        out.withWriter { writer ->
                            writer.write("${result.pkgInsts[0].name} on ${params.dateA}, ${result.pkgInsts[1].name} on ${params.dateB}\n")
                            writer.write('Title, pISSN, eISSN, Start Date A, Start Date B, Start Volume A, Start Volume B, Start Issue A, Start Issue B, End Date A, End Date B, End Volume A,End  Volume B,End  Issue A,End  Issue B, Coverage Note A, Coverage Note B, ColorCode\n');
                            // log.debug("UnionList size is ${unionList.size}")
                            comparisonMap.each { title, values ->
                                def tippA = values[0]
                                def tippB = values[1]
                                def colorCode = values[2]
                                def pissn = tippA ? tippA.title.getIdentifierValue('issn') : tippB.title.getIdentifierValue('issn');
                                def eissn = tippA ? tippA.title.getIdentifierValue('eISSN') : tippB.title.getIdentifierValue('eISSN');

                                writer.write("\"${title}\",\"${pissn ?: ''}\",\"${eissn ?: ''}\",\"${formatDateOrNull(dateFormatter, tippA?.startDate)}\",\"${formatDateOrNull(dateFormatter, tippB?.startDate)}\",\"${tippA?.startVolume ?: ''}\",\"${tippB?.startVolume ?: ''}\",\"${tippA?.startIssue ?: ''}\",\"${tippB?.startIssue ?: ''}\",\"${formatDateOrNull(dateFormatter, tippA?.endDate)}\",\"${formatDateOrNull(dateFormatter, tippB?.endDate)}\",\"${tippA?.endVolume ?: ''}\",\"${tippB?.endVolume ?: ''}\",\"${tippA?.endIssue ?: ''}\",\"${tippB?.endIssue ?: ''}\",\"${tippA?.coverageNote ?: ''}\",\"${tippB?.coverageNote ?: ''}\",\"${colorCode}\"\n")
                            }
                            writer.write("END");
                            writer.flush();
                            writer.close();
                        }
                        out.close()

                    } catch (Exception e) {
                        log.error("An Exception was thrown here", e)
                    }
                }
            }

        } else {
            SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
            def currentDate = sdf?.format(new Date())
            params.dateA = currentDate
            params.dateB = currentDate
            params.insrt = "Y"
            params.dlt = "Y"
            params.updt = "Y"
            flash.message = message(code: 'package.compare.flash')
            result
        }

    }

    private def formatDateOrNull(formatter, date) {
        def result;
        if (date) {
            result = formatter.format(date)
        } else {
            result = ''
        }
        return result
    }

    private def createCompareList(pkg, dateStr, params, result) {
        def returnVals = [:]
        SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
        def date = dateStr ? sdf.parse(dateStr) : new Date()
        def packageId = pkg.substring(pkg.indexOf(":") + 1)

        def packageInstance = Package.get(packageId)

        if (date < packageInstance.startDate) {
            throw new IllegalArgumentException(
                    "${packageInstance.name} start date is ${sdf.format(packageInstance.startDate)}. " +
                            "Date to compare it on is ${sdf.format(date)}, this is before start date.")
        }
        if (packageInstance.endDate && date > packageInstance.endDate) {
            throw new IllegalArgumentException(
                    "${packageInstance.name} end date is ${sdf.format(packageInstance.endDate)}. " +
                            "Date to compare it on is ${sdf.format(date)}, this is after end date.")
        }

        result.pkgInsts.add(packageInstance)

        result.pkgDates.add(sdf.format(date))

        def queryParams = [packageInstance]

        def query = filterService.generateBasePackageQuery(params, queryParams, true, date, "Platform")
        def list = TitleInstancePackagePlatform.executeQuery("select tipp " + query.base_qry, query.qry_params)

        return list
    }

    @Secured(['ROLE_USER'])
    def show() {
        def verystarttime = exportService.printStart("Package show")

        Map<String, Object> result = [:]
        boolean showDeletedTipps = false

        //ask Daniel: downgrade to ROLE_DATAMANAGER?
        if (SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN,ROLE_PACKAGE_EDITOR')) {
            result.editable = true
            showDeletedTipps = true
        } else {
            result.editable = false
        }

        result.user = User.get(springSecurityService.principal.id)
        def packageInstance = Package.get(params.id)
        if (!packageInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'package.label'), params.id])
            redirect action: 'list'
            return
        }

        def pending_change_pending_status = RefdataValue.getByValueAndCategory('Pending', RDConstants.PENDING_CHANGE_STATUS)

        result.pendingChanges = PendingChange.executeQuery("select pc from PendingChange as pc where pc.pkg=? and ( pc.status is null or pc.status = ? ) order by ts, payload", [packageInstance, pending_change_pending_status]);

        log.debug("Package has ${result.pendingChanges?.size()} pending changes");

        result.pkg_link_str = "${grailsApplication.config.grails.serverURL}/package/show/${params.id}"

        // tasks
        def contextOrg = contextService.getOrg()
        result.tasks = taskService.getTasksByResponsiblesAndObject(User.get(springSecurityService.principal.id), contextOrg, packageInstance)
        def preCon = taskService.getPreconditionsWithoutTargets(contextOrg)
        result << preCon

        result.contextOrg = contextOrg

        result.modalPrsLinkRole = RefdataValue.getByValueAndCategory('Specific package editor', RDConstants.PERSON_RESPONSIBILITY)
        result.modalVisiblePersons = addressbookService.getPrivatePersonsByTenant(contextService.getOrg())

        // restrict visible for templates/links/orgLinksAsList
        result.visibleOrgs = packageInstance.orgs
        result.visibleOrgs.sort { it.org.sortname }

        List<RefdataValue> roleTypes = [RDStore.OR_SUBSCRIBER]
        if(accessService.checkPerm('ORG_CONSORTIUM')) {
            roleTypes.addAll([RDStore.OR_SUBSCRIPTION_CONSORTIA, RDStore.OR_SUBSCRIBER_CONS])
        }
        else if(accessService.checkPerm('ORG_INST_COLLECTIVE')) {
            roleTypes.addAll([RDStore.OR_SUBSCRIPTION_COLLECTIVE, RDStore.OR_SUBSCRIBER_COLLECTIVE])
        }

        result.subscriptionList = Subscription.executeQuery('select oo.sub from OrgRole oo where oo.org = :contextOrg and oo.roleType in :roleTypes and oo.sub.status = :current and not exists (select sp.subscription from SubscriptionPackage sp where sp.subscription = oo.sub)',
                [contextOrg: contextService.org, roleTypes: roleTypes, current: RDStore.SUBSCRIPTION_CURRENT])

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        params.max = result.max
        def paginate_after = params.paginate_after ?: ((2 * result.max) - 1);
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

        def limits = (!params.format || params.format.equals("html")) ? [max: result.max, offset: result.offset] : [offset: 0]

        // def base_qry = "from TitleInstancePackagePlatform as tipp where tipp.pkg = ? "
        def qry_params = [pkgInstance: packageInstance]

        SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
        def today = new Date()
        if (!params.asAt) {
            if (packageInstance.startDate > today) {
                params.asAt = sdf.format(packageInstance.startDate)
            } else if (packageInstance.endDate < today && packageInstance.endDate) {
                params.asAt = sdf.format(packageInstance.endDate)
            }
        }
        def date_filter
        if (params.mode == 'advanced') {
            date_filter = null
            params.asAt = null
        } else if (params.asAt && params.asAt.length() > 0) {
            date_filter = sdf.parse(params.asAt)
        } else {
            date_filter = today
        }

        def query = filterService.generateBasePackageQuery(params, qry_params, showDeletedTipps, date_filter,"Package")

        // log.debug("Base qry: ${base_qry}, params: ${qry_params}, result:${result}");
        List<TitleInstancePackagePlatform> titlesList = TitleInstancePackagePlatform.executeQuery("select tipp " + query.base_qry, query.qry_params, limits)
        //result.num_tipp_rows = TitleInstancePackagePlatform.executeQuery("select tipp.id " + base_qry, qry_params).size()
        result.unfiltered_num_tipp_rows = TitleInstancePackagePlatform.executeQuery(
                "select tipp.id from TitleInstancePackagePlatform as tipp where tipp.pkg = ?", [packageInstance]).size()

        result.lasttipp = result.offset + result.max > result.num_tipp_rows ? result.num_tipp_rows : result.offset + result.max;

        if (OrgSettings.get(contextOrg, OrgSettings.KEYS.NATSTAT_SERVER_REQUESTOR_ID) instanceof OrgSettings){
            result.statsWibid = contextOrg.getIdentifierByType('wibid')?.value
            result.usageMode = accessService.checkPerm("ORG_CONSORTIUM") ? 'package' : 'institution'
            result.packageIdentifier = packageInstance.getIdentifierByType('isil')?.value
        }

        result.packageInstance = packageInstance
        if (executorWrapperService.hasRunningProcess(packageInstance)) {
            result.processingpc = true
        }

        def filename = "${escapeService.escapeString(result.packageInstance.name)}_asAt_${date_filter ? sdf.format(date_filter) : sdf.format(today)}"
        withFormat {
            html result
            json {
                def map = exportService.getPackageMap(packageInstance, result.titlesList)

                def json = map as JSON

                response.setHeader("Content-disposition", "attachment; filename=\"${filename}.json\"")
                response.contentType = "application/json"
                render json
            }
            xml {
                def starttime = exportService.printStart("Building XML Doc")
                def doc = exportService.buildDocXML("Packages")
                exportService.addPackageIntoXML(doc, doc.getDocumentElement(), packageInstance, result.titlesList)
                exportService.printDuration(starttime, "Building XML Doc")

                response.setHeader("Content-disposition", "attachment; filename=\"${filename}.xml\"")
                response.contentType = "text/xml"
                exportService.streamOutXML(doc, response.outputStream)
            }
        }
    }

    @Secured(['ROLE_USER'])
    def current() {
        log.debug("current ${params}");
        Map<String, Object> result = [:]
        boolean showDeletedTipps = false
        result.user = User.get(springSecurityService.principal.id)
        result.editable = isEditable()

        def packageInstance = Package.get(params.id)
        if (!packageInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'package.label'), params.id])
            redirect action: 'list'
            return
        }
        result.packageInstance = packageInstance

        if (executorWrapperService.hasRunningProcess(packageInstance)) {
            result.processingpc = true
        }

        def pending_change_pending_status = RefdataValue.getByValueAndCategory('Pending', RDConstants.PENDING_CHANGE_STATUS)

        result.pendingChanges = PendingChange.executeQuery("select pc from PendingChange as pc where pc.pkg=? and ( pc.status is null or pc.status = ? ) order by ts, payload", [packageInstance, pending_change_pending_status]);

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP().intValue()
        params.max = result.max
        def paginate_after = params.paginate_after ?: ((2 * result.max) - 1);
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

        // def base_qry = "from TitleInstancePackagePlatform as tipp where tipp.pkg = ? "
        def qry_params = [pkgInstance: packageInstance]
        def date_filter = params.mode == 'advanced' ? null : new Date()

        def query = filterService.generateBasePackageQuery(params, qry_params, showDeletedTipps, date_filter,"Package")

        List<TitleInstancePackagePlatform> titlesList = TitleInstancePackagePlatform.executeQuery("select tipp "+query.base_qry, query.qry_params)
        result.titlesList = titlesList.drop(result.offset).take(result.max)
        result.num_tipp_rows = titlesList.size()

        result.lasttipp = result.offset + result.max > result.num_tipp_rows ? result.num_tipp_rows : result.offset + result.max


        result
    }

    @Secured(['ROLE_USER'])
    def deleteDocuments() {
        def ctxlist = []

        log.debug("deleteDocuments ${params}");

        docstoreService.unifiedDeleteDocuments(params)

        redirect controller: 'package', action: params.redirectAction, id: params.instanceId
    }


    @Secured(['ROLE_USER'])
    def documents() {
        Map<String, Object> result = [:]
        result.user = User.get(springSecurityService.principal.id)
        result.institution = contextService.org
        result.packageInstance = Package.get(params.id)
        result.editable = isEditable()

        result
    }

    @Secured(['ROLE_USER'])
    def expected() {
        previous_expected(params, "expected")
    }

    @Secured(['ROLE_USER'])
    def previous() {
        previous_expected(params, "previous")
    }

    @Secured(['ROLE_USER'])
    def previous_expected(params, func) {
        log.debug("previous_expected ${params}");
        Map<String, Object> result = [:]
        boolean showDeletedTipps = false
        result.user = User.get(springSecurityService.principal.id)
        result.editable = isEditable()
        def packageInstance = Package.get(params.id)
        if (!packageInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'package.label'), params.id])
            redirect action: 'list'
            return
        }
        result.packageInstance = packageInstance

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP()
        params.max = result.max
        def paginate_after = params.paginate_after ?: ((2 * result.max) - 1);
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

        def limits = (!params.format || params.format.equals("html")) ? [max: result.max, offset: result.offset] : [offset: 0]

        def qry_params = [packageInstance, RDStore.TIPP_STATUS_DELETED]

        String base_qry = "from TitleInstancePackagePlatform as tipp where tipp.pkg = ? and tipp.status != ? "

        if (func == "expected") {
            base_qry += " and ( coalesce(tipp.accessStartDate, tipp.pkg.startDate) >= ? ) "
        }
        else {
            base_qry += " and ( tipp.accessEndDate <= ? ) "
        }
        qry_params.add(new Date());


        base_qry += " order by ${params.sort ?: 'tipp.title.sortTitle'} ${params.order ?: 'asc'} "

    log.debug("Base qry: ${base_qry}, params: ${qry_params}, result:${result}");
    result.titlesList = TitleInstancePackagePlatform.executeQuery("select tipp "+base_qry, qry_params, limits);
    result.num_tipp_rows = TitleInstancePackagePlatform.executeQuery("select tipp.id " + base_qry, qry_params ).size()

        result.lasttipp = result.offset + result.max > result.num_tipp_rows ? result.num_tipp_rows : result.offset + result.max;

        result
    }

    @Secured(['ROLE_ADMIN'])
    def uploadTitles() {
        def pkg = Package.get(params.id)
        def upload_mime_type = request.getFile("titleFile")?.contentType
        log.debug("Uploaded content type: ${upload_mime_type}");
        def input_stream = request.getFile("titleFile")?.inputStream

        if (upload_mime_type == 'application/vnd.ms-excel') {
            attemptXLSLoad(pkg, input_stream);
        } else {
            attemptCSVLoad(pkg, input_stream);
        }

        redirect action: 'show', id: params.id
    }

    private def attemptXLSLoad(pkg, stream) {
        log.debug("attemptXLSLoad");
        HSSFWorkbook wb = new HSSFWorkbook(stream);
        HSSFSheet hssfSheet = wb.getSheetAt(0);

        attemptv1XLSLoad(pkg, hssfSheet);
    }

    private def attemptCSVLoad(pkg, stream) {
        log.debug("attemptCSVLoad");
        attemptv1CSVLoad(pkg, stream);
    }

    private def attemptv1XLSLoad(pkg, hssfSheet) {

        log.debug("attemptv1XLSLoad");
        def extracted = [:]
        extracted.rows = []

        int row_counter = 0;
        Iterator rowIterator = hssfSheet.rowIterator();
        while (rowIterator.hasNext()) {
            HSSFRow hssfRow = (HSSFRow) rowIterator.next();
            switch (row_counter++) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    // Record header row
                    log.debug("Header");
                    hssfRow.cellIterator().each { c ->
                        log.debug("Col: ${c.toString()}");
                    }
                    break;
                default:
                    // A real data row
                    def row_info = [
                            issn                    : hssfRow.getCell(0)?.toString(),
                            eissn                   : hssfRow.getCell(1)?.toString(),
                            date_first_issue_online : hssfRow.getCell(2)?.toString(),
                            num_first_volume_online : hssfRow.getCell(3)?.toString(),
                            num_first_issue_online  : hssfRow.getCell(4)?.toString(),
                            date_last_issue_online  : hssfRow.getCell(5)?.toString(),
                            date_first_volume_online: hssfRow.getCell(6)?.toString(),
                            date_first_issue_online : hssfRow.getCell(7)?.toString(),
                            embargo                 : hssfRow.getCell(8)?.toString(),
                            coverageDepth           : hssfRow.getCell(9)?.toString(),
                            coverageNote            : hssfRow.getCell(10)?.toString(),
                            platformUrl             : hssfRow.getCell(11)?.toString()
                    ]

                    extracted.rows.add(row_info);
                    log.debug("datarow: ${row_info}");
                    break;
            }
        }

        processExractedData(pkg, extracted);
    }

    private def attemptv1CSVLoad(pkg, stream) {
        log.debug("attemptv1CSVLoad");
        def extracted = [:]
        processExractedData(pkg, extracted);
    }

    private def processExractedData(pkg, extracted_data) {
        log.debug("processExractedData...");
        List old_title_list = [[title: [id: 667]], [title: [id: 553]], [title: [id: 19]]]
        List new_title_list = [[title: [id: 19]], [title: [id: 554]], [title: [id: 667]]]

        reconcile(old_title_list, new_title_list);
    }

    private def reconcile(old_title_list, new_title_list) {
        def title_list_comparator = new com.k_int.kbplus.utils.TitleComparator()
        Collections.sort(old_title_list, title_list_comparator)
        Collections.sort(new_title_list, title_list_comparator)

        Iterator i1 = old_title_list.iterator()
        Iterator i2 = new_title_list.iterator()

        def current_old_title = i1.hasNext() ? i1.next() : null;
        def current_new_title = i2.hasNext() ? i2.next() : null;

        while (current_old_title || current_new_title) {
            if (current_old_title == null) {
                // We have exhausted all old titles. Everything in the new title list must be newly added
                log.debug("Title added: ${current_new_title.title.id}");
                current_new_title = i2.hasNext() ? i2.next() : null;
            } else if (current_new_title == null) {
                // We have exhausted new old titles. Everything remaining in the old titles list must have been removed
                log.debug("Title removed: ${current_old_title.title.id}");
                current_old_title = i1.hasNext() ? i1.next() : null;
            } else {
                // Work out whats changed
                if (current_old_title.title.id == current_new_title.title.id) {
                    // This title appears in both old and new lists, it may be an updated
                    log.debug("title ${current_old_title.title.id} appears in both lists - possible update / unchanged");
                    current_old_title = i1.hasNext() ? i1.next() : null;
                    current_new_title = i2.hasNext() ? i2.next() : null;
                } else {
                    if (current_old_title.title.id > current_new_title.title.id) {
                        // The current old title id is greater than the current new title. This means that a new title must
                        // have been introduced into the new list with a lower title id than the one on the current list.
                        // hence, current_new_title.title.id is a new record. Consume it and move forwards.
                        log.debug("Title added: ${current_new_title.title.id}");
                        current_new_title = i2.hasNext() ? i2.next() : null;
                    } else {
                        // The current old title is less than the current new title. This indicates that the current_old_title
                        // must have been removed in the new list. Process it as a removal and continue.
                        log.debug("Title removed: ${current_old_title.title.id}");
                        current_old_title = i1.hasNext() ? i1.next() : null;
                    }
                }
            }
        }
    }

    def isEditable() {
        if (SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN, ROLE_PACKAGE_EDITOR')) {
            return true
        } else {
            return false
        }
    }

    @DebugAnnotation(test = 'hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def addToSub() {
        def pkg = Package.get(params.id)
        def sub = Subscription.get(params.subid)
        boolean add_entitlements = params.addEntitlements == 'true'
        GlobalRecordInfo gri = GlobalRecordInfo.findByUuid(pkg.gokbId)
        GlobalRecordTracker grt = GlobalRecordTracker.findByOwner(gri)
        executorWrapperService.processClosure({
            globalSourceSyncService.initialiseTracker(grt)
            dataloadService.updateFTIndexes()
            println "Sync done, adding package to subscription ${sub}, with entitlements?: ${add_entitlements}"
            pkg.addToSubscription(sub, add_entitlements)
        },pkg)
        if(add_entitlements) {
            flash.message = message(code:'subscription.details.link.processingWithEntitlements')
            redirect controller: 'subscription', action: 'index', id: params.subid
        }
        else {
            flash.message = message(code:'subscription.details.link.processingWithoutEntitlements')
            redirect controller: 'subscription', action: 'addEntitlements', id: params.subid
        }
    }


    @Secured(['ROLE_USER'])
    def notes() {
        Map<String, Object> result = [:]
        result.user = User.get(springSecurityService.principal.id)
        result.packageInstance = Package.get(params.id)
        result.editable = isEditable()
        result
    }

    @Secured(['ROLE_USER'])
    def tasks() {
        Map<String, Object> result = [:]
        result.user = User.get(springSecurityService.principal.id)
        result.packageInstance = Package.get(params.id)
        result.editable = isEditable()

        if (params.deleteId) {
            def dTask = Task.get(params.deleteId)
            if (dTask && dTask.creator.id == result.user.id) {
                try {
                    flash.message = message(code: 'default.deleted.message', args: [message(code: 'task.label'), dTask.title])
                    dTask.delete(flush: true)
                }
                catch (Exception e) {
                    flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'task.label'), params.deleteId])
                }
            }
        }

        int offset = params.offset ? Integer.parseInt(params.offset) : 0
        result.taskInstanceList = taskService.getTasksByResponsiblesAndObject(result.user, contextService.getOrg(), result.packageInstance)
        result.taskInstanceCount = result.taskInstanceList?.size()
        result.taskInstanceList = taskService.chopOffForPageSize(result.taskInstanceList, result.user, offset)

        result.myTaskInstanceList = taskService.getTasksByCreatorAndObject(result.user,  result.packageInstance)
        result.myTaskInstanceCount = result.myTaskInstanceList?.size()
        result.myTaskInstanceList = taskService.chopOffForPageSize(result.myTaskInstanceList, result.user, offset)

        log.debug(result.taskInstanceList)

        result
    }

    @Secured(['ROLE_USER'])
    def packageBatchUpdate() {

        def packageInstance = Package.get(params.id)
        boolean showDeletedTipps = false

        if (SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN, ROLE_PACKAGE_EDITOR')) {
            showDeletedTipps = true
        }

        log.debug("packageBatchUpdate ${params}");

        SimpleDateFormat formatter = DateUtil.getSDF_NoTime()

        def bulk_fields = [
                [formProp: 'start_date', domainClassProp: 'startDate', type: 'date'],
                [formProp: 'start_volume', domainClassProp: 'startVolume'],
                [formProp: 'start_issue', domainClassProp: 'startIssue'],
                [formProp: 'end_date', domainClassProp: 'endDate', type: 'date'],
                [formProp: 'end_volume', domainClassProp: 'endVolume'],
                [formProp: 'end_issue', domainClassProp: 'endIssue'],
                [formProp: 'coverage_depth', domainClassProp: 'coverageDepth'],
                [formProp: 'coverage_note', domainClassProp: 'coverageNote'],
                [formProp: 'embargo', domainClassProp: 'embargo'],
                [formProp: 'delayedOA', domainClassProp: 'delayedOA', type: 'ref'],
                [formProp: 'hybridOA', domainClassProp: 'hybridOA', type: 'ref'],
                [formProp: 'payment', domainClassProp: 'payment', type: 'ref'],
                [formProp: 'hostPlatformURL', domainClassProp: 'hostPlatformURL'],
        ]


        if (params.BatchSelectedBtn == 'on') {
            log.debug("Apply batch changes - selected")
            params.filter = null //remove filters
            params.coverageNoteFilter = null
            params.startsBefore = null
            params.endsAfter = null
            params.each { p ->
                if (p.key.startsWith('_bulkflag.') && (p.value == 'on')) {
                    def tipp_id_to_edit = p.key.substring(10);
                    log.debug("row selected for bulk edit: ${tipp_id_to_edit}");
                    def tipp_to_bulk_edit = TitleInstancePackagePlatform.get(tipp_id_to_edit);
                    boolean changed = false

                    if (params.bulkOperation == 'edit') {
                        bulk_fields.each { bulk_field_defn ->
                            if (params["clear_${bulk_field_defn.formProp}"] == 'on') {
                                log.debug("Request to clear field ${bulk_field_defn.formProp}");
                                tipp_to_bulk_edit[bulk_field_defn.domainClassProp] = null
                                changed = true
                            } else {
                                def proposed_value = params['bulk_' + bulk_field_defn.formProp]
                                if ((proposed_value != null) && (proposed_value.length() > 0)) {
                                    log.debug("Set field ${bulk_field_defn.formProp} to ${proposed_value}");
                                    if (bulk_field_defn.type == 'date') {
                                        tipp_to_bulk_edit[bulk_field_defn.domainClassProp] = formatter.parse(proposed_value)
                                    } else if (bulk_field_defn.type == 'ref') {
                                        tipp_to_bulk_edit[bulk_field_defn.domainClassProp] = genericOIDService.resolveOID(proposed_value)
                                    } else {
                                        tipp_to_bulk_edit[bulk_field_defn.domainClassProp] = proposed_value
                                    }
                                    changed = true
                                }
                            }
                        }
                        if (changed)
                            tipp_to_bulk_edit.save();
                    } else {
                        log.debug("Bulk removal ${tipp_to_bulk_edit.id}");
                        tipp_to_bulk_edit.status = RefdataValue.getByValueAndCategory('Deleted', RDConstants.TIPP_STATUS)
                        tipp_to_bulk_edit.save();
                    }
                }
            }
        } else if (params.BatchAllBtn == 'on') {
            log.debug("Batch process all filtered by: " + params.filter);
            def qry_params = [pkgInstance: packageInstance]
            def query = filterService.generateBasePackageQuery(params, qry_params, showDeletedTipps, new Date(),"Package")
            def tipplist = TitleInstancePackagePlatform.executeQuery("select tipp " + query.base_qry, query.qry_params)
            tipplist.each { tipp_to_bulk_edit ->
                boolean changed = false
                log.debug("update tipp ${tipp_to_bulk_edit.id}");
                if (params.bulkOperation == 'edit') {
                    bulk_fields.each { bulk_field_defn ->
                        if (params["clear_${bulk_field_defn.formProp}"] == 'on') {
                            log.debug("Request to clear field ${bulk_field_defn.formProp}");
                            tipp_to_bulk_edit[bulk_field_defn.domainClassProp] = null
                            changed = true
                        } else {
                            def proposed_value = params['bulk_' + bulk_field_defn.formProp]
                            if ((proposed_value != null) && (proposed_value.length() > 0)) {
                                log.debug("Set field ${bulk_field_defn.formProp} to proposed_value");
                                if (bulk_field_defn.type == 'date') {
                                    tipp_to_bulk_edit[bulk_field_defn.domainClassProp] = formatter.parse(proposed_value)
                                } else if (bulk_field_defn.type == 'ref') {
                                    tipp_to_bulk_edit[bulk_field_defn.domainClassProp] = genericOIDService.resolveOID(proposed_value)
                                } else {
                                    tipp_to_bulk_edit[bulk_field_defn.domainClassProp] = proposed_value
                                }
                                changed = true
                            }
                        }
                    }
                    if (changed)
                        tipp_to_bulk_edit.save();
                }
            }
        }

        redirect(action: 'show', params: [id: params.id, sort: params.sort, order: params.order, max: params.max, offset: params.offset]);
    }

    @Secured(['ROLE_USER'])
    def history() {
        Map<String, Object> result = [:]
        def exporting = params.format == 'csv' ? true : false

        if (exporting) {
            result.max = 9999999
            params.max = 9999999
            result.offset = 0
        } else {
            User user = User.get(springSecurityService.principal.id)
            result.max = params.max ? Integer.parseInt(params.max) : user.getDefaultPageSizeTMP()
            params.max = result.max
            result.offset = params.offset ? Integer.parseInt(params.offset) : 0;
        }

    result.packageInstance = Package.get(params.id)
    result.editable=isEditable()

      def limits = (!params.format||params.format.equals("html"))?[max:result.max, offset:result.offset]:[offset:0]

        // postgresql migration
        String subQuery = 'select cast(id as string) from TitleInstancePackagePlatform as tipp where tipp.pkg = cast(:pkgid as int)'
        def subQueryResult = AuditLogEvent.executeQuery(subQuery, [pkgid: params.id])

        //def base_query = 'from org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent as e where ( e.className = :pkgcls and e.persistedObjectId = cast(:pkgid as string)) or ( e.className = :tippcls and e.persistedObjectId in ( select id from TitleInstancePackagePlatform as tipp where tipp.pkg = :pkgid ) )'
        //def query_params = [ pkgcls:'com.k_int.kbplus.Package', tippcls:'com.k_int.kbplus.TitleInstancePackagePlatform', pkgid:params.id, subQueryResult:subQueryResult]

        String base_query   = 'from org.codehaus.groovy.grails.plugins.orm.auditable.AuditLogEvent as e where ( e.className = :pkgcls and e.persistedObjectId = cast(:pkgid as string))'
        def query_params = [ pkgcls:'com.k_int.kbplus.Package', pkgid:params.id]

      // postgresql migration
        if (subQueryResult) {
            base_query += ' or ( e.className = :tippcls and e.persistedObjectId in (:subQueryResult) )'
            query_params.'tippcls' = 'com.k_int.kbplus.TitleInstancePackagePlatform'
            query_params.'subQueryResult' = subQueryResult
        }


        log.debug("base_query: ${base_query}, params:${query_params}, limits:${limits}");

    result.historyLines = AuditLogEvent.executeQuery('select e ' + base_query + ' order by e.lastUpdated desc', query_params, limits);
    result.num_hl = AuditLogEvent.executeQuery('select e.id '+ base_query, query_params).size()
    result.formattedHistoryLines = []


        result.historyLines.each { hl ->

            def line_to_add = [:]
            def linetype = null

            switch (hl.className) {
                case 'com.k_int.kbplus.Package':
                    def package_object = Package.get(hl.persistedObjectId);
                    line_to_add = [link        : createLink(controller: 'package', action: 'show', id: hl.persistedObjectId),
                                   name        : package_object.toString(),
                                   lastUpdated : hl.lastUpdated,
                                   propertyName: hl.propertyName,
                                   actor       : User.findByUsername(hl.actor),
                                   oldValue    : hl.oldValue,
                                   newValue    : hl.newValue
                    ]
                    linetype = 'Package'
                    break;
                case 'com.k_int.kbplus.TitleInstancePackagePlatform':
                    def tipp_object = TitleInstancePackagePlatform.get(hl.persistedObjectId);
                    if (tipp_object != null) {
                        line_to_add = [link        : createLink(controller: 'tipp', action: 'show', id: hl.persistedObjectId),
                                       name        : tipp_object.title?.title + " / " + tipp_object.pkg?.name,
                                       lastUpdated : hl.lastUpdated,
                                       propertyName: hl.propertyName,
                                       actor       : User.findByUsername(hl.actor),
                                       oldValue    : hl.oldValue,
                                       newValue    : hl.newValue
                        ]
                        linetype = 'TIPP'
                    } else {
                        log.debug("Cleaning up history line that relates to a deleted item");
                        hl.delete();
                    }
            }
            switch (hl.eventName) {
                case 'INSERT':
                    line_to_add.eventName = "New ${linetype}"
                    break;
                case 'UPDATE':
                    line_to_add.eventName = "Updated ${linetype}"
                    break;
                case 'DELETE':
                    line_to_add.eventName = "Deleted ${linetype}"
                    break;
                default:
                    line_to_add.eventName = "Unknown ${linetype}"
                    break;
            }
            result.formattedHistoryLines.add(line_to_add);
        }

        result
    }

    //for that no accidental call may occur ... ROLE_YODA is correct!
    @Secured(['ROLE_YODA'])
    Map getDuplicatePackages() {
        List<Package> pkgDuplicates = Package.executeQuery('select pkg from Package pkg where pkg.gokbId in (select p.gokbId from Package p group by p.gokbId having count(p.gokbId) > 1)')
        Map<String,List<Package>> result = [pkgDuplicates: pkgDuplicates]
        if(pkgDuplicates) {
            List<Package> pkgDupsWithTipps = Package.executeQuery('select distinct(tipp.pkg) from TitleInstancePackagePlatform tipp where tipp.pkg in (:pkg) and tipp.status != :deleted',[pkg:pkgDuplicates,deleted:RDStore.TIPP_STATUS_DELETED])
            List<Package> pkgDupsWithoutTipps = []
            pkgDuplicates.each { pkg ->
                if(!pkgDupsWithTipps.contains(pkg))
                    pkgDupsWithoutTipps << pkg
            }
            result.pkgDupsWithTipps = pkgDupsWithTipps
            result.pkgDupsWithoutTipps = pkgDupsWithoutTipps
        }
        result
    }

    @Secured(['ROLE_YODA'])
    def purgeDuplicatePackages() {
        List<Long> toDelete = (List<Long>) JSON.parse(params.toDelete)
        if(params.doIt == "true") {
            toDelete.each { pkgId ->
                Package pkg = Package.get(pkgId)
                DeletionService.deletePackage(pkg)
            }
            redirect action: 'index'
        }
        else {
            flash.message = "Betroffene Paket-IDs wären gelöscht worden: ${toDelete.join(", ")}"
            redirect action: 'getDuplicatePackages'
        }
    }
}
