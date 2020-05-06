package de.laser

import com.k_int.kbplus.*
import de.laser.helper.DateUtil
import de.laser.helper.RDStore
import de.laser.interfaces.CalculatedType
import grails.transaction.Transactional
import org.springframework.context.i18n.LocaleContextHolder

import java.text.SimpleDateFormat

@Transactional
class ControlledListService {

    def contextService
    def genericOIDService
    def messageSource
    def accessService

    /**
     * Retrieves a list of providers and agencies
     * @param params - eventual request params
     * @return a map containing a sorted list of providers, an empty one if no providers match the filter
     */
    Map getProvidersAgencies(Map params) {
        LinkedHashMap result = [results:[]]
        Org org = contextService.getOrg()
        if(params.forFinanceView) {
            //PLEASE! Do not assign providers or agencies to administrative subscriptions! That will screw up this query ...
            List subscriptions = Subscription.executeQuery('select s from CostItem ci join ci.sub s join s.orgRelations orgRoles where orgRoles.org = :org and orgRoles.roleType in (:orgRoles)',[org:org,orgRoles:[RDStore.OR_SUBSCRIBER_CONS,RDStore.OR_SUBSCRIBER,RDStore.OR_SUBSCRIPTION_CONSORTIA,RDStore.OR_SUBSCRIPTION_COLLECTIVE,RDStore.OR_SUBSCRIBER_COLLECTIVE]])
            if(subscriptions) {
                Map filter = [providerAgency: [RDStore.OR_PROVIDER,RDStore.OR_AGENCY],subscriptions:subscriptions]
                String filterString = " "
                if(params.query && params.query.length() > 0) {
                    filter.put("query",params.query)
                    filterString += " and genfunc_filter_matcher(oo.org.name,:query) = true "
                }
                List providers = Org.executeQuery('select distinct oo.org, oo.org.name from OrgRole oo where oo.sub in (:subscriptions) and oo.roleType in (:providerAgency)'+filterString+'order by oo.org.name asc',filter)
                providers.each { p ->
                    result.results.add([name:p[1],value:p[0].class.name + ":" + p[0].id])
                }
            }
        }
        else {
            String queryString = 'select o from Org o where o.type in (:provider) '
            LinkedHashMap filter = [provider:[RDStore.OT_PROVIDER,RDStore.OT_AGENCY]]
            if(params.query && params.query.length() > 0) {
                filter.put("query",params.query)
                queryString += " and genfunc_filter_matcher(o.name,:query) = true "
            }
            List providers = Org.executeQuery(queryString+" order by o.sortname asc",filter)
            providers.each { p ->
                result.results.add([name:p.name,value:p.class.name + ":" + p.id])
            }
        }
        result
    }

