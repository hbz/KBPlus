package de.laser.api.v0

import com.k_int.kbplus.*
import com.k_int.kbplus.auth.Role
import com.k_int.kbplus.auth.User
import com.k_int.kbplus.auth.UserRole
import de.laser.CacheService
import de.laser.api.v0.entities.ApiDoc
import de.laser.api.v0.entities.ApiIssueEntitlement
import de.laser.api.v0.entities.ApiLicense
import de.laser.api.v0.entities.ApiOrg
import de.laser.api.v0.entities.ApiPkg
import de.laser.helper.Constants
import groovy.util.logging.Log4j
//import org.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil

@Log4j
class ApiReader {

    static SUPPORTED_FORMATS = [
            'document':             [],
            'issueEntitlements':    [Constants.MIME_TEXT_PLAIN, Constants.MIME_APPLICATION_JSON],
            'license':              [Constants.MIME_APPLICATION_JSON],
            'onixpl':               [Constants.MIME_APPLICATION_XML],
            'organisation':         [Constants.MIME_APPLICATION_JSON],
            'package':              [Constants.MIME_APPLICATION_JSON],
            'refdatas':             [Constants.MIME_APPLICATION_JSON],
            'subscription':         [Constants.MIME_APPLICATION_JSON]
    ]

    static SUPPORTED_SIMPLE_QUERIES = [
            "refdataCategories",
            "refdataValues"
    ]

    /**
     * @param com.k_int.kbplus.SubscriptionPackage subPkg
     * @param ignoreRelation
     * @param com.k_int.kbplus.Org context
     * @return
     */
    static exportIssueEntitlements(SubscriptionPackage subPkg, def ignoreRelation, Org context){
        def result = []

        def tipps = TitleInstancePackagePlatform.findAllByPkg(subPkg.pkg)
        tipps.each{ tipp ->
            def ie = IssueEntitlement.findBySubscriptionAndTipp(subPkg.subscription, tipp)
            if (ie) {
                result << ApiReaderHelper.resolveIssueEntitlement(ie, ignoreRelation, context) // com.k_int.kbplus.IssueEntitlement
            }
        }
        return ApiReaderHelper.cleanUp(result, true, true)
    }

    /**
     * @param com.k_int.kbplus.License lic
     * @param ignoreRelation
     * @param com.k_int.kbplus.Org context
     * @return
     */
    static exportLicense(License lic, def ignoreRelation, Org context){
        def result = [:]

        lic = GrailsHibernateUtil.unwrapIfProxy(lic)

        result.globalUID        = lic.globalUID
        // removed - result.contact          = lic.contact
        result.dateCreated      = lic.dateCreated
        result.endDate          = lic.endDate
        result.impId            = lic.impId
        result.lastmod          = lic.lastmod
        result.lastUpdated      = lic.lastUpdated
        result.licenseUrl       = lic.licenseUrl
        // removed - result.licensorRef      = lic.licensorRef
        // removed - result.licenseeRef      = lic.licenseeRef
        result.licenseType      = lic.licenseType
        result.licenseStatus    = lic.licenseStatus
        result.noticePeriod     = lic.noticePeriod
        result.reference        = lic.reference
        result.startDate        = lic.startDate
        result.sortableReference= lic.sortableReference

        // RefdataValues

        result.isPublic         = lic.isPublic?.value
        result.licenseCategory  = lic.licenseCategory?.value
        result.status           = lic.status?.value
        result.type             = lic.type?.value

        // References

        result.identifiers      = ApiReaderHelper.resolveIdentifiers(lic.ids) // com.k_int.kbplus.IdentifierOccurrence
        result.instanceOf       = ApiReaderHelper.resolveLicenseStub(lic.instanceOf, context) // com.k_int.kbplus.License
        result.properties       = ApiReaderHelper.resolveProperties(lic, context)  // com.k_int.kbplus.(LicenseCustomProperty, LicensePrivateProperty)
        result.documents        = ApiReaderHelper.resolveDocuments(lic.documents) // com.k_int.kbplus.DocContext
        result.onixplLicense    = ApiReaderHelper.resolveOnixplLicense(lic.onixplLicense, lic, context) // com.k_int.kbplus.OnixplLicense

        if (ignoreRelation != ApiReaderHelper.IGNORE_ALL) {
            if (ignoreRelation != ApiReaderHelper.IGNORE_SUBSCRIPTION) {
                result.subscriptions = ApiReaderHelper.resolveStubs(lic.subscriptions, ApiReaderHelper.SUBSCRIPTION_STUB, context) // com.k_int.kbplus.Subscription
            }
            if (ignoreRelation != ApiReaderHelper.IGNORE_LICENSE) {
                result.organisations = ApiReaderHelper.resolveOrgLinks(lic.orgLinks, ApiReaderHelper.IGNORE_LICENSE, context) // com.k_int.kbplus.OrgRole
            }
        }

        // Ignored

        //result.packages         = exportHelperService.resolveStubs(lic.pkgs, exportHelperService.PACKAGE_STUB) // com.k_int.kbplus.Package
        /*result.persons          = exportHelperService.resolvePrsLinks(
                lic.prsLinks, exportHelperService.NO_CONSTRAINT, exportHelperService.NO_CONSTRAINT, context
        ) // com.k_int.kbplus.PersonRole
        */
        return ApiReaderHelper.cleanUp(result, true, true)
    }

