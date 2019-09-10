package com.k_int.kbplus

import com.k_int.kbplus.auth.User
import com.k_int.properties.PropertyDefinition
import de.laser.AccessService
import de.laser.helper.DateUtil
import de.laser.helper.DebugAnnotation
import de.laser.helper.RDStore
import de.laser.interfaces.TemplateSupport
import grails.plugin.springsecurity.annotation.Secured
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.dao.DataIntegrityViolationException

import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat

@Secured(['IS_AUTHENTICATED_FULLY'])
class SurveyController {

    def springSecurityService
    def accessService
    def contextService
    def subscriptionsQueryService
    def filterService
    def docstoreService
    def orgTypeService
    def genericOIDService
    def surveyService
    def financeService
    def exportService
    def taskService

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def currentSurveysConsortia() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0;

        params.max = result.max
        params.offset = result.offset

        params.tab = params.tab ?: 'created'

        DateFormat sdFormat = new DateUtil().getSimpleDateFormat_NoTime()
        def fsq = filterService.getSurveyConfigQueryConsortia(params, sdFormat, result.institution)

        result.surveys = SurveyInfo.executeQuery(fsq.query, fsq.queryParams, params)
        result.countSurveyConfigs = getSurveyConfigCounts()

        result
    }


    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def createGeneralSurvey() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        result
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processCreateGeneralSurvey() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }
        def sdf = new DateUtil().getSimpleDateFormat_NoTime()
        def surveyInfo = new SurveyInfo(
                name: params.name,
                startDate: params.startDate ? sdf.parse(params.startDate) : null,
                endDate: params.endDate ? sdf.parse(params.endDate) : null,
                type: params.type,
                owner: contextService.getOrg(),
                status: RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung']),
                comment: params.comment ?: null,
                isSubscriptionSurvey: false
        )

        if (!(surveyInfo.save(flush: true))) {
            flash.error = g.message(code: "createGeneralSurvey.create.fail")
            redirect(url: request.getHeader('referer'))
        }
        flash.message = g.message(code: "createGeneralSurvey.create.successfull")
        redirect action: 'show', id: surveyInfo.id

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def createSubscriptionSurvey() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.max = params.max ? Integer.parseInt(params.max) : result.user.getDefaultPageSizeTMP();
        result.offset = params.offset ? Integer.parseInt(params.offset) : 0

        def date_restriction = null;
        def sdf = new DateUtil().getSimpleDateFormat_NoTime()

        if (params.validOn == null || params.validOn.trim() == '') {
            result.validOn = ""
        } else {
            result.validOn = params.validOn
            date_restriction = sdf.parse(params.validOn)
        }

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")

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

        List<Org> providers = orgTypeService.getCurrentProviders(contextService.getOrg())
        List<Org> agencies = orgTypeService.getCurrentAgencies(contextService.getOrg())

        providers.addAll(agencies)
        List orgIds = providers.unique().collect { it2 -> it2.id }

        result.providers = Org.findAllByIdInList(orgIds).sort { it?.name }

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

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def addSubtoSubscriptionSurvey() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")

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

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processCreateSubscriptionSurvey() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }
        def sdf = new DateUtil().getSimpleDateFormat_NoTime()

        def surveyInfo = new SurveyInfo(
                name: params.name,
                startDate: params.startDate ? sdf.parse(params.startDate) : null,
                endDate: params.endDate ? sdf.parse(params.endDate) : null,
                type: params.type,
                owner: contextService.getOrg(),
                status: RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung']),
                comment: params.comment ?: null,
                isSubscriptionSurvey: true
        )

        if (!(surveyInfo.save(flush: true))) {
            flash.error = g.message(code: "createSubscriptionSurvey.create.fail")
            redirect(url: request.getHeader('referer'))
        }

        def subscription = Subscription.get(Long.parseLong(params.sub))
        def surveyConfig = subscription ? SurveyConfig.findAllBySubscriptionAndSurveyInfo(subscription, surveyInfo) : null
        if (!surveyConfig && subscription) {
            surveyConfig = new SurveyConfig(
                    subscription: subscription,
                    configOrder: surveyInfo?.surveyConfigs?.size() ? surveyInfo?.surveyConfigs?.size() + 1 : 1,
                    type: 'Subscription',
                    surveyInfo: surveyInfo,
                    isSubscriptionSurveyFix: SurveyConfig.findAllBySubscriptionAndIsSubscriptionSurveyFix(subscription, true) ? false : (params.isSubscriptionSurveyFix ? true : false)

            )

            surveyConfig.save(flush: true)

            //Wenn es eine Umfrage schon gibt, die als Übertrag dient. Dann ist es auch keine Lizenz Umfrage mit einem Teilname-Merkmal abfragt!
            if(!SurveyConfig.findAllBySubscriptionAndIsSubscriptionSurveyFix(subscription, true)) {
                def configProperty = new SurveyConfigProperties(
                        surveyProperty: SurveyProperty.findByName('Participation'),
                        surveyConfig: surveyConfig)
                if(configProperty.save(flush: true)) {
                    addSubMembers(surveyConfig)
                }
            }else{
                addSubMembers(surveyConfig)
            }



        } else {
            surveyInfo.delete(flush: true)
            flash.error = g.message(code: "createSubscriptionSurvey.create.fail")
            redirect(url: request.getHeader('referer'))
        }

        flash.message = g.message(code: "createSubscriptionSurvey.create.successfull")
        redirect action: 'show', id: surveyInfo.id

    }



    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def show() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.surveyConfigs = result.surveyInfo?.surveyConfigs?.sort { it?.configOrder }

        result

    }


    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyConfigs() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.surveyProperties = SurveyProperty.findAllByOwner(result.institution)

        result.properties = getSurveyProperties(result.institution)

        result.editable = (result.surveyInfo && result.surveyInfo?.status != RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung'])) ? false : result.editable

        result.surveyConfigs = result.surveyInfo.surveyConfigs.sort { it?.getConfigNameShort() }

        result

    }


    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyConfigDocs() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyParticipants() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        // new: filter preset
        params.orgType = RDStore.OT_INSTITUTION?.id?.toString()
        params.orgSector = RDStore.O_SECTOR_HIGHER_EDU?.id?.toString()

        result.propList = PropertyDefinition.findAllPublicAndPrivateOrgProp(contextService.org)

        params.comboType = RDStore.COMBO_TYPE_CONSORTIUM.value
        def fsq = filterService.getOrgComboQuery(params, result.institution)
        def tmpQuery = "select o.id " + fsq.query.minus("select o ")
        def consortiaMemberIds = Org.executeQuery(tmpQuery, fsq.queryParams)

        if (params.filterPropDef && consortiaMemberIds) {
            fsq = propertyService.evalFilterQuery(params, "select o FROM Org o WHERE o.id IN (:oids)", 'o', [oids: consortiaMemberIds])
        }
        result.consortiaMembers = Org.executeQuery(fsq.query, fsq.queryParams, params)
        result.consortiaMembersCount = Org.executeQuery(fsq.query, fsq.queryParams).size()

        result.editable = (result.surveyInfo && result.surveyInfo?.status != RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung'])) ? false : result.editable

        result.surveyConfigs = result.surveyInfo?.surveyConfigs.sort { it?.configOrder }

        params.surveyConfigID = params.surveyConfigID ?: result?.surveyConfigs[0]?.id?.toString()

        result.surveyConfig = SurveyConfig.get(params.surveyConfigID)

        def surveyOrgs = result.surveyConfig?.getSurveyOrgsIDs()

        result.selectedParticipants = getfilteredSurveyOrgs(surveyOrgs.orgsWithoutSubIDs, fsq.query, fsq.queryParams, params)
        result.selectedSubParticipants = getfilteredSurveyOrgs(surveyOrgs.orgsWithSubIDs, fsq.query, fsq.queryParams, params)

        params.tab = params.tab ?: (result.surveyConfig.type == 'Subscription' ? 'selectedSubParticipants' : 'selectedParticipants')

        result

    }


    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyCostItems() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        params.tab = params.tab ?: 'selectedSubParticipants'

        // new: filter preset
        params.orgType = RDStore.OT_INSTITUTION?.id?.toString()
        params.orgSector = RDStore.O_SECTOR_HIGHER_EDU?.id?.toString()

        result.propList = PropertyDefinition.findAllPublicAndPrivateOrgProp(contextService.org)

        params.comboType = RDStore.COMBO_TYPE_CONSORTIUM.value
        def fsq = filterService.getOrgComboQuery(params, result.institution)
        def tmpQuery = "select o.id " + fsq.query.minus("select o ")
        def consortiaMemberIds = Org.executeQuery(tmpQuery, fsq.queryParams)

        if (params.filterPropDef && consortiaMemberIds) {
            fsq = propertyService.evalFilterQuery(params, "select o FROM Org o WHERE o.id IN (:oids)", 'o', [oids: consortiaMemberIds])
        }

        result.editable = (result.surveyInfo.status != RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung'])) ? false : result.editable

        //Only SurveyConfigs with Subscriptions
        result.surveyConfigs = result.surveyInfo?.surveyConfigs.findAll { it.subscription != null }?.sort {
            it?.configOrder
        }

        params.surveyConfigID = params.surveyConfigID ?: result?.surveyConfigs[0]?.id?.toString()

        result.surveyConfig = SurveyConfig.get(params.surveyConfigID)

        def surveyOrgs = result.surveyConfig?.getSurveyOrgsIDs()

        result.selectedParticipants = getfilteredSurveyOrgs(surveyOrgs.orgsWithoutSubIDs, fsq.query, fsq.queryParams, params)
        result.selectedSubParticipants = getfilteredSurveyOrgs(surveyOrgs.orgsWithSubIDs, fsq.query, fsq.queryParams, params)

        result.selectedCostItemElement = params.selectedCostItemElement ?: RefdataValue.getByValueAndCategory('price: consortial price', 'CostItemElement').id.toString()

        if (params.selectedCostItemElement) {
            params.remove('selectedCostItemElement')
        }
        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyConfigFinish() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.surveyConfig.configFinish = params.configFinish ?: false
        if (result.surveyConfig.save(flush: true)) {
            flash.message = g.message(code: 'survey.change.successfull')
        } else {
            flash.error = g.message(code: 'survey.change.fail')
        }

        redirect(url: request.getHeader('referer'))

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyCostItemsFinish() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.surveyConfig.costItemsFinish = params.costItemsFinish ?: false

        if (result.surveyConfig.save(flush: true)) {
            flash.message = g.message(code: 'survey.change.successfull')
        } else {
            flash.error = g.message(code: 'survey.change.fail')
        }

        redirect(url: request.getHeader('referer'))

    }


    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyEvaluation() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        params.tab = params.tab ?: 'surveyConfigsView'

        result.surveyConfigs = result.surveyInfo?.surveyConfigs.sort { it?.configOrder }

        def orgs = result.surveyConfigs?.orgs.org.flatten().unique { a, b -> a.id <=> b.id }
        result.participants = orgs.sort { it.sortname }

        result.participantsNotFinish = SurveyResult.findAllBySurveyConfigInListAndFinishDateIsNull(result.surveyConfigs)?.participant?.flatten()?.unique { a, b -> a.id <=> b.id }.sort {
            it.sortname
        }
        result.participantsFinish = SurveyResult.findAllBySurveyConfigInListAndFinishDateIsNotNull(result.surveyConfigs)?.participant?.flatten()?.unique { a, b -> a.id <=> b.id }.sort {
            it.sortname
        }

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def evaluationParticipantInfo() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        //params.tab = params.tab ?: 'surveyConfigsView'

        result.participant = Org.get(params.participant)

        result.surveyResult = SurveyResult.findAllByOwnerAndParticipantAndSurveyConfigInList(result.institution, result.participant, result.surveyInfo.surveyConfigs).sort {
            it?.surveyConfig?.getConfigNameShort()
        }.groupBy { it?.surveyConfig?.id }

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def evaluationConfigsInfo() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.subscriptionInstance = result.surveyConfig?.subscription?.getDerivedSubscriptionBySubscribers(result.institution)


        result.surveyResult = SurveyResult.findAllByOwnerAndSurveyConfig(result.institution, result.surveyConfig).sort {
            it.participant?.sortname
        }


        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def evaluationConfigResult() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        //result.editable = (result.surveyInfo.status != RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung'])) ? false : true

        result.surveyProperty = SurveyProperty.get(params.prop)

        result.surveyResult = SurveyResult.findAllByOwnerAndSurveyConfigAndType(result.institution, result.surveyConfig, result.surveyProperty).sort {
            it.participant?.sortname
        }

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def allSurveyProperties() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.surveyProperties = SurveyProperty.findAllByOwner(result.institution)

        result.properties = getSurveyProperties(result.institution)

        result.addSurveyConfigs = params.addSurveyConfigs ?: false

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def surveyConfigsInfo() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.surveyProperties = result.surveyConfig?.surveyProperties

        result.navigation = surveyService.getConfigNavigation(result.surveyInfo, result.surveyConfig)

        if (result.surveyConfig?.type == 'Subscription') {
            result.authorizedOrgs = result.user?.authorizedOrgs
            result.contextOrg = contextService.getOrg()
            // restrict visible for templates/links/orgLinksAsList
            result.visibleOrgRelations = []
            result.surveyConfig?.subscription?.orgRelations?.each { or ->
                if (!(or.org?.id == contextService.getOrg()?.id) && !(or.roleType.value in ['Subscriber', 'Subscriber_Consortial'])) {
                    result.visibleOrgRelations << or
                }
            }
            result.visibleOrgRelations.sort { it.org.sortname }

            result.subscription = result.surveyConfig?.subscription ?: null
            result.subscriptionInstance = result.surveyConfig?.subscription ?: null

            //costs
            if (result.subscription.getCalculatedType().equals(TemplateSupport.CALCULATED_TYPE_CONSORTIAL))
                params.view = "cons"
            else if (result.subscription.getCalculatedType().equals(TemplateSupport.CALCULATED_TYPE_PARTICIPATION) && result.subscription.getConsortia().equals(result.institution))
                params.view = "consAtSubscr"
            else if (result.subscription.getCalculatedType().equals(TemplateSupport.CALCULATED_TYPE_PARTICIPATION) && !result.subscription.getConsortia().equals(result.institution))
                params.view = "subscr"
            //cost items
            //params.forExport = true
            LinkedHashMap costItems = financeService.getCostItemsForSubscription(result.subscription, params, 10, 0)
            result.costItemSums = [:]
            if (costItems.own.count > 0) {
                result.costItemSums.ownCosts = costItems.own.sums
            }
            if (costItems.cons.count > 0) {
                result.costItemSums.consCosts = costItems.cons.sums
            }
            if (costItems.subscr.count > 0) {
                result.costItemSums.subscrCosts = costItems.subscr.sums
            }
        }

        result.properties = []
        def allProperties = getSurveyProperties(result.institution)
        allProperties.each {

            if (!(it.id in result?.surveyProperties?.surveyProperty?.id)) {
                result.properties << it
            }
        }

        def contextOrg = contextService.getOrg()
        result.tasks = taskService.getTasksByResponsiblesAndObject(result.user, contextOrg, result.surveyConfig)
        def preCon = taskService.getPreconditions(contextOrg)
        result << preCon

        result

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def changeConfigOrder() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        if (result.surveyInfo.surveyConfigs.size() > 0) {
            def surveyConfig = SurveyConfig.get(params.surveyConfigID)

            if (params.change == 'up') {

                def secoundSurveyConfig = SurveyConfig.findBySurveyInfoAndConfigOrder(result.surveyInfo, surveyConfig.configOrder - 1)
                secoundSurveyConfig.configOrder = surveyConfig.configOrder
                secoundSurveyConfig.save(flush: true)
                surveyConfig.configOrder = surveyConfig.configOrder - 1
                if (surveyConfig.save(flush: true)) {
                    flash.message = g.message(code: 'survey.change.successfull')
                } else {
                    flash.error = g.message(code: 'survey.change.fail')
                }

            }

            if (params.change == 'down') {
                def secoundSurveyConfig = SurveyConfig.findBySurveyInfoAndConfigOrder(result.surveyInfo, surveyConfig.configOrder + 1)
                secoundSurveyConfig.configOrder = surveyConfig.configOrder
                secoundSurveyConfig.save(flush: true)
                surveyConfig.configOrder = surveyConfig.configOrder + 1

                if (surveyConfig.save(flush: true)) {
                    flash.message = g.message(code: 'survey.change.successfull')
                } else {
                    flash.error = g.message(code: 'survey.change.fail')
                }
            }

        }
        redirect(url: request.getHeader('referer'))
    }

    /*@DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def addSurveyConfig() {

        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR')

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        def surveyInfo = SurveyInfo.get(params.id) ?: null

        if (surveyInfo) {
            if (params.subscription) {
                def subscription = Subscription.get(Long.parseLong(params.subscription))
                def surveyConfig = subscription ? SurveyConfig.findAllBySubscriptionAndSurveyInfo(subscription, surveyInfo) : null
                if (!surveyConfig && subscription) {
                    surveyConfig = new SurveyConfig(
                            subscription: subscription,
                            configOrder: surveyInfo.surveyConfigs.size() + 1,
                            type: 'Subscription',
                            surveyInfo: surveyInfo

                    )
                    surveyConfig.save(flush: true)

                    def configProperty = new SurveyConfigProperties(
                            surveyProperty: SurveyProperty.findByName('Continue to license'),
                            surveyConfig: surveyConfig).save(flush: true)

                    flash.message = g.message(code: "surveyConfigs.add.successfully")

                } else {
                    flash.error = g.message(code: "surveyConfigs.exists")
                }
            }
            if (params.property && !params.addtoallSubs) {
                def property = SurveyProperty.get(Long.parseLong(params.property))
                def surveyConfigProp = property ? SurveyConfig.findAllBySurveyPropertyAndSurveyInfo(property, surveyInfo) : null
                if (!surveyConfigProp && property) {
                    surveyConfigProp = new SurveyConfig(
                            surveyProperty: property,
                            configOrder: surveyInfo.surveyConfigs.size() + 1,
                            type: 'SurveyProperty',
                            surveyInfo: surveyInfo

                    )
                    surveyConfigProp.save(flush: true)

                    flash.message = g.message(code: "surveyConfigs.add.successfully")

                } else {
                    flash.error = g.message(code: "surveyConfigs.exists")
                }
            }
            if (params.propertytoSub) {
                def property = SurveyProperty.get(Long.parseLong(params.propertytoSub))
                def surveyConfig = SurveyConfig.get(Long.parseLong(params.surveyConfig))

                def propertytoSub = property ? SurveyConfigProperties.findAllBySurveyPropertyAndSurveyConfig(property, surveyConfig) : null
                if (!propertytoSub && property && surveyConfig) {
                    propertytoSub = new SurveyConfigProperties(
                            surveyConfig: surveyConfig,
                            surveyProperty: property

                    )
                    propertytoSub.save(flush: true)

                    flash.message = g.message(code: "surveyConfigs.add.successfully")

                } else {
                    flash.error = g.message(code: "surveyConfigs.exists")
                }
            }

            if (params.property && params.addtoallSubs) {
                def property = SurveyProperty.get(Long.parseLong(params.property))

                surveyInfo.surveyConfigs.each { surveyConfig ->

                    if (surveyConfig.type == 'Subscription') {
                        def propertytoSub = property ? SurveyConfigProperties.findAllBySurveyPropertyAndSurveyConfig(property, surveyConfig) : null
                        if (!propertytoSub && property && surveyConfig) {
                            propertytoSub = new SurveyConfigProperties(
                                    surveyConfig: surveyConfig,
                                    surveyProperty: property

                            )
                            propertytoSub.save(flush: true)

                            flash.message = g.message(code: "surveyConfigs.add.successfully")

                        } else {
                            flash.error = g.message(code: "surveyConfigs.exists")
                        }
                    }
                }
            }


            redirect action: 'surveyConfigs', id: surveyInfo.id

        } else {
            redirect action: 'currentSurveysConsortia'
        }
    }*/

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def addSurveyConfigs() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }


        result.editable = (result.surveyInfo && result.surveyInfo?.status != RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung'])) ? false : result.editable

        if (result.surveyInfo && result.editable) {

            if (params.selectedProperty) {

                params.list('selectedProperty').each { propertyID ->

                    if (propertyID) {
                        def property = SurveyProperty.get(Long.parseLong(propertyID))
                        //Config is Sub
                        if (params.surveyConfigID && !params.addtoallSubs) {
                            def surveyConfig = SurveyConfig.get(Long.parseLong(params.surveyConfigID))

                            def propertytoSub = property ? SurveyConfigProperties.findAllBySurveyPropertyAndSurveyConfig(property, surveyConfig) : null
                            if (!propertytoSub && property && surveyConfig) {
                                propertytoSub = new SurveyConfigProperties(
                                        surveyConfig: surveyConfig,
                                        surveyProperty: property

                                )
                                propertytoSub.save(flush: true)

                                flash.message = g.message(code: "surveyConfigs.add.successfully")

                            } else {
                                flash.error = g.message(code: "surveyConfigs.exists")
                            }
                        } else if (params.surveyConfigID && params.addtoallSubs) {

                            result.surveyInfo.surveyConfigs.each { surveyConfig ->

                                def propertytoSub = property ? SurveyConfigProperties.findAllBySurveyPropertyAndSurveyConfig(property, surveyConfig) : null
                                if (!propertytoSub && property && surveyConfig) {
                                    propertytoSub = new SurveyConfigProperties(
                                            surveyConfig: surveyConfig,
                                            surveyProperty: property

                                    )
                                    propertytoSub.save(flush: true)

                                    flash.message = g.message(code: "surveyConfigs.add.successfully")

                                } else {
                                    flash.error = g.message(code: "surveyConfigs.exists")
                                }
                            }
                        } else {
                            def surveyConfigProp = property ? SurveyConfig.findAllBySurveyPropertyAndSurveyInfo(property, result.surveyInfo) : null
                            if (!surveyConfigProp && property) {
                                surveyConfigProp = new SurveyConfig(
                                        surveyProperty: property,
                                        configOrder: result.surveyInfo.surveyConfigs.size() + 1,
                                        type: 'SurveyProperty',
                                        surveyInfo: result.surveyInfo

                                )
                                surveyConfigProp.save(flush: true)

                                flash.message = g.message(code: "surveyConfigs.add.successfully")

                            } else {
                                flash.error = g.message(code: "surveyConfigs.exists")
                            }

                        }
                    }


                }

                if (params.surveyConfigID) {
                    redirect action: 'surveyConfigsInfo', id: result.surveyInfo.id, params: [surveyConfigID: params.surveyConfigID]
                } else {
                    redirect action: 'surveyConfigs', id: result.surveyInfo.id
                }
            } else {
                redirect action: 'currentSurveysConsortia'
            }
        }
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def addSurveyConfig() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.editable = (result.surveyInfo && result.surveyInfo?.status != RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung'])) ? false : result.editable

        if (result.surveyInfo && result.editable) {

            if (params.selectedProperty) {
                def property = genericOIDService.resolveOID(params.selectedProperty)
                //Config is Sub
                if (params.surveyConfigID && !params.addtoallSubs) {
                    def surveyConfig = SurveyConfig.get(Long.parseLong(params.surveyConfigID))

                    def propertytoSub = property ? SurveyConfigProperties.findAllBySurveyPropertyAndSurveyConfig(property, surveyConfig) : null
                    if (!propertytoSub && property && surveyConfig) {
                        propertytoSub = new SurveyConfigProperties(
                                surveyConfig: surveyConfig,
                                surveyProperty: property

                        )
                        propertytoSub.save(flush: true)

                        flash.message = g.message(code: "surveyConfigs.add.successfully")

                    } else {
                        flash.error = g.message(code: "surveyConfigs.exists")
                    }
                } else if (params.surveyConfigID && params.addtoallSubs) {

                    result.surveyInfo.surveyConfigs.each { surveyConfig ->

                        def propertytoSub = property ? SurveyConfigProperties.findAllBySurveyPropertyAndSurveyConfig(property, surveyConfig) : null
                        if (!propertytoSub && property && surveyConfig) {
                            propertytoSub = new SurveyConfigProperties(
                                    surveyConfig: surveyConfig,
                                    surveyProperty: property

                            )
                            propertytoSub.save(flush: true)

                            flash.message = g.message(code: "surveyConfigs.add.successfully")

                        } else {
                            flash.error = g.message(code: "surveyConfigs.exists")
                        }
                    }
                } else {
                    def surveyConfigProp = property ? SurveyConfig.findAllBySurveyPropertyAndSurveyInfo(property, result.surveyInfo) : null
                    if (!surveyConfigProp && property) {
                        surveyConfigProp = new SurveyConfig(
                                surveyProperty: property,
                                configOrder: result.surveyInfo.surveyConfigs.size() + 1,
                                type: 'SurveyProperty',
                                surveyInfo: result.surveyInfo

                        )
                        surveyConfigProp.save(flush: true)

                        flash.message = g.message(code: "surveyConfigs.add.successfully")

                    } else {
                        flash.error = g.message(code: "surveyConfigs.exists")
                    }

                }
            }

            if (params.surveyConfigID) {
                redirect action: 'surveyConfigsInfo', id: result.surveyInfo.id, params: [surveyConfigID: params.surveyConfigID]
            } else {
                redirect action: 'surveyConfigs', id: result.surveyInfo.id
            }
        } else {
            redirect action: 'currentSurveysConsortia'
        }
    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def deleteSurveyConfig() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        def surveyConfig = SurveyConfig.get(params.id)
        def surveyInfo = surveyConfig.surveyInfo
        //surveyInfo.removeFromSurveyConfigs(surveyConfig)

        result.editable = (surveyInfo && surveyInfo?.status != RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung'])) ? false : result.editable

        if (result.editable) {
            try {

                SurveyConfigProperties.findAllBySurveyConfig(surveyConfig).each {
                    it.delete(flush: true)
                }

                SurveyOrg.findAllBySurveyConfig(surveyConfig).each {
                    it.delete(flush: true)
                }

                surveyConfig.delete(flush: true)
                flash.message = g.message(code: "default.deleted.message", args: [g.message(code: "surveyConfig.label"), ''])
            }
            catch (DataIntegrityViolationException e) {
                flash.error = g.message(code: "default.not.deleted.message", args: [g.message(code: "surveyConfig.label"), ''])
            }
        }

        redirect(url: request.getHeader('referer'))

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def deleteSurveyPropfromSub() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        def surveyConfigProp = SurveyConfigProperties.get(params.id)

        def surveyInfo = surveyConfigProp?.surveyConfig?.surveyInfo

        result.editable = (surveyInfo && surveyInfo?.status != RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung'])) ? false : result.editable

        if (result.editable) {
            try {
                surveyConfigProp.delete(flush: true)
                flash.message = g.message(code: "default.deleted.message", args: [g.message(code: "surveyConfig.label"), ''])
            }
            catch (DataIntegrityViolationException e) {
                flash.error = g.message(code: "default.not.deleted.message", args: [g.message(code: "surveyConfig.label"), ''])
            }
        }

        redirect(url: request.getHeader('referer'))

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def addSurveyProperty() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        def surveyProperty = SurveyProperty.findWhere(
                name: params.name,
                type: params.type,
                owner: result.institution,
        )

        if ((!surveyProperty) && params.name && params.type) {
            def rdc
            if (params.refdatacategory) {
                rdc = RefdataCategory.findById(Long.parseLong(params.refdatacategory))
            }
            surveyProperty = SurveyProperty.loc(
                    params.name,
                    params.type,
                    rdc,
                    params.explain,
                    params.comment,
                    params.introduction,
                    result.institution
            )

            if (surveyProperty.save(flush: true)) {
                flash.message = message(code: 'surveyProperty.create.successfully', args: [surveyProperty.name])
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

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def addSurveyParticipants() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")

        if (!result.editable) {
            flash.error = g.message(code: "default.notAutorized.message")
            redirect(url: request.getHeader('referer'))
        }

        def surveyConfig = SurveyConfig.get(params.surveyConfigID)
        def surveyInfo = surveyConfig?.surveyInfo

        result.editable = (surveyInfo && surveyInfo?.status != RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung'])) ? false : result.editable

        if (params.selectedOrgs && result.editable) {

            params.list('selectedOrgs').each { soId ->

                def org = Org.get(Long.parseLong(soId))

                if (!(SurveyOrg.findAllBySurveyConfigAndOrg(surveyConfig, org))) {
                    def surveyOrg = new SurveyOrg(
                            surveyConfig: surveyConfig,
                            org: org
                    )

                    if (!surveyOrg.save(flush: true)) {
                        log.debug("Error by add Org to SurveyOrg ${surveyOrg.errors}");
                    } else {
                        flash.message = g.message(code: "surveyParticipants.add.successfully")
                    }
                }
            }
            surveyConfig.save(flush: true)

        }

        redirect action: 'surveyParticipants', id: params.id, params: [surveyConfigID: params.surveyConfigID, tab: 'selectedParticipants']

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def processOpenSurvey() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.editable = (result.surveyInfo && result.surveyInfo?.status != RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung'])) ? false : result.editable

        if (result.editable) {

            result.surveyConfigs = result.surveyInfo?.surveyConfigs.sort { it?.configOrder }

            result.surveyConfigs.each { config ->

                if (config?.type == 'Subscription') {

                    config.orgs?.org?.each { org ->

                        config?.surveyProperties?.each { property ->

                            def surveyResult = new SurveyResult(
                                    owner: result.institution,
                                    participant: org ?: null,
                                    startDate: result.surveyInfo.startDate,
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

                } else {
                    config.orgs?.org?.each { org ->

                        def surveyResult = new SurveyResult(
                                owner: result.institution,
                                participant: org ?: null,
                                startDate: result.surveyInfo.startDate,
                                endDate: result.surveyInfo.endDate,
                                type: config.surveyProperty,
                                surveyConfig: config
                        )

                        if (surveyResult.save(flush: true)) {

                        }


                    }

                }

            }

            result.surveyInfo.status = RefdataValue.loc('Survey Status', [en: 'Ready', de: 'Bereit'])
            result.surveyInfo.save(flush: true)
            flash.message = g.message(code: "openSurvey.successfully")
        }

        redirect action: 'surveyEvaluation', id: params.id

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def deleteSurveyParticipants() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.editable = (result.surveyInfo && result.surveyInfo?.status != RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung'])) ? false : result.editable

        if (params.selectedOrgs && result.editable) {

            params.list('selectedOrgs').each { soId ->
                if (SurveyOrg.findBySurveyConfigAndOrg(result.surveyConfig, Org.get(Long.parseLong(soId))).delete(flush: true)) {
                    flash.message = g.message(code: "surveyParticipants.delete.successfully")
                }
            }
        }

        redirect action: 'surveyParticipants', id: params.id, params: [surveyConfigID: params.surveyConfigID]

    }


    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
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

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def deleteSurveyInfo() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.editable = (result.surveyInfo && result.surveyInfo?.status != RefdataValue.loc('Survey Status', [en: 'In Processing', de: 'In Bearbeitung'])) ? false : result.editable

        if (result.editable) {
            result.surveyInfo.surveyConfigs.each { config ->

                config.documents.each {

                    it.delete()

                }
                it.delete()
            }
        }

        redirect action: 'currentSurveysConsortia'
    }


    @DebugAnnotation(test = 'hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def editSurveyCostItem() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }
        result.costItem = CostItem.findById(params.costItem)

        def costItemElementConfigurations = []
        def orgConfigurations = []

        def ciecs = RefdataValue.findAllByOwner(RefdataCategory.findByDesc('Cost configuration'))
        ciecs.each { ciec ->
            costItemElementConfigurations.add([id: ciec.class.name + ":" + ciec.id, value: ciec.getI10n('value')])
        }
        def orgConf = CostItemElementConfiguration.findAllByForOrganisation(contextService.org)
        orgConf.each { oc ->
            orgConfigurations.add([id: oc.costItemElement.id, value: oc.elementSign.class.name + ":" + oc.elementSign.id])
        }

        result.costItemElementConfigurations = costItemElementConfigurations
        result.orgConfigurations = orgConfigurations
        //result.selectedCostItemElement = params.selectedCostItemElement ?: RefdataValue.getByValueAndCategory('price: consortial price', 'CostItemElement').id.toString()

        result.participant = Org.get(params.participant)
        result.surveyOrg = SurveyOrg.findBySurveyConfigAndOrg(result.surveyConfig, result.participant)

        result.mode = result.costItem ? "edit" : ""
        render(template: "/survey/costItemModal", model: result)
    }

    @DebugAnnotation(test = 'hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def addForAllSurveyCostItem() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        def costItemElementConfigurations = []
        def orgConfigurations = []

        def ciecs = RefdataValue.findAllByOwner(RefdataCategory.findByDesc('Cost configuration'))
        ciecs.each { ciec ->
            costItemElementConfigurations.add([id: ciec.class.name + ":" + ciec.id, value: ciec.getI10n('value')])
        }
        def orgConf = CostItemElementConfiguration.findAllByForOrganisation(contextService.org)
        orgConf.each { oc ->
            orgConfigurations.add([id: oc.costItemElement.id, value: oc.elementSign.class.name + ":" + oc.elementSign.id])
        }

        result.costItemElementConfigurations = costItemElementConfigurations
        result.orgConfigurations = orgConfigurations
        //result.selectedCostItemElement = params.selectedCostItemElement ?: RefdataValue.getByValueAndCategory('price: consortial price', 'CostItemElement').id.toString()

        result.setting = 'bulkForAll'

        result.surveyOrgList = []

        if (params.get('orgsIDs')) {
            List idList = (params.get('orgsIDs')?.split(',')?.collect { Long.valueOf(it.trim()) }).toList()
            result.surveyOrgList = SurveyOrg.findAllByOrgInListAndSurveyConfig(Org.findAllByIdInList(idList), result.surveyConfig)
        }

        render(template: "/survey/costItemModal", model: result)
    }

    @DebugAnnotation(test = 'hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def exportParticipantResult() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }


        def surveyResults

        if (result.surveyConfig) {
            surveyResults = SurveyResult.findAllByOwnerAndSurveyConfig(result.institution, result.surveyConfig).sort {
                it?.participant.sortname
            }

        } else {

            surveyResults = SurveyResult.findAllByOwnerAndSurveyConfigInList(result.institution, result.surveyInfo.surveyConfigs).sort {
                params.exportConfigs ? it?.surveyConfig?.configOrder : it?.participant.sortname
            }
        }


        if (params.exportXLS) {
            def sdf = new SimpleDateFormat(g.message(code: 'default.date.format.notimenopoint'));
            String datetoday = sdf.format(new Date(System.currentTimeMillis()))
            String filename = "${datetoday}_" + g.message(code: "survey.label")
            //if(wb instanceof XSSFWorkbook) file += "x";
            response.setHeader "Content-disposition", "attachment; filename=\"${filename}.xlsx\""
            response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            SXSSFWorkbook wb = (SXSSFWorkbook) ((params.surveyConfigID) ? exportSurveyParticipantResultMin(surveyResults, "xls", result.institution) : exportSurveyParticipantResult(surveyResults, "xls", result.institution))
            wb.write(response.outputStream)
            response.outputStream.flush()
            response.outputStream.close()
            wb.dispose()

            return
        } else {
            redirect(uri: request.getHeader('referer'))
        }

    }

    @DebugAnnotation(perm = "ORG_CONSORTIUM_SURVEY", affil = "INST_EDITOR", specRole = "ROLE_ADMIN")
    @Secured(closure = {
        ctx.accessService.checkPermAffiliationX("ORG_CONSORTIUM_SURVEY", "INST_EDITOR", "ROLE_ADMIN")
    })
    def setInEvaluation() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

        result.surveyInfo.status = RDStore.SURVEY_IN_EVALUATION
        result.surveyInfo.save(flush:true)

        redirect(action: "currentSurveysConsortia", params:[tab: "inEvaluation"])

    }

    @DebugAnnotation(test = 'hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def exportSurCostItems() {
        def result = setResultGenericsAndCheckAccess()
        if (!result.editable) {
            response.sendError(401); return
        }

     /*   def surveyInfo = SurveyInfo.findByIdAndOwner(params.id, result.institution) ?: null

        def surveyConfig = SurveyConfig.findByIdAndSurveyInfo(params.surveyConfigID, surveyInfo)*/

        if (params.exportXLS) {
            def sdf = new SimpleDateFormat(g.message(code: 'default.date.format.notimenopoint'));
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


    @DebugAnnotation(test = 'hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def copyEmailaddresses() {
        def result = [:]
        result.modalID = params.targetId
        result.orgList = []

        if (params.get('orgListIDs')) {
            List idList = (params.get('orgListIDs').split(',').collect { Long.valueOf(it.trim()) }).toList()
            result.orgList = Org.findAllByIdInList(idList)
        }

        render(template: "/survey/copyEmailaddresses", model: result)
    }


    @DebugAnnotation(test = 'hasAffiliation("INST_EDITOR")')
    @Secured(closure = { ctx.springSecurityService.getCurrentUser()?.hasAffiliation("INST_EDITOR") })
    def newSurveyCostItem() {

        def dateFormat = new java.text.SimpleDateFormat(message(code: 'default.date.format.notime', default: 'yyyy-MM-dd'))

        def result = [:]
        def newCostItem = null

        try {
            log.debug("SurveyController::newCostItem() ${params}");

            result.institution = contextService.getOrg()
            def user = User.get(springSecurityService.principal.id)
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
            def cost_currency_rate = params.newCostCurrencyRate ? params.double('newCostCurrencyRate', 1.00) : 1.00
            def cost_local_currency = params.newCostInLocalCurrency ? format.parse(params.newCostInLocalCurrency).doubleValue() : 0.00

            def cost_billing_currency_after_tax = params.newCostInBillingCurrencyAfterTax ? format.parse(params.newCostInBillingCurrencyAfterTax).doubleValue() : cost_billing_currency
            def cost_local_currency_after_tax = params.newCostInLocalCurrencyAfterTax ? format.parse(params.newCostInLocalCurrencyAfterTax).doubleValue() : cost_local_currency
            //moved to TAX_TYPES
            //def new_tax_rate                      = params.newTaxRate ? params.int( 'newTaxRate' ) : 0
            def tax_key = null
            if (!params.newTaxRate.contains("null")) {
                String[] newTaxRate = params.newTaxRate.split("§")
                RefdataValue taxType = genericOIDService.resolveOID(newTaxRate[0])
                int taxRate = Integer.parseInt(newTaxRate[1])
                switch (taxType.id) {
                    case RefdataValue.getByValueAndCategory("taxable", "TaxType").id:
                        switch (taxRate) {
                            case 7: tax_key = CostItem.TAX_TYPES.TAXABLE_7
                                break
                            case 19: tax_key = CostItem.TAX_TYPES.TAXABLE_19
                                break
                        }
                        break
                    case RefdataValue.getByValueAndCategory("taxable tax-exempt", "TaxType").id:
                        tax_key = CostItem.TAX_TYPES.TAX_EXEMPT
                        break
                    case RefdataValue.getByValueAndCategory("not taxable", "TaxType").id:
                        tax_key = CostItem.TAX_TYPES.TAX_NOT_TAXABLE
                        break
                    case RefdataValue.getByValueAndCategory("not applicable", "TaxType").id:
                        tax_key = CostItem.TAX_TYPES.TAX_NOT_APPLICABLE
                        break
                }
            }
            def cost_item_element_configuration = params.ciec ? genericOIDService.resolveOID(params.ciec) : null

            def cost_item_isVisibleForSubscriber = false
            // (params.newIsVisibleForSubscriber ? (RefdataValue.get(params.newIsVisibleForSubscriber)?.value == 'Yes') : false)

            def surveyOrgsDo = []

            if (params.surveyOrg) {
                try {
                    surveyOrgsDo << genericOIDService.resolveOID(params.surveyOrg)
                } catch (Exception e) {
                    log.error("Non-valid surveyOrg sent ${params.surveyOrg}", e)
                }
            }

            if (params.get('surveyOrgs')) {
                List surveyOrgs = (params.get('surveyOrgs')?.split(',')?.collect { String.valueOf(it.replaceAll("\\s","")) }).toList()
                surveyOrgs.each {
                    try {

                        def surveyOrg = genericOIDService.resolveOID(it)
                        if (!CostItem.findBySurveyOrg(surveyOrg)) {
                            surveyOrgsDo << surveyOrg
                        }
                    } catch (Exception e) {
                        log.error("Non-valid surveyOrg sent ${it}", e)
                    }
                }
            }

           /* if (params.surveyConfig) {
                def surveyConfig = genericOIDService.resolveOID(params.surveyConfig)

                surveyConfig?.orgs?.each {

                    if (!CostItem.findBySurveyOrg(it)) {
                        surveyOrgsDo << it
                    }
                }
            }*/

            surveyOrgsDo?.each { surveyOrg ->

                if (!surveyOrg?.existsMultiYearTerm()) {

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
                    newCostItem.costInLocalCurrency = cost_local_currency as Double

                    newCostItem.finalCostRounding = params.newFinalCostRounding ? true : false
                    newCostItem.costInBillingCurrencyAfterTax = cost_billing_currency_after_tax as Double
                    newCostItem.costInLocalCurrencyAfterTax = cost_local_currency_after_tax as Double
                    newCostItem.currencyRate = cost_currency_rate as Double
                    //newCostItem.taxRate = new_tax_rate as Integer -> to taxKey
                    newCostItem.taxKey = tax_key
                    newCostItem.costItemElementConfiguration = cost_item_element_configuration

                    newCostItem.costDescription = params.newDescription ? params.newDescription.trim() : null

                    newCostItem.startDate = startDate ?: null
                    newCostItem.endDate = endDate ?: null

                    newCostItem.includeInSubscription = null
                    //todo Discussion needed, nobody is quite sure of the functionality behind this...


                    if (!newCostItem.validate()) {
                        result.error = newCostItem.errors.allErrors.collect {
                            log.error("Field: ${it.properties.field}, user input: ${it.properties.rejectedValue}, Reason! ${it.properties.code}")
                            message(code: 'finance.addNew.error', args: [it.properties.field])
                        }
                    } else {
                        if (newCostItem.save(flush: true)) {
                            /* def newBcObjs = []

                         params.list('newBudgetCodes')?.each { newbc ->
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

    private getSurveyProperties(Org contextOrg) {
        def props = []

        //private Property
        SurveyProperty.findAllByOwnerAndOwnerIsNotNull(contextOrg).each { it ->
            props << it

        }

        //global Property
        SurveyProperty.findAllByOwnerIsNull().each { it ->
            props << it

        }

        props.sort { a, b -> a.getI10n('name').compareToIgnoreCase b.getI10n('name') }

        return props
    }
    private def addSubMembers(SurveyConfig surveyConfig) {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = (accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR') && surveyConfig.surveyInfo?.owner?.id == contextService.getOrg()?.id)

        if (!result.editable) {
            return
        }

        def orgs = com.k_int.kbplus.Subscription.get(surveyConfig.subscription?.id)?.getDerivedSubscribers()

        if (orgs) {

            orgs.each { org ->

                if (!(SurveyOrg.findAllBySurveyConfigAndOrg(surveyConfig, org))) {

                    boolean existsMultiYearTerm = false
                    def sub = surveyConfig?.subscription
                    if (sub) {
                        def subChild = sub?.getDerivedSubscriptionBySubscribers(org)
                        def property = PropertyDefinition.findByName("Mehrjahreslaufzeit ausgewählt")

                        if (subChild?.getCalculatedSuccessor()) {
                            existsMultiYearTerm = true
                        }

                        if (!existsMultiYearTerm && property?.type == 'class com.k_int.kbplus.RefdataValue') {
                            if (subChild?.customProperties?.find {
                                it?.type?.id == property?.id
                            }?.refValue == RefdataValue.getByValueAndCategory('Yes', property?.refdataCategory)) {
                                existsMultiYearTerm = true
                                return existsMultiYearTerm
                            }
                        }
                    }
                    if (!existsMultiYearTerm) {
                        def surveyOrg = new SurveyOrg(
                                surveyConfig: surveyConfig,
                                org: org
                        )

                        if (!surveyOrg.save(flush: true)) {
                            log.debug("Error by add Org to SurveyOrg ${surveyOrg.errors}");
                        }
                    }
                }
            }

        }
    }

    private def deleteSubMembers(SurveyConfig surveyConfig) {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)

        result.editable = (accessService.checkMinUserOrgRole(result.user, result.institution, 'INST_EDITOR') && surveyConfig.surveyInfo?.owner?.id == contextService.getOrg()?.id)

        if (!result.editable) {
            return
        }

        def orgs = com.k_int.kbplus.Subscription.get(surveyConfig.subscription?.id)?.getDerivedSubscribers()

        if (orgs) {

            orgs.each { org ->
                CostItem.findBySurveyOrg(SurveyOrg.findBySurveyConfigAndOrg(surveyConfig, org))?.delete(flush: true)
                SurveyOrg.findBySurveyConfigAndOrg(surveyConfig, org)?.delete(flush: true)
            }
        }
    }

    private static def getSubscriptionMembers(Subscription subscription) {
        def result = []

        Subscription.findAllByInstanceOf(subscription).each { s ->
            def ors = OrgRole.findAllWhere(sub: s)
            ors.each { or ->
                if (or.roleType?.value in ['Subscriber', 'Subscriber_Consortial']) {
                    result << or.org
                }
            }
        }
        result = result.sort { it.name }
    }

    private static def getfilteredSurveyOrgs(List orgIDs, String query, queryParams, params) {

        if (!(orgIDs?.size() > 0)) {
            return []
        }
        def tmpQuery = query
        tmpQuery = tmpQuery.replace("order by", "and o.id in (:orgIDs) order by")

        def tmpQueryParams = queryParams
        tmpQueryParams.put("orgIDs", orgIDs)

        return Org.executeQuery(tmpQuery, tmpQueryParams, params)
    }


    private def exportSurveyParticipantResult(List<SurveyResult> results, String format, Org org) {
        SimpleDateFormat sdf = new SimpleDateFormat(g.message(code: 'default.date.format.notime'))
        List titles = [g.message(code: 'surveyInfo.owner.label'),

                       g.message(code: 'surveyConfigsInfo.comment'),
                       g.message(code: 'surveyParticipants.label'),
                       g.message(code: 'org.shortname.label'),
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
        results.findAll { it.surveyConfig.type == 'Subscription' }.each { result ->
            List row = []
            switch (format) {
                case "xls":
                case "xlsx":

                    def sub = result.surveyConfig.subscription.getDerivedSubscriptionBySubscribers(result?.participant)

                    def surveyCostItem = CostItem.findBySurveyOrg(SurveyOrg.findBySurveyConfigAndOrg(result?.surveyConfig, result?.participant))

                    row.add([field: result?.owner?.name ?: '', style: null])
                    row.add([field: result?.surveyConfig?.comment ?: '', style: null])
                    row.add([field: result?.participant?.name ?: '', style: null])
                    row.add([field: result?.participant?.shortname ?: '', style: null])
                    row.add([field: sub?.name ?: "", style: null])


                    row.add([field: sub?.providers ? sub?.providers?.join(", ") : '', style: null])
                    row.add([field: sub?.agencies ? sub?.agencies?.join(", ") : '', style: null])

                    row.add([field: sub?.owner?.reference ?: '', style: null])
                    List packageNames = sub?.packages?.collect {
                        it.pkg.name
                    }
                    row.add([field: packageNames ? packageNames.join(", ") : '', style: null])
                    row.add([field: sub?.status?.getI10n("value") ?: '', style: null])
                    row.add([field: sub?.type?.getI10n("value") ?: '', style: null])
                    row.add([field: sub?.form?.getI10n("value") ?: '', style: null])
                    row.add([field: sub?.resource?.getI10n("value") ?: '', style: null])

                    row.add([field: surveyCostItem?.costInBillingCurrencyAfterTax ? g.formatNumber(number: surveyCostItem?.costInBillingCurrencyAfterTax, minFractionDigits: "2", maxFractionDigits: "2", type: "number") : '', style: null])

                    row.add([field: surveyCostItem?.costDescription ?: '', style: null])

                    row.add([field: result.type?.getI10n('name') ?: '', style: null])
                    row.add([field: result.type?.getLocalizedType() ?: '', style: null])

                    def value = ""

                    if (result?.type?.type == Integer.toString()) {
                        value = result?.intValue ? result?.intValue.toString() : ""
                    } else if (result?.type?.type == String.toString()) {
                        value = result?.stringValue ?: ""
                    } else if (result?.type?.type == BigDecimal.toString()) {
                        value = result?.decValue ? result?.decValue.toString() : ""
                    } else if (result?.type?.type == Date.toString()) {
                        value = result?.dateValue ? sdf.format(result?.dateValue) : ""
                    } else if (result?.type?.type == URL.toString()) {
                        value = result?.urlValue ? result?.urlValue.toString() : ""
                    } else if (result?.type?.type == RefdataValue.toString()) {
                        value = result?.refValue ? result?.refValue.getI10n('value') : ""
                    }

                    def surveyOrg = SurveyOrg.findBySurveyConfigAndOrg(result?.surveyConfig, result?.participant)

                    if (surveyOrg?.existsMultiYearTerm()) {
                        value = g.message(code: "surveyOrg.perennialTerm.available")
                    }

                    row.add([field: value ?: '', style: null])
                    row.add([field: result.comment ?: '', style: null])
                    row.add([field: result.finishDate ? sdf.format(result?.finishDate) : '', style: null])

                    surveyData.add(row)
                    break
            }
        }
        switch (format) {
            case 'xls':
            case 'xlsx':
                Map sheetData = [:]
                sheetData[message(code: 'menu.my.surveys')] = [titleRow: titles, columnData: surveyData]
                return exportService.generateXLSXWorkbook(sheetData)
        }
    }

    private def exportSurveyParticipantResultMin(List<SurveyResult> results, String format, Org org) {
        SimpleDateFormat sdf = new SimpleDateFormat(g.message(code: 'default.date.format.notime'))
        List titles = [
                g.message(code: 'org.shortname.label'),
                g.message(code: 'surveyParticipants.label'),

                g.message(code: 'surveyProperty.subName'),

                g.message(code: 'surveyConfigsInfo.newPrice'),
                g.message(code: 'surveyConfigsInfo.newPrice.comment'),
                g.message(code: 'surveyResult.finishDate')
        ]

        results.groupBy {it?.type.id
        }.sort{it?.value[0]?.type?.name}.each { property ->
            titles << SurveyProperty.get(property.key)?.getI10n('name')
            titles << g.message(code: 'surveyResult.participantComment')
        }

        List surveyData = []
        results.findAll { it.surveyConfig.type == 'Subscription' }.groupBy { it?.participant.id }.each { result ->
            List row = []
            switch (format) {
                case "xls":
                case "xlsx":

                    def participant = Org.get(result?.key)

                    row.add([field: participant?.shortname ?: '', style: null])
                    row.add([field: participant?.name ?: '', style: null])

                    row.add([field: result?.value[0]?.surveyConfig?.getConfigNameShort() ?: "", style: null])
                    def surveyCostItem = CostItem.findBySurveyOrg(SurveyOrg.findBySurveyConfigAndOrg(result?.value[0]?.surveyConfig, participant))

                    row.add([field: surveyCostItem?.costInBillingCurrencyAfterTax ? g.formatNumber(number: surveyCostItem?.costInBillingCurrencyAfterTax, minFractionDigits: "2", maxFractionDigits: "2", type: "number") : '', style: null])

                    row.add([field: surveyCostItem?.costDescription ?: '', style: null])

                    row.add([field: result?.value[0]?.finishDate ? sdf.format(result?.value[0]?.finishDate) : '', style: null])

                    result.value.sort{it?.type?.name}.each { resultProperty ->

                        def surveyOrg = SurveyOrg.findBySurveyConfigAndOrg(resultProperty?.surveyConfig, participant)

                        def value = ""

                        if (resultProperty?.type?.type == Integer.toString()) {
                            value = resultProperty?.intValue ? resultProperty?.intValue.toString() : ""
                        } else if (resultProperty?.type?.type == String.toString()) {
                            value = resultProperty?.stringValue ?: ""
                        } else if (resultProperty?.type?.type == BigDecimal.toString()) {
                            value = resultProperty?.decValue ? resultProperty?.decValue.toString() : ""
                        } else if (resultProperty?.type?.type == Date.toString()) {
                            value = resultProperty?.dateValue ? sdf.format(resultProperty?.dateValue) : ""
                        } else if (resultProperty?.type?.type == URL.toString()) {
                            value = resultProperty?.urlValue ? resultProperty?.urlValue.toString() : ""
                        } else if (resultProperty?.type?.type == RefdataValue.toString()) {
                            value = resultProperty?.refValue ? resultProperty?.refValue.getI10n('value') : ""
                        }

                        if (surveyOrg?.existsMultiYearTerm()) {
                            value = g.message(code: "surveyOrg.perennialTerm.available")
                        }

                        row.add([field: value ?: '', style: null])
                        row.add([field: resultProperty.comment ?: '', style: null])
                    }

                    surveyData.add(row)
                    break
            }
        }
        switch (format) {
            case 'xls':
            case 'xlsx':
                Map sheetData = [:]
                sheetData[message(code: 'menu.my.surveys')] = [titleRow: titles, columnData: surveyData]
                return exportService.generateXLSXWorkbook(sheetData)
        }
    }

    private def exportSurveyCostItems(SurveyConfig surveyConfig, String format, Org org) {
        SimpleDateFormat sdf = new SimpleDateFormat(g.message(code: 'default.date.format.notime'))
        List titles = ['Name',
                       '',
                       g.message(code: 'surveyConfig.type.label'),
                       g.message(code: 'surveyConfigsInfo.comment'),

                       g.message(code: 'surveyParticipants.label'),
                       g.message(code: 'org.shortname.label'),
                       g.message(code: 'org.libraryNetwork.label'),
                       g.message(code: 'surveyProperty.subName'),
                       g.message(code: 'surveyConfigsInfo.newPrice'),
                       g.message(code: 'surveyConfigsInfo.newPrice.comment')
        ]

        List surveyData = []

        def surveyOrgs = SurveyOrg.findAllBySurveyConfig(surveyConfig)

        surveyOrgs.each { surveyOrg ->
            List row = []

            row.add([field: surveyConfig?.surveyInfo?.name ?: '', style: null])

            row.add([field: surveyConfig?.getConfigName() ?: '', style: null])

            row.add([field: surveyConfig?.type == 'Subscription' ? com.k_int.kbplus.SurveyConfig.getLocalizedValue(surveyConfig?.type) : com.k_int.kbplus.SurveyConfig.getLocalizedValue(config?.type) + '(' + surveyConfig?.surveyProperty?.getLocalizedType() + ')', style: null])

            row.add([field: surveyConfig?.comment ?: '', style: null])

            row.add([field: surveyOrg?.org?.name ?: '', style: null])

            row.add([field: surveyOrg?.org?.shortname ?: '', style: null])

            row.add([field: surveyOrg?.org?.libraryType?.getI10n('value') ?: '', style: null])

            row.add([field: surveyConfig?.subscription?.getDerivedSubscriptionBySubscribers(surveyOrg?.org)?.name ?: '', style: null])


            def costItem = CostItem.findBySurveyOrg(surveyOrg)

            if (!surveyOrg?.existsMultiYearTerm()) {
                if (costItem) {
                    row.add([field: g.formatNumber(number: costItem?.costInBillingCurrencyAfterTax, minFractionDigits: 2, maxFractionDigits: 2, type: "number") + costItem?.billingCurrency?.getI10n('value').split('-').first(), style: null])
                }
            } else {
                row.add([field: g.message(code: "surveyOrg.perennialTerm.available"), style: null])
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
        def result = [:]

        def contextOrg = contextService.getOrg()

        result.created = SurveyConfig.executeQuery("from SurveyInfo surInfo left join surInfo.surveyConfigs surConfig where surInfo.owner = :contextOrg and (surInfo.status = :status or surInfo.status = :status2)",
                [contextOrg: contextOrg, status: RDStore.SURVEY_READY, status2: RDStore.SURVEY_IN_PROCESSING]).size()

        result.active = SurveyConfig.executeQuery("from SurveyInfo surInfo left join surInfo.surveyConfigs surConfig where surInfo.owner = :contextOrg and surInfo.status = :status",
                [contextOrg: contextOrg, status: RDStore.SURVEY_SURVEY_STARTED]).size()

        result.finish = SurveyConfig.executeQuery("from SurveyInfo surInfo left join surInfo.surveyConfigs surConfig where surInfo.owner = :contextOrg and surInfo.status = :status",
                [contextOrg: contextOrg, status: RDStore.SURVEY_SURVEY_COMPLETED]).size()

        result.inEvaluation = SurveyConfig.executeQuery("from SurveyInfo surInfo left join surInfo.surveyConfigs surConfig where surInfo.owner = :contextOrg and surInfo.status = :status",
                [contextOrg: contextOrg, status: RDStore.SURVEY_IN_EVALUATION]).size()

        result.completed = SurveyConfig.executeQuery("from SurveyInfo surInfo left join surInfo.surveyConfigs surConfig where surInfo.owner = :contextOrg and surInfo.status = :status",
                [contextOrg: contextOrg, status: RDStore.SURVEY_COMPLETED]).size()

        return result
    }

    private LinkedHashMap setResultGenericsAndCheckAccess() {
        def result = [:]
        result.institution = contextService.getOrg()
        result.user = User.get(springSecurityService.principal.id)
        result.surveyInfo = SurveyInfo.get(params.id)
        result.surveyConfig = SurveyConfig.get(params.surveyConfigID) ?: (result.surveyInfo?.isSubscriptionSurvey ? result.surveyInfo?.surveyConfigs[0] : result.surveyInfo?.surveyConfigs[0])

        result.editable = result.surveyInfo?.isEditable() ?: false


        result
    }

}