    /**
     * Retrieves a list of subscriptions owned by the context organisation matching given parameters
     * @param params - eventual request params
     * @return a map containing a sorted list of subscriptions, an empty one if no subscriptions match the filter
     */
    Map getSubscriptions(Map params) {
        Org org = contextService.getOrg()
        LinkedHashMap result = [results:[]]
        String queryString = 'select distinct s, orgRoles.org.sortname from Subscription s join s.orgRelations orgRoles where orgRoles.org = :org and orgRoles.roleType in ( :orgRoles )'
        LinkedHashMap filter = [org:org,orgRoles:[RDStore.OR_SUBSCRIBER]]
        //may be generalised later - here it is where to expand the query filter
        if(params.query && params.query.length() > 0) {
            filter.put("query",params.query)
            queryString += " and (genfunc_filter_matcher(s.name,:query) = true or genfunc_filter_matcher(orgRoles.org.sortname,:query) = true) "
        }
        if(params.ctx) {
            Subscription ctx = genericOIDService.resolveOID(params.ctx)
            filter.ctx = ctx
            queryString += " and s != :ctx "
        }
        if(params.status) {
            if (params.status instanceof List){
                if (params.status.size() > 0) {
                    queryString += " and s.status in (:status) "
                    if (params.status instanceof List<RefdataValue>){
                        filter.status = params.status
                    } else {
                        List statusList = []
                        params.status.each{
                            statusList += RefdataValue.get(it)
                        }
                        filter.status = statusList
                    }
                }
            } else {
                if(params.status != 'FETCH_ALL') { //FETCH_ALL may be sent from finances/_filter.gsp
                    if(params.status instanceof RefdataValue)
                        filter.status = params.status
                    else filter.status = RefdataValue.get(params.status)
                    queryString += " and s.status = :status "
                }
            }
        }
        else {
            filter.status = RDStore.SUBSCRIPTION_CURRENT
            queryString += " and s.status = :status "
        }
        if(accessService.checkPerm("ORG_CONSORTIUM"))
            filter.orgRoles.addAll([RDStore.OR_SUBSCRIBER_CONS,RDStore.OR_SUBSCRIPTION_CONSORTIA])
        else if(accessService.checkPerm("ORG_INST_COLLECTIVE"))
            filter.orgRoles.addAll([RDStore.OR_SUBSCRIBER_COLLECTIVE,RDStore.OR_SUBSCRIPTION_COLLECTIVE])
        List subscriptions = Subscription.executeQuery(queryString+" order by s.name asc, s.startDate asc, s.endDate asc, orgRoles.org.sortname asc",filter)

        subscriptions.each { row ->
            Subscription s = (Subscription) row[0]

            switch (params.ltype) {
                case CalculatedType.TYPE_PARTICIPATION:
                    if (s.getCalculatedType() in [CalculatedType.TYPE_PARTICIPATION, CalculatedType.TYPE_PARTICIPATION_AS_COLLECTIVE]){
                        if(org in s.orgRelations.collect { or -> or.org })
                            result.results.add([name:s.dropdownNamingConvention(org), value:s.class.name + ":" + s.id])
                    }
                    break
                case CalculatedType.TYPE_CONSORTIAL:
                    if (s.getCalculatedType() == CalculatedType.TYPE_CONSORTIAL)
                        result.results.add([name:s.dropdownNamingConvention(org), value:s.class.name + ":" + s.id])
                    break
                case CalculatedType.TYPE_COLLECTIVE:
                    if (s.getCalculatedType() == CalculatedType.TYPE_COLLECTIVE)
                        result.results.add([name:s.dropdownNamingConvention(org), value:s.class.name + ":" + s.id])
                    break
                default:
                    result.results.add([name:s.dropdownNamingConvention(org), value:s.class.name + ":" + s.id])
                    break
            }
        }
		//log.debug ("getSubscriptions(): ${result.results.size()} Matches")
        result
    }