    /**
     * @param com.k_int.kbplus.Org org
     * @param com.k_int.kbplus.Org context
     * @return
     */
    static exportOrganisation(Org org, Org context) {
        def result = [:]

        org = GrailsHibernateUtil.unwrapIfProxy(org)

        result.globalUID    = org.globalUID
        result.comment      = org.comment
        result.name         = org.name
        result.scope        = org.scope
        result.fteStudents  = org.fteStudents
        result.fteStaff     = org.fteStaff

        // RefdataValues

        result.sector       = org.sector?.value
        result.type         = org.orgType?.value
        result.status       = org.status?.value

        // References

        result.addresses    = ApiReaderHelper.resolveAddresses(org.addresses, ApiReaderHelper.NO_CONSTRAINT) // com.k_int.kbplus.Address
        result.contacts     = ApiReaderHelper.resolveContacts(org.contacts, ApiReaderHelper.NO_CONSTRAINT) // com.k_int.kbplus.Contact
        result.identifiers  = ApiReaderHelper.resolveIdentifiers(org.ids) // com.k_int.kbplus.IdentifierOccurrence
        result.persons      = ApiReaderHelper.resolvePrsLinks(
                org.prsLinks, ApiReaderHelper.NO_CONSTRAINT, ApiReaderHelper.NO_CONSTRAINT, context
        ) // com.k_int.kbplus.PersonRole

        result.properties   = ApiReaderHelper.resolveProperties(org, context) // com.k_int.kbplus.(OrgCustomProperty, OrgPrivateProperty)

        // Ignored

        //result.affiliations         = org.affiliations // com.k_int.kblpus.UserOrg
        //result.incomingCombos       = org.incomingCombos // com.k_int.kbplus.Combo
        //result.links                = exportHelperService.resolveOrgLinks(org.links) // com.k_int.kbplus.OrgRole
        //result.membership           = org.membership?.value // RefdataValue
        //result.outgoingCombos       = org.outgoingCombos // com.k_int.kbplus.Combo

        return ApiReaderHelper.cleanUp(result, true, true)
    }

