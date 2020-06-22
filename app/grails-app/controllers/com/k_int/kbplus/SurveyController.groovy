package com.k_int.kbplus

import com.k_int.kbplus.abstract_domain.AbstractPropertyWithCalculatedLastUpdated
import com.k_int.kbplus.auth.User
import com.k_int.properties.PropertyDefinition
import de.laser.AccessService
import de.laser.AuditConfig
import de.laser.ComparisonService
import de.laser.ContextService
import de.laser.EscapeService
import de.laser.FilterService
import de.laser.OrgTypeService
import de.laser.PropertyService
import de.laser.SubscriptionService
import de.laser.SubscriptionsQueryService
import de.laser.SurveyService
import de.laser.SurveyUpdateService
import de.laser.TaskService
import de.laser.batch.SurveyUpdateJob
import de.laser.domain.IssueEntitlementGroup
import de.laser.domain.IssueEntitlementGroupItem
import de.laser.domain.PendingChangeConfiguration
import de.laser.helper.DateUtil
import de.laser.helper.DebugAnnotation
import de.laser.helper.RDConstants
import de.laser.helper.RDStore
import de.laser.interfaces.ShareSupport
import de.laser.interfaces.CalculatedType
import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured
import groovy.time.TimeCategory
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.dao.DataIntegrityViolationException

import javax.servlet.ServletOutputStream
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat


@Secured(['IS_AUTHENTICATED_FULLY'])
class SurveyController {

    SpringSecurityService springSecurityService
    AccessService accessService
    ContextService contextService
    SubscriptionsQueryService subscriptionsQueryService
    FilterService filterService
    DocstoreService docstoreService
    OrgTypeService orgTypeService
    GenericOIDService genericOIDService
    SurveyService surveyService
    FinanceService financeService
    ExportService exportService
    TaskService taskService
    SubscriptionService subscriptionService
    ComparisonService comparisonService
    SurveyUpdateService surveyUpdateService
    EscapeService escapeService
    InstitutionsService institutionsService
    PropertyService propertyService

    public static final String WORKFLOW_DATES_OWNER_RELATIONS = '1'
    public static final String WORKFLOW_PACKAGES_ENTITLEMENTS = '5'
    public static final String WORKFLOW_DOCS_ANNOUNCEMENT_TASKS = '2'
    public static final String WORKFLOW_SUBSCRIBER = '3'
    public static final String WORKFLOW_PROPERTIES = '4'
    public static final String WORKFLOW_END = '6'

    def possible_date_formats = [
            new SimpleDateFormat('yyyy/MM/dd'),
            new SimpleDateFormat('dd.MM.yyyy'),
            new SimpleDateFormat('dd/MM/yyyy'),
            new SimpleDateFormat('dd/MM/yy'),
            new SimpleDateFormat('yyyy/MM'),
            new SimpleDateFormat('yyyy')
    ]


    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    Map<String, Object> currentSurveysConsortia() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

        params.max = result.max
        params.offset = result.offset
        params.filterStatus = params.filterStatus ?: ((params.size() > 4) ? "" : [RDStore.SURVEY_SURVEY_STARTED.id.toString(), RDStore.SURVEY_READY.id.toString(), RDStore.SURVEY_IN_PROCESSING.id.toString()])

        List orgIds = orgTypeService.getCurrentOrgIdsOfProvidersAndAgencies( contextService.org )

        result.providers = Org.findAllByIdInList(orgIds).sort { it.name }

        result.subscriptions = Subscription.executeQuery("select DISTINCT s.name from Subscription as s where ( exists ( select o from s.orgRelations as o where ( o.roleType = :roleType AND o.org = :activeInst ) ) ) " +
                " AND s.instanceOf is not null order by s.name asc ", ['roleType': RDStore.OR_SUBSCRIPTION_CONSORTIA, 'activeInst': result.institution])

        DateFormat sdFormat = DateUtil.getSDF_NoTime()
        def fsq = filterService.getSurveyConfigQueryConsortia(params, sdFormat, result.institution)

        result.surveys = SurveyInfo.executeQuery(fsq.query, fsq.queryParams, params)

        if ( params.exportXLSX ) {

            SXSSFWorkbook wb
            if ( params.surveyCostItems ) {
                SimpleDateFormat sdf = DateUtil.getSDF_NoTimeNoPoint()
                String datetoday = sdf.format(new Date(System.currentTimeMillis()))
                String filename = "${datetoday}_" + g.message(code: "surveyCostItems.label")
                //if(wb instanceof XSSFWorkbook) file += "x";
                response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
                response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                wb = (SXSSFWorkbook) surveyService.exportSurveyCostItems(result.surveys.collect {it[1]}, result.institution)
            }else{
                SimpleDateFormat sdf = DateUtil.getSDF_NoTimeNoPoint()
                String datetoday = sdf.format(new Date(System.currentTimeMillis()))
                String filename = "${datetoday}_" + g.message(code: "survey.plural")
                //if(wb instanceof XSSFWorkbook) file += "x";
                response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
                response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                wb = (SXSSFWorkbook) surveyService.exportSurveys(result.surveys.collect {it[1]}, result.institution)
            }

            return
        }else {
            result.surveysCount = SurveyInfo.executeQuery(fsq.query, fsq.queryParams).size()
            result.filterSet = params.filterSet ? true : false

            result
        }
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    def workflowsSurveysConsortia() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

        params.max = result.max
        params.offset = result.offset

        params.tab = params.tab ?: 'created'

        result.providers = orgTypeService.getCurrentOrgsOfProvidersAndAgencies( contextService.org )

        result.subscriptions = Subscription.executeQuery("select DISTINCT s.name from Subscription as s where ( exists ( select o from s.orgRelations as o where ( o.roleType = :roleType AND o.org = :activeInst ) ) ) " +
                " AND s.instanceOf is not null order by s.name asc ", ['roleType': RDStore.OR_SUBSCRIPTION_CONSORTIA, 'activeInst': result.institution])


        DateFormat sdFormat = DateUtil.getSDF_NoTime()
        def fsq = filterService.getSurveyConfigQueryConsortia(params, sdFormat, result.institution)

        result.surveys = SurveyInfo.executeQuery(fsq.query, fsq.queryParams, params)