    /**
     * Retrieves a list of issue entitlements owned by the context organisation matching given parameters
     * @param params - eventual request params
     * @return a map containing a list of issue entitlements, an empty one if no issue entitlements match the filter
     */
    Map getIssueEntitlements(Map params) {
        Org org = contextService.getOrg()
        LinkedHashMap issueEntitlements = [results:[]]
        //build up set of subscriptions which are owned by the current organisation or instances of such - or filter for a given subscription
        String filter = 'in (select distinct o.sub from OrgRole as o where o.org = :org and o.roleType in ( :orgRoles ) and o.sub.status = :current ) '
        LinkedHashMap filterParams = [org:org, orgRoles: [RDStore.OR_SUBSCRIPTION_CONSORTIA,RDStore.OR_SUBSCRIPTION_COLLECTIVE,RDStore.OR_SUBSCRIBER,RDStore.OR_SUBSCRIBER_CONS,RDStore.OR_SUBSCRIBER_COLLECTIVE], current:RDStore.SUBSCRIPTION_CURRENT]
        if(params.sub) {
            filter = '= :sub'
            filterParams = ['sub':genericOIDService.resolveOID(params.sub)]
        }
        if(params.pkg) {
            try {
                def pkgObj = genericOIDService.resolveOID(params.pkg)
                if(pkgObj && pkgObj instanceof SubscriptionPackage) {
                    SubscriptionPackage pkg = (SubscriptionPackage) pkgObj
                    filter += ' and ie.tipp.pkg.gokbId = :pkg'
                    filterParams.pkg = pkg.pkg.gokbId
                }
            }
            catch (Exception e) {
                return [results:[]]
            }
        }
        if(params.query && params.query.length() > 0) {
            filter += ' and genfunc_filter_matcher(ie.tipp.title.title,:query) = true '
            filterParams.put('query',params.query)
        }
        List result = IssueEntitlement.executeQuery('select ie from IssueEntitlement as ie where ie.subscription '+filter+' order by ie.tipp.title.title asc, ie.subscription asc, ie.subscription.startDate asc, ie.subscription.endDate asc',filterParams)
        if(result.size() > 0) {
            result.each { res ->
                Subscription s = (Subscription) res.subscription

                issueEntitlements.results.add([name:"${res.tipp.title.title} (${res.tipp.title.printTitleType()}) (${s.dropdownNamingConvention(org)})",value:res.class.name+":"+res.id])
            }
        }
        issueEntitlements
    }

    /**
     * Retrieves a list of licenses owned by the context organisation matching given parameters
     * @param params - eventual request params (currently not in use, handed for an eventual extension)
     * @return a map containing licenses, an empty one if no licenses match the filter
     */
    Map getLicenses(Map params) {
        Org org = contextService.getOrg()
        LinkedHashMap licenses = [results:[]]
        List<License> result = []
        String licFilter = ''
        LinkedHashMap filterParams = [org:org,orgRoles:[RDStore.OR_LICENSING_CONSORTIUM,RDStore.OR_LICENSEE,RDStore.OR_LICENSEE_CONS]]
        if(params.query && params.query.length() > 0) {
            licFilter = ' and genfunc_filter_matcher(l.reference,:query) = true '
            filterParams.put('query',params.query)
        }
        result = License.executeQuery('select l from License as l join l.orgLinks ol where ol.org = :org and ol.roleType in (:orgRoles)'+licFilter+" order by l.reference asc",filterParams)
        if(result.size() > 0) {
            SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
            log.debug("licenses found")
            result.each { res ->
                licenses.results += ([name:"${res.reference} (${res.startDate ? sdf.format(res.startDate) : '???'} - ${res.endDate ? sdf.format(res.endDate) : ''})",value:res.class.name+":"+res.id])
            }
        }
        licenses
    }