    /**
     * @param com.k_int.kbplus.Package pkg
     * @param com.k_int.kbplus.Org context
     * @return
     */
    static exportPackage(com.k_int.kbplus.Package pkg, Org context) {
        def result = [:]

        pkg = GrailsHibernateUtil.unwrapIfProxy(pkg)

        result.globalUID        = pkg.globalUID
        result.autoAccept       = pkg.autoAccept
        result.cancellationAllowances = pkg.cancellationAllowances
        result.dateCreated      = pkg.dateCreated
        result.endDate          = pkg.endDate
        result.forumId          = pkg.forumId
        result.identifier       = pkg.identifier
        result.impId            = pkg.impId
        result.lastUpdated      = pkg.lastUpdated
        result.name             = pkg.name
        result.identifier       = pkg.identifier
        result.vendorURL        = pkg.vendorURL
        result.sortName         = pkg.sortName
        result.startDate        = pkg.startDate

        // RefdataValues

        result.packageType      = pkg.packageType?.value
        result.packageStatus    = pkg.packageStatus?.value
        result.packageListStatus = pkg.packageListStatus?.value
        result.breakable        = pkg.breakable?.value
        result.consistent       = pkg.consistent?.value
        result.fixed            = pkg.fixed?.value
        result.isPublic         = pkg.isPublic?.value
        result.packageScope     = pkg.packageScope?.value

        // References

        result.documents        = ApiReaderHelper.resolveDocuments(pkg.documents) // com.k_int.kbplus.DocContext
        result.identifiers      = ApiReaderHelper.resolveIdentifiers(pkg.ids) // com.k_int.kbplus.IdentifierOccurrence
        result.license          = ApiReaderHelper.resolveLicenseStub(pkg.license, context) // com.k_int.kbplus.License
        result.nominalPlatform  = ApiReaderHelper.resolvePlatform(pkg.nominalPlatform) // com.k_int.kbplus.Platform
        result.organisations    = ApiReaderHelper.resolveOrgLinks(pkg.orgs, ApiReaderHelper.IGNORE_PACKAGE, context) // com.k_int.kbplus.OrgRole
        result.subscriptions    = ApiReaderHelper.resolveSubscriptionPackageStubs(pkg.subscriptions, ApiReaderHelper.IGNORE_PACKAGE, context) // com.k_int.kbplus.SubscriptionPackage
        result.tipps            = ApiReaderHelper.resolveTipps(pkg.tipps, ApiReaderHelper.IGNORE_ALL, context) // com.k_int.kbplus.TitleInstancePackagePlatform

        // Ignored
        /*
        result.persons          = exportHelperService.resolvePrsLinks(
                pkg.prsLinks, exportHelperService.NO_CONSTRAINT, exportHelperService.NO_CONSTRAINT, context
        ) // com.k_int.kbplus.PersonRole
        */
        return ApiReaderHelper.cleanUp(result, true, true)
    }


