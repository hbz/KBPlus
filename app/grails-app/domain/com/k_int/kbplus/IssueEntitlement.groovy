package com.k_int.kbplus

import de.laser.domain.AbstractBaseDomain
import de.laser.domain.IssueEntitlementCoverage
import de.laser.domain.PriceItem
import de.laser.helper.RefdataAnnotation

import javax.persistence.Transient

class IssueEntitlement extends AbstractBaseDomain implements Comparable {

    /*
    Date startDate
    String startVolume
    String startIssue
    Date endDate
    String endVolume
    String endIssue
    String embargo
    String coverageDepth
    String coverageNote
     */
  Date coreStatusStart
  Date coreStatusEnd
  Date accessStartDate
  Date accessEndDate

  String ieReason

  //merged as the difference between an IssueEntitlement and a TIPP is mainly former's attachment to a subscription, otherwise, they are functionally identical, even dependent upon each other. So why keep different refdata categories?
  @RefdataAnnotation(cat = 'TIPP Status')
  RefdataValue status

  @RefdataAnnotation(cat = 'CoreStatus')
  RefdataValue coreStatus // core Status is really core Medium.. dont ask

  @RefdataAnnotation(cat = '?')
  RefdataValue medium
    
  @RefdataAnnotation(cat = 'IE ACCEPT STATUS')
  RefdataValue acceptStatus

  static belongsTo = [subscription: Subscription, tipp: TitleInstancePackagePlatform]

  static hasOne =    [priceItem: PriceItem]

  static hasMany = [coverages: IssueEntitlementCoverage]

  @Transient
  def comparisonProps = ['derivedAccessStartDate', 'derivedAccessEndDate',
'coverageNote','coverageDepth','embargo','startVolume','startIssue','startDate','endDate','endIssue','endVolume']

  int compareTo(obj) {
    int cmp = tipp?.title?.title.compareTo(obj.tipp?.title?.title)
    if(cmp == 0)
      return tipp?.id?.compareTo(obj.tipp?.id)
    return cmp
  }

  static mapping = {
                id column:'ie_id'
         globalUID column:'ie_guid'
           version column:'ie_version'
            status column:'ie_status_rv_fk'
      subscription column:'ie_subscription_fk', index: 'ie_sub_idx'
              tipp column:'ie_tipp_fk',         index: 'ie_tipp_idx'
          ieReason column:'ie_reason'
            medium column:'ie_medium_rv_fk'
   accessStartDate column:'ie_access_start_date'
     accessEndDate column:'ie_access_end_date'
         coverages sort: 'startDate', order: 'asc'
      acceptStatus column:'ie_accept_status_rv_fk'
      /*
      startDate column:'ie_start_date',      index: 'ie_dates_idx'
      startVolume column:'ie_start_volume'
      startIssue column:'ie_start_issue'
      endDate column:'ie_end_date',        index: 'ie_dates_idx'
      endVolume column:'ie_end_volume'
      endIssue column:'ie_end_issue'
      embargo column:'ie_embargo'
      coverageDepth column:'ie_coverage_depth'
      coverageNote column:'ie_coverage_note',type: 'text'
      */
  }

  static constraints = {
    globalUID      (nullable:true, blank:false, unique:true, maxSize:255)
    status         (nullable:true, blank:false)
    subscription   (nullable:true, blank:false)
    tipp           (nullable:true, blank:false)
    ieReason       (nullable:true, blank:true)
    medium         (nullable:true, blank:true)
    priceItem      (nullable:true, blank:true)
    accessStartDate(nullable:true, blank:true)
    accessEndDate  (nullable:true, blank:true)
    coreStatus     (nullable:true, blank:true)
    coreStatusStart(nullable:true, blank:true)
    coreStatusEnd  (nullable:true, blank:true)
    acceptStatus  (nullable:true, blank:true)
      /*
      startDate     (nullable:true, blank:true)
      startVolume   (nullable:true, blank:true)
      startIssue    (nullable:true, blank:true)
      endDate       (nullable:true, blank:true)
      endVolume     (nullable:true, blank:true)
      endIssue      (nullable:true, blank:true)
      embargo       (nullable:true, blank:true)
      coverageDepth (nullable:true, blank:true)
      coverageNote  (nullable:true, blank:true)
       */
  }