    /**
     * Retrieves a list of issue entitlements owned by the context organisation matching given parameters
     * @param params - eventual request params
     * @return a map containing a sorted list of issue entitlements, an empty one if no issue entitlements match the filter
     */
    Map getSubscriptionPackages(Map params) {
        Org org = contextService.getOrg()
        LinkedHashMap result = [results:[]]
        String queryString = 'select distinct s, orgRoles.org.sortname from Subscription s join s.orgRelations orgRoles where orgRoles.org = :org and orgRoles.roleType in ( :orgRoles )'
        LinkedHashMap filter = [org:org,orgRoles:[RDStore.OR_SUBSCRIPTION_CONSORTIA,RDStore.OR_SUBSCRIPTION_COLLECTIVE,RDStore.OR_SUBSCRIBER_CONS,RDStore.OR_SUBSCRIBER_COLLECTIVE,RDStore.OR_SUBSCRIBER]]
        //may be generalised later - here it is where to expand the query filter
        if(params.query && params.query.length() > 0) {
            filter.put("query",params.query)
            queryString += " and (genfunc_filter_matcher(s.name,:query) = true or genfunc_filter_matcher(orgRoles.org.sortname,:query) = true) "
        }
        if(params.ctx) {
            Subscription ctx = genericOIDService.resolveOID(params.ctx)
            filter.ctx = ctx
            if(org.hasPerm("ORG_CONSORTIUM"))
                queryString += " and (s = :ctx or s.instanceOf = :ctx)"
            else
                queryString += " and s = :ctx"
        }
        else if(params.sub) {
            filter.sub = genericOIDService.resolveOID(params.sub)
            queryString += " and s = :sub"
        }
        if(params.status) {
            if(params.status != 'FETCH_ALL') { //FETCH_ALL may be sent from finances/_filter.gsp
                if(params.status instanceof RefdataValue)
                    filter.status = params.status
                else filter.status = RefdataValue.get(params.status)
                queryString += " and s.status = :status "
            }
        }
        else {
            filter.status = RDStore.SUBSCRIPTION_CURRENT
            queryString += " and s.status = :status "
        }
        List subscriptions = Subscription.executeQuery(queryString+" order by s.name asc, orgRoles.org.sortname asc, s.startDate asc, s.endDate asc",filter)
        subscriptions.each { row ->
            Subscription s = (Subscription) row[0]
            s.packages.each { sp ->
                result.results.add([name:"${sp.pkg.name}/${s.dropdownNamingConvention(org)}",value:sp.class.name + ":" + sp.id])
            }
        }
        result
    }

    Map getBudgetCodes(Map params) {
        Map result = [results:[]]
        Org org = contextService.getOrg()
        String queryString = 'select bc from BudgetCode bc where bc.owner = :owner'
        LinkedHashMap filter = [owner:org]
        if(params.query && params.query.length() > 0) {
            filter.put("query",params.query)
            queryString += " and genfunc_filter_matcher(bc.value,:query) = true"
        }
        queryString += " order by bc.value asc"
        List budgetCodes = BudgetCode.executeQuery(queryString,filter)
        budgetCodes.each { bc ->
            result.results.add([name:bc.value,value:bc.id])
        }
        result
    }

    Map getInvoiceNumbers(Map params) {
        Map result = [results:[]]
        Org org = contextService.getOrg()
        String queryString = 'select i from Invoice i where i.owner = :owner'
        LinkedHashMap filter = [owner:org]
        if(params.query && params.query.length() > 0) {
            filter.put("query",params.query)
            queryString += " and genfunc_filter_matcher(i.invoiceNumber,:query) = true"
        }
        queryString += " order by i.invoiceNumber asc"
        List invoiceNumbers = Invoice.executeQuery(queryString,filter)
        invoiceNumbers.each { inv ->
            result.results.add([name:inv.invoiceNumber,value:inv.invoiceNumber])
        }
        result
    }

    Map getOrderNumbers(Map params) {
        Map result = [results:[]]
        Org org = contextService.getOrg()
        String queryString = 'select ord from Order ord where ord.owner = :owner'
        LinkedHashMap filter = [owner:org]
        if(params.query && params.query.length() > 0) {
            filter.put("query",params.query)
            //queryString += " and ord.orderNumber like :query"
            queryString += " and genfunc_filter_matcher(ord.orderNumber,:query) = true"
        }
        queryString += " order by ord.orderNumber asc"
        List orderNumbers = Order.executeQuery(queryString,filter)
        orderNumbers.each { ord ->
            result.results.add([name:ord.orderNumber,value:ord.orderNumber])
        }
        result
    }

    Map getReferences(Map params) {
        Map result = [results:[]]
        Org org = contextService.getOrg()
        String queryString = 'select distinct(ci.reference) from CostItem ci where ci.owner = :owner and ci.reference != null'
        LinkedHashMap filter = [owner:org]
        if(params.query && params.query.length() > 0) {
            filter.put("query",params.query)
            queryString += " and genfunc_filter_matcher(ci.reference,:query) = true"
        }
        queryString += " order by ci.reference asc"
        List references = CostItem.executeQuery(queryString,filter)
        references.each { r ->
            result.results.add([name:r,value:r])
        }
        result
    }