    /**
     * @param com.k_int.kbplus.Subscription sub
     * @param com.k_int.kbplus.Org context
     * @return
     */
    static exportSubscription(Subscription sub, Org context){
        def result = [:]

        sub = GrailsHibernateUtil.unwrapIfProxy(sub)

        result.globalUID            = sub.globalUID
        result.cancellationAllowances = sub.cancellationAllowances
        result.dateCreated          = sub.dateCreated
        result.endDate              = sub.endDate
        result.identifier           = sub.identifier
        result.lastUpdated          = sub.lastUpdated
        result.manualCancellationDate = sub.manualCancellationDate
        result.manualRenewalDate    = sub.manualRenewalDate
        result.name                 = sub.name
        result.noticePeriod         = sub.noticePeriod
        result.startDate            = sub.startDate

        // RefdataValues

        result.form         = sub.form?.value
        result.isSlaved     = sub.isSlaved?.value
        result.isPublic     = sub.isPublic?.value
        result.resource     = sub.resource?.value
        result.status       = sub.status?.value
        result.type         = sub.type?.value

        // References

        result.documents            = ApiReaderHelper.resolveDocuments(sub.documents) // com.k_int.kbplus.DocContext
        //result.derivedSubscriptions = ApiReaderHelper.resolveStubs(sub.derivedSubscriptions, ApiReaderHelper.SUBSCRIPTION_STUB, context) // com.k_int.kbplus.Subscription
        result.identifiers          = ApiReaderHelper.resolveIdentifiers(sub.ids) // com.k_int.kbplus.IdentifierOccurrence
        result.instanceOf           = ApiReaderHelper.resolveSubscriptionStub(sub.instanceOf, context) // com.k_int.kbplus.Subscription
        result.license              = ApiReaderHelper.resolveLicenseStub(sub.owner, context) // com.k_int.kbplus.License
        //removed: result.license          = ApiReaderHelper.resolveLicense(sub.owner, ApiReaderHelper.IGNORE_ALL, context) // com.k_int.kbplus.License

        //result.organisations        = ApiReaderHelper.resolveOrgLinks(sub.orgRelations, ApiReaderHelper.IGNORE_SUBSCRIPTION, context) // com.k_int.kbplus.OrgRole
        result.previousSubscription = ApiReaderHelper.resolveSubscriptionStub(sub.previousSubscription, context) // com.k_int.kbplus.Subscription
        result.properties           = ApiReaderHelper.resolveCustomProperties(sub, context) // com.k_int.kbplus.SubscriptionCustomProperty

        def allOrgRoles = []
        sub.derivedSubscriptions.each { member ->
            allOrgRoles.addAll(
                OrgRole.findAllBySubAndRoleType(member, RefdataValue.getByValueAndCategory('Subscriber_Consortial', 'Organisational Role'))
            )
            allOrgRoles.addAll(
                OrgRole.findAllBySubAndRoleType(member, RefdataValue.getByValueAndCategory('Subscriber', 'Organisational Role'))
            )
        }
        allOrgRoles.addAll(sub.orgRelations)
        result.organisations = ApiReaderHelper.resolveOrgLinks(allOrgRoles, ApiReaderHelper.IGNORE_SUBSCRIPTION, context) // com.k_int.kbplus.OrgRole

        // TODO refactoring with issueEntitlementService
        result.packages = ApiReaderHelper.resolvePackagesWithIssueEntitlements(sub.packages, context) // com.k_int.kbplus.SubscriptionPackage

        // Ignored

        //result.packages = exportHelperService.resolvePackagesWithIssueEntitlements(sub.packages, context) // com.k_int.kbplus.SubscriptionPackage
        //result.issueEntitlements = exportHelperService.resolveIssueEntitlements(sub.issueEntitlements, context) // com.k_int.kbplus.IssueEntitlement
        //result.packages = exportHelperService.resolveSubscriptionPackageStubs(sub.packages, exportHelperService.IGNORE_SUBSCRIPTION, context) // com.k_int.kbplus.SubscriptionPackage
        /*
        result.persons      = exportHelperService.resolvePrsLinks(
                sub.prsLinks,  true, true, context
        ) // com.k_int.kbplus.PersonRole
        */
        // result.costItems    = exportHelperService.resolveCostItems(sub.costItems) // com.k_int.kbplus.CostItem

        return ApiReaderHelper.cleanUp(result, true, true)
    }

    // ################### CATALOGUE ###################

    /**
     * @return
     */
    static exportRefdatas(){
        CacheService cacheService = grails.util.Holders.applicationContext.getBean('cacheService') as CacheService

        def cache = cacheService.getTTL1800Cache('ApiReader/exportRefdatas/')
        def result = []

        if (cache.get('refdatas')) {
            result = cache.get('refdatas')
            log.debug('refdatas from cache')
        }
        else {
            def validLabel = { value ->
                return (value != 'null' && value != 'null °') ? value : null
            }

            RefdataCategory.where {}.sort('desc').each { rdc ->
                def rdcTmp = [:]

                rdcTmp.desc = rdc.desc
                rdcTmp.label_de = validLabel(rdc.getI10n('desc', 'de'))
                rdcTmp.label_en = validLabel(rdc.getI10n('desc', 'en'))
                rdcTmp.entries = []

                RefdataCategory.getAllRefdataValues(rdc.desc).each { rdv ->
                    def tmpRdv = [:]

                    tmpRdv.value = rdv.value
                    tmpRdv.label_de = validLabel(rdc.getI10n('value', 'de'))
                    tmpRdv.label_en = validLabel(rdc.getI10n('value', 'en'))

                    rdcTmp.entries << ApiReaderHelper.cleanUp(tmpRdv, true, true)
                }
                result << ApiReaderHelper.cleanUp(rdcTmp, true, true)
            }
            cache.put('refdatas', result)
        }
        result
    }

    // ################### HELPER ###################

    static isDataManager(User user) {
        def role = UserRole.findAllWhere(user: user, role: Role.findByAuthority('ROLE_API_DATAMANAGER'))
        return ! role.isEmpty()
    }
}