        if ( params.exportXLSX ) {
            SimpleDateFormat sdf = DateUtil.getSDF_NoTimeNoPoint()
            String datetoday = sdf.format(new Date(System.currentTimeMillis()))
            String filename = "${datetoday}_" + g.message(code: "survey.plural")
            //if(wb instanceof XSSFWorkbook) file += "x";
            response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
            response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            SXSSFWorkbook wb = (SXSSFWorkbook) surveyService.exportSurveys(SurveyConfig.findAllByIdInList(result.surveys.collect {it[1].id}), result.institution)
            wb.write(response.outputStream)
            response.outputStream.flush()
            response.outputStream.close()
            wb.dispose()

            return
        }else {
            result.surveysCount = SurveyInfo.executeQuery(fsq.query, fsq.queryParams).size()
            result.countSurveyConfigs = getSurveyConfigCounts()

            result.filterSet = params.filterSet ? true : false

            result
        }

    }


    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def createGeneralSurvey() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        result
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processCreateGeneralSurvey() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
        Date startDate = params.startDate ? sdf.parse(params.startDate) : null
        Date endDate = params.endDate ? sdf.parse(params.endDate) : null

        if(startDate != null && endDate != null) {
            if(startDate > endDate) {
                flash.error = g.message(code: "createSurvey.create.fail.startDateAndEndDate")
                redirect(action: 'createGeneralSurvey', params: params)
                return
            }
        }

        SurveyInfo surveyInfo = new SurveyInfo(
                name: params.name,
                startDate: startDate,
                endDate: endDate,
                type: RDStore.SURVEY_TYPE_INTEREST,
                owner: contextService.getOrg(),
                status: RDStore.SURVEY_IN_PROCESSING,
                comment: params.comment ?: null,
                isSubscriptionSurvey: false,
                isMandatory: params.mandatory ?: false
        )

        if (!(surveyInfo.save(flush: true))) {
            flash.error = g.message(code: "createGeneralSurvey.create.fail")
            redirect(action: 'createGeneralSurvey', params: params)
            return
        }

        if (!SurveyConfig.findAllBySurveyInfo(surveyInfo)) {
            SurveyConfig surveyConfig = new SurveyConfig(
                    type: 'GeneralSurvey',
                    surveyInfo: surveyInfo,
                    configOrder: 1
            )

            if(!(surveyConfig.save(flush: true))){
                surveyInfo.delete(flush: true)
                flash.error = g.message(code: "createGeneralSurvey.create.fail")
                redirect(action: 'createGeneralSurvey', params: params)
                return
            }

        }

        //flash.message = g.message(code: "createGeneralSurvey.create.successfull")
        redirect action: 'show', id: surveyInfo.id

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def createSubscriptionSurvey() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0

        def date_restriction = null;
        def sdf = DateUtil.getSDF_NoTime()

        if (params.validOn == null || params.validOn.trim() == '') {
            result.validOn = ""
        } else {
            result.validOn = params.validOn
            date_restriction = sdf.parse(params.validOn)
        }

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        if (!params.status) {
            if (params.isSiteReloaded != "yes") {
                params.status = RDStore.SUBSCRIPTION_CURRENT.id
                result.defaultSet = true
            } else {
                params.status = 'FETCH_ALL'
            }
        }

        List orgIds = orgTypeService.getCurrentOrgIdsOfProvidersAndAgencies( contextService.org )

        result.providers = Org.findAllByIdInList(orgIds).sort { it.name }

        def tmpQ = subscriptionsQueryService.myInstitutionCurrentSubscriptionsBaseQuery(params, contextService.org)
        result.filterSet = tmpQ[2]
        List subscriptions = Subscription.executeQuery("select s ${tmpQ[0]}", tmpQ[1])
        //,[max: result.max, offset: result.offset]

        result.propList = PropertyDefinition.findAllPublicAndPrivateProp([PropertyDefinition.SUB_PROP], contextService.org)

        if (params.sort && params.sort.indexOf("§") >= 0) {
            switch (params.sort) {
                case "orgRole§provider":
                    subscriptions.sort { x, y ->
                        String a = x.getProviders().size() > 0 ? x.getProviders().first().name : ''
                        String b = y.getProviders().size() > 0 ? y.getProviders().first().name : ''
                        a.compareToIgnoreCase b
                    }
                    if (params.order.equals("desc"))
                        subscriptions.reverse(true)
                    break
            }
        }
        result.num_sub_rows = subscriptions.size()
        result.subscriptions = subscriptions.drop((int) result.offset).take((int) result.max)

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def createIssueEntitlementsSurvey() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0

        def date_restriction = null;
        def sdf = DateUtil.getSDF_NoTime()

        if (params.validOn == null || params.validOn.trim() == '') {
            result.validOn = ""
        } else {
            result.validOn = params.validOn
            date_restriction = sdf.parse(params.validOn)
        }

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        if (!params.status) {
            if (params.isSiteReloaded != "yes") {
                params.status = RDStore.SUBSCRIPTION_INTENDED.id
                result.defaultSet = true
            } else {
                params.status = 'FETCH_ALL'
            }
        }

        List orgIds = orgTypeService.getCurrentOrgIdsOfProvidersAndAgencies( contextService.org )

        result.providers = Org.findAllByIdInList(orgIds).sort { it.name }

        def tmpQ = subscriptionsQueryService.myInstitutionCurrentSubscriptionsBaseQuery(params, contextService.org)
        result.filterSet = tmpQ[2]
        List subscriptions = Subscription.executeQuery("select s ${tmpQ[0]}", tmpQ[1])
        //,[max: result.max, offset: result.offset]

        result.propList = PropertyDefinition.findAllPublicAndPrivateProp([PropertyDefinition.SUB_PROP], contextService.org)

        if (params.sort && params.sort.indexOf("§") >= 0) {
            switch (params.sort) {
                case "orgRole§provider":
                    subscriptions.sort { x, y ->
                        String a = x.getProviders().size() > 0 ? x.getProviders().first().name : ''
                        String b = y.getProviders().size() > 0 ? y.getProviders().first().name : ''
                        a.compareToIgnoreCase b
                    }
                    if (params.order.equals("desc"))
                        subscriptions.reverse(true)
                    break
            }
        }
        result.num_sub_rows = subscriptions.size()
        result.subscriptions = subscriptions.drop((int) result.offset).take((int) result.max)

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def addSubtoSubscriptionSurvey() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        result.subscription = Subscription.get(Long.parseLong(params.sub))
        if (!result.subscription) {
            redirect action: 'createSubscriptionSurvey'
        }

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def addSubtoIssueEntitlementsSurvey() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        result.subscription = Subscription.get(Long.parseLong(params.sub))
        result.pickAndChoose = true
        if (!result.subscription) {
            redirect action: 'createIssueEntitlementsSurvey'
        }

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processCreateSubscriptionSurvey() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }
        SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
        Date startDate = params.startDate ? sdf.parse(params.startDate) : null
        Date endDate = params.endDate ? sdf.parse(params.endDate) : null

        if(startDate != null && endDate != null) {
            if(startDate > endDate) {
                flash.error = g.message(code: "createSurvey.create.fail.startDateAndEndDate")
                redirect(action: 'addSubtoSubscriptionSurvey', params: params)
                return
            }
        }

        Subscription subscription = Subscription.get(Long.parseLong(params.sub))
        boolean subSurveyUseForTransfer = (SurveyConfig.findAllBySubscriptionAndSubSurveyUseForTransfer(subscription, true) || subscription.getCalculatedSuccessor()) ? false : (params.subSurveyUseForTransfer ? true : false)

        SurveyInfo surveyInfo = new SurveyInfo(
                name: params.name,
                startDate: startDate,
                endDate: endDate,
                type: subSurveyUseForTransfer ? RDStore.SURVEY_TYPE_RENEWAL : RDStore.SURVEY_TYPE_SUBSCRIPTION,
                owner: contextService.getOrg(),
                status: RDStore.SURVEY_IN_PROCESSING,
                comment: params.comment ?: null,
                isSubscriptionSurvey: true,
                isMandatory: subSurveyUseForTransfer ? true : (params.mandatory ?: false)
        )

        if (!(surveyInfo.save(flush: true))) {
            flash.error = g.message(code: "createSubscriptionSurvey.create.fail")
            redirect(action: 'addSubtoSubscriptionSurvey', params: params)
            return
        }

        if (subscription && !SurveyConfig.findAllBySubscriptionAndSurveyInfo(subscription, surveyInfo)) {
            SurveyConfig surveyConfig = new SurveyConfig(
                    subscription: subscription,
                    configOrder: surveyInfo.surveyConfigs.size() ? surveyInfo.surveyConfigs.size() + 1 : 1,
                    type: 'Subscription',
                    surveyInfo: surveyInfo,
                    subSurveyUseForTransfer: subSurveyUseForTransfer

            )

            surveyConfig.save(flush: true)

            //Wenn es eine Umfrage schon gibt, die als Übertrag dient. Dann ist es auch keine Lizenz Umfrage mit einem Teilnahme-Merkmal abfragt!
            if (subSurveyUseForTransfer) {
                    SurveyConfigProperties configProperty = new SurveyConfigProperties(
                            surveyProperty: PropertyDefinition.getByNameAndDescr('Participation', PropertyDefinition.SUR_PROP),
                            surveyConfig: surveyConfig)

                    if (configProperty.save(flush: true)) {
                        addSubMembers(surveyConfig)
                    }
                } else {
                    addSubMembers(surveyConfig)
                }
        } else {
            surveyInfo.delete(flush: true)
            flash.error = g.message(code: "createSubscriptionSurvey.create.fail")
            redirect(action: 'addSubtoSubscriptionSurvey', params: params)
            return
        }

        //flash.message = g.message(code: "createSubscriptionSurvey.create.successfull")
        redirect action: 'show', id: surveyInfo.id

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processCreateIssueEntitlementsSurvey() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }
        SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
        Date startDate = params.startDate ? sdf.parse(params.startDate) : null
        Date endDate = params.endDate ? sdf.parse(params.endDate) : null

        if(startDate != null && endDate != null) {
            if(startDate > endDate) {
                flash.error = g.message(code: "createSurvey.create.fail.startDateAndEndDate")
                redirect(action: 'addSubtoIssueEntitlementsSurvey', params: params)
                return
            }
        }

        SurveyInfo surveyInfo = new SurveyInfo(
                name: params.name,
                startDate: startDate,
                endDate: endDate,
                type: RDStore.SURVEY_TYPE_TITLE_SELECTION,
                owner: contextService.getOrg(),
                status: RDStore.SURVEY_IN_PROCESSING,
                comment: params.comment ?: null,
                isSubscriptionSurvey: true,
                isMandatory: params.mandatory ? true : false
        )

        if (!(surveyInfo.save(flush: true))) {
            flash.error = g.message(code: "createSubscriptionSurvey.create.fail")
            redirect(action: 'addSubtoIssueEntitlementsSurvey', params: params)
            return
        }

        Subscription subscription = Subscription.get(Long.parseLong(params.sub))
        if (subscription && !SurveyConfig.findAllBySubscriptionAndSurveyInfo(subscription, surveyInfo)) {
            SurveyConfig surveyConfig = new SurveyConfig(
                    subscription: subscription,
                    configOrder: surveyInfo.surveyConfigs.size() ? surveyInfo.surveyConfigs.size() + 1 : 1,
                    type: 'IssueEntitlementsSurvey',
                    surveyInfo: surveyInfo,
                    subSurveyUseForTransfer: false,
                    pickAndChoose: true,
                    createTitleGroups: params.createTitleGroups ? true : false

            )

            surveyConfig.save(flush: true)

            addSubMembers(surveyConfig)

        } else {
            surveyInfo.delete(flush: true)
            flash.error = g.message(code: "createIssueEntitlementsSurvey.create.fail")
            redirect(action: 'addSubtoIssueEntitlementsSurvey', params: params)
            return
        }

        //flash.message = g.message(code: "createIssueEntitlementsSurvey.create.successfull")
        redirect action: 'show', id: surveyInfo.id

    }


    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    def show() {
        def result = setResultGenericsAndCheckAccess()
        if (!accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")) {
            response.sendError(401); return
        }

        if(result.surveyInfo.surveyConfigs.size() >= 1  || params.surveyConfigID) {

            result.surveyConfig = params.surveyConfigID ? SurveyConfig.get(params.surveyConfigID) : result.surveyInfo.surveyConfigs[0]

            result.navigation = surveyService.getConfigNavigation(result.surveyInfo,  result.surveyConfig)
            result.contextOrg = contextService.getOrg()

            if ( result.surveyConfig.type == 'Subscription') {
                result.authorizedOrgs = result.user.authorizedOrgs

                // restrict visible for templates/links/orgLinksAsList
                result.visibleOrgRelations = []
                 result.surveyConfig.subscription.orgRelations.each { or ->
                    if (!(or.org.id == contextService.getOrg().id) && !(or.roleType.value in ['Subscriber', 'Subscriber_Consortial'])) {
                        result.visibleOrgRelations << or
                    }
                }
                result.visibleOrgRelations.sort { it.org.sortname }

                result.subscription =  result.surveyConfig.subscription ?: null

                //costs dataToDisplay
               result.dataToDisplay = ['own','cons']
               result.offsets = [consOffset:0,ownOffset:0]
               result.sortConfig = [consSort:'sortname',consOrder:'asc',
                                    ownSort:'ci.costTitle',ownOrder:'asc']

                result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP().toInteger()
                //cost items
                //params.forExport = true
                LinkedHashMap costItems = result.subscription ? financeService.getCostItemsForSubscription(params, result) : null
                result.costItemSums = [:]
                if (costItems?.own) {
                    result.costItemSums.ownCosts = costItems.own.sums
                }
                if (costItems?.cons) {
                    result.costItemSums.consCosts = costItems.cons.sums
                }
            }

            Org contextOrg = contextService.getOrg()
            result.tasks = taskService.getTasksByResponsiblesAndObject(result.user, contextOrg,  result.surveyConfig)
            def preCon = taskService.getPreconditionsWithoutTargets(contextOrg)
            result << preCon

            result.properties = []
            def allProperties = getSurveyProperties(contextOrg)
            result.properties = allProperties
            /*allProperties.each {

                if (!(it.id in SurveyConfigProperties.findAllBySurveyConfig(result.surveyConfig)?.surveyProperty.id)) {
                    result.properties << it
                }
            }*/
        }

        if ( params.exportXLSX ) {

            SXSSFWorkbook wb
            if ( params.surveyCostItems ) {
                SimpleDateFormat sdf = DateUtil.getSDF_NoTimeNoPoint()
                String datetoday = sdf.format(new Date(System.currentTimeMillis()))
                String filename = "${datetoday}_" + g.message(code: "survey.label")
                //if(wb instanceof XSSFWorkbook) file += "x";
                response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
                response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                wb = (SXSSFWorkbook) surveyService.exportSurveyCostItems([result.surveyConfig], result.institution)
            }else{
                SimpleDateFormat sdf = DateUtil.getSDF_NoTimeNoPoint()
                String datetoday = sdf.format(new Date(System.currentTimeMillis()))
                String filename = "${datetoday}_" + g.message(code: "survey.label")
                //if(wb instanceof XSSFWorkbook) file += "x";
                response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
                response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                wb = (SXSSFWorkbook) surveyService.exportSurveys([result.surveyConfig], result.institution)
            }
            wb.write(response.outputStream)
            response.outputStream.flush()
            response.outputStream.close()
            wb.dispose()

            return
        }else {
            result
        }
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    def surveyTitles() {
        def result = setResultGenericsAndCheckAccess()
        if (!accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")) {
            response.sendError(401); return
        }

        result.contextOrg = contextService.getOrg()

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP()
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0

        String base_qry = null
        Map<String,Object> qry_params = [subscription: result.surveyConfig.subscription]



        def date_filter
        date_filter = new Date()
        result.as_at_date = date_filter
        base_qry = " from IssueEntitlement as ie where ie.subscription = :subscription "
        base_qry += " and (( :startDate >= coalesce(ie.accessStartDate,subscription.startDate) ) OR ( ie.accessStartDate is null )) and ( ( :endDate <= coalesce(ie.accessEndDate,subscription.endDate) ) OR ( ie.accessEndDate is null ) ) "
        qry_params.startDate = date_filter
        qry_params.endDate = date_filter


        base_qry += " and ie.status = :current "
        qry_params.current = RDStore.TIPP_STATUS_CURRENT

        base_qry += "order by lower(ie.tipp.title.title) asc"

        result.num_sub_rows = IssueEntitlement.executeQuery("select ie.id " + base_qry, qry_params).size()

        result.entitlements = IssueEntitlement.executeQuery("select ie " + base_qry, qry_params, [max: result.max, offset: result.offset])

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    def surveyConfigDocs() {
        def result = setResultGenericsAndCheckAccess()
        if (!accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")) {
            response.sendError(401); return
        }

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    def surveyParticipants() {
        def result = setResultGenericsAndCheckAccess()
        if (!accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")) {
            response.sendError(401); return
        }

        // new: filter preset
        params.orgType = RDStore.OT_INSTITUTION.id.toString()
        params.orgSector = RDStore.O_SECTOR_HIGHER_EDU.id.toString()

        result.propList = PropertyDefinition.findAllPublicAndPrivateOrgProp(contextService.org)

        params.comboType = RDStore.COMBO_TYPE_CONSORTIUM.value
        def fsq = filterService.getOrgComboQuery(params, result.institution)
        def tmpQuery = "select o.id " + fsq.query.minus("select o ")
        def consortiaMemberIds = Org.executeQuery(tmpQuery, fsq.queryParams)

        if (params.filterPropDef && consortiaMemberIds) {
            fsq = propertyService.evalFilterQuery(params, "select o FROM Org o WHERE o.id IN (:oids) order by o.sortname", 'o', [oids: consortiaMemberIds])
        }
        result.consortiaMembers = Org.executeQuery(fsq.query, fsq.queryParams, params)

        if(result.surveyConfig.pickAndChoose){

            List orgs = subscriptionService.getValidSurveySubChildOrgs(result.surveyConfig.subscription)
            result.consortiaMembers = result.consortiaMembers.findAll{ (it in orgs)}
        }

        result.consortiaMembersCount = Org.executeQuery(fsq.query, fsq.queryParams).size()

        result.editable = (result.surveyInfo && result.surveyInfo.status.id != RDStore.SURVEY_IN_PROCESSING.id) ? false : result.editable

        def surveyOrgs = result.surveyConfig.getSurveyOrgsIDs()

        result.selectedParticipants = getfilteredSurveyOrgs(surveyOrgs.orgsWithoutSubIDs, fsq.query, fsq.queryParams, params)
        result.selectedSubParticipants = getfilteredSurveyOrgs(surveyOrgs.orgsWithSubIDs, fsq.query, fsq.queryParams, params)

        params.tab = params.tab ?: (result.surveyConfig.type == 'GeneralSurvey' ? 'selectedParticipants' : 'selectedSubParticipants')

        result

    }


    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    def surveyCostItems() {
        def result = setResultGenericsAndCheckAccess()
        if (!accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")) {
            response.sendError(401); return
        }
        result.putAll(financeService.setEditVars(result.institution))

        Map<Long,Object> orgConfigurations = [:]
        result.costItemElements.each { oc ->
            orgConfigurations.put(oc.costItemElement.id,oc.elementSign.id)
        }

        result.orgConfigurations = orgConfigurations as JSON

        params.tab = params.tab ?: 'selectedSubParticipants'

        // new: filter preset
        params.orgType = RDStore.OT_INSTITUTION.id.toString()
        params.orgSector = RDStore.O_SECTOR_HIGHER_EDU.id.toString()

        result.propList = PropertyDefinition.findAllPublicAndPrivateOrgProp(contextService.org)

        params.comboType = RDStore.COMBO_TYPE_CONSORTIUM.value
        def fsq = filterService.getOrgComboQuery(params, result.institution)
        def tmpQuery = "select o.id " + fsq.query.minus("select o ")
        def consortiaMemberIds = Org.executeQuery(tmpQuery, fsq.queryParams)

        if (params.filterPropDef && consortiaMemberIds) {
            fsq = propertyService.evalFilterQuery(params, "select o FROM Org o WHERE o.id IN (:oids) order by o.sortname", 'o', [oids: consortiaMemberIds])
        }

        result.editable = (result.surveyInfo.status != RDStore.SURVEY_IN_PROCESSING) ? false : result.editable

        //Only SurveyConfigs with Subscriptions
        result.surveyConfigs = result.surveyInfo.surveyConfigs.findAll { it.subscription != null }.sort {
            it.configOrder
        }

        params.surveyConfigID = params.surveyConfigID ?: result.surveyConfigs[0].id.toString()

        result.surveyConfig = SurveyConfig.get(params.surveyConfigID)

        def surveyOrgs = result.surveyConfig.getSurveyOrgsIDs()

        result.selectedParticipants = getfilteredSurveyOrgs(surveyOrgs.orgsWithoutSubIDs, fsq.query, fsq.queryParams, params)
        result.selectedSubParticipants = getfilteredSurveyOrgs(surveyOrgs.orgsWithSubIDs, fsq.query, fsq.queryParams, params)

        result.selectedCostItemElement = params.selectedCostItemElement ?: RefdataValue.getByValueAndCategory('price: consortial price', RDConstants.COST_ITEM_ELEMENT).id.toString()

        if (params.selectedCostItemElement) {
            params.remove('selectedCostItemElement')
        }
        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processSurveyCostItemsBulk() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.putAll(financeService.setEditVars(result.institution))
        List selectedMembers = params.list("selectedOrgs")

        if(selectedMembers) {

            def billing_currency = null
            if (params.long('newCostCurrency2')) //GBP,etc
            {
                billing_currency = RefdataValue.get(params.newCostCurrency2)
            }


            NumberFormat format = NumberFormat.getInstance(LocaleContextHolder.getLocale())
            def cost_billing_currency = params.newCostInBillingCurrency2 ? format.parse(params.newCostInBillingCurrency2).doubleValue() : null //0.00
            //def cost_currency_rate = params.newCostCurrencyRate2 ? params.double('newCostCurrencyRate2', 1.00) : null //1.00
            //def cost_local_currency = params.newCostInLocalCurrency2 ? format.parse(params.newCostInLocalCurrency2).doubleValue() : null //0.00

            def tax_key = null
            if (!params.newTaxRate2.contains("null")) {
                String[] newTaxRate = params.newTaxRate2.split("§")
                RefdataValue taxType = genericOIDService.resolveOID(newTaxRate[0])
                int taxRate = Integer.parseInt(newTaxRate[1])
                switch (taxType.id) {
                    case RefdataValue.getByValueAndCategory("taxable", RDConstants.TAX_TYPE).id:
                        switch (taxRate) {
                            case 7: tax_key = CostItem.TAX_TYPES.TAXABLE_7
                                break
                            case 19: tax_key = CostItem.TAX_TYPES.TAXABLE_19
                                break
                        }
                        break
                    case RefdataValue.getByValueAndCategory("taxable tax-exempt", RDConstants.TAX_TYPE).id:
                        tax_key = CostItem.TAX_TYPES.TAX_EXEMPT
                        break
                    case RefdataValue.getByValueAndCategory("not taxable", RDConstants.TAX_TYPE).id:
                        tax_key = CostItem.TAX_TYPES.TAX_NOT_TAXABLE
                        break
                    case RefdataValue.getByValueAndCategory("not applicable", RDConstants.TAX_TYPE).id:
                        tax_key = CostItem.TAX_TYPES.TAX_NOT_APPLICABLE
                        break
                    case RefdataValue.getByValueAndCategory("reverse charge", RDConstants.TAX_TYPE).id:
                        tax_key = CostItem.TAX_TYPES.TAX_REVERSE_CHARGE
                        break
                }
            }

            selectedMembers.each { id ->
                SurveyOrg surveyOrg = SurveyOrg.findByOrgAndSurveyConfig(Org.get(Long.parseLong(id)), result.surveyConfig)
                CostItem surveyCostItem = CostItem.findBySurveyOrgAndCostItemStatusNotEqual(surveyOrg, RDStore.COST_ITEM_DELETED)
                if(surveyCostItem){

                    if(params.percentOnOldPrice){
                        Double percentOnOldPrice = params.double('percentOnOldPrice', 0.00)
                        Subscription orgSub = result.surveyConfig.subscription.getDerivedSubscriptionBySubscribers(Org.get(Long.parseLong(id)))
                        CostItem costItem = CostItem.findBySubAndOwnerAndCostItemStatusNotEqualAndCostItemElement(orgSub, contextService.getOrg(), RDStore.COST_ITEM_DELETED, RDStore.COST_ITEM_ELEMENT_CONSORTIAL_PRICE)
                        surveyCostItem.costInBillingCurrency = costItem ? costItem.costInBillingCurrency*(1+(percentOnOldPrice/100)) : surveyCostItem.costInBillingCurrency
                    }
                    else
                    {
                        surveyCostItem.costInBillingCurrency = cost_billing_currency ?: surveyCostItem.costInBillingCurrency
                    }

                    surveyCostItem.billingCurrency = billing_currency ?: surveyCostItem.billingCurrency
                    //Not specified default to GDP
                    //surveyCostItem.costInLocalCurrency = cost_local_currency ?: surveyCostItem.costInLocalCurrency

                    surveyCostItem.finalCostRounding = params.newFinalCostRounding2 ? true : false

                    //surveyCostItem.currencyRate = cost_currency_rate ?: surveyCostItem.currencyRate
                    surveyCostItem.taxKey = tax_key ?: surveyCostItem.taxKey

                    surveyCostItem.save()
                }
            }
        }

        redirect(url: request.getHeader('referer'))
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyConfigFinish() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.surveyConfig.configFinish = params.configFinish ?: false
        if (result.surveyConfig.save(flush: true)) {
            //flash.message = g.message(code: 'survey.change.successfull')
        } else {
            flash.error = g.message(code: 'survey.change.fail')
        }

        redirect(url: request.getHeader('referer'))

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyCostItemsFinish() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.surveyConfig.costItemsFinish = params.costItemsFinish ?: false

        if (result.surveyConfig.save(flush: true)) {
            //flash.message = g.message(code: 'survey.change.successfull')
        } else {
            flash.error = g.message(code: 'survey.change.fail')
        }

        redirect(url: request.getHeader('referer'))

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyTransferConfig() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        def transferWorkflow = result.surveyConfig.transferWorkflow ? JSON.parse(result.surveyConfig.transferWorkflow) : [:]

        if(params.transferMembers != null)
        {
            transferWorkflow.transferMembers = params.transferMembers
        }

        if(params.transferSurveyCostItems != null)
        {
            transferWorkflow.transferSurveyCostItems = params.transferSurveyCostItems
        }

        if(params.transferSurveyProperties != null)
        {
            transferWorkflow.transferSurveyProperties = params.transferSurveyProperties
        }

        if(params.transferCustomProperties != null)
        {
            transferWorkflow.transferCustomProperties = params.transferCustomProperties
        }

        if(params.transferPrivateProperties != null)
        {
            transferWorkflow.transferPrivateProperties = params.transferPrivateProperties
        }

        result.surveyConfig.transferWorkflow = transferWorkflow ?  (new JSON(transferWorkflow)).toString() : null

        if (result.surveyConfig.save(flush: true)) {
            //flash.message = g.message(code: 'survey.change.successfull')
        } else {
            flash.error = g.message(code: 'survey.change.fail')
        }

        redirect(url: request.getHeader('referer'))

    }


    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    def surveyEvaluation() {
        def result = setResultGenericsAndCheckAccess()
        if (!accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")) {
            response.sendError(401); return
        }

        params.tab = params.tab ?: 'surveyConfigsView'

        result.participantsNotFinish = SurveyResult.findAllBySurveyConfigAndFinishDateIsNull(result.surveyConfig).sort {
            it.participant.sortname
        }

        result.participantsFinish = SurveyResult.findAllBySurveyConfigAndFinishDateIsNotNull(result.surveyConfig).sort {
            it.participant.sortname
        }

        result.participantsNotFinishTotal = SurveyResult.findAllBySurveyConfigAndFinishDateIsNull(result.surveyConfig)?.participant.flatten().unique { a, b -> a.id <=> b.id }
        result.participantsFinishTotal = SurveyResult.findAllBySurveyConfigAndFinishDateIsNotNull(result.surveyConfig)?.participant.flatten().unique { a, b -> a.id <=> b.id }

        result.surveyResult = SurveyResult.findAllByOwnerAndSurveyConfig(result.institution, result.surveyConfig).sort {
            it.participant.sortname
        }

        result.participants = result.surveyResult
        result.participantsTotal = result.surveyResult?.participant.flatten().unique { a, b -> a.id <=> b.id }

        if ( params.exportXLSX ) {
            SXSSFWorkbook wb
            if ( params.surveyCostItems ) {
                SimpleDateFormat sdf = DateUtil.getSDF_NoTimeNoPoint()
                String datetoday = sdf.format(new Date(System.currentTimeMillis()))
                String filename = "${datetoday}_" + g.message(code: "survey.label")
                //if(wb instanceof XSSFWorkbook) file += "x";
                response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
                response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                wb = (SXSSFWorkbook) surveyService.exportSurveyCostItems([result.surveyConfig], result.institution)
            }else {
                SimpleDateFormat sdf = DateUtil.getSDF_NoTimeNoPoint()
                String datetoday = sdf.format(new Date(System.currentTimeMillis()))
                String filename = "${datetoday}_" + g.message(code: "survey.label")
                //if(wb instanceof XSSFWorkbook) file += "x";
                response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
                response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                wb = (SXSSFWorkbook) surveyService.exportSurveys([result.surveyConfig], result.institution)
            }
            wb.write(response.outputStream)
            response.outputStream.flush()
            response.outputStream.close()
            wb.dispose()

            return
        }else {
            result
        }

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    def surveyTitlesEvaluation() {
        def result = setResultGenericsAndCheckAccess()
        if (!accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")) {
            response.sendError(401); return
        }

        def orgs = result.surveyConfig.orgs.org.flatten().unique { a, b -> a.id <=> b.id }
        result.participants = orgs.sort { it.sortname }

        result.participantsNotFinish = SurveyOrg.findAllByFinishDateIsNullAndSurveyConfig(result.surveyConfig).org.flatten().unique { a, b -> a.id <=> b.id }.sort {
            it.sortname
        }
        result.participantsFinish = SurveyOrg.findAllByFinishDateIsNotNullAndSurveyConfig(result.surveyConfig).org.flatten().unique { a, b -> a.id <=> b.id }.sort {
            it.sortname
        }

        if(result.surveyConfig.surveyProperties.size() > 0){
            result.surveyResult = SurveyResult.findAllByOwnerAndSurveyConfig(result.institution, result.surveyConfig).sort {
                it.participant.sortname
            }
        }

        if ( params.exportXLSX ) {
            SimpleDateFormat sdf = DateUtil.getSDF_NoTimeNoPoint()
            String datetoday = sdf.format(new Date(System.currentTimeMillis()))
            String filename = "${datetoday}_" + g.message(code: "survey.label")
            //if(wb instanceof XSSFWorkbook) file += "x";
            response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
            response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            SXSSFWorkbook wb = (SXSSFWorkbook) surveyService.exportSurveys([result.surveyConfig], result.institution)
            wb.write(response.outputStream)
            response.outputStream.flush()
            response.outputStream.close()
            wb.dispose()

            return
        }else {
                    result
            }
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    def showEntitlementsRenew() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)
        result.participant = params.participant ? Org.get(params.participant) : null

        result.surveyConfig = SurveyConfig.get(params.id)
        result.surveyInfo = result.surveyConfig.surveyInfo

        result.editable = result.surveyInfo.isEditable() ?: false

        if (!accessService.checkPermAffiliationX('ORG_CONSORTIUM','INST_USER','ROLE_ADMIN')) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        result.surveyOrg = SurveyOrg.findByOrgAndSurveyConfig(result.participant, result.surveyConfig)


        result.subscriptionParticipant = result.surveyConfig.subscription?.getDerivedSubscriptionBySubscribers(result.participant)

        result.ies = subscriptionService.getIssueEntitlementsNotFixed(result.subscriptionParticipant)

        def filename = "renewEntitlements_${escapeService.escapeString(result.surveyConfig.subscription.dropdownNamingConvention(result.participant))}"

        if (params.exportKBart) {
            response.setHeader("Content-disposition", "attachment; filename=${filename}.tsv")
            response.contentType = "text/tsv"
            ServletOutputStream out = response.outputStream
            Map<String, List> tableData = exportService.generateTitleExportKBART(result.ies)
            out.withWriter { writer ->
                writer.write(exportService.generateSeparatorTableString(tableData.titleRow, tableData.columnData, '\t'))
            }
            out.flush()
            out.close()
        }else if(params.exportXLSX) {
            response.setHeader("Content-disposition", "attachment; filename=\"${filename}.xlsx\"")
            response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            Map<String,List> export = exportService.generateTitleExportXLS(result.ies)
            Map sheetData = [:]
            sheetData[g.message(code:'subscription.details.renewEntitlements.label')] = [titleRow:export.titles,columnData:export.rows]
            SXSSFWorkbook workbook = exportService.generateXLSXWorkbook(sheetData)
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
            }
        }
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    def surveyTitlesSubscriber() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)
        result.participant = params.participant ? Org.get(params.participant) : null

        result.surveyConfig = SurveyConfig.get(params.id)
        result.surveyInfo = result.surveyConfig.surveyInfo
        result.surveyOrg = SurveyOrg.findByOrgAndSurveyConfig(result.participant, result.surveyConfig)

        result.editable = result.surveyInfo.isEditable() ?: false

        if (!accessService.checkPermAffiliationX('ORG_CONSORTIUM','INST_USER','ROLE_ADMIN')) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        result.subscriptionInstance = result.surveyConfig.subscription.getDerivedSubscriptionBySubscribers(result.participant)

        result.ies = subscriptionService.getIssueEntitlementsNotFixed(result.subscriptionInstance)
        result.iesListPriceSum = 0
        result.ies.each{
            result.iesListPriceSum = result.iesListPriceSum + (it.priceItem ? (it.priceItem.listPrice ? it.priceItem.listPrice : 0) : 0)
        }


        result.iesFix = subscriptionService.getIssueEntitlementsFixed(result.subscriptionInstance)
        result.iesFixListPriceSum = 0
        result.iesFix.each{
            result.iesFixListPriceSum = result.iesFixListPriceSum + (it.priceItem ? (it.priceItem.listPrice ? it.priceItem.listPrice : 0) : 0)
        }


        result.ownerId = result.surveyConfig.surveyInfo.owner.id ?: null

        if(result.subscriptionInstance) {
            result.authorizedOrgs = result.user.authorizedOrgs
            result.contextOrg = contextService.getOrg()
            // restrict visible for templates/links/orgLinksAsList
            result.visibleOrgRelations = []
            result.subscriptionInstance.orgRelations.each { or ->
                if (!(or.org.id == contextService.getOrg().id) && !(or.roleType.value in ['Subscriber', 'Subscriber_Consortial'])) {
                    result.visibleOrgRelations << or
                }
            }
            result.visibleOrgRelations.sort { it.org.sortname }
        }

        result.surveyResults = SurveyResult.findAllByParticipantAndSurveyConfig(result.participant, result.surveyConfig).sort { it.surveyConfig.configOrder }

        result

    }


    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def openIssueEntitlementsSurveyAgain() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)
        result.participant = params.participant ? Org.get(params.participant) : null

        result.surveyConfig = SurveyConfig.get(params.id)
        result.surveyInfo = result.surveyConfig.surveyInfo

        result.editable = result.surveyInfo.isEditable() ?: false

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        def surveyOrg = SurveyOrg.findByOrgAndSurveyConfig(result.participant, result.surveyConfig)

        result.subscriptionInstance =  result.surveyConfig.subscription

        def ies = subscriptionService.getIssueEntitlementsUnderNegotiation(result.surveyConfig.subscription.getDerivedSubscriptionBySubscribers(result.participant))

        ies.each { ie ->
            ie.acceptStatus = RDStore.IE_ACCEPT_STATUS_UNDER_CONSIDERATION
            ie.save(flush: true)
        }

        surveyOrg.finishDate = null
        surveyOrg.save(flush: true)

        //flash.message = message(code: 'openIssueEntitlementsSurveyAgain.info')

        redirect(action: 'showEntitlementsRenew', id: result.surveyConfig.id, params:[participant: result.participant.id])

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def openSurveyAgainForParticipant() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)
        result.participant = params.participant ? Org.get(params.participant) : null

        result.surveyConfig = SurveyConfig.get(params.surveyConfigID)
        result.surveyInfo = result.surveyConfig.surveyInfo

        result.editable = result.surveyInfo.isEditable() ?: false

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        List<SurveyResult> surveyResults = SurveyResult.findAllByParticipantAndSurveyConfig(result.participant, result.surveyConfig)

        surveyResults.each {
                it.finishDate = null
                it.save()
        }

        redirect(action: 'evaluationParticipant', id: result.surveyInfo.id, params:[surveyConfigID: result.surveyConfig.id, participant: result.participant.id])

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def completeIssueEntitlementsSurveyforParticipant() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)
        result.participant = params.participant ? Org.get(params.participant) : null

        result.surveyConfig = SurveyConfig.get(params.id)
        result.surveyInfo = result.surveyConfig.surveyInfo

        result.editable = result.surveyInfo.isEditable() ?: false

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        Subscription participantSub = result.surveyConfig.subscription.getDerivedSubscriptionBySubscribers(result.participant)
        List<IssueEntitlement> ies = subscriptionService.getIssueEntitlementsUnderNegotiation(participantSub)

        IssueEntitlementGroup issueEntitlementGroup
        if(result.surveyConfig.createTitleGroups){

            Integer countTitleGroups = IssueEntitlementGroup.findAllBySubAndNameIlike(participantSub, 'Phase').size()

            issueEntitlementGroup = new IssueEntitlementGroup(sub: participantSub, name: "Phase ${countTitleGroups+1}").save()
        }

        ies.each { ie ->
            ie.acceptStatus = RDStore.IE_ACCEPT_STATUS_FIXED
            ie.save(flush: true)

            if(issueEntitlementGroup){
                println(issueEntitlementGroup)
                IssueEntitlementGroupItem issueEntitlementGroupItem = new IssueEntitlementGroupItem(
                        ie: ie,
                        ieGroup: issueEntitlementGroup)

                if (!issueEntitlementGroupItem.save(flush: true)) {
                    log.error("Problem saving IssueEntitlementGroupItem by Survey ${issueEntitlementGroupItem.errors}")
                }
            }
        }

        flash.message = message(code: 'completeIssueEntitlementsSurvey.forParticipant.info')

        redirect(action: 'showEntitlementsRenew', id: result.surveyConfig.id, params:[participant: result.participant.id])

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def completeIssueEntitlementsSurvey() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.surveyConfig = SurveyConfig.get(params.id)
        result.surveyInfo = result.surveyConfig.surveyInfo

        result.editable = result.surveyInfo.isEditable() ?: false

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        def participantsFinish = SurveyOrg.findAllByFinishDateIsNotNullAndSurveyConfig(result.surveyConfig)?.org.flatten().unique { a, b -> a.id <=> b.id }

        participantsFinish.each { org ->
            Subscription participantSub = result.surveyConfig.subscription.getDerivedSubscriptionBySubscribers(org)

            List<IssueEntitlement> ies = subscriptionService.getIssueEntitlementsUnderNegotiation(participantSub)

            IssueEntitlementGroup issueEntitlementGroup
            if(result.surveyConfig.createTitleGroups){

                Integer countTitleGroups = IssueEntitlementGroup.findAllBySubAndNameIlike(participantSub, 'Phase').size()

                issueEntitlementGroup = new IssueEntitlementGroup(sub: participantSub, name: "Phase ${countTitleGroups+1}").save()
            }

            ies.each { ie ->
                ie.acceptStatus = RDStore.IE_ACCEPT_STATUS_FIXED
                ie.save(flush: true)

                if(issueEntitlementGroup){
                    IssueEntitlementGroupItem issueEntitlementGroupItem = new IssueEntitlementGroupItem(
                            ie: ie,
                            ieGroup: issueEntitlementGroup)

                    if (!issueEntitlementGroupItem.save(flush: true)) {
                        log.error("Problem saving IssueEntitlementGroupItem by Survey ${issueEntitlementGroupItem.errors}")
                    }
                }
            }
        }

        flash.message = message(code: 'completeIssueEntitlementsSurvey.forFinishParticipant.info')

        redirect(action: 'surveyTitlesEvaluation', id: result.surveyInfo.id, params:[surveyConfigID: result.surveyConfig.id])

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    def evaluateIssueEntitlementsSurvey() {
        def result = setResultGenericsAndCheckAccess()

        if (!accessService.checkPermAffiliationX('ORG_CONSORTIUM','INST_USER','ROLE_ADMIN')) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        result.participant = params.participant ? Org.get(params.participant) : null

        result.surveyConfig = SurveyConfig.get(params.id)
        result.surveyInfo = result.surveyConfig.surveyInfo

        result.surveyOrg = SurveyOrg.findByOrgAndSurveyConfig(result.participant, result.surveyConfig)

        result.subscriptionInstance =  result.surveyConfig.subscription

        result.ies = subscriptionService.getCurrentIssueEntitlements(result.surveyConfig.subscription.getDerivedSubscriptionBySubscribers(result.participant))

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    def evaluationParticipant() {
        def result = setResultGenericsAndCheckAccess()
        if (!accessService.checkPermAffiliationX('ORG_CONSORTIUM','INST_USER','ROLE_ADMIN')) {
            response.sendError(401); return
        }

        result.participant = Org.get(params.participant)

        result.surveyInfo = SurveyInfo.get(params.id) ?: null

        result.surveyConfig = SurveyConfig.get(params.surveyConfigID)

        result.subscriptionInstance = result.surveyConfig.subscription.getDerivedSubscriptionBySubscribers(result.participant)
        result.subscription =  result.subscriptionInstance ?: null

        result.surveyResults = SurveyResult.findAllByParticipantAndSurveyConfig(result.participant, result.surveyConfig)

        result.ownerId = result.surveyResults[0].owner.id

        if(result.surveyConfig.type == 'Subscription') {
            // restrict visible for templates/links/orgLinksAsList
            result.visibleOrgRelations = []
            result.subscriptionInstance.orgRelations.each { or ->
                if (!(or.org.id == contextService.getOrg().id) && !(or.roleType.value in ['Subscriber', 'Subscriber_Consortial'])) {
                    result.visibleOrgRelations << or
                }
            }
            result.visibleOrgRelations.sort { it.org.sortname }

            //costs dataToDisplay
            result.dataToDisplay = ['consAtSubscr']
            result.offsets = [consOffset:0]
            result.sortConfig = [consSort:'ci.costTitle',consOrder:'asc']

            result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP().toInteger()
            //cost items
            //params.forExport = true
            LinkedHashMap costItems = result.subscription ? financeService.getCostItemsForSubscription(params, result) : null
            result.costItemSums = [:]
            if (costItems.cons) {
                result.costItemSums.consCosts = costItems.cons.sums
            }
        }

        result.editable = surveyService.isEditableSurvey(result.institution, result.surveyInfo)
        result.institution = result.participant

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_USER", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_USER", "ROLE_ADMIN")
    })
    def allSurveyProperties() {
        def result = setResultGenericsAndCheckAccess()
        if (!accessService.checkPermAffiliationX('ORG_CONSORTIUM','INST_USER','ROLE_ADMIN')) {
            response.sendError(401); return
        }

        result.properties = getSurveyProperties(result.institution)

        result.language = LocaleContextHolder.getLocale().toString()

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def addSurveyPropToConfig() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.editable = (result.surveyInfo && result.surveyInfo.status != RDStore.SURVEY_IN_PROCESSING) ? false : result.editable

        if (result.surveyInfo && result.editable) {

            if (params.selectedProperty) {
                PropertyDefinition property = PropertyDefinition.get(Long.parseLong(params.selectedProperty))
                //Config is Sub
                if (params.surveyConfigID) {
                    SurveyConfig surveyConfig = SurveyConfig.get(Long.parseLong(params.surveyConfigID))

                    if (addSurPropToSurvey(surveyConfig, property)) {

                        //flash.message = g.message(code: "surveyConfigs.property.add.successfully")

                    } else {
                        flash.error = g.message(code: "surveyConfigs.property.exists")
                    }
                }
            }
        }
        redirect(url: request.getHeader('referer'))

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def deleteSurveyPropFromConfig() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        def surveyConfigProp = SurveyConfigProperties.get(params.id)

        def surveyInfo = surveyConfigProp.surveyConfig.surveyInfo

        result.editable = (surveyInfo && surveyInfo.status != RDStore.SURVEY_IN_PROCESSING) ? false : result.editable

        if (result.editable) {
            try {
                surveyConfigProp.delete(flush: true)
                //flash.message = g.message(code: "default.deleted.message", args: [g.message(code: "surveyProperty.label"), ''])
            }
            catch (DataIntegrityViolationException e) {
                flash.error = g.message(code: "default.not.deleted.message", args: [g.message(code: "surveyProperty.label"), ''])
            }
        }

        redirect(url: request.getHeader('referer'))

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def createSurveyProperty() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        PropertyDefinition surveyProperty = PropertyDefinition.findWhere(
                name: params.pd_name,
                type: params.pd_type,
                tenant: result.institution,
                descr: PropertyDefinition.SUR_PROP
        )

        if ((!surveyProperty) && params.pd_name && params.pd_type) {
            def rdc
            if (params.refdatacategory) {
                rdc = RefdataCategory.findById(Long.parseLong(params.refdatacategory))
            }

            Map<String, Object> map = [
                    token       : params.pd_name,
                    category    : PropertyDefinition.SUR_PROP,
                    type        : params.pd_type,
                    rdc         : rdc.getDesc(),
                    tenant      : result.institution.globalUID,
                    i10n        : [
                            name_de: params.pd_name,
                            name_en: params.pd_name,
                            expl_de: params.pd_expl,
                            expl_en: params.pd_expl
                    ]
            ]

            if (PropertyDefinition.construct(map)) {
                //flash.message = message(code: 'surveyProperty.create.successfully', args: [surveyProperty.name])
            } else {
                flash.error = message(code: 'surveyProperty.create.fail')
            }
        } else if (surveyProperty) {
            flash.error = message(code: 'surveyProperty.create.exist')
        } else {
            flash.error = message(code: 'surveyProperty.create.fail')
        }

        redirect(url: request.getHeader('referer'))

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def deleteSurveyProperty() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        def surveyProperty = PropertyDefinition.findByIdAndTenant(params.deleteId, result.institution)

        if (surveyProperty.countUsages()==0 && surveyProperty.owner.id == result.institution.id && surveyProperty.delete())
        {
            //flash.message = message(code: 'default.deleted.message', args:[message(code: 'surveyProperty.label'), surveyProperty.getI10n('name')])
        }

        redirect(action: 'allSurveyProperties', id: params.id)

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def addSurveyParticipants() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        SurveyConfig surveyConfig = SurveyConfig.get(params.surveyConfigID)
        SurveyInfo surveyInfo = surveyConfig.surveyInfo

        result.editable = (surveyInfo && surveyInfo.status in [RDStore.SURVEY_IN_PROCESSING, RDStore.SURVEY_READY, RDStore.SURVEY_SURVEY_STARTED]) ? result.editable : false

        if (params.selectedOrgs && result.editable) {

            params.list('selectedOrgs').each { soId ->

                Org org = Org.get(Long.parseLong(soId))

                boolean existsMultiYearTerm = false
                Subscription sub = surveyConfig.subscription
                if (sub && !surveyConfig.pickAndChoose && surveyConfig.subSurveyUseForTransfer) {
                    Subscription subChild = sub.getDerivedSubscriptionBySubscribers(org)

                    if (subChild.isCurrentMultiYearSubscriptionNew()) {
                        existsMultiYearTerm = true
                    }

                }

                if (!(SurveyOrg.findAllBySurveyConfigAndOrg(surveyConfig, org)) && !existsMultiYearTerm) {
                    SurveyOrg surveyOrg = new SurveyOrg(
                            surveyConfig: surveyConfig,
                            org: org
                    )

                    if (!surveyOrg.save(flush: true)) {
                        log.debug("Error by add Org to SurveyOrg ${surveyOrg.errors}");
                    } else {
                        if(surveyInfo.status in [RDStore.SURVEY_READY, RDStore.SURVEY_SURVEY_STARTED]){
                            surveyConfig.surveyProperties.each { property ->

                                SurveyResult surveyResult = new SurveyResult(
                                        owner: result.institution,
                                        participant: org ?: null,
                                        startDate: surveyInfo.startDate,
                                        endDate: surveyInfo.endDate ?: null,
                                        type: property.surveyProperty,
                                        surveyConfig: surveyConfig
                                )

                                if (surveyResult.save(flush: true)) {
                                    log.debug(surveyResult)
                                } else {
                                    log.error("Not create surveyResult: "+ surveyResult)
                                }
                            }

                            if(surveyInfo.status == RDStore.SURVEY_SURVEY_STARTED){
                                surveyUpdateService.emailsToSurveyUsersOfOrg(surveyInfo, org)
                            }
                        }
                    }
                }
            }
            surveyConfig.save(flush: true)

        }

        redirect action: 'surveyParticipants', id: params.id, params: [surveyConfigID: params.surveyConfigID]

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processOpenSurvey() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.editable = (result.surveyInfo && result.surveyInfo.status != RDStore.SURVEY_IN_PROCESSING) ? false : result.editable

        if (result.editable) {

            result.surveyConfigs = result.surveyInfo.surveyConfigs.sort { it.configOrder }

            result.surveyConfigs.each { config ->


                    config.orgs.org.each { org ->

                            config.surveyProperties.each { property ->

                                def surveyResult = new SurveyResult(
                                        owner: result.institution,
                                        participant: org ?: null,
                                        startDate: result.surveyInfo.startDate,
                                        endDate: result.surveyInfo.endDate ?: null,
                                        type: property.surveyProperty,
                                        surveyConfig: config
                                )

                                if (surveyResult.save(flush: true)) {
                                    log.debug(surveyResult)
                                } else {
                                    log.error("Not create surveyResult: "+ surveyResult)
                                }
                            }
                        }

            }

            result.surveyInfo.status = RDStore.SURVEY_READY
            result.surveyInfo.save(flush: true)
            flash.message = g.message(code: "openSurvey.successfully")
        }

            redirect action: 'show', id: params.id

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processEndSurvey() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        if (result.editable) {

            result.surveyInfo.status = RDStore.SURVEY_IN_EVALUATION
            result.surveyInfo.save(flush: true)
            flash.message = g.message(code: "endSurvey.successfully")
        }

        if(result.surveyConfig && result.surveyConfig.subSurveyUseForTransfer) {
            redirect action: 'renewalWithSurvey', params: [surveyConfigID: result.surveyConfig.id, id: result.surveyInfo.id]
        }else{
            redirect(uri: request.getHeader('referer'))
        }
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processOpenSurveyNow() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.editable = (result.surveyInfo && result.surveyInfo.status != RDStore.SURVEY_IN_PROCESSING) ? false : result.editable

        def currentDate = new Date(System.currentTimeMillis())

        if (result.editable) {

            result.surveyConfigs = result.surveyInfo.surveyConfigs.sort { it.configOrder }

            result.surveyConfigs.each { config ->
                        config.orgs.org.each { org ->

                            config.surveyProperties.each { property ->

                                def surveyResult = new SurveyResult(
                                        owner: result.institution,
                                        participant: org ?: null,
                                        startDate: currentDate,
                                        endDate: result.surveyInfo.endDate,
                                        type: property.surveyProperty,
                                        surveyConfig: config
                                )

                                if (surveyResult.save(flush: true)) {
                                    log.debug(surveyResult)
                                } else {
                                    log.debug(surveyResult)
                                }
                            }
                        }


            }

            result.surveyInfo.status = RDStore.SURVEY_SURVEY_STARTED
            result.surveyInfo.startDate = currentDate
            result.surveyInfo.save(flush: true)
            flash.message = g.message(code: "openSurveyNow.successfully")

            surveyUpdateService.emailsToSurveyUsers([result.surveyInfo.id])

        }

            redirect action: 'show', id: params.id

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def openSurveyAgain() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        if(result.surveyInfo && result.surveyInfo.status.id in [RDStore.SURVEY_IN_EVALUATION.id, RDStore.SURVEY_COMPLETED.id, RDStore.SURVEY_SURVEY_COMPLETED.id ]){

            SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
            Date endDate = params.newEndDate ? sdf.parse(params.newEndDate) : null

            if(result.surveyInfo.startDate != null && endDate != null) {
                if(result.surveyInfo.startDate > endDate) {
                    flash.error = g.message(code: "openSurveyAgain.fail.startDateAndEndDate")
                    redirect(uri: request.getHeader('referer'))
                    return
                }
            }

            result.surveyInfo.status = RDStore.SURVEY_SURVEY_STARTED
            result.surveyInfo.endDate = endDate
            result.surveyInfo.save(flush: true)
        }

        redirect action: 'show', id: params.id

    }

        @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def deleteSurveyParticipants() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.editable = (result.surveyInfo && result.surveyInfo.status != RDStore.SURVEY_IN_PROCESSING) ? false : result.editable

        if (params.selectedOrgs && result.editable) {

            params.list('selectedOrgs').each { soId ->
                SurveyOrg surveyOrg = SurveyOrg.findBySurveyConfigAndOrg(result.surveyConfig, Org.get(Long.parseLong(soId)))

                CostItem.findAllBySurveyOrg(surveyOrg).each {
                    it.delete(flush: true)
                }

                if (surveyOrg.delete(flush: true)) {
                    //flash.message = g.message(code: "surveyParticipants.delete.successfully")
                }
            }
        }

        redirect(uri: request.getHeader('referer'))

    }


    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def deleteDocuments() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        log.debug("deleteDocuments ${params}");

        docstoreService.unifiedDeleteDocuments(params)

        redirect action: 'surveyConfigDocs', id: result.surveyInfo.id, params: [surveyConfigID: result.surveyConfig.id]
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def deleteSurveyInfo() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.editable = (result.surveyInfo.status in [RDStore.SURVEY_IN_PROCESSING, RDStore.SURVEY_READY])

        if (result.editable) {

            try {

                SurveyInfo surveyInfo = SurveyInfo.get(result.surveyInfo.id)
                SurveyInfo.withTransaction {

                    SurveyConfig.findAllBySurveyInfo(surveyInfo).each { config ->

                        DocContext.findAllBySurveyConfig(config).each {
                            it.delete(flush: true)
                        }

                        SurveyConfigProperties.findAllBySurveyConfig(config).each {
                            it.delete(flush: true)
                        }

                        SurveyOrg.findAllBySurveyConfig(config).each { surveyOrg ->
                            CostItem.findAllBySurveyOrg(surveyOrg).each {
                                it.delete(flush: true)
                            }

                            surveyOrg.delete(flush: true)
                        }

                        SurveyResult.findAllBySurveyConfig(config) {
                            it.delete(flush: true)
                        }

                        Task.findAllBySurveyConfig(config) {
                            it.delete(flush: true)
                        }
                    }

                    SurveyConfig.executeUpdate("delete from SurveyConfig sc where sc.id in (:surveyConfigIDs)", [surveyConfigIDs: SurveyConfig.findAllBySurveyInfo(surveyInfo).id])


                    surveyInfo.delete(flush: true)
                }

                flash.message = message(code: 'surveyInfo.delete.successfully')

                redirect action: 'currentSurveysConsortia'
            }
            catch (DataIntegrityViolationException e) {
                flash.error = message(code: 'surveyInfo.delete.fail')

                redirect(uri: request.getHeader('referer'))
            }
        }


    }


    @DebugAnnotation(test = 'hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser().hasAffiliation("INST_EDITOR") })
    def editSurveyCostItem() {
        def result = setResultGenericsAndCheckAccess()
        result.putAll(financeService.setEditVars(result.institution))
        if (!result.editable) {
            response.sendError(401); return
        }
        result.costItem = CostItem.findById(params.costItem)


        Map<Long,Object> orgConfigurations = [:]
        result.costItemElements.each { oc ->
            orgConfigurations.put(oc.costItemElement.id,oc.elementSign.id)
        }

        result.orgConfigurations = orgConfigurations as JSON
        //result.selectedCostItemElement = params.selectedCostItemElement ?: RefdataValue.getByValueAndCategory('price: consortial price', 'CostItemElement').id.toString()

        result.participant = Org.get(params.participant)
        result.surveyOrg = SurveyOrg.findBySurveyConfigAndOrg(result.surveyConfig, result.participant)


        result.mode = result.costItem ? "edit" : ""
        result.taxKey = result.costItem ? result.costItem.taxKey : null
        render(template: "/survey/costItemModal", model: result)
    }

    @DebugAnnotation(test = 'hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser().hasAffiliation("INST_EDITOR") })
    def addForAllSurveyCostItem() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.putAll(financeService.setEditVars(result.institution))

        Map<Long,Object> orgConfigurations = [:]
        result.costItemElements.each { oc ->
            orgConfigurations.put(oc.costItemElement.id,oc.elementSign.id)
        }

        result.orgConfigurations = orgConfigurations as JSON
        //result.selectedCostItemElement = params.selectedCostItemElement ?: RefdataValue.getByValueAndCategory('price: consortial price', 'CostItemElement').id.toString()

        result.setting = 'bulkForAll'

        result.surveyOrgList = []

        if (params.get('orgsIDs')) {
            List idList = (params.get('orgsIDs')?.split(',').collect { Long.valueOf(it.trim()) }).toList()
            result.surveyOrgList = SurveyOrg.findAllByOrgInListAndSurveyConfig(Org.findAllByIdInList(idList), result.surveyConfig)
        }

        render(template: "/survey/costItemModal", model: result)
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def setInEvaluation() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.surveyInfo.status = RDStore.SURVEY_IN_EVALUATION

        if (result.surveyInfo.save(flush: true)) {
            //flash.message = g.message(code: 'survey.change.successfull')
        } else {
            flash.error = g.message(code: 'survey.change.fail')
        }

        redirect action: 'renewalWithSurvey', params:[surveyConfigID: result.surveyConfig.id, id: result.surveyInfo.id]

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def setCompleted() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.surveyInfo.status = RDStore.SURVEY_COMPLETED


        if (result.surveyInfo.save(flush: true)) {
            //flash.message = g.message(code: 'survey.change.successfull')
        } else {
            flash.error = g.message(code: 'survey.change.fail')
        }

        redirect(url: request.getHeader('referer'))

    }


    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def setCompleteSurvey() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.surveyInfo.status = RDStore.SURVEY_SURVEY_COMPLETED
        if (result.surveyInfo.save(flush: true)) {
            //flash.message = g.message(code: 'survey.change.successfull')
        } else {
            flash.error = g.message(code: 'survey.change.fail')
        }

        redirect(url: request.getHeader('referer'))

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def setSurveyConfigComment() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.surveyConfig.comment = params.comment

        if (!result.surveyConfig.save(flush: true)) {
            flash.error = g.message(code: 'default.save.error.general.message')
        }

        redirect(url: request.getHeader('referer'))

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def renewalWithSurvey() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }
        result.superOrgType = []
        if(accessService.checkPerm('ORG_CONSORTIUM')) {
            result.superOrgType << message(code:'consortium.superOrgType')
        }
        if(accessService.checkPerm('ORG_INST_COLLECTIVE')) {
            result.superOrgType << message(code:'collective.superOrgType')
        }

        result.parentSubscription = result.surveyConfig.subscription
        result.parentSubChilds = subscriptionService.getValidSubChilds(result.parentSubscription)
        result.parentSuccessorSubscription = result.surveyConfig.subscription.getCalculatedSuccessor()
        result.parentSuccessorSubChilds = result.parentSuccessorSubscription ? subscriptionService.getValidSubChilds(result.parentSuccessorSubscription) : null

        result.participationProperty = RDStore.SURVEY_PROPERTY_PARTICIPATION

        result.properties = []
        result.properties.addAll(SurveyConfigProperties.findAllBySurveyPropertyNotEqualAndSurveyConfig(result.participationProperty, result.surveyConfig)?.surveyProperty.sort {
            it.getI10n('name')
        })


        result.multiYearTermThreeSurvey = null
        result.multiYearTermTwoSurvey = null

        if (RDStore.SURVEY_PROPERTY_MULTI_YEAR_3.id in result.properties.id) {
            result.multiYearTermThreeSurvey = RDStore.SURVEY_PROPERTY_MULTI_YEAR_3
            result.properties.remove(result.multiYearTermThreeSurvey)
        }
        if (RDStore.SURVEY_PROPERTY_MULTI_YEAR_2.id in result.properties.id) {
            result.multiYearTermTwoSurvey = RDStore.SURVEY_PROPERTY_MULTI_YEAR_2
            result.properties.remove(result.multiYearTermTwoSurvey)

        }

        PropertyDefinition lateCommersProperty = PropertyDefinition.getByNameAndDescr("Späteinsteiger", PropertyDefinition.SUB_PROP)
        def currentParticipantIDs = []
        result.orgsWithMultiYearTermSub = []
        result.orgsLateCommers = []
        def orgsWithMultiYearTermOrgsID = []
        def orgsLateCommersOrgsID = []
        result.parentSubChilds.each { sub ->
            if (sub.isCurrentMultiYearSubscriptionNew())
            {
                result.orgsWithMultiYearTermSub << sub
                sub.getAllSubscribers().each { org ->
                    orgsWithMultiYearTermOrgsID << org.id
                }
            }
            else if (lateCommersProperty && lateCommersProperty.type == 'class com.k_int.kbplus.RefdataValue') {
                def subProp = sub.customProperties.find { it.type.id == lateCommersProperty.id }
                if(subProp.refValue == RefdataValue.getByValueAndCategory('Yes', lateCommersProperty.refdataCategory))
                {
                    result.orgsLateCommers << sub
                    sub.getAllSubscribers().each { org ->
                        orgsLateCommersOrgsID << org.id
                    }

                } else
                {
                    sub.getAllSubscribers().each { org ->
                        currentParticipantIDs << org.id
                    }
                }
            }
            else
            {
                sub.getAllSubscribers().each { org ->
                    currentParticipantIDs << org.id
                }
            }
        }


        result.orgsWithParticipationInParentSuccessor = []
        result.parentSuccessorSubChilds.each { sub ->
            sub.getAllSubscribers().each { org ->
                if(! (org.id in currentParticipantIDs)) {
                    result.orgsWithParticipationInParentSuccessor  << sub
                }
            }
        }

        result.orgsWithTermination = []

            //Orgs with termination there sub
            SurveyResult.executeQuery("from SurveyResult where owner.id = :owner and surveyConfig.id = :surConfig and type.id = :surProperty and refValue = :refValue  order by participant.sortname",
                    [
                     owner      : result.institution.id,
                     surProperty: result.participationProperty.id,
                     surConfig  : result.surveyConfig.id,
                     refValue   : RDStore.YN_NO]).each {
                def newSurveyResult = [:]
                newSurveyResult.participant = it.participant
                newSurveyResult.resultOfParticipation = it
                newSurveyResult.surveyConfig = result.surveyConfig
                newSurveyResult.sub = Subscription.executeQuery("Select s from Subscription s left join s.orgRelations orgR where s.instanceOf = :parentSub and orgR.org = :participant",
                        [parentSub  : result.parentSubscription,
                         participant: it.participant
                        ])[0]
                newSurveyResult.properties = SurveyResult.findAllByParticipantAndOwnerAndSurveyConfigAndTypeInList(it.participant, result.institution, result.surveyConfig, result.properties).sort {
                    it.type.getI10n('name')
                }

                result.orgsWithTermination << newSurveyResult

            }


        // Orgs that renew or new to Sub
        result.orgsContinuetoSubscription = []
        result.newOrgsContinuetoSubscription = []

            SurveyResult.executeQuery("from SurveyResult where owner.id = :owner and surveyConfig.id = :surConfig and type.id = :surProperty and refValue = :refValue order by participant.sortname",
                    [
                     owner      : result.institution.id,
                     surProperty: result.participationProperty.id,
                     surConfig  : result.surveyConfig.id,
                     refValue   : RDStore.YN_YES]).each {
                def newSurveyResult = [:]
                newSurveyResult.participant = it.participant
                newSurveyResult.resultOfParticipation = it
                newSurveyResult.surveyConfig = result.surveyConfig
                newSurveyResult.properties = SurveyResult.findAllByParticipantAndOwnerAndSurveyConfigAndTypeInList(it.participant, result.institution, result.surveyConfig, result.properties).sort {
                    it.type.getI10n('name')
                }

                if (it.participant.id in currentParticipantIDs) {

                    newSurveyResult.sub = Subscription.executeQuery("Select s from Subscription s left join s.orgRelations orgR where s.instanceOf = :parentSub and orgR.org = :participant",
                            [parentSub  : result.parentSubscription,
                             participant: it.participant
                            ])[0]

                    //newSurveyResult.sub = result.parentSubscription.getDerivedSubscriptionBySubscribers(it.participant)

                    if (result.multiYearTermTwoSurvey) {

                        newSurveyResult.newSubPeriodTwoStartDate = null
                        newSurveyResult.newSubPeriodTwoEndDate = null

                        def participantPropertyTwo = SurveyResult.findByParticipantAndOwnerAndSurveyConfigAndType(it.participant, result.institution, result.surveyConfig, result.multiYearTermTwoSurvey)

                        if (participantPropertyTwo.refValue.id == RDStore.YN_YES.id) {
                            use(TimeCategory) {
                                newSurveyResult.newSubPeriodTwoStartDate = newSurveyResult.sub.startDate ? (newSurveyResult.sub.endDate + 1.day) : null
                                newSurveyResult.newSubPeriodTwoEndDate = newSurveyResult.sub.endDate ? (newSurveyResult.sub.endDate + 2.year) : null
                                newSurveyResult.participantPropertyTwoComment = participantPropertyTwo.comment
                            }
                        }

                    }
                    if (result.multiYearTermThreeSurvey) {
                        newSurveyResult.newSubPeriodThreeStartDate = null
                        newSurveyResult.newSubPeriodThreeEndDate = null

                        def participantPropertyThree = SurveyResult.findByParticipantAndOwnerAndSurveyConfigAndType(it.participant, result.institution, result.surveyConfig, result.multiYearTermThreeSurvey)
                        if (participantPropertyThree.refValue.id == RDStore.YN_YES.id) {
                            use(TimeCategory) {
                                newSurveyResult.newSubPeriodThreeStartDate = newSurveyResult.sub.startDate ? (newSurveyResult.sub.endDate + 1.day) : null
                                newSurveyResult.newSubPeriodThreeEndDate = newSurveyResult.sub.endDate ? (newSurveyResult.sub.endDate + 3.year) : null
                                newSurveyResult.participantPropertyThreeComment = participantPropertyThree.comment
                            }
                        }
                    }

                    result.orgsContinuetoSubscription << newSurveyResult
                }
                if (!(it.participant.id in currentParticipantIDs) && !(it.participant.id in orgsLateCommersOrgsID) && !(it.participant.id in orgsWithMultiYearTermOrgsID)) {


                    if (result.multiYearTermTwoSurvey) {

                        newSurveyResult.newSubPeriodTwoStartDate = null
                        newSurveyResult.newSubPeriodTwoEndDate = null

                        def participantPropertyTwo = SurveyResult.findByParticipantAndOwnerAndSurveyConfigAndType(it.participant, result.institution, result.surveyConfig, result.multiYearTermTwoSurvey)

                        if (participantPropertyTwo?.refValue.id == RDStore.YN_YES.id) {
                            use(TimeCategory) {
                                newSurveyResult.newSubPeriodTwoStartDate = result.parentSubscription.startDate ? (result.parentSubscription.endDate + 1.day) : null
                                newSurveyResult.newSubPeriodTwoEndDate = result.parentSubscription.endDate ? (result.parentSubscription.endDate + 2.year) : null
                                newSurveyResult.participantPropertyTwoComment = participantPropertyTwo.comment
                            }
                        }

                    }
                    if (result.multiYearTermThreeSurvey) {
                        newSurveyResult.newSubPeriodThreeStartDate = null
                        newSurveyResult.newSubPeriodThreeEndDate = null

                        def participantPropertyThree = SurveyResult.findByParticipantAndOwnerAndSurveyConfigAndType(it.participant, result.institution, result.surveyConfig, result.multiYearTermThreeSurvey)
                        if (participantPropertyThree?.refValue.id == RDStore.YN_YES.id) {
                            use(TimeCategory) {
                                newSurveyResult.newSubPeriodThreeStartDate = result.parentSubscription.startDate ? (result.parentSubscription.endDate + 1.day) : null
                                newSurveyResult.newSubPeriodThreeEndDate = result.parentSubscription.endDate ? (result.parentSubscription.endDate + 3.year) : null
                                newSurveyResult.participantPropertyThreeComment = participantPropertyThree.comment
                            }
                        }
                    }

                    result.newOrgsContinuetoSubscription << newSurveyResult
                }


            }


        //Orgs without really result
        result.orgsWithoutResult = []

            SurveyResult.executeQuery("from SurveyResult where owner.id = :owner and surveyConfig.id = :surConfig and type.id = :surProperty and refValue is null order by participant.sortname",
                    [
                     owner      : result.institution.id,
                     surProperty: result.participationProperty.id,
                     surConfig  : result.surveyConfig.id]).each {
                def newSurveyResult = [:]
                newSurveyResult.participant = it.participant
                newSurveyResult.resultOfParticipation = it
                newSurveyResult.surveyConfig = result.surveyConfig
                newSurveyResult.properties = SurveyResult.findAllByParticipantAndOwnerAndSurveyConfigAndTypeInList(it.participant, result.institution, result.surveyConfig, result.properties).sort {
                    it.type.getI10n('name')
                }

                if (it.participant.id in currentParticipantIDs) {
                    newSurveyResult.sub = Subscription.executeQuery("Select s from Subscription s left join s.orgRelations orgR where s.instanceOf = :parentSub and orgR.org = :participant",
                            [parentSub  : result.parentSubscription,
                             participant: it.participant
                            ])[0]
                    //newSurveyResult.sub = result.parentSubscription.getDerivedSubscriptionBySubscribers(it.participant)
                } else {
                    newSurveyResult.sub = null
                }
                result.orgsWithoutResult << newSurveyResult
            }


        //MultiYearTerm Subs
        def sumParticipantWithSub = ((result.orgsContinuetoSubscription.groupBy {
            it.participant.id
        }.size()?:0) + (result.orgsWithTermination.groupBy { it.participant.id }.size()?:0) + (result.orgsWithMultiYearTermSub.size()?:0))

        if (sumParticipantWithSub < result.parentSubChilds.size()?:0) {
            def property = PropertyDefinition.getByNameAndDescr("Perennial term checked", PropertyDefinition.SUB_PROP)

            def removeSurveyResultOfOrg = []
            result.orgsWithoutResult.each { surveyResult ->
                if (surveyResult.participant.id in currentParticipantIDs && surveyResult.sub) {

                    if (property.type == 'class com.k_int.kbplus.RefdataValue') {
                        if (surveyResult.sub.customProperties.find {
                            it.type.id == property.id
                        }.refValue == RefdataValue.getByValueAndCategory('Yes', property.refdataCategory)) {

                            result.orgsWithMultiYearTermSub << surveyResult.sub
                            removeSurveyResultOfOrg << surveyResult
                        }
                    }
                }
            }
            removeSurveyResultOfOrg.each{ it
                result.orgsWithoutResult?.remove(it)
            }

            result.orgsWithMultiYearTermSub = result.orgsWithMultiYearTermSub.sort{it.getAllSubscribers()[0].sortname}

        }


        def message = g.message(code: 'renewalexport.renewals')
        SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
        String datetoday = sdf.format(new Date(System.currentTimeMillis()))
        String filename = message + "_" + result.surveyConfig.getSurveyName() +"_${datetoday}"
        if (params.exportXLSX) {
            try {
                SXSSFWorkbook wb = (SXSSFWorkbook) exportRenewalResult(result)
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
                log.error("Problem", e);
                response.sendError(500)
            }
        }

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def copySurvey() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0

        if(result.surveyInfo.type.id == RDStore.SURVEY_TYPE_INTEREST.id){
            result.workFlow = '2'
        }else{
            if(params.targetSubs){
                result.workFlow = '2'
            }else{
                result.workFlow = '1'
            }
        }

        if(result.workFlow == '1') {
            def date_restriction = null;
            def sdf = DateUtil.getSDF_NoTime()

            if (params.validOn == null || params.validOn.trim() == '') {
                result.validOn = ""
            } else {
                result.validOn = params.validOn
                date_restriction = sdf.parse(params.validOn)
            }

            result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")

            if (!result.editable) {
                flash.error = g.message(code: "default.notAutorized.message")
                redirect(url: request.getHeader('referer'))
            }

            if (!params.status) {
                if (params.isSiteReloaded != "yes") {
                    params.status = RDStore.SUBSCRIPTION_CURRENT.id
                    result.defaultSet = true
                } else {
                    params.status = 'FETCH_ALL'
                }
            }

            List orgIds = orgTypeService.getCurrentOrgIdsOfProvidersAndAgencies(contextService.org)

            result.providers = Org.findAllByIdInList(orgIds).sort { it.name }

            def tmpQ = subscriptionsQueryService.myInstitutionCurrentSubscriptionsBaseQuery(params, contextService.org)
            result.filterSet = tmpQ[2]
            List subscriptions = Subscription.executeQuery("select s ${tmpQ[0]}", tmpQ[1])
            //,[max: result.max, offset: result.offset]

            result.propList = PropertyDefinition.findAllPublicAndPrivateProp([PropertyDefinition.SUB_PROP], contextService.org)

            if (params.sort && params.sort.indexOf("§") >= 0) {
                switch (params.sort) {
                    case "orgRole§provider":
                        subscriptions.sort { x, y ->
                            String a = x.getProviders().size() > 0 ? x.getProviders().first().name : ''
                            String b = y.getProviders().size() > 0 ? y.getProviders().first().name : ''
                            a.compareToIgnoreCase b
                        }
                        if (params.order.equals("desc"))
                            subscriptions.reverse(true)
                        break
                }
            }
            result.num_sub_rows = subscriptions.size()
            result.subscriptions = subscriptions.drop((int) result.offset).take((int) result.max)
        }
        
        result.targetSubs = params.targetSubs ? Subscription.findAllByIdInList(params.list('targetSubs').collect { it -> Long.parseLong(it) }): null

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def addSubMembersToSurvey() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        addSubMembers(result.surveyConfig)

        redirect(action: 'surveyParticipants', params: [id: result.surveyInfo.id, surveyConfigID: result.surveyConfig.id, tab: 'selectedSubParticipants'])

    }
    
    
    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processCopySurvey() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        SurveyInfo baseSurveyInfo = result.surveyInfo
        SurveyConfig baseSurveyConfig = result.surveyConfig

        if (baseSurveyInfo && baseSurveyConfig) {

            result.targetSubs = params.targetSubs ? Subscription.findAllByIdInList(params.list('targetSubs').collect { it -> Long.parseLong(it) }): null

            List newSurveyIds = []

            if(result.targetSubs){
                result.targetSubs.each { sub ->
                    SurveyInfo newSurveyInfo = new SurveyInfo(
                            name: sub.name,
                            status: RDStore.SURVEY_IN_PROCESSING,
                            type: baseSurveyInfo.type,
                            startDate: params.copySurvey.copyDates ? baseSurveyInfo.startDate : null,
                            endDate: params.copySurvey.copyDates ? baseSurveyInfo.endDate : null,
                            comment: params.copySurvey.copyComment ? baseSurveyInfo.comment : null,
                            isMandatory: params.copySurvey.copyMandatory ? baseSurveyInfo.isMandatory : false,
                            owner: contextService.getOrg()
                    ).save()

                    SurveyConfig newSurveyConfig = new SurveyConfig(
                            type: baseSurveyConfig.type,
                            subscription: sub,
                            surveyInfo: newSurveyInfo,
                            comment: params.copySurvey.copySurveyConfigComment ? baseSurveyConfig.comment : null,
                            url: params.copySurvey.copySurveyConfigUrl ? baseSurveyConfig.url : null,
                            configOrder: newSurveyInfo.surveyConfigs ? newSurveyInfo.surveyConfigs.size() + 1 : 1
                    ).save()

                    copySurveyConfigCharacteristic(baseSurveyConfig, newSurveyConfig, params)

                    newSurveyIds << newSurveyInfo.id

                }

                redirect controller: 'survey', action: 'currentSurveysConsortia', params: [ids: newSurveyIds]
            }else{
                SurveyInfo newSurveyInfo = new SurveyInfo(
                        name: params.name,
                        status: RDStore.SURVEY_IN_PROCESSING,
                        type: baseSurveyInfo.type,
                        startDate: params.copySurvey.copyDates ? baseSurveyInfo.startDate : null,
                        endDate: params.copySurvey.copyDates ? baseSurveyInfo.endDate : null,
                        comment: params.copySurvey.copyComment ? baseSurveyInfo.comment : null,
                        isMandatory: params.copySurvey.copyMandatory ? baseSurveyInfo.isMandatory : false,
                        owner: contextService.getOrg()
                ).save()

                SurveyConfig newSurveyConfig = new SurveyConfig(
                        type: baseSurveyConfig.type,
                        surveyInfo: newSurveyInfo,
                        comment: params.copySurvey.copySurveyConfigComment ? baseSurveyConfig.comment : null,
                        url: params.copySurvey.copySurveyConfigUrl ? baseSurveyConfig.url : null,
                        configOrder: newSurveyInfo.surveyConfigs ? newSurveyInfo.surveyConfigs.size() + 1 : 1
                ).save()

                copySurveyConfigCharacteristic(baseSurveyConfig, newSurveyConfig, params)

                redirect controller: 'survey', action: 'show', params: [id: newSurveyInfo.id, surveyConfigID: newSurveyConfig.id]
            }
        }

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def renewSubscriptionConsortiaWithSurvey() {

        def result = setResultGenericsAndCheckAccess()
        result.institution = contextService.org
        if (!(result || accessService.checkPerm("ORG_CONSORTIUM"))) {
            response.sendError(401); return
        }

        def subscription = Subscription.get(params.parentSub ?: null)

        SimpleDateFormat sdf = new SimpleDateFormat('dd.MM.yyyy')

        result.errors = []
        def newStartDate
        def newEndDate
        use(TimeCategory) {
            newStartDate = subscription.endDate ? (subscription.endDate + 1.day) : null
            newEndDate = subscription.endDate ? (subscription.endDate + 1.year) : null
        }
        params.surveyConfig = params.surveyConfig ?: null
        result.isRenewSub = true
        result.permissionInfo = [sub_startDate: newStartDate ? sdf.format(newStartDate) : null,
                                 sub_endDate  : newEndDate ? sdf.format(newEndDate) : null,
                                 sub_name     : subscription.name,
                                 sub_id       : subscription.id,
                                 sub_license  : subscription.owner?.reference ?: '',
                                 sub_status   : RDStore.SUBSCRIPTION_INTENDED.id.toString(),
                                 sub_type     : subscription.type?.id.toString(),
                                 sub_form     : subscription.form?.id.toString(),
                                 sub_resource : subscription.resource?.id.toString(),
                                 sub_kind     : subscription.kind?.id.toString(),
                                 sub_isPublicForApi : subscription.isPublicForApi ? RDStore.YN_YES.id.toString() : RDStore.YN_NO.id.toString(),
                                 sub_hasPerpetualAccess : subscription.hasPerpetualAccess ? RDStore.YN_YES.id.toString() : RDStore.YN_NO.id.toString()

        ]

        result.subscription = subscription
        result
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processRenewalwithSurvey() {

        def result = setResultGenericsAndCheckAccess()
        if (!(result || accessService.checkPerm("ORG_CONSORTIUM"))) {
            response.sendError(401); return
        }

        Subscription baseSub = Subscription.get(params.parentSub ?: null)

        ArrayList<Links> previousSubscriptions = Links.findAllByDestinationAndObjectTypeAndLinkType(baseSub.id, Subscription.class.name, RDStore.LINKTYPE_FOLLOWS)
        if (previousSubscriptions.size() > 0) {
            flash.error = message(code: 'subscription.renewSubExist')
        } else {
            def sub_startDate = params.subscription.start_date ? parseDate(params.subscription.start_date, possible_date_formats) : null
            def sub_endDate = params.subscription.end_date ? parseDate(params.subscription.end_date, possible_date_formats) : null
            def sub_status = params.subStatus
            def sub_type = params.subType
            def sub_kind = params.subKind
            def sub_form = params.subForm
            def sub_resource = params.subResource
            def sub_hasPerpetualAccess = params.subHasPerpetualAccess == '1'
            def sub_isPublicForApi = params.subIsPublicForApi == '1'
            def old_subOID = params.subscription.old_subid
            def new_subname = params.subscription.name
            def manualCancellationDate = null

            use(TimeCategory) {
                manualCancellationDate =  baseSub.manualCancellationDate ? (baseSub.manualCancellationDate + 1.year) : null
            }
            def newSub = new Subscription(
                    name: new_subname,
                    startDate: sub_startDate,
                    endDate: sub_endDate,
                    manualCancellationDate: manualCancellationDate,
                    identifier: java.util.UUID.randomUUID().toString(),
                    isSlaved: baseSub.isSlaved,
                    type: sub_type,
                    kind: sub_kind,
                    status: sub_status,
                    resource: sub_resource,
                    form: sub_form,
                    hasPerpetualAccess: sub_hasPerpetualAccess,
                    isPublicForApi: sub_isPublicForApi
            )

            if (!newSub.save(flush: true)) {
                log.error("Problem saving subscription ${newSub.errors}");
                return newSub
            } else {

                log.debug("Save ok");
                if (params.list('auditList')) {
                    //copy audit
                    params.list('auditList').each { auditField ->
                        //All ReferenceFields were copied!
                        //'name', 'startDate', 'endDate', 'manualCancellationDate', 'status', 'type', 'form', 'resource'
                        //println(auditField)
                        AuditConfig.addConfig(newSub, auditField)
                    }
                }
                //Copy References
                //OrgRole
                baseSub.orgRelations.each { or ->

                    if ((or.org.id == contextService.getOrg().id) || (or.roleType.value in ['Subscriber', 'Subscriber_Consortial'])) {
                        OrgRole newOrgRole = new OrgRole()
                        InvokerHelper.setProperties(newOrgRole, or.properties)
                        newOrgRole.sub = newSub
                        newOrgRole.save(flush: true)
                    }
                }
                //link to previous subscription
                Links prevLink = new Links(source: newSub.id, destination: baseSub.id, objectType: Subscription.class.name, linkType: RDStore.LINKTYPE_FOLLOWS, owner: contextService.org)
                if (!prevLink.save(flush: true)) {
                    log.error("Problem linking to previous subscription: ${prevLink.errors}")
                }
                result.newSub = newSub

                if (params.targetSubscriptionId == "null") params.remove("targetSubscriptionId")
                result.isRenewSub = true

                    redirect controller: 'survey',
                            action: 'copyElementsIntoRenewalSubscription',
                            id: old_subOID,
                            params: [sourceSubscriptionId: old_subOID, targetSubscriptionId: newSub.id, isRenewSub: true]

            }
        }
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def copyElementsIntoRenewalSubscription() {
        def result = setResultGenericsAndCheckAccessforSub(AccessService.CHECK_VIEW)
        if (!result) {
            response.sendError(401); return
        }
        flash.error = ""
        flash.message = ""
        if (params.sourceSubscriptionId == "null") params.remove("sourceSubscriptionId")
        result.sourceSubscriptionId = params.sourceSubscriptionId ?: params.id
        result.sourceSubscription = Subscription.get(Long.parseLong(params.sourceSubscriptionId ?: params.id))

        if (params.targetSubscriptionId == "null") params.remove("targetSubscriptionId")
        if (params.targetSubscriptionId) {
            result.targetSubscriptionId = params.targetSubscriptionId
            result.targetSubscription = Subscription.get(Long.parseLong(params.targetSubscriptionId))
        }

        if (params.isRenewSub) {
            result.isRenewSub = params.isRenewSub
        }

        result.isConsortialSubs = (result.sourceSubscription.getCalculatedType() == CalculatedType.TYPE_CONSORTIAL && result.targetSubscription.getCalculatedType() == CalculatedType.TYPE_CONSORTIAL) ?: false

        result.allSubscriptions_readRights = subscriptionService.getMySubscriptions_readRights()
        result.allSubscriptions_writeRights = subscriptionService.getMySubscriptions_writeRights()

        switch (params.workFlowPart) {
            case WORKFLOW_DATES_OWNER_RELATIONS:
                result << copySubElements_DatesOwnerRelations();
                if (params.isRenewSub) {
                    params.workFlowPart = WORKFLOW_PACKAGES_ENTITLEMENTS
                    result << loadDataFor_PackagesEntitlements()
                } else {
                    result << loadDataFor_DatesOwnerRelations()
                }
                break;
            case WORKFLOW_PACKAGES_ENTITLEMENTS:
                result << copySubElements_PackagesEntitlements();
                if (params.isRenewSub) {
                    params.workFlowPart = WORKFLOW_DOCS_ANNOUNCEMENT_TASKS
                    result << loadDataFor_DocsAnnouncementsTasks()
                } else {
                    result << loadDataFor_PackagesEntitlements()
                }
                break;
            case WORKFLOW_DOCS_ANNOUNCEMENT_TASKS:
                result << copySubElements_DocsAnnouncementsTasks();
                if (params.isRenewSub) {
                        params.workFlowPart = WORKFLOW_PROPERTIES
                        result << loadDataFor_Properties()
                } else {
                    result << loadDataFor_DocsAnnouncementsTasks()
                }
                break;
            case WORKFLOW_SUBSCRIBER:
                result << copySubElements_Subscriber();
                if (params.isRenewSub) {
                    params.workFlowPart = WORKFLOW_PROPERTIES
                    result << loadDataFor_Properties()
                } else {
                    result << loadDataFor_Subscriber()
                }
                break;
            case WORKFLOW_PROPERTIES:
                result << copySubElements_Properties();
                if (params.isRenewSub && params.targetSubscriptionId) {
                    def surveyConfig = SurveyConfig.findBySubscriptionAndSubSurveyUseForTransfer(result.sourceSubscription, true)
                    /*flash.error = ""
                    flash.message = ""*/
                    if(surveyConfig) {
                        redirect controller: 'survey', action: 'renewalWithSurvey', params: [id: surveyConfig.surveyInfo.id, surveyConfigID: surveyConfig.id]
                    }else {
                        redirect controller: 'subscription', action: 'show', params: [id: params.targetSubscriptionId]
                    }
                } else {
                    result << loadDataFor_Properties()
                }
                break;
            case WORKFLOW_END:
                result << copySubElements_Properties();
                if (params.targetSubscriptionId) {
                    def surveyConfig = SurveyConfig.findBySubscriptionAndSubSurveyUseForTransfer(result.sourceSubscription, true)
                    /*flash.error = ""
                    flash.message = ""*/
                    if(surveyConfig) {
                        redirect controller: 'survey', action: 'renewalWithSurvey', params: [id: surveyConfig.surveyInfo.id, surveyConfigID: surveyConfig.id]
                    }else {
                        redirect controller: 'subscription', action: 'show', params: [id: params.targetSubscriptionId]
                    }
                }
                break;
            default:
                result << loadDataFor_DatesOwnerRelations()
                break;
        }

        if (params.targetSubscriptionId) {
            result.targetSubscription = Subscription.get(Long.parseLong(params.targetSubscriptionId))
        }
        result.workFlowPart = params.workFlowPart ?: WORKFLOW_DATES_OWNER_RELATIONS
        result.workFlowPartNext = params.workFlowPartNext ?: WORKFLOW_DOCS_ANNOUNCEMENT_TASKS

        if (params.isRenewSub) {
            result.isRenewSub = params.isRenewSub
        }
        result
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    Map copySubElements_Properties() {
        LinkedHashMap result = [customProperties: [:], privateProperties: [:]]
        Subscription baseSub = Subscription.get(params.sourceSubscriptionId ? Long.parseLong(params.sourceSubscriptionId) : params.id)
        boolean isRenewSub = params.isRenewSub ? true : false

        Subscription newSub = null
        List auditProperties = params.list('auditProperties')
        List<Subscription> subsToCompare = [baseSub]
        if (params.targetSubscriptionId) {
            newSub = Subscription.get(Long.parseLong(params.targetSubscriptionId))
            subsToCompare.add(newSub)
        }
        List<AbstractPropertyWithCalculatedLastUpdated> propertiesToTake = params.list('subscription.takeProperty').collect {
            genericOIDService.resolveOID(it)
        }
        if (propertiesToTake && isBothSubscriptionsSet(baseSub, newSub)) {
            surveyService.copyProperties(propertiesToTake, newSub, isRenewSub, flash, auditProperties)
        }

        List<AbstractPropertyWithCalculatedLastUpdated> propertiesToDelete = params.list('subscription.deleteProperty').collect {
            genericOIDService.resolveOID(it)
        }
        if (propertiesToDelete && isBothSubscriptionsSet(baseSub, newSub)) {
            surveyService.deleteProperties(propertiesToDelete, newSub, isRenewSub, flash, auditProperties)
        }

        if (newSub) {
            result.newSub = Subscription.get(newSub.id)
        }
        result
    }


    private loadDataFor_Properties() {
        LinkedHashMap result = [customProperties: [:], privateProperties: [:]]
        Subscription baseSub = Subscription.get(params.sourceSubscriptionId ? Long.parseLong(params.sourceSubscriptionId) : params.id)
        Subscription newSub = null
        List<Subscription> subsToCompare = [baseSub]
        if (params.targetSubscriptionId) {
            newSub = Subscription.get(Long.parseLong(params.targetSubscriptionId))
            subsToCompare.add(newSub)
        }

        if (newSub) {
            result.newSub = newSub.refresh()
        }
        subsToCompare.each { sub ->
            Map customProperties = result.customProperties
            sub = GrailsHibernateUtil.unwrapIfProxy(sub)
            customProperties = comparisonService.buildComparisonTree(customProperties, sub, sub.customProperties.sort{it.type.getI10n('name')})
            result.customProperties = customProperties
            Map privateProperties = result.privateProperties
            privateProperties = comparisonService.buildComparisonTree(privateProperties, sub, sub.privateProperties.sort{it.type.getI10n('name')})
            result.privateProperties = privateProperties
        }
        result
    }

    private boolean isBothSubscriptionsSet(Subscription baseSub, Subscription newSub) {
        if (!baseSub || !newSub) {
            if (!baseSub) flash.error += message(code: 'subscription.details.copyElementsIntoSubscription.noSubscriptionSource') + '<br />'
            if (!newSub) flash.error += message(code: 'subscription.details.copyElementsIntoSubscription.noSubscriptionTarget') + '<br />'
            return false
        }
        return true
    }


    private copySubElements_PackagesEntitlements() {
        def result = setResultGenericsAndCheckAccessforSub(AccessService.CHECK_VIEW)
        Subscription baseSub = Subscription.get(params.sourceSubscriptionId ? Long.parseLong(params.sourceSubscriptionId) : params.id)
        Subscription newSub = params.targetSubscriptionId ? Subscription.get(Long.parseLong(params.targetSubscriptionId)) : null

        boolean isTargetSubChanged = false
        if (params.subscription.deletePackageIds && isBothSubscriptionsSet(baseSub, newSub)) {
            List<SubscriptionPackage> packagesToDelete = params.list('subscription.deletePackageIds').collect {
                genericOIDService.resolveOID(it)
            }
            subscriptionService.deletePackages(packagesToDelete, newSub, flash)
            isTargetSubChanged = true
        }
        if (params.subscription.takePackageIds && isBothSubscriptionsSet(baseSub, newSub)) {
            List<SubscriptionPackage> packagesToTake = params.list('subscription.takePackageIds').collect {
                genericOIDService.resolveOID(it)
            }
            subscriptionService.copyPackages(packagesToTake, newSub, flash)
            isTargetSubChanged = true
        }
        if(params.subscription.deletePackageSettings && isBothSubscriptionsSet(baseSub, newSub)) {
            List<SubscriptionPackage> packageSettingsToDelete = params.list('subscription.deletePackageSettings').collect {
                genericOIDService.resolveOID(it)
            }
            packageSettingsToDelete.each { SubscriptionPackage toDelete ->
                PendingChangeConfiguration.SETTING_KEYS.each { String setting ->
                    if(AuditConfig.getConfig(toDelete.subscription,setting))
                        AuditConfig.removeConfig(toDelete.subscription,setting)
                }
                PendingChangeConfiguration.executeUpdate('delete from PendingChangeConfiguration pcc where pcc.subscriptionPackage = :sp',[sp:toDelete])
            }
            isTargetSubChanged = true
        }
        if(params.subscription.takePackageSettings && isBothSubscriptionsSet(baseSub, newSub)) {
            List<SubscriptionPackage> packageSettingsToTake = params.list('subscription.takePackageSettings').collect {
                genericOIDService.resolveOID(it)
            }
            packageSettingsToTake.each { SubscriptionPackage sp ->
                subscriptionService.copyPendingChangeConfiguration(PendingChangeConfiguration.findAllBySubscriptionPackage(sp),SubscriptionPackage.findBySubscriptionAndPkg(newSub,sp.pkg))
            }
            isTargetSubChanged = true
        }

        if (params.subscription.deleteEntitlementIds && isBothSubscriptionsSet(baseSub, newSub)) {
            List<IssueEntitlement> entitlementsToDelete = params.list('subscription.deleteEntitlementIds').collect {
                genericOIDService.resolveOID(it)
            }
            subscriptionService.deleteEntitlements(entitlementsToDelete, newSub, flash)
            isTargetSubChanged = true
        }
        if (params.subscription.takeEntitlementIds && isBothSubscriptionsSet(baseSub, newSub)) {
            List<IssueEntitlement> entitlementsToTake = params?.list('subscription.takeEntitlementIds').collect {
                genericOIDService.resolveOID(it)
            }
            subscriptionService.copyEntitlements(entitlementsToTake, newSub, flash)
            isTargetSubChanged = true
        }

        if (isTargetSubChanged) {
            newSub = newSub.refresh()
        }
        result.newSub = newSub
        result.subscription = baseSub
        result
    }

    private loadDataFor_PackagesEntitlements() {
        def result = setResultGenericsAndCheckAccessforSub(AccessService.CHECK_VIEW)
        Subscription baseSub = Subscription.get(params.sourceSubscriptionId ? Long.parseLong(params.sourceSubscriptionId) : params.id)
        Subscription newSub = params.targetSubscriptionId ? Subscription.get(Long.parseLong(params.targetSubscriptionId)) : null
        result.sourceIEs = subscriptionService.getIssueEntitlements(baseSub)
        result.targetIEs = subscriptionService.getIssueEntitlements(newSub)
        result.newSub = newSub
        result.subscription = baseSub
        result
    }

    private copySubElements_DatesOwnerRelations() {
        def result = setResultGenericsAndCheckAccessforSub(AccessService.CHECK_VIEW)
        Subscription baseSub = Subscription.get(params.sourceSubscriptionId ? Long.parseLong(params.sourceSubscriptionId) : params.id)
        Subscription newSub = params.targetSubscriptionId ? Subscription.get(Long.parseLong(params.targetSubscriptionId)) : null

        //boolean isTargetSubChanged = false
        if (params.subscription?.deleteDates && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.deleteDates(newSub, flash)
            //isTargetSubChanged = true
        } else if (params?.subscription?.takeDates && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.copyDates(baseSub, newSub, flash)
            //isTargetSubChanged = true
        }

        if (params.subscription?.deleteStatus && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.deleteStatus(newSub, flash)
            //isTargetSubChanged = true
        }else if (params.subscription?.takeStatus && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.copyStatus(baseSub, newSub, flash)
            //isTargetSubChanged = true
        }

        if (params.subscription?.deleteKind && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.deleteKind(newSub, flash)
            //isTargetSubChanged = true
        }else if (params.subscription?.takeKind && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.copyKind(baseSub, newSub, flash)
            //isTargetSubChanged = true
        }

        if (params.subscription.deleteForm && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.deleteForm(newSub, flash)
            //isTargetSubChanged = true
        }else if (params.subscription?.takeForm && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.copyForm(baseSub, newSub, flash)
            //isTargetSubChanged = true
        }

        if (params.subscription?.deleteResource && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.deleteResource(newSub, flash)
            //isTargetSubChanged = true
        }else if (params.subscription?.takeResource && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.copyResource(baseSub, newSub, flash)
            //isTargetSubChanged = true
        }

        if (params.subscription?.deletePublicForApi && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.deletePublicForApi(newSub, flash)
            //isTargetSubChanged = true
        }else if (params.subscription?.takePublicForApi && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.copyPublicForApi(baseSub, newSub, flash)
            //isTargetSubChanged = true
        }

        if (params.subscription?.deletePerpetualAccess && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.deletePerpetualAccess(newSub, flash)
            //isTargetSubChanged = true
        }else if (params.subscription?.takePerpetualAccess && isBothSubscriptionsSet(baseSub, newSub)) {
            subscriptionService.copyPerpetualAccess(baseSub, newSub, flash)
            //isTargetSubChanged = true
        }

        if (params.subscription?.deleteOwner && isBothSubscriptionsSet(baseSub, newSub)) {
            if(!subscriptionService.setOrgLicRole(newSub, null)) {
                Object[] args = [newSub]
                flash.error += message(code:'default.save.error.message',args:args)
            }
        } else if (params.subscription?.takeOwner && isBothSubscriptionsSet(baseSub, newSub)) {
            if(!subscriptionService.setOrgLicRole(newSub, baseSub.owner)) {
                Object[] args = [newSub]
                flash.error += message(code:'default.save.error.message',args:args)
            }
        }

        if (params.subscription?.deleteOrgRelations && isBothSubscriptionsSet(baseSub, newSub)) {
            List<OrgRole> toDeleteOrgRelations = params.list('subscription.deleteOrgRelations').collect {
                genericOIDService.resolveOID(it)
            }
            subscriptionService.deleteOrgRelations(toDeleteOrgRelations, newSub, flash)
            //isTargetSubChanged = true
        }
        if (params.subscription?.takeOrgRelations && isBothSubscriptionsSet(baseSub, newSub)) {
            List<OrgRole> toCopyOrgRelations = params.list('subscription.takeOrgRelations').collect {
                genericOIDService.resolveOID(it)
            }
            subscriptionService.copyOrgRelations(toCopyOrgRelations, baseSub, newSub, flash)
            //isTargetSubChanged = true

            List<OrgRole> toggleShareOrgRoles = params.list('toggleShareOrgRoles').collect {
                genericOIDService.resolveOID(it)
            }

            newSub = newSub.refresh()
            newSub.orgRelations.each {newSubOrgRole ->

                if(newSubOrgRole.org in toggleShareOrgRoles.org)
                {
                    newSubOrgRole.isShared = true
                    newSubOrgRole.save(flush:true)

                    ((ShareSupport) newSub).updateShare(newSubOrgRole)
                }
            }
        }

        /*if (isTargetSubChanged) {
            newSub = newSub.refresh()
        }*/
        result.subscription = baseSub
        result.newSub = newSub
        result.targetSubscription = newSub
        result
    }

    private loadDataFor_DatesOwnerRelations() {
        def result = setResultGenericsAndCheckAccessforSub(AccessService.CHECK_VIEW)
        Subscription baseSub = Subscription.get(params.sourceSubscriptionId ? Long.parseLong(params.sourceSubscriptionId) : params.id)
        Subscription newSub = params.targetSubscriptionId ? Subscription.get(Long.parseLong(params.targetSubscriptionId)) : null

        // restrict visible for templates/links/orgLinksAsList
        result.source_visibleOrgRelations = subscriptionService.getVisibleOrgRelations(baseSub)
        result.target_visibleOrgRelations = subscriptionService.getVisibleOrgRelations(newSub)
        result
    }

    private copySubElements_DocsAnnouncementsTasks() {
        def result = setResultGenericsAndCheckAccessforSub(AccessService.CHECK_VIEW)
        Subscription baseSub = Subscription.get(params.sourceSubscriptionId ? Long.parseLong(params.sourceSubscriptionId) : Long.parseLong(params.id))
        Subscription newSub = null
        if (params.targetSubscriptionId) {
            newSub = Subscription.get(Long.parseLong(params.targetSubscriptionId))
        }
        boolean isTargetSubChanged = false
        if (params.subscription?.deleteDocIds && isBothSubscriptionsSet(baseSub, newSub)) {
            def toDeleteDocs = []
            params.list('subscription.deleteDocIds').each { doc -> toDeleteDocs << Long.valueOf(doc) }
            subscriptionService.deleteDocs(toDeleteDocs, newSub, flash)
            isTargetSubChanged = true
        }

        if (params.subscription?.takeDocIds && isBothSubscriptionsSet(baseSub, newSub)) {
            def toCopyDocs = []
            params.list('subscription.takeDocIds').each { doc -> toCopyDocs << Long.valueOf(doc) }
            subscriptionService.copyDocs(baseSub, toCopyDocs, newSub, flash)
            isTargetSubChanged = true
        }

        if (params.subscription?.deleteAnnouncementIds && isBothSubscriptionsSet(baseSub, newSub)) {
            def toDeleteAnnouncements = []
            params.list('subscription.deleteAnnouncementIds').each { announcement -> toDeleteAnnouncements << Long.valueOf(announcement) }
            subscriptionService.deleteAnnouncements(toDeleteAnnouncements, newSub, flash)
            isTargetSubChanged = true
        }

        if (params.subscription?.takeAnnouncementIds && isBothSubscriptionsSet(baseSub, newSub)) {
            def toCopyAnnouncements = []
            params.list('subscription.takeAnnouncementIds').each { announcement -> toCopyAnnouncements << Long.valueOf(announcement) }
            subscriptionService.copyAnnouncements(baseSub, toCopyAnnouncements, newSub, flash)
            isTargetSubChanged = true
        }

        if (params.subscription?.deleteTaskIds && isBothSubscriptionsSet(baseSub, newSub)) {
            def toDeleteTasks = []
            params.list('subscription.deleteTaskIds').each { tsk -> toDeleteTasks << Long.valueOf(tsk) }
            subscriptionService.deleteTasks(toDeleteTasks, newSub, flash)
            isTargetSubChanged = true
        }

        if (params.subscription?.takeTaskIds && isBothSubscriptionsSet(baseSub, newSub)) {
            def toCopyTasks = []
            params.list('subscription.takeTaskIds').each { tsk -> toCopyTasks << Long.valueOf(tsk) }
            subscriptionService.copyTasks(baseSub, toCopyTasks, newSub, flash)
            isTargetSubChanged = true
        }

        if (isTargetSubChanged) {
            newSub = newSub.refresh()
        }

        result.sourceSubscription = baseSub.refresh()
        result.targetSubscription = newSub
        result
    }

    private loadDataFor_DocsAnnouncementsTasks() {
        def result = setResultGenericsAndCheckAccessforSub(AccessService.CHECK_VIEW)
        Subscription baseSub = Subscription.get(params.sourceSubscriptionId ? Long.parseLong(params.sourceSubscriptionId) : Long.parseLong(params.id))
        Subscription newSub = null
        if (params.targetSubscriptionId) {
            newSub = Subscription.get(Long.parseLong(params.targetSubscriptionId))
        }

        result.sourceSubscription = baseSub
        result.targetSubscription = newSub
        result.sourceTasks = taskService.getTasksByResponsiblesAndObject(result.user, contextService.org, result.sourceSubscription)
        result.targetTasks = taskService.getTasksByResponsiblesAndObject(result.user, contextService.org, result.targetSubscription)
        result
    }

    private copySubElements_Subscriber() {
        def result = setResultGenericsAndCheckAccessforSub(AccessService.CHECK_VIEW)
        Subscription baseSub = Subscription.get(params.sourceSubscriptionId ? Long.parseLong(params.sourceSubscriptionId) : Long.parseLong(params.id))
        Subscription newSub = null
        if (params.targetSubscriptionId) {
            newSub = Subscription.get(Long.parseLong(params.targetSubscriptionId))
        }

        if (params.subscription?.copySubscriber && isBothSubscriptionsSet(baseSub, newSub)) {
            List<Subscription> toCopySubs = params.list('subscription.copySubscriber').collect {
                genericOIDService.resolveOID(it)
            }
            subscriptionService.copySubscriber(toCopySubs, newSub, flash)
        }

        result.sourceSubscription = baseSub
        result.targetSubscription = newSub
        result
    }

    private loadDataFor_Subscriber() {
        def result = setResultGenericsAndCheckAccessforSub(AccessService.CHECK_VIEW)
        result.sourceSubscription = Subscription.get(params.sourceSubscriptionId ? Long.parseLong(params.sourceSubscriptionId) : Long.parseLong(params.id))
        result.validSourceSubChilds = subscriptionService.getValidSubChilds(result.sourceSubscription)
        if (params.targetSubscriptionId) {
            result.targetSubscription = Subscription.get(Long.parseLong(params.targetSubscriptionId))
            result.validTargetSubChilds = subscriptionService.getValidSubChilds(result.targetSubscription)
        }
        result
    }

    @DebugAnnotation(test = 'hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser().hasAffiliation("INST_USER") })
    def exportSurCostItems() {
        def result = setResultGenericsAndCheckAccess()
        if (!accessService.checkPermAffiliationX('ORG_CONSORTIUM','INST_USER','ROLE_ADMIN')) {
            response.sendError(401); return
        }
        result.putAll(financeService.setEditVars(result.institution))

        /*   def surveyInfo = SurveyInfo.findByIdAndOwner(params.id, result.institution) ?: null

           def surveyConfig = SurveyConfig.findByIdAndSurveyInfo(params.surveyConfigID, surveyInfo)*/

        if (params.exportXLSX) {
            def sdf = DateUtil.getSDF_NoTimeNoPoint()
            String datetoday = sdf.format(new Date(System.currentTimeMillis()))
            String filename = "${datetoday}_" + g.message(code: "survey.label")
            //if(wb instanceof XSSFWorkbook) file += "x";
            response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
            response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            SXSSFWorkbook wb = (SXSSFWorkbook) exportSurveyCostItems(result.surveyConfig, "xls", result.institution)
            wb.write(response.outputStream)
            response.outputStream.flush()
            response.outputStream.close()
            wb.dispose()

            return
        } else {
            redirect(uri: request.getHeader('referer'))
        }

    }


    @DebugAnnotation(test = 'hasAffiliation("INST_USER")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser().hasAffiliation("INST_USER") })
    def copyEmailaddresses() {
        Map<String, Object> result = [:]
        result.modalID = params.targetId
        result.orgList = []

        if (params.get('orgListIDs')) {
            List idList = (params.get('orgListIDs').split(',').collect { Long.valueOf(it.trim()) }).toList()
            result.orgList = Org.findAllByIdInList(idList)
        }

        render(template: "/templates/copyEmailaddresses", model: result)
    }


    @DebugAnnotation(test = 'hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser().hasAffiliation("INST_EDITOR") })
    def newSurveyCostItem() {

        SimpleDateFormat dateFormat = DateUtil.getSDF_NoTime()

        Map<String, Object> result = [:]
        def newCostItem = null
        result.putAll(financeService.setEditVars(contextService.getOrg()))

        try {
            log.debug("SurveyController::newCostItem() ${params}");

            result.institution = contextService.getOrg()
            User user = User.get(springSecurityService.principal.id)
            result.error = [] as List

            if (!accessService.checkMinUserOrgRole(user, result.institution, "INST_EDITOR")) {
                result.error = message(code: 'financials.permission.unauthorised', args: [result.institution ? result.institution.name : 'N/A'])
                response.sendError(403)
            }


            Closure newDate = { param, format ->
                Date date
                try {
                    date = dateFormat.parse(param)
                } catch (Exception e) {
                    log.debug("Unable to parse date : ${param} in format ${format}")
                }
                date
            }

            def startDate = newDate(params.newStartDate, dateFormat.toPattern())
            def endDate = newDate(params.newEndDate, dateFormat.toPattern())
            def billing_currency = null
            if (params.long('newCostCurrency')) //GBP,etc
            {
                billing_currency = RefdataValue.get(params.newCostCurrency)
                if (!billing_currency)
                    billing_currency = defaultCurrency
            }

            //def tempCurrencyVal       = params.newCostCurrencyRate?      params.double('newCostCurrencyRate',1.00) : 1.00//def cost_local_currency   = params.newCostInLocalCurrency?   params.double('newCostInLocalCurrency', cost_billing_currency * tempCurrencyVal) : 0.00
            def cost_item_status = params.newCostItemStatus ? (RefdataValue.get(params.long('newCostItemStatus'))) : null;
            //estimate, commitment, etc
            def cost_item_element = params.newCostItemElement ? (RefdataValue.get(params.long('newCostItemElement'))) : null
            //admin fee, platform, etc
            //moved to TAX_TYPES
            //def cost_tax_type         = params.newCostTaxType ?          (RefdataValue.get(params.long('newCostTaxType'))) : null           //on invoice, self declared, etc

            def cost_item_category = params.newCostItemCategory ? (RefdataValue.get(params.long('newCostItemCategory'))) : null
            //price, bank charge, etc

            NumberFormat format = NumberFormat.getInstance(LocaleContextHolder.getLocale())
            def cost_billing_currency = params.newCostInBillingCurrency ? format.parse(params.newCostInBillingCurrency).doubleValue() : 0.00
            //def cost_currency_rate = params.newCostCurrencyRate ? params.double('newCostCurrencyRate', 1.00) : 1.00
            //def cost_local_currency = params.newCostInLocalCurrency ? format.parse(params.newCostInLocalCurrency).doubleValue() : 0.00

            def cost_billing_currency_after_tax = params.newCostInBillingCurrencyAfterTax ? format.parse(params.newCostInBillingCurrencyAfterTax).doubleValue() : cost_billing_currency
            //def cost_local_currency_after_tax = params.newCostInLocalCurrencyAfterTax ? format.parse(params.newCostInLocalCurrencyAfterTax).doubleValue() : cost_local_currency
            //moved to TAX_TYPES
            //def new_tax_rate                      = params.newTaxRate ? params.int( 'newTaxRate' ) : 0
            def tax_key = null
            if (!params.newTaxRate.contains("null")) {
                String[] newTaxRate = params.newTaxRate.split("§")
                RefdataValue taxType = genericOIDService.resolveOID(newTaxRate[0])
                int taxRate = Integer.parseInt(newTaxRate[1])
                switch (taxType.id) {
                    case RefdataValue.getByValueAndCategory("taxable", RDConstants.TAX_TYPE).id:
                        switch (taxRate) {
                            case 7: tax_key = CostItem.TAX_TYPES.TAXABLE_7
                                break
                            case 19: tax_key = CostItem.TAX_TYPES.TAXABLE_19
                                break
                        }
                        break
                    case RefdataValue.getByValueAndCategory("taxable tax-exempt", RDConstants.TAX_TYPE).id:
                        tax_key = CostItem.TAX_TYPES.TAX_EXEMPT
                        break
                    case RefdataValue.getByValueAndCategory("not taxable", RDConstants.TAX_TYPE).id:
                        tax_key = CostItem.TAX_TYPES.TAX_NOT_TAXABLE
                        break
                    case RefdataValue.getByValueAndCategory("not applicable", RDConstants.TAX_TYPE).id:
                        tax_key = CostItem.TAX_TYPES.TAX_NOT_APPLICABLE
                        break
                    case RefdataValue.getByValueAndCategory("reverse charge", RDConstants.TAX_TYPE).id:
                        tax_key = CostItem.TAX_TYPES.TAX_REVERSE_CHARGE
                        break
                }
            }
            def cost_item_element_configuration = params.ciec ? RefdataValue.get(Long.parseLong(params.ciec)) : null

            boolean cost_item_isVisibleForSubscriber = false
            // (params.newIsVisibleForSubscriber ? (RefdataValue.get(params.newIsVisibleForSubscriber).value == 'Yes') : false)

            def surveyOrgsDo = []

            if (params.surveyOrg) {
                try {
                    surveyOrgsDo << genericOIDService.resolveOID(params.surveyOrg)
                } catch (Exception e) {
                    log.error("Non-valid surveyOrg sent ${params.surveyOrg}", e)
                }
            }

            if (params.get('surveyOrgs')) {
                List surveyOrgs = (params.get('surveyOrgs').split(',').collect {
                    String.valueOf(it.replaceAll("\\s", ""))
                }).toList()
                surveyOrgs.each {
                    try {

                        def surveyOrg = genericOIDService.resolveOID(it)
                        if (!CostItem.findBySurveyOrgAndCostItemStatusNotEqual(surveyOrg,RDStore.COST_ITEM_DELETED)) {
                            surveyOrgsDo << surveyOrg
                        }
                    } catch (Exception e) {
                        log.error("Non-valid surveyOrg sent ${it}", e)
                    }
                }
            }

            /* if (params.surveyConfig) {
                 def surveyConfig = genericOIDService.resolveOID(params.surveyConfig)

                 surveyConfig.orgs.each {

                     if (!CostItem.findBySurveyOrg(it)) {
                         surveyOrgsDo << it
                     }
                 }
             }*/

            surveyOrgsDo.each { surveyOrg ->

                if (!surveyOrg.existsMultiYearTerm()) {

                    if (params.oldCostItem && genericOIDService.resolveOID(params.oldCostItem)) {
                        newCostItem = genericOIDService.resolveOID(params.oldCostItem)
                    } else {
                        newCostItem = new CostItem()
                    }

                    newCostItem.owner = result.institution
                    newCostItem.surveyOrg = newCostItem.surveyOrg ?: surveyOrg
                    newCostItem.isVisibleForSubscriber = cost_item_isVisibleForSubscriber
                    newCostItem.costItemCategory = cost_item_category
                    newCostItem.costItemElement = cost_item_element
                    newCostItem.costItemStatus = cost_item_status
                    newCostItem.billingCurrency = billing_currency //Not specified default to GDP
                    //newCostItem.taxCode = cost_tax_type -> to taxKey
                    newCostItem.costTitle = params.newCostTitle ?: null
                    newCostItem.costInBillingCurrency = cost_billing_currency as Double
                    //newCostItem.costInLocalCurrency = cost_local_currency as Double

                    newCostItem.finalCostRounding = params.newFinalCostRounding ? true : false
                    newCostItem.costInBillingCurrencyAfterTax = cost_billing_currency_after_tax as Double
                    //newCostItem.costInLocalCurrencyAfterTax = cost_local_currency_after_tax as Double
                    //newCostItem.currencyRate = cost_currency_rate as Double
                    //newCostItem.taxRate = new_tax_rate as Integer -> to taxKey
                    newCostItem.taxKey = tax_key
                    newCostItem.costItemElementConfiguration = cost_item_element_configuration

                    newCostItem.costDescription = params.newDescription ? params.newDescription.trim() : null

                    newCostItem.startDate = startDate ?: null
                    newCostItem.endDate = endDate ?: null

                    //newCostItem.includeInSubscription = null
                    //todo Discussion needed, nobody is quite sure of the functionality behind this...


                    if (!newCostItem.validate()) {
                        result.error = newCostItem.errors.allErrors.collect {
                            log.error("Field: ${it.properties.field}, user input: ${it.properties.rejectedValue}, Reason! ${it.properties.code}")
                            message(code: 'finance.addNew.error', args: [it.properties.field])
                        }
                    } else {
                        if (newCostItem.save(flush: true)) {
                            /* def newBcObjs = []

                         params.list('newBudgetCodes').each { newbc ->
                             def bc = genericOIDService.resolveOID(newbc)
                             if (bc) {
                                 newBcObjs << bc
                                 if (! CostItemGroup.findByCostItemAndBudgetCode( newCostItem, bc )) {
                                     new CostItemGroup(costItem: newCostItem, budgetCode: bc).save(flush: true)
                                 }
                             }
                         }

                         def toDelete = newCostItem.getBudgetcodes().minus(newBcObjs)
                         toDelete.each{ bc ->
                             def cig = CostItemGroup.findByCostItemAndBudgetCode( newCostItem, bc )
                             if (cig) {
                                 log.debug('deleting ' + cig)
                                 cig.delete()
                             }
                         }*/

                        } else {
                            result.error = "Unable to save!"
                        }
                    }
                }
            } // subsToDo.each

        }
        catch (Exception e) {
            log.error("Problem in add cost item", e);
        }


        redirect(uri: request.getHeader('referer'))
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def compareMembersOfTwoSubs() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.parentSubscription = result.surveyConfig.subscription
        result.parentSubChilds = subscriptionService.getValidSubChilds(result.parentSubscription)
        result.parentSuccessorSubscription = result.surveyConfig.subscription?.getCalculatedSuccessor()
        result.parentSuccessorSubChilds = result.parentSuccessorSubscription ? subscriptionService.getValidSubChilds(result.parentSuccessorSubscription) : null

        result.superOrgType = []
        if(accessService.checkPerm('ORG_CONSORTIUM')) {
            result.superOrgType << message(code:'consortium.superOrgType')
        }
        if(accessService.checkPerm('ORG_INST_COLLECTIVE')) {
            result.superOrgType << message(code:'collective.superOrgType')
        }

        result.participantsList = []

        result.parentParticipantsList = []
        result.parentSuccessortParticipantsList = []

        result.parentSubChilds.each { sub ->
            def org = sub.getSubscriber()
            result.participantsList << org
            result.parentParticipantsList << org

        }

        result.parentSuccessorSubChilds.each { sub ->
            def org = sub.getSubscriber()
            if(!(org in result.participantsList)) {
                result.participantsList << org
            }
            result.parentSuccessortParticipantsList << org

        }

        result.participantsList = result.participantsList.sort{it.sortname}


        result.participationProperty = RDStore.SURVEY_PROPERTY_PARTICIPATION

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def copySurveyCostItems() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.parentSubscription = result.surveyConfig.subscription
        result.parentSuccessorSubscription = result.surveyConfig.subscription?.getCalculatedSuccessor()
        result.parentSuccessorSubChilds = result.parentSuccessorSubscription ? subscriptionService.getValidSubChilds(result.parentSuccessorSubscription) : null

        result.participantsList = []

        result.parentSuccessortParticipantsList = []

        result.parentSuccessorSubChilds.each { sub ->
            def newMap = [:]
            def org = sub.getSubscriber()
            newMap.id = org.id
            newMap.sortname = org.sortname
            newMap.name = org.name
            newMap.newSub = sub
            newMap.oldSub = sub.getCalculatedPrevious()

            newMap.surveyOrg = SurveyOrg.findBySurveyConfigAndOrg(result.surveyConfig, org)
            newMap.surveyCostItem =newMap.surveyOrg ? com.k_int.kbplus.CostItem.findBySurveyOrgAndCostItemStatusNotEqual(newMap.surveyOrg,RDStore.COST_ITEM_DELETED) : null

            result.participantsList << newMap

        }

        result.participantsList = result.participantsList.sort{it.sortname}

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def proccessCopySurveyCostItems() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.parentSubscription = result.surveyConfig.subscription

        result.parentSuccessorSubscription = result.surveyConfig.subscription?.getCalculatedSuccessor()

        def countNewCostItems = 0
        def costElement = RefdataValue.getByValueAndCategory('price: consortial price', RDConstants.COST_ITEM_ELEMENT)
        params.list('selectedSurveyCostItem').each { costItemId ->

            def costItem = CostItem.get(costItemId)
            def participantSub = result.parentSuccessorSubscription?.getDerivedSubscriptionBySubscribers(costItem.surveyOrg.org)
            def participantSubCostItem = CostItem.findAllBySubAndOwnerAndCostItemElementAndCostItemStatusNotEqual(participantSub, result.institution, costElement, RDStore.COST_ITEM_DELETED)
            if(costItem && participantSub && !participantSubCostItem){

                def properties = costItem.properties
                CostItem copyCostItem = new CostItem()
                InvokerHelper.setProperties(copyCostItem, properties)
                copyCostItem.globalUID = null
                copyCostItem.surveyOrg = null
                copyCostItem.isVisibleForSubscriber = params.isVisibleForSubscriber ? true : null
                copyCostItem.sub = participantSub
                if(copyCostItem.save(flush:true)) {
                    countNewCostItems++
                }

            }

        }

        flash.message = message(code: 'copySurveyCostItems.copy.success', args: [countNewCostItems])
        redirect(action: 'copySurveyCostItems', id: params.id, params: [surveyConfigID: result.surveyConfig.id])

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def copySurveyCostItemsToSub() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.parentSubscription = result.surveyConfig.subscription
        result.parentSubChilds = result.parentSubscription ? subscriptionService.getValidSubChilds(result.parentSubscription) : null

        result.participantsList = []

        result.parentSubChilds.each { sub ->
            def newMap = [:]
            def org = sub.getSubscriber()
            newMap.id = org.id
            newMap.sortname = org.sortname
            newMap.name = org.name
            newMap.newSub = sub

            newMap.surveyOrg = SurveyOrg.findBySurveyConfigAndOrg(result.surveyConfig, org)
            newMap.surveyCostItem =newMap.surveyOrg ? com.k_int.kbplus.CostItem.findBySurveyOrgAndCostItemStatusNotEqual(newMap.surveyOrg,RDStore.COST_ITEM_DELETED) : null

            result.participantsList << newMap

        }

        result.participantsList = result.participantsList.sort{it.sortname}

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def proccessCopySurveyCostItemsToSub() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.parentSubscription = result.surveyConfig.subscription


        def countNewCostItems = 0
        def costElement = RefdataValue.getByValueAndCategory('price: consortial price', RDConstants.COST_ITEM_ELEMENT)
        params.list('selectedSurveyCostItem').each { costItemId ->

            def costItem = CostItem.get(costItemId)
            def participantSub = result.parentSubscription.getDerivedSubscriptionBySubscribers(costItem.surveyOrg.org)
            def participantSubCostItem = CostItem.findAllBySubAndOwnerAndCostItemElementAndCostItemStatusNotEqual(participantSub, result.institution, costElement, RDStore.COST_ITEM_DELETED)
            if(costItem && participantSub && !participantSubCostItem){

                def properties = costItem.properties
                CostItem copyCostItem = new CostItem()
                InvokerHelper.setProperties(copyCostItem, properties)
                copyCostItem.globalUID = null
                copyCostItem.surveyOrg = null
                copyCostItem.isVisibleForSubscriber = params.isVisibleForSubscriber ? true : null
                copyCostItem.sub = participantSub
                if(copyCostItem.save(flush:true)) {
                    countNewCostItems++
                }

            }

        }

        flash.message = message(code: 'copySurveyCostItems.copy.success', args: [countNewCostItems])
        redirect(action: 'copySurveyCostItemsToSub', id: params.id, params: [surveyConfigID: result.surveyConfig.id])

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def copyProperties() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        params.tab = params.tab ?: 'surveyProperties'

        result.parentSubscription = result.surveyConfig.subscription
        result.parentSuccessorSubscription = result.surveyConfig.subscription?.getCalculatedSuccessor()
        result.parentSuccessorSubChilds = result.parentSuccessorSubscription ? subscriptionService.getValidSubChilds(result.parentSuccessorSubscription) : null

        result.selectedProperty
        result.properties
        if(params.tab == 'surveyProperties') {
            result.properties = SurveyConfigProperties.findAllBySurveyConfig(result.surveyConfig).surveyProperty.findAll{it.tenant == null}
            result.properties -= RDStore.SURVEY_PROPERTY_PARTICIPATION
            result.properties -= RDStore.SURVEY_PROPERTY_MULTI_YEAR_2
            result.properties -= RDStore.SURVEY_PROPERTY_MULTI_YEAR_3
        }

        if(params.tab == 'customProperties') {
            result.properties = result.parentSubscription.customProperties.type
        }

        if(params.tab == 'privateProperties') {
            result.properties = result.parentSubscription.privateProperties.type
        }

        if(result.properties) {
            result.selectedProperty = params.selectedProperty ?: result.properties[0].id

            result.participantsList = []
            result.parentSuccessorSubChilds.each { sub ->

                def newMap = [:]
                def org = sub.getSubscriber()
                newMap.id = org.id
                newMap.sortname = org.sortname
                newMap.name = org.name
                newMap.newSub = sub
                newMap.oldSub = sub.getCalculatedPrevious()


                if (params.tab == 'surveyProperties') {
                    def surProp = PropertyDefinition.get(result.selectedProperty)
                    newMap.surveyProperty = SurveyResult.findBySurveyConfigAndTypeAndParticipant(result.surveyConfig, surProp, org)
                    def propDef = surProp ? PropertyDefinition.getByNameAndDescr(surProp.name, PropertyDefinition.SUB_PROP) : null

                    newMap.newCustomProperty = (sub && propDef) ? sub.customProperties.find {
                        it.type.id == propDef.id
                    } : null
                    newMap.oldCustomProperty = (newMap.oldSub && propDef) ? newMap.oldSub.customProperties.find {
                        it.type.id == propDef.id
                    } : null
                }
                if(params.tab == 'customProperties') {
                    newMap.newCustomProperty = (sub) ? sub.customProperties.find {
                        it.type.id == (result.selectedProperty instanceof Long ?: Long.parseLong(result.selectedProperty))
                    } : null
                    newMap.oldCustomProperty = (newMap.oldSub) ? newMap.oldSub.customProperties.find {
                        it.type.id == (result.selectedProperty instanceof Long ?: Long.parseLong(result.selectedProperty))
                    } : null
                }

                if(params.tab == 'privateProperties') {
                    newMap.newPrivateProperty = (sub) ? sub.privateProperties.find {
                        it.type.id == (result.selectedProperty instanceof Long ?: Long.parseLong(result.selectedProperty))
                    } : null
                    newMap.oldPrivateProperty = (newMap.oldSub) ? newMap.oldSub.privateProperties.find {
                        it.type.id == (result.selectedProperty instanceof Long ?: Long.parseLong(result.selectedProperty))
                    } : null
                }


                result.participantsList << newMap
            }

            result.participantsList = result.participantsList.sort { it.sortname }
        }

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def proccessCopyProperties() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        if(params.list('selectedSub')) {
            result.selectedProperty
            def propDef
            def surveyProperty
            if (params.tab == 'surveyProperties') {
                result.selectedProperty = params.selectedProperty ?: null

                surveyProperty = params.copyProperty ? PropertyDefinition.get(Long.parseLong(params.copyProperty)) : null

                propDef = surveyProperty ? PropertyDefinition.getByNameAndDescr(surveyProperty.name, PropertyDefinition.SUB_PROP) : null
                if (!propDef && surveyProperty) {

                    Map<String, Object> map = [
                            token       : surveyProperty.name,
                            category    : 'Subscription Property',
                            type        : surveyProperty.type,
                            rdc         : (surveyProperty.type == RefdataValue.toString()) ? surveyProperty.refdataCategory : null,
                            i10n        : [
                                    name_de: surveyProperty.getI10n('name', 'de'),
                                    name_en: surveyProperty.getI10n('name', 'en'),
                                    expl_de: surveyProperty.getI10n('expl', 'de'),
                                    expl_en: surveyProperty.getI10n('expl', 'en')
                            ]
                    ]
                    propDef = PropertyDefinition.construct(map)
                }

            } else {
                result.selectedProperty = params.selectedProperty ?: null
                propDef = params.selectedProperty ? PropertyDefinition.get(Long.parseLong(params.selectedProperty)) : null
            }

            result.parentSuccessorSubscription = result.surveyConfig.subscription?.getCalculatedSuccessor()
            result.parentSuccessorSubChilds = result.parentSuccessorSubscription ? subscriptionService.getValidSubChilds(result.parentSuccessorSubscription) : null

            def countSuccessfulCopy = 0

            if (propDef && params.list('selectedSub')) {
                params.list('selectedSub').each { subID ->
                    if (Long.parseLong(subID) in result.parentSuccessorSubChilds.id) {
                        def sub = Subscription.get(Long.parseLong(subID))
                        def org = sub.getSubscriber()
                        def oldSub = sub.getCalculatedPrevious()

                        def copyProperty
                        if (params.tab == 'surveyProperties') {
                            copyProperty = SurveyResult.findBySurveyConfigAndTypeAndParticipant(result.surveyConfig, surveyProperty, org)
                        } else {
                            if (params.tab == 'privateProperties') {
                                copyProperty = oldSub ? oldSub.privateProperties.find {
                                    it.type.id == propDef.id
                                } : []
                            } else {
                                copyProperty = oldSub ? oldSub.customProperties.find {
                                    it.type.id == propDef.id
                                } : []
                            }
                        }

                        if (copyProperty) {
                            if (propDef.tenant != null) {
                                //private Property
                                def existingProps = sub.privateProperties.findAll {
                                    it.owner.id == sub.id && it.type.id == propDef.id
                                }
                                existingProps.removeAll { it.type.name != propDef.name } // dubious fix

                                if (existingProps.size() == 0 || propDef.multipleOccurrence) {
                                    def newProp = PropertyDefinition.createGenericProperty(PropertyDefinition.PRIVATE_PROPERTY, sub, propDef)
                                    if (newProp.hasErrors()) {
                                        log.error(newProp.errors)
                                    } else {
                                        log.debug("New private property created: " + newProp.type.name)
                                        def newValue = copyProperty.getValue()
                                        if (copyProperty.type.type == RefdataValue.toString()) {
                                            newValue = copyProperty.refValue ? copyProperty.refValue : null
                                        }
                                        def prop = setNewProperty(newProp, newValue)
                                        countSuccessfulCopy++
                                    }
                                }
                            } else {
                                //custom Property
                                def existingProp = sub.customProperties.find {
                                    it.type.id == propDef.id && it.owner.id == sub.id
                                }

                                if (existingProp == null || propDef.multipleOccurrence) {
                                    def newProp = PropertyDefinition.createGenericProperty(PropertyDefinition.CUSTOM_PROPERTY, sub, propDef)
                                    if (newProp.hasErrors()) {
                                        log.error(newProp.errors)
                                    } else {
                                        log.debug("New custom property created: " + newProp.type.name)
                                        def newValue = copyProperty.getValue()
                                        if (copyProperty.type.type == RefdataValue.toString()) {
                                            newValue = copyProperty.refValue ? copyProperty.refValue : null
                                        }
                                        def prop = setNewProperty(newProp, newValue)
                                        countSuccessfulCopy++
                                    }
                                }

                                /*if (existingProp) {
                            def customProp = SubscriptionCustomProperty.get(existingProp.id)
                            def prop = setNewProperty(customProp, copyProperty)
                        }*/
                            }
                        }
                    }
                }
            }
            flash.message = message(code: 'copyProperties.successful', args: [countSuccessfulCopy, message(code: 'copyProperties.' + params.tab) ,params.list('selectedSub').size()])
        }

        redirect(action: 'copyProperties', id: params.id, params: [surveyConfigID: result.surveyConfig.id, tab: params.tab, selectedProperty: params.selectedProperty])

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processTransferParticipants() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.parentSubscription = result.surveyConfig.subscription
        result.parentSubChilds = subscriptionService.getValidSubChilds(result.parentSubscription)
        result.parentSuccessorSubscription = result.surveyConfig.subscription?.getCalculatedSuccessor()
        result.parentSuccessorSubChilds = result.parentSuccessorSubscription ? subscriptionService.getValidSubChilds(result.parentSuccessorSubscription) : null

        result.participationProperty = RDStore.SURVEY_PROPERTY_PARTICIPATION

        result.properties = []
        result.properties.addAll(SurveyConfigProperties.findAllBySurveyPropertyNotEqualAndSurveyConfig(result.participationProperty, result.surveyConfig)?.surveyProperty)

        result.multiYearTermThreeSurvey = null
        result.multiYearTermTwoSurvey = null

        if (RDStore.SURVEY_PROPERTY_MULTI_YEAR_3.id in result.properties.id) {
            result.multiYearTermThreeSurvey = RDStore.SURVEY_PROPERTY_MULTI_YEAR_3
            result.properties.remove(result.multiYearTermThreeSurvey)
        }
        if (RDStore.SURVEY_PROPERTY_MULTI_YEAR_2.id in result.properties.id) {
            result.multiYearTermTwoSurvey = RDStore.SURVEY_PROPERTY_MULTI_YEAR_2
            result.properties.remove(result.multiYearTermTwoSurvey)

        }

        result.parentSuccessortParticipantsList = []

        result.parentSuccessorSubChilds.each { sub ->
            def org = sub.getSubscriber()
            result.parentSuccessortParticipantsList << org

        }

        // Orgs that renew or new to Sub
        result.orgsContinuetoSubscription = []
        result.newOrgsContinuetoSubscription = []

        result.newSubs = []

        def countNewSubs = 0

        SurveyResult.executeQuery("from SurveyResult where owner.id = :owner and surveyConfig.id = :surConfig and type.id = :surProperty and refValue = :refValue order by participant.sortname",
                [
                        owner      : result.institution.id,
                        surProperty: result.participationProperty.id,
                        surConfig  : result.surveyConfig.id,
                        refValue   : RDStore.YN_YES]).each {

            // Keine Kindlizenz in der Nachfolgerlizenz vorhanden
            if(!(it.participant in result.parentSuccessortParticipantsList)){

                def oldSubofParticipant = Subscription.executeQuery("Select s from Subscription s left join s.orgRelations orgR where s.instanceOf = :parentSub and orgR.org = :participant",
                        [parentSub  : result.parentSubscription,
                         participant: it.participant
                        ])[0]


                if(!oldSubofParticipant)
                {
                    oldSubofParticipant = result.parentSubscription
                }

                def newStartDate = null
                def newEndDate = null

                //Umfrage-Merkmal MJL2
                if (result.multiYearTermTwoSurvey) {

                    def participantPropertyTwo = SurveyResult.findByParticipantAndOwnerAndSurveyConfigAndType(it.participant, result.institution, result.surveyConfig, result.multiYearTermTwoSurvey)

                    if (participantPropertyTwo.refValue?.id == RDStore.YN_YES.id) {
                        use(TimeCategory) {
                            newStartDate = oldSubofParticipant.startDate ? (oldSubofParticipant.endDate + 1.day) : null
                            newEndDate = oldSubofParticipant.endDate ? (oldSubofParticipant.endDate + 2.year) : null
                        }
                            countNewSubs++
                            result.newSubs << processAddMember(((oldSubofParticipant != result.parentSubscription) ? oldSubofParticipant: null), result.parentSuccessorSubscription, it.participant, newStartDate, newEndDate, true, params)
                    } else {
                        use(TimeCategory) {
                            newStartDate = oldSubofParticipant.startDate ? (oldSubofParticipant.endDate + 1.day) : null
                            newEndDate = oldSubofParticipant.endDate ? (oldSubofParticipant.endDate + 1.year) : null
                        }
                        countNewSubs++
                        result.newSubs << processAddMember(((oldSubofParticipant != result.parentSubscription) ? oldSubofParticipant: null), result.parentSuccessorSubscription, it.participant, newStartDate, newEndDate, false, params)
                    }

                }
                //Umfrage-Merkmal MJL3
                else if (result.multiYearTermThreeSurvey) {

                    def participantPropertyThree = SurveyResult.findByParticipantAndOwnerAndSurveyConfigAndType(it.participant, result.institution, result.surveyConfig, result.multiYearTermThreeSurvey)
                    if (participantPropertyThree.refValue?.id == RDStore.YN_YES.id) {
                        use(TimeCategory) {
                            newStartDate = oldSubofParticipant.startDate ? (oldSubofParticipant.endDate + 1.day) : null
                            newEndDate = oldSubofParticipant.endDate ? (oldSubofParticipant.endDate + 3.year) : null
                        }
                        countNewSubs++
                        result.newSubs << processAddMember(((oldSubofParticipant != result.parentSubscription) ? oldSubofParticipant: null), result.parentSuccessorSubscription, it.participant, newStartDate, newEndDate, true, params)
                    }
                    else {
                        use(TimeCategory) {
                            newStartDate = oldSubofParticipant.startDate ? (oldSubofParticipant.endDate + 1.day) : null
                            newEndDate = oldSubofParticipant.endDate ? (oldSubofParticipant.endDate + 1.year) : null
                        }
                        countNewSubs++
                        result.newSubs << processAddMember(((oldSubofParticipant != result.parentSubscription) ? oldSubofParticipant: null), result.parentSuccessorSubscription, it.participant, newStartDate, newEndDate, false, params)
                    }
                }else {
                    use(TimeCategory) {
                        newStartDate = oldSubofParticipant.startDate ? (oldSubofParticipant.endDate + 1.day) : null
                        newEndDate = oldSubofParticipant.endDate ? (oldSubofParticipant.endDate + 1.year) : null
                    }
                    countNewSubs++
                    result.newSubs << processAddMember(((oldSubofParticipant != result.parentSubscription) ? oldSubofParticipant: null), result.parentSuccessorSubscription, it.participant, newStartDate, newEndDate, false, params)
                }
            }
        }



        //MultiYearTerm Subs  //Späteinsteiger Unwichtig
        result.parentSubChilds.each { sub ->
            if (sub.isCurrentMultiYearSubscriptionNew()){
                sub.getAllSubscribers().each { org ->
                    if (!(org in result.parentSuccessortParticipantsList)) {

                        countNewSubs++
                        result.newSubs << processAddMember(sub, result.parentSuccessorSubscription, org, sub.startDate, sub.endDate, true, params)
                    }
                }
            }
            /*else if(sub.islateCommer()){
                sub.getAllSubscribers().each { org ->
                    if (!(org in result.parentSuccessortParticipantsList)) {

                        countNewSubs++
                        result.newSubs << processAddMember(sub, result.parentSuccessorSubscription, org, sub.startDate, sub.endDate, false, params)
                    }
                }
            }*/

        }

        result.countNewSubs = countNewSubs
        if(result.newSubs) {
            result.parentSuccessorSubscription.syncAllShares(result.newSubs)
        }
        flash.message = message(code: 'surveyInfo.transfer.info', args: [countNewSubs, result.newSubs.size() ?: 0])


        redirect(action: 'compareMembersOfTwoSubs', id: params.id, params: [surveyConfigID: result.surveyConfig.id])


    }

    private def processAddMember(def oldSub, Subscription newParentSub, Org org, Date newStartDate, Date newEndDate, boolean multiYear, params) {

        List orgType = [RDStore.OT_INSTITUTION.id.toString()]
        if (accessService.checkPerm("ORG_CONSORTIUM")) {
            orgType = [RDStore.OT_CONSORTIUM.id.toString()]
        }

        Org institution = contextService.getOrg()

        RefdataValue subStatus = RDStore.SUBSCRIPTION_INTENDED

        RefdataValue role_sub       = RDStore.OR_SUBSCRIBER_CONS
        RefdataValue role_sub_cons  = RDStore.OR_SUBSCRIPTION_CONSORTIA
        RefdataValue role_coll      = RDStore.OR_SUBSCRIBER_COLLECTIVE
        RefdataValue role_sub_coll  = RDStore.OR_SUBSCRIPTION_COLLECTIVE
        RefdataValue role_sub_hidden = RDStore.OR_SUBSCRIBER_CONS_HIDDEN
        RefdataValue role_lic       = RDStore.OR_LICENSEE_CONS

        if(accessService.checkPerm("ORG_INST_COLLECTIVE")) {
            role_lic = RDStore.OR_LICENSEE_COLL
        }
        RefdataValue role_lic_cons  = RDStore.OR_LICENSING_CONSORTIUM

        RefdataValue role_provider  = RDStore.OR_PROVIDER
        RefdataValue role_agency    = RDStore.OR_AGENCY

        if (accessService.checkPerm("ORG_INST_COLLECTIVE,ORG_CONSORTIUM")) {

                License licenseCopy

                def subLicense = newParentSub.owner

                Set<Package> packagesToProcess = []

                //copy package data
                if(params.linkAllPackages) {
                    newParentSub.packages.each { sp ->
                        packagesToProcess << sp.pkg
                    }
                }else if(params.packageSelection) {
                    List packageIds = params.list("packageSelection")
                    packageIds.each { spId ->
                        packagesToProcess << SubscriptionPackage.get(spId).pkg
                    }
                }

                    if(accessService.checkPerm("ORG_INST_COLLECTIVE,ORG_CONSORTIUM")) {
                       // def postfix = (members.size() > 1) ? 'Teilnehmervertrag' : (cm.shortname ?: cm.name)
                        def postfix = 'Teilnehmervertrag'

                        if (subLicense) {
                            def subLicenseParams = [
                                    lic_name     : "${subLicense.reference} (${postfix})",
                                    isSlaved     : params.isSlaved,
                                    asOrgType: orgType,
                                    copyStartEnd : true
                            ]

                            if (params.generateSlavedLics == 'explicit') {
                                licenseCopy = institutionsService.copyLicense(
                                        subLicense, subLicenseParams, InstitutionsService.CUSTOM_PROPERTIES_ONLY_INHERITED)
                            }
                            else if (params.generateSlavedLics == 'shared' && !licenseCopy) {
                                licenseCopy = institutionsService.copyLicense(
                                        subLicense, subLicenseParams, InstitutionsService.CUSTOM_PROPERTIES_ONLY_INHERITED)
                            }
                            else if ((params.generateSlavedLics == 'reference' || params.attachToParticipationLic == "true") && !licenseCopy) {
                                licenseCopy = genericOIDService.resolveOID(params.generateSlavedLicsReference)
                            }

                            if (licenseCopy) {
                                new OrgRole(org: org, lic: licenseCopy, roleType: role_lic).save()
                            }
                        }
                    }

                    log.debug("Generating seperate slaved instances for members")

                    SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
                    Date startDate = newStartDate ?: null
                    Date endDate = newEndDate ?: null

                    Subscription memberSub = new Subscription(
                            type: newParentSub.type ?: null,
                            kind: newParentSub.kind ?: null,
                            status: subStatus,
                            name: newParentSub.name,
                            startDate: startDate,
                            endDate: endDate,
                            administrative: newParentSub.getCalculatedType() == CalculatedType.TYPE_ADMINISTRATIVE,
                            manualRenewalDate: newParentSub.manualRenewalDate,
                            identifier: UUID.randomUUID().toString(),
                            instanceOf: newParentSub,
                            isSlaved: true,
                            owner: licenseCopy,
                            resource: newParentSub.resource ?: null,
                            form: newParentSub.form ?: null,
                            isMultiYear: multiYear ?: false
                    )

                    if (!memberSub.save()) {
                        memberSub.errors.each { e ->
                            log.debug("Problem creating new sub: ${e}")
                        }
                    }

                    if (memberSub) {
                        if(accessService.checkPerm("ORG_CONSORTIUM")) {

                            new OrgRole(org: org, sub: memberSub, roleType: role_sub).save()
                            new OrgRole(org: institution, sub: memberSub, roleType: role_sub_cons).save()

                            if(params.transferProviderAgency) {
                                newParentSub.getProviders().each { provider ->
                                    new OrgRole(org: provider, sub: memberSub, roleType: role_provider).save()
                                }
                                newParentSub.getAgencies().each { provider ->
                                    new OrgRole(org: provider, sub: memberSub, roleType: role_agency).save()
                                }
                            }else if(params.providersSelection) {
                                List orgIds = params.list("providersSelection")
                                orgIds.each { orgID ->
                                    new OrgRole(org: Org.get(orgID), sub: memberSub, roleType: role_provider).save()
                                }
                            }else if(params.agenciesSelection) {
                                List orgIds = params.list("agenciesSelection")
                                orgIds.each { orgID ->
                                    new OrgRole(org: Org.get(orgID), sub: memberSub, roleType: role_agency).save()
                                }
                            }

                        }

                        SubscriptionCustomProperty.findAllByOwner(newParentSub).each { scp ->
                            AuditConfig ac = AuditConfig.getConfig(scp)

                            if (ac) {
                                // multi occurrence props; add one additional with backref
                                if (scp.type.multipleOccurrence) {
                                    def additionalProp = PropertyDefinition.createGenericProperty(PropertyDefinition.CUSTOM_PROPERTY, memberSub, scp.type)
                                    additionalProp = scp.copyInto(additionalProp)
                                    additionalProp.instanceOf = scp
                                    additionalProp.save(flush: true)
                                }
                                else {
                                    // no match found, creating new prop with backref
                                    def newProp = PropertyDefinition.createGenericProperty(PropertyDefinition.CUSTOM_PROPERTY, memberSub, scp.type)
                                    newProp = scp.copyInto(newProp)
                                    newProp.instanceOf = scp
                                    newProp.save(flush: true)
                                }
                            }
                        }

                        packagesToProcess.each { pkg ->
                            if(params.linkWithEntitlements)
                                pkg.addToSubscriptionCurrentStock(memberSub, newParentSub)
                            else
                                pkg.addToSubscription(memberSub, false)
                        }

                        if(oldSub){
                            new Links(linkType: RDStore.LINKTYPE_FOLLOWS, source: memberSub.id, destination: oldSub.id, owner: contextService.getOrg(), objectType:Subscription.class.name).save(flush: true)
                        }

                        if(Org.get(orgID).getCustomerType() == 'ORG_INST') {
                            PendingChange.construct([target: memberSub, oid: "${memberSub.getClass().getName()}:${memberSub.id}", msgToken: "pendingChange.message_SU_NEW_01", status: RDStore.PENDING_CHANGE_PENDING, owner: org])
                        }

                        return memberSub
                    }
            }
    }

    private getSurveyProperties(Org contextOrg) {
        def props = []

        //private Property
        PropertyDefinition.getAllByDescrAndTenant(PropertyDefinition.SUR_PROP, contextOrg).each { it ->
            props << it

        }

        //global Property
        PropertyDefinition.getAllByDescr(PropertyDefinition.SUR_PROP).each { it ->
            props << it

        }

        props.sort { a, b -> a.getI10n('name').compareToIgnoreCase b.getI10n('name') }

        return props
    }

    private def addSubMembers(SurveyConfig surveyConfig) {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = (accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR') && surveyConfig.surveyInfo.owner.id == contextService.getOrg().id)

        if (!result.editable) {
            return
        }

        def orgs = []
        def currentMembersSubs = subscriptionService.getValidSurveySubChilds(surveyConfig.subscription)

        currentMembersSubs.each{ sub ->
            orgs.addAll(sub.getAllSubscribers())
        }

        if (orgs) {

            orgs.each { org ->

                if (!(SurveyOrg.findAllBySurveyConfigAndOrg(surveyConfig, org))) {

                    boolean existsMultiYearTerm = false
                    Subscription sub = surveyConfig.subscription
                    if (sub && !surveyConfig.pickAndChoose && surveyConfig.subSurveyUseForTransfer) {
                        Subscription subChild = sub.getDerivedSubscriptionBySubscribers(org)

                        if (subChild.isCurrentMultiYearSubscriptionNew()) {
                            existsMultiYearTerm = true
                        }

                    }
                    if (!existsMultiYearTerm) {
                        SurveyOrg surveyOrg = new SurveyOrg(
                                surveyConfig: surveyConfig,
                                org: org
                        )

                        if (!surveyOrg.save(flush: true)) {
                            log.debug("Error by add Org to SurveyOrg ${surveyOrg.errors}");
                        }else{
                            if(surveyConfig.surveyInfo.status in [RDStore.SURVEY_READY, RDStore.SURVEY_SURVEY_STARTED]) {
                                surveyConfig.surveyProperties.each { property ->

                                    SurveyResult surveyResult = new SurveyResult(
                                            owner: result.institution,
                                            participant: org ?: null,
                                            startDate: surveyConfig.surveyInfo.startDate,
                                            endDate: surveyConfig.surveyInfo.endDate ?: null,
                                            type: property.surveyProperty,
                                            surveyConfig: surveyConfig
                                    )

                                    if (surveyResult.save(flush: true)) {
                                        log.debug(surveyResult)
                                    } else {
                                        log.error("Not create surveyResult: " + surveyResult)
                                    }
                                }

                                if (surveyConfig.surveyInfo.status == RDStore.SURVEY_SURVEY_STARTED) {
                                    surveyUpdateService.emailsToSurveyUsersOfOrg(surveyConfig.surveyInfo, org)
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private static def getfilteredSurveyOrgs(List orgIDs, String query, queryParams, params) {

        if (!(orgIDs.size() > 0)) {
            return []
        }
        def tmpQuery = query
        tmpQuery = tmpQuery.replace("order by", "and o.id in (:orgIDs) order by")

        def tmpQueryParams = queryParams
        tmpQueryParams.put("orgIDs", orgIDs)
        //println(tmpQueryParams)
        //println(tmpQuery)

        return Org.executeQuery(tmpQuery, tmpQueryParams, params)
    }

    private def exportRenewalResult(Map renewalResult) {
        SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
        List titles = [g.message(code: 'org.sortname.label'),
                       g.message(code: 'default.name.label'),

                       renewalResult.participationProperty?.getI10n('name'),
                       g.message(code: 'surveyResult.participantComment') + " " + renewalResult.participationProperty?.getI10n('name')
        ]


        titles << g.message(code: 'renewalWithSurvey.period')

        if (renewalResult.multiYearTermTwoSurvey || renewalResult.multiYearTermThreeSurvey)
        {
            titles << g.message(code: 'renewalWithSurvey.periodComment')
        }

        renewalResult.properties.each { surveyProperty ->
            titles << surveyProperty?.getI10n('name')
            titles << g.message(code: 'surveyResult.participantComment') + " " + g.message(code: 'renewalWithSurvey.exportRenewal.to') +" " + surveyProperty?.getI10n('name')
        }
        titles << g.message(code: 'renewalWithSurvey.costBeforeTax')
        titles << g.message(code: 'renewalWithSurvey.costAfterTax')
        titles << g.message(code: 'renewalWithSurvey.costTax')
        titles << g.message(code: 'renewalWithSurvey.currency')

        List renewalData = []

        renewalData.add([[field: g.message(code: 'renewalWithSurvey.continuetoSubscription.label')+ " (${renewalResult.orgsContinuetoSubscription.size() ?: 0})", style: 'positive']])

        renewalResult.orgsContinuetoSubscription.each { participantResult ->
            List row = []

            row.add([field: participantResult.participant.sortname ?: '', style: null])
            row.add([field: participantResult.participant.name ?: '', style: null])
            row.add([field: participantResult.resultOfParticipation.getResult() ?: '', style: null])

            row.add([field: participantResult.resultOfParticipation.comment ?: '', style: null])


            def period = ""
            if (renewalResult.multiYearTermTwoSurvey) {
                period = participantResult.newSubPeriodTwoStartDate ? sdf.format(participantResult.newSubPeriodTwoStartDate) : ""
                period = participantResult.newSubPeriodTwoEndDate ? period + " - " +sdf.format(participantResult.newSubPeriodTwoEndDate) : ""
            }

            if (renewalResult.multiYearTermThreeSurvey) {
                period = participantResult.newSubPeriodThreeStartDate ? sdf.format(participantResult.newSubPeriodThreeStartDate) : ""
                period = participantResult.newSubPeriodThreeEndDate ? period + " - " +sdf.format(participantResult.newSubPeriodThreeEndDate) : ""
            }

            row.add([field: period ?: '', style: null])

            if (renewalResult.multiYearTermTwoSurvey) {
                row.add([field: participantResult.participantPropertyTwoComment ?: '', style: null])
            }

            if (renewalResult.multiYearTermThreeSurvey) {
                row.add([field: participantResult.participantPropertyThreeComment ?: '', style: null])
            }

            participantResult.properties.sort { it.type.name }.each { participantResultProperty ->
                row.add([field: participantResult.getResult() ?: "", style: null])

                row.add([field: participantResult.comment ?: "", style: null])

            }

            def costItem = CostItem.findBySurveyOrgAndCostItemStatusNotEqual(SurveyOrg.findBySurveyConfigAndOrg(participantResult.resultOfParticipation.surveyConfig, participantResult.participant),RDStore.COST_ITEM_DELETED)

            row.add([field: costItem?.costInBillingCurrency ? costItem.costInBillingCurrency : "", style: null])
            row.add([field: costItem?.costInBillingCurrencyAfterTax ? costItem.costInBillingCurrencyAfterTax : "", style: null])
            row.add([field: costItem?.taxKey ? costItem.taxKey.taxRate+'%' : "", style: null])
            row.add([field: costItem?.billingCurrency ? costItem.billingCurrency.getI10n('value').split('-').first() : "", style: null])


            renewalData.add(row)
        }

        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: g.message(code: 'renewalWithSurvey.withMultiYearTermSub.label')+ " (${renewalResult.orgsWithMultiYearTermSub.size() ?: 0})", style: 'positive']])


        renewalResult.orgsWithMultiYearTermSub.each { sub ->
            List row = []

            sub.getAllSubscribers().each{ subscriberOrg ->

                row.add([field: subscriberOrg.sortname ?: '', style: null])
                row.add([field: subscriberOrg.name ?: '', style: null])

                row.add([field: '', style: null])

                row.add([field: '', style: null])

                def period = ""

                period = sub.startDate ? sdf.format(sub.startDate) : ""
                period = sub.endDate ? period + " - " +sdf.format(sub.endDate) : ""

                row.add([field: period?: '', style: null])

                if (renewalResult.multiYearTermTwoSurvey || renewalResult.multiYearTermThreeSurvey)
                {
                    row.add([field: '', style: null])
                }

            }


            renewalData.add(row)
        }

        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: g.message(code: 'renewalWithSurvey.orgsWithParticipationInParentSuccessor.label')+ " (${renewalResult.orgsWithParticipationInParentSuccessor.size() ?: 0})", style: 'positive']])


        renewalResult.orgsWithParticipationInParentSuccessor.each { sub ->
            List row = []

            sub.getAllSubscribers().each{ subscriberOrg ->

                row.add([field: subscriberOrg.sortname ?: '', style: null])
                row.add([field: subscriberOrg.name ?: '', style: null])

                row.add([field: '', style: null])

                row.add([field: '', style: null])

                def period = ""

                period = sub.startDate ? sdf.format(sub.startDate) : ""
                period = sub.endDate ? period + " - " +sdf.format(sub.endDate) : ""

                row.add([field: period?: '', style: null])

                if (renewalResult.multiYearTermTwoSurvey || renewalResult.multiYearTermThreeSurvey)
                {
                    row.add([field: '', style: null])
                }
            }


            renewalData.add(row)
        }

        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: g.message(code: 'renewalWithSurvey.orgsLateCommers.label')+ " (${renewalResult.orgsLateCommers.size() ?: 0})", style: 'positive']])


        renewalResult.orgsLateCommers.each { sub ->
            List row = []

            sub.getAllSubscribers().each{ subscriberOrg ->

                row.add([field: subscriberOrg.sortname ?: '', style: null])
                row.add([field: subscriberOrg.name ?: '', style: null])

                row.add([field: '', style: null])

                row.add([field: '', style: null])

                def period = ""

                period = sub.startDate ? sdf.format(sub.startDate) : ""
                period = sub.endDate ? period + " - " +sdf.format(sub.endDate) : ""

                row.add([field: period?: '', style: null])

                if (renewalResult.multiYearTermTwoSurvey || renewalResult.multiYearTermThreeSurvey)
                {
                    row.add([field: '', style: null])
                }
            }


            renewalData.add(row)
        }

        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: g.message(code: 'renewalWithSurvey.newOrgstoSubscription.label')+ " (${renewalResult.newOrgstoSubscription.size() ?: 0})", style: 'positive']])


        renewalResult.newOrgsContinuetoSubscription.each { participantResult ->
            List row = []

            row.add([field: participantResult.participant.sortname ?: '', style: null])
            row.add([field: participantResult.participant.name ?: '', style: null])

            row.add([field: participantResult.resultOfParticipation.getResult() ?: '', style: null])

            row.add([field: participantResult.resultOfParticipation.comment ?: '', style: null])


            def period = ""
            if (renewalResult.multiYearTermTwoSurvey) {
                period = participantResult.newSubPeriodTwoStartDate ? sdf.format(participantResult.newSubPeriodTwoStartDate) : ""
                period = period + " - " + participantResult.newSubPeriodTwoEndDate ? sdf.format(participantResult.newSubPeriodTwoEndDate) : ""
            }
            period = ""
            if (renewalResult.multiYearTermThreeSurvey) {
                period = participantResult.newSubPeriodThreeStartDate ?: ""
                period = period + " - " + participantResult.newSubPeriodThreeEndDate ?: ""
            }
            row.add([field: period ?: '', style: null])

            if (renewalResult.multiYearTermTwoSurvey) {
                row.add([field: participantResult.participantPropertyTwoComment ?: '', style: null])
            }

            if (renewalResult.multiYearTermThreeSurvey) {
                row.add([field: participantResult.participantPropertyThreeComment ?: '', style: null])
            }

            participantResult.properties.sort {
                it.type.name
            }.each { participantResultProperty ->
                row.add([field: participantresultProperty.getResult() ?: "", style: null])

                row.add([field: participantResultProperty.comment ?: "", style: null])

            }

            def costItem = CostItem.findBySurveyOrgAndCostItemStatusNotEqual(SurveyOrg.findBySurveyConfigAndOrg(participantResult.resultOfParticipation.surveyConfig, participantResult.participant),RDStore.COST_ITEM_DELETED)
            row.add([field: costItem?.costInBillingCurrency ? costItem.costInBillingCurrency : "", style: null])
            row.add([field: costItem?.costInBillingCurrencyAfterTax ? costItem.costInBillingCurrencyAfterTax : "", style: null])
            row.add([field: costItem?.taxKey ? costItem.taxKey.taxRate+'%' : "", style: null])
            row.add([field: costItem?.billingCurrency ? costItem.billingCurrency.getI10n('value').split('-').first() : "", style: null])

            renewalData.add(row)
        }

        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: '', style: null]])
        renewalData.add([[field: g.message(code: 'renewalWithSurvey.withTermination.label')+ " (${renewalResult.orgsWithTermination.size() ?: 0})", style: 'negative']])


        renewalResult.orgsWithTermination.each { participantResult ->
            List row = []

            row.add([field: participantResult.participant.sortname ?: '', style: null])
            row.add([field: participantResult.participant.name ?: '', style: null])

            row.add([field: participantResult.resultOfParticipation.getResult() ?: '', style: null])

            row.add([field: participantResult.resultOfParticipation.comment ?: '', style: null])

            row.add([field: '', style: null])

            if (renewalResult.multiYearTermTwoSurvey || renewalResult.multiYearTermThreeSurvey)
            {
                row.add([field: '', style: null])
            }

            participantResult.properties.sort {
                it.type.name
            }.each { participantResultProperty ->
                row.add([field: participantResultProperty.getResult() ?: "", style: null])

                row.add([field: participantResultProperty.comment ?: "", style: null])

            }

            def costItem = CostItem.findBySurveyOrgAndCostItemStatusNotEqual(SurveyOrg.findBySurveyConfigAndOrg(participantResult.resultOfParticipation.surveyConfig, participantResult.participant),RDStore.COST_ITEM_DELETED)

            row.add([field: costItem?.costInBillingCurrency ? costItem.costInBillingCurrency : "", style: null])
            row.add([field: costItem?.costInBillingCurrencyAfterTax ? costItem.costInBillingCurrencyAfterTax : "", style: null])
            row.add([field: costItem?.taxKey ? costItem.taxKey.taxRate+'%' : "", style: null])
            row.add([field: costItem?.billingCurrency ? costItem.billingCurrency.getI10n('value').split('-').first() : "", style: null])

            renewalData.add(row)
        }


        Map sheetData = [:]
        sheetData[message(code: 'renewalexport.renewals')] = [titleRow: titles, columnData: renewalData]
        return exportService.generateXLSXWorkbook(sheetData)
    }

    private def exportSurveyCostItems(SurveyConfig surveyConfig, String format, Org org) {
        SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
        List titles = ['Name',
                       '',
                       g.message(code: 'surveyConfig.type.label'),
                       g.message(code: 'surveyConfigsInfo.comment'),

                       g.message(code: 'surveyParticipants.label'),
                       g.message(code: 'org.shortname.label'),
                       g.message(code: 'org.libraryNetwork.label'),
                       g.message(code: 'surveyProperty.subName'),
                       g.message(code: 'surveyConfigsInfo.newPrice'),
                       g.message(code: 'financials.currency'),
                       g.message(code: 'surveyConfigsInfo.newPrice.comment')
        ]

        List surveyData = []

        def surveyOrgs = SurveyOrg.findAllBySurveyConfig(surveyConfig)

        surveyOrgs.each { surveyOrg ->
            List row = []

            row.add([field: surveyConfig.surveyInfo.name ?: '', style: null])

            row.add([field: surveyConfig.getConfigNameShort() ?: '', style: null])

            row.add([field: surveyConfig.type == 'Subscription' ? com.k_int.kbplus.SurveyConfig.getLocalizedValue(surveyConfig.type) : com.k_int.kbplus.SurveyConfig.getLocalizedValue(config.type) + '(' + PropertyDefinition.getLocalizedValue(surveyConfig.surveyProperty.type) + ')', style: null])

            row.add([field: surveyConfig.comment ?: '', style: null])

            row.add([field: surveyOrg.org.name ?: '', style: null])

            row.add([field: surveyOrg.org.shortname ?: '', style: null])

            row.add([field: surveyOrg.org.libraryType?.getI10n('value') ?: '', style: null])

            row.add([field: surveyConfig.subscription.getDerivedSubscriptionBySubscribers(surveyOrg.org).name ?: '', style: null])


            def costItem = CostItem.findBySurveyOrgAndCostItemStatusNotEqual(surveyOrg,RDStore.COST_ITEM_DELETED)

            if (!surveyOrg.existsMultiYearTerm()) {
                if (costItem) {
                    row.add([field: g.formatNumber(number: costItem.costInBillingCurrencyAfterTax, minFractionDigits: 2, maxFractionDigits: 2, type: "number"), style: null])
                    row.add([field: costItem.billingCurrency?.getI10n('value').split('-').first(), style: null])
                }
            } else {
                row.add([field: g.message(code: "surveyOrg.perennialTerm.available"), style: null])
                row.add([field: '', style: null])
            }

            row.add([field: costItem.costDescription ?: '', style: null])

            surveyData.add(row)
        }

        switch (format) {
            case 'xls':
            case 'xlsx':
                Map sheetData = [:]
                sheetData[message(code: 'menu.my.surveys')] = [titleRow: titles, columnData: surveyData]
                return exportService.generateXLSXWorkbook(sheetData)
        }
    }

    private def getSurveyConfigCounts() {
        Map<String, Object> result = [:]

        Org contextOrg = contextService.getOrg()

        result.created = SurveyConfig.executeQuery("from SurveyInfo surInfo left join surInfo.surveyConfigs surConfig where surInfo.owner = :contextOrg and (surInfo.status = :status or surInfo.status = :status2)",
                [contextOrg: contextOrg, status: RDStore.SURVEY_READY, status2: RDStore.SURVEY_IN_PROCESSING]).size()

        result.active = SurveyConfig.executeQuery("from SurveyInfo surInfo left join surInfo.surveyConfigs surConfig where surInfo.owner = :contextOrg and surInfo.status = :status",
                [contextOrg: contextOrg, status: RDStore.SURVEY_SURVEY_STARTED]).size()

        result.finish = SurveyConfig.executeQuery("from SurveyInfo surInfo left join surInfo.surveyConfigs surConfig where surInfo.owner = :contextOrg and surInfo.status = :status",
                [contextOrg: contextOrg, status: RDStore.SURVEY_SURVEY_COMPLETED]).size()

        result.inEvaluation = SurveyConfig.executeQuery("from SurveyInfo surInfo left join surInfo.surveyConfigs surConfig where surInfo.owner = :contextOrg and surInfo.status = :status",
                [contextOrg: contextOrg, status: RDStore.SURVEY_IN_EVALUATION]).size()


        return result
    }

    private LinkedHashMap setResultGenericsAndCheckAccess() {
        Map<String, Object> result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)
        result.surveyInfo = SurveyInfo.get(params.id)
        result.surveyConfig = params.surveyConfigID ? SurveyConfig.get(params.surveyConfigID as Long ? params.surveyConfigID: Long.parseLong(params.surveyConfigID)) : result.surveyInfo.surveyConfigs[0]
        result.surveyWithManyConfigs = (result.surveyInfo.surveyConfigs.size() > 1)

        result.editable = result.surveyInfo.isEditable() ?: false

        if(result.surveyConfig)
        {
            result.transferWorkflow = result.surveyConfig.transferWorkflow ? JSON.parse(result.surveyConfig.transferWorkflow) : null
        }

        result.subscriptionInstance =  result.surveyConfig.subscription ?: null



        result
    }

    private LinkedHashMap setResultGenericsAndCheckAccessforSub(checkOption) {
        Map<String, Object> result = [:]
        result.user = User.get(springSecurityService.principal.id)
        result.subscriptionInstance = Subscription.get(params.id)
        result.subscription = Subscription.get(params.id)
        result.institution = result.subscription.subscriber

        if (checkOption in [AccessService.CHECK_VIEW, AccessService.CHECK_VIEW_AND_EDIT]) {
            if (!result.subscriptionInstance.isVisibleBy(result.user)) {
                log.debug("--- NOT VISIBLE ---")
                return null
            }
        }
        result.editable = result.subscriptionInstance.isEditableBy(result.user)

        if (checkOption in [AccessService.CHECK_EDIT, AccessService.CHECK_VIEW_AND_EDIT]) {
            if (!result.editable) {
                log.debug("--- NOT EDITABLE ---")
                return null
            }
        }

        result
    }

    private def setNewProperty(def property, def value) {

        def field = null

        if(property.type.type == Integer.toString()) {
            field = "intValue"
        }
        else if (property.type.type == String.toString())  {
            field = "stringValue"
        }
        else if (property.type.type == BigDecimal.toString())  {
            field = "decValue"
        }
        else if (property.type.type == Date.toString())  {
            field = "dateValue"
        }
        else if (property.type.type == URL.toString())  {
            field = "urlValue"
        }
        else if (property.type.type == RefdataValue.toString())  {
            field = "refValue"
        }

        //Wenn eine Vererbung vorhanden ist.
        if(field && property.hasProperty('instanceOf') && property.instanceOf && AuditConfig.getConfig(property.instanceOf)){
            if(property.instanceOf."${field}" == '' || property.instanceOf."${field}" == null)
            {
                value = property.instanceOf."${field}" ?: ''
            }else{
                //
                return
            }
        }

        if (value == '' && field) {
            // Allow user to set a rel to null be calling set rel ''
            property[field] = null
            property.save(flush: true);
        } else {

                if (property && value && field){

                if(field == "refValue") {
                    def binding_properties = ["${field}": value]
                    bindData(property, binding_properties)
                    //property.save()
                    if(!property.save(failOnError: true, flush: true))
                    {
                        println(property.error)
                    }
                } else if(field == "dateValue") {
                    SimpleDateFormat sdf = DateUtil.getSDF_NoTime()

                    def backup = property."${field}"
                    try {
                        if (value && value.size() > 0) {
                            // parse new date
                            def parsed_date = sdf.parse(value)
                            property."${field}" = parsed_date
                        } else {
                            // delete existing date
                            property."${field}" = null
                        }
                        property.save(failOnError: true, flush: true);
                    }
                    catch (Exception e) {
                        property."${field}" = backup
                        log.error(e)
                    }
                } else if(field == "urlValue") {

                    def backup = property."${field}"
                    try {
                        if (value && value.size() > 0) {
                            property."${field}" = new URL(value)
                        } else {
                            // delete existing url
                            property."${field}" = null
                        }
                        property.save(failOnError: true, flush: true)
                    }
                    catch (Exception e) {
                        property."${field}" = backup
                        log.error(e)
                    }
                } else {
                    def binding_properties = [:]
                    if (property."${field}" instanceof Double) {
                        value = Double.parseDouble(value)
                    }

                    binding_properties["${field}"] = value
                    bindData(property, binding_properties)

                    property.save(failOnError: true, flush: true)

                }

            }
        }

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

    boolean addSurPropToSurvey(SurveyConfig surveyConfig, PropertyDefinition surveyProperty) {

        if (!SurveyConfigProperties.findAllBySurveyPropertyAndSurveyConfig(surveyProperty, surveyConfig) && surveyProperty && surveyConfig) {
            SurveyConfigProperties propertytoSub = new SurveyConfigProperties(surveyConfig: surveyConfig, surveyProperty: surveyProperty)
            if(propertytoSub.save(flush: true)){
                return true
            }else {
                return false
            }
        }else {
            return false
        }
    }
    
    boolean copySurveyConfigCharacteristic(SurveyConfig oldSurveyConfig, SurveyConfig newSurveyConfig, params){

        oldSurveyConfig.documents.each { dctx ->
                //Copy Docs
                if (params.copySurvey.copyDocs) {
                    if (((dctx.owner?.contentType == 1) || (dctx.owner?.contentType == 3)) && (dctx.status != RDStore.DOC_CTX_STATUS_DELETED)) {
                        Doc clonedContents = new Doc(
                                blobContent: dctx.owner.blobContent,
                                status: dctx.owner.status,
                                type: dctx.owner.type,
                                content: dctx.owner.content,
                                uuid: dctx.owner.uuid,
                                contentType: dctx.owner.contentType,
                                title: dctx.owner.title,
                                creator: dctx.owner.creator,
                                filename: dctx.owner.filename,
                                mimeType: dctx.owner.mimeType,
                                user: dctx.owner.user,
                                migrated: dctx.owner.migrated,
                                owner: dctx.owner.owner
                        ).save()

                        DocContext ndc = new DocContext(
                                owner: clonedContents,
                                surveyConfig: newSurveyConfig,
                                domain: dctx.domain,
                                status: dctx.status,
                                doctype: dctx.doctype
                        ).save()
                    }
                }
                //Copy Announcements
                if (params.copySurvey.copyAnnouncements) {
                    if ((dctx.owner?.contentType == com.k_int.kbplus.Doc.CONTENT_TYPE_STRING) && !(dctx.domain) && (dctx.status != RDStore.DOC_CTX_STATUS_DELETED)) {
                        Doc clonedContents = new Doc(
                                blobContent: dctx.owner.blobContent,
                                status: dctx.owner.status,
                                type: dctx.owner.type,
                                content: dctx.owner.content,
                                uuid: dctx.owner.uuid,
                                contentType: dctx.owner.contentType,
                                title: dctx.owner.title,
                                creator: dctx.owner.creator,
                                filename: dctx.owner.filename,
                                mimeType: dctx.owner.mimeType,
                                user: dctx.owner.user,
                                migrated: dctx.owner.migrated
                        ).save()

                        DocContext ndc = new DocContext(
                                owner: clonedContents,
                                surveyConfig: newSurveyConfig,
                                domain: dctx.domain,
                                status: dctx.status,
                                doctype: dctx.doctype
                        ).save()
                    }
                }
            }
            //Copy Tasks
            if (params.copySurvey.copyTasks) {

                Task.findAllBySurveyConfig(oldSurveyConfig).each { task ->

                    Task newTask = new Task()
                    InvokerHelper.setProperties(newTask, task.properties)
                    newTask.systemCreateDate = new Date()
                    newTask.surveyConfig = newSurveyConfig
                    newTask.save()
                }

            }

        //Copy Participants
        if (params.copySurvey.copyParticipants) {
            oldSurveyConfig.orgs.each { surveyOrg ->

                SurveyOrg newSurveyOrg = new SurveyOrg(surveyConfig: newSurveyConfig, org: surveyOrg.org).save()
            }
        }

        //Copy Properties
        if (params.copySurvey.copySurveyProperties) {
            oldSurveyConfig.surveyProperties.each { surveyConfigProperty ->

                SurveyConfigProperties configProperty = new SurveyConfigProperties(
                        surveyProperty: surveyConfigProperty.surveyProperty,
                        surveyConfig: newSurveyConfig).save()
            }
        }
    }
}