  Date getDerivedAccessStartDate() {
    accessStartDate ? accessStartDate : subscription?.startDate
  }

  Date getDerivedAccessEndDate() {
    accessEndDate ? accessEndDate : subscription?.endDate
  }

  public RefdataValue getAvailabilityStatus() {
    return getAvailabilityStatus(new Date());
  }

  @Transient
  public int compare(IssueEntitlement ieB){
    if(ieB == null) return -1;

    def noChange =true 
    comparisonProps.each{ noChange &= this."${it}" == ieB."${it}" }

    if(noChange) return 0;
    return 1;
  }

  public RefdataValue getAvailabilityStatus(Date as_at) {
    def result = null
    // If StartDate <= as_at <= EndDate - Current
    // if Date < StartDate - Expected
    // if Date > EndDate - Expired
    def ie_access_start_date = getDerivedAccessStartDate()
    def ie_access_end_date = getDerivedAccessEndDate()

    result = RefdataCategory.lookupOrCreate('IE Access Status','Current')

    if (ie_access_start_date && as_at < ie_access_start_date ) {
      result = RefdataCategory.lookupOrCreate('IE Access Status','Expected');
    }
    else if (ie_access_end_date && as_at > ie_access_end_date ) {
      result = RefdataCategory.lookupOrCreate('IE Access Status','Expired');
    }


    /* legacy stuff ...
    if ( ( ie_access_start_date == null ) || ( ie_access_end_date == null ) ) {
      result = RefdataCategory.lookupOrCreate('IE Access Status','ERROR - No Subscription Start and/or End Date');
    }
    else if ( ( accessEndDate == null ) && ( as_at > ie_access_end_date ) ) {
      result = RefdataCategory.lookupOrCreate('IE Access Status','Current(*)');
    }
    else if ( as_at < ie_access_start_date ) {
      // expected
      result = RefdataCategory.lookupOrCreate('IE Access Status','Expected');
    }
    else if ( as_at > ie_access_end_date ) {
      // expired
      result = RefdataCategory.lookupOrCreate('IE Access Status','Expired');
    }
    else {
      result = RefdataCategory.lookupOrCreate('IE Access Status','Current');
    }
    */
    result
  }

  @Transient
  def getTIP(){
    def inst = subscription?.getSubscriber()
    def title = tipp?.title
    def provider = tipp?.pkg?.getContentProvider()
    if ( inst && title && provider ) {
      def tip = TitleInstitutionProvider.findByTitleAndInstitutionAndprovider(title, inst, provider)
      if(!tip){
        tip = new TitleInstitutionProvider(title:title,institution:inst,provider:provider)
        tip.save()
      }
      return tip
    }
    return null
  }
  
  @Transient
  def coreStatusOn(as_at) {
    // Use the new core system to determine if this title really is core
    def tip = getTIP()
    if(tip) return tip.coreStatus(as_at);
    return false
  }
  
  @Transient
  def extendCoreDates(startDate, endDate){
    def tip = getTIP()
      tip?.extendCoreExtent(startDate,endDate)
  }

  @Transient
  static def refdataFind(params) {

    def result = [];
    def hqlParams = []
    def hqlString = "select ie from IssueEntitlement as ie"

    if ( params.subFilter ) {
      hqlString += ' where ie.subscription.id = ?'
      hqlParams.add(params.long('subFilter'))
    }

    def results = IssueEntitlement.executeQuery(hqlString,hqlParams)

    results?.each { t ->
      def resultText = t.tipp.title.title
      result.add([id:"${t.class.name}:${t.id}",text:resultText])
    }

    result

  }

}