    Map getElements(Map params) {
        Map result = [results:[]]
        Org org = contextService.getOrg()
        SimpleDateFormat sdf = DateUtil.getSDF_NoTime()
        if(params.org == "true") {
            List allOrgs = DocContext.executeQuery('select distinct dc.org,dc.org.sortname from DocContext dc where dc.owner.owner = :ctxOrg and dc.org != null and (genfunc_filter_matcher(dc.org.name,:query) = true or genfunc_filter_matcher(dc.org.sortname,:query) = true) order by dc.org.sortname asc',[ctxOrg:org,query:params.query])
            allOrgs.each { it ->
                result.results.add([name:"(${messageSource.getMessage('spotlight.organisation',null,LocaleContextHolder.locale)}) ${it[0].name}",value:"${it[0].class.name}:${it[0].id}"])
            }
        }
        if(params.license == "true") {
            List allLicenses = DocContext.executeQuery('select distinct dc.license,dc.license.reference from DocContext dc where dc.owner.owner = :ctxOrg and dc.license != null and genfunc_filter_matcher(dc.license.reference,:query) = true order by dc.license.reference asc',[ctxOrg:org,query:params.query])
            allLicenses.each { it ->
                License license = (License) it[0]
                String licenseStartDate = license.startDate ? sdf.format(license.startDate) : '???'
                String licenseEndDate = license.endDate ? sdf.format(license.endDate) : ''
                result.results.add([name:"(${messageSource.getMessage('spotlight.license',null,LocaleContextHolder.locale)}) ${it[1]} - ${license.status.getI10n("value")} (${licenseStartDate} - ${licenseEndDate})",value:"${license.class.name}:${license.id}"])
            }
        }
        if(params.subscription == "true") {
            List allSubscriptions = DocContext.executeQuery('select distinct dc.subscription,dc.subscription.name from DocContext dc where dc.owner.owner = :ctxOrg and dc.subscription != null and genfunc_filter_matcher(dc.subscription.name,:query) = true order by dc.subscription.name asc',[ctxOrg:org,query:params.query])
            allSubscriptions.each { it ->
                Subscription subscription = (Subscription) it[0]
                /*
                String tenant
                if(subscription.getCalculatedType() == CalculatedType.TYPE_PARTICIPATION && subscription.getConsortia().id == org.id) {
                    try {
                        tenant = " - ${subscription.getAllSubscribers().get(0).sortname}"
                    }
                    catch (IndexOutOfBoundsException e) {
                        log.debug("Please check subscription #${subscription.id}")
                    }
                }
                else {
                    tenant = ''
                }
                String dateString = "("
                if (subscription.startDate)
                    dateString += sdf.format(subscription.startDate) + " - "
                else dateString += "???"
                if (subscription.endDate)
                    dateString += sdf.format(subscription.endDate)
                else dateString += ""
                dateString += ")"
                */
                result.results.add([name:"(${messageSource.getMessage('spotlight.subscription',null,LocaleContextHolder.locale)}) ${subscription.dropdownNamingConvention()}",value:"${it[0].class.name}:${it[0].id}"])
            }
        }
        if(params.package == "true") {
            List allPackages = DocContext.executeQuery('select distinct dc.pkg,dc.pkg.name from DocContext dc where dc.owner.owner = :ctxOrg and dc.pkg != null and genfunc_filter_matcher(dc.pkg.name,:query) = true order by dc.pkg.name asc', [ctxOrg: org, query: params.query])
            allPackages.each { it ->
                result.results.add([name: "(${messageSource.getMessage('spotlight.package', null, LocaleContextHolder.locale)}) ${it[1]}", value: "${it[0].class.name}:${it[0].id}"])
            }
        }
        result
    }
}
