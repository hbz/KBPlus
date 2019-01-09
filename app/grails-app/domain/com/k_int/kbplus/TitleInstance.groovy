package com.k_int.kbplus

import de.laser.traits.AuditTrait
import de.laser.domain.AbstractBaseDomain

import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import javax.persistence.Transient
import org.apache.commons.logging.*
import java.text.Normalizer
import groovy.util.logging.*

@Log4j
class TitleInstance extends AbstractBaseDomain implements AuditTrait {

  @Transient
  def grailsApplication

    // AuditTrait
    static auditable = true
    static controlledProperties = ['title']

  static Log static_logger = LogFactory.getLog(TitleInstance)

  static final Pattern alphanum = Pattern.compile("\\p{Punct}|\\p{Cntrl}");

  String title
  String normTitle
  String keyTitle
  String sortTitle
  String impId
  RefdataValue status
  RefdataValue type
  Date dateCreated
  Date lastUpdated

  static mappedBy = [
                     tipps:     'title',
                     ids:       'ti',
                     orgs:      'title',
                     historyEvents: 'participant',
                     prsLinks:  'title'
                     ]
  static hasMany = [
                    tipps:  TitleInstancePackagePlatform,
                    ids:    IdentifierOccurrence,
                    orgs:   OrgRole,
                    historyEvents: TitleHistoryEventParticipant,
                    prsLinks: PersonRole,
                    creators: CreatorTitle
                    ]

  static mapping = {
           id column:'ti_id'
    globalUID column:'ti_guid'
        title column:'ti_title', type:'text'
    normTitle column:'ti_norm_title', type:'text'
     keyTitle column:'ti_key_title', type:'text'
      version column:'ti_version'
        impId column:'ti_imp_id', index:'ti_imp_id_idx'
       status column:'ti_status_rv_fk'
         type column:'ti_type_rv_fk'
        tipps sort:'startDate', order: 'asc'
    sortTitle column:'sort_title', type:'text'

  }

    static constraints = {
        globalUID(nullable:true, blank:false, unique:true, maxSize:255)
        status(nullable:true, blank:false);
        type(nullable:true, blank:false);
        title(nullable:true, blank:false,maxSize:2048);
        normTitle(nullable:true, blank:false,maxSize:2048);
        sortTitle(nullable:true, blank:false,maxSize:2048);
        keyTitle(nullable:true, blank:false,maxSize:2048);
        creators(nullable:true, blank:false);
    }

  String getIdentifierValue(idtype) {
    def result=null
    ids?.each { id ->
      if ( id.identifier?.ns?.ns?.toLowerCase() == idtype.toLowerCase() )
        result = id.identifier?.value
    }
    result
  }

  Org getPublisher() {
    def result = null;
    orgs.each { o ->
      if ( o.roleType?.value == 'Publisher' ) {
        result = o.org
      }
    }
    result
  }

  static def lookupByIdentifierString(idstr) {

      static_logger.debug("lookupByIdentifierString(${idstr})")

    def result = null;
    def qr = null;
    def idstr_components = idstr.split(':');

    switch ( idstr_components.size() ) {
      case 1:
        qr = TitleInstance.executeQuery('select t from TitleInstance as t join t.ids as io where io.identifier.value = ?',[idstr_components[0]])
        break;
      case 2:
        qr = TitleInstance.executeQuery('select t from TitleInstance as t join t.ids as io where io.identifier.value = ? and lower(io.identifier.ns.ns) = ?',[idstr_components[1],idstr_components[0]?.toLowerCase()])
        break;
      default:
        // static_logger.debug("Unable to split");
        break;
    }

    // static_logger.debug("components: ${idstr_components} : ${qr}");

    if ( qr ) {
      switch ( qr.size() ) {
        case 0:
            static_logger.debug("No matches - trying to locate via identifier group");
          switch ( idstr_components.size() ) {
            case 1:
              qr = TitleInstance.executeQuery('select t from TitleInstance as t join t.ids as io where exists ( select i from Identifier where i.value = ? and i.ig = io.identifier.ig )',[idstr_components[0]])
              break;
            case 2:
              qr = TitleInstance.executeQuery('select t from TitleInstance as t join t.ids as io where exists ( select i from Identifier where i.value = ? and i.ns.ns = ? and i.ig = io.identifier.ig )',[idstr_components[1],idstr_components[0]?.toLowerCase()])
              break;
            default:
              // static_logger.debug("Unable to split");
              break;
          }

          break;
        case 1:
          result = qr.get(0);
          break;
        default:
            static_logger.error("WARNING:: Identifier '${idstr}' matched multiple rows");
          break;
      }
    }

    result
  }

  /**
   * Attempt to look up a title instance which has any of the listed identifiers
   * @param candidate_identifiers A list of maps containing identifiers and namespaces [ { namespace:'ISSN', value:'Xnnnn-nnnn' }, {namespace:'ISSN', value:'Xnnnn-nnnn'} ]
   */
  static def findByIdentifier(candidate_identifiers) {
    def matched = []
    candidate_identifiers.each { i ->
      def id = Identifier.lookupOrCreateCanonicalIdentifier(i.namespace, i.value)
      def io = IdentifierOccurrence.findByIdentifier(id)
      if ( ( io != null ) && ( io.ti != null ) ) {
        if ( matched.contains(io.ti) ) {
          // Already in the list
        }
        else {
          matched.add(io.ti);
        }
      }
    }

    // Didn't match anything - see if we can match based on identifier without namespace [In case of duff supplier data]
    if ( matched.size() == 0 ) {
      candidate_identifiers.each { i ->
        def id1 = Identifier.executeQuery('Select io from IdentifierOccurrence as io where io.identifier.value = ?',[i.value]);
        id1.each {
          if ( it.ti != null ) {
            if ( matched.contains(it.ti) ) {
              // Already in the list
            }
            else {
              matched.add(it.ti);
            }
          }
        }
      }
    }

    def result = null;
    if ( matched.size() == 1 ) {
      result = matched.get(0);
    }
    else if ( matched.size() > 1 ) {
      throw new Exception("Identifier set ${candidate_identifiers} matched multiple titles");
    }

    return result;
  }

  static def lookupOrCreate(candidate_identifiers, title) {
    lookupOrCreate(candidate_identifiers, title, false, null)
  }

  static def lookupOrCreate(candidate_identifiers, title, titletyp) {
        lookupOrCreate(candidate_identifiers, title, false, titletyp)
    }

  static def lookupOrCreate(candidate_identifiers, title, enrich, titletyp) {
    def result = null;
    def origin_uri = null
    def skip_creation = false
    def ti_candidates = []
    def canonical_ids = []

    candidate_identifiers.each { i ->
      if(i.namespace.toLowerCase() == 'uri'){
        origin_uri = i.value
      }

      def id = Identifier.lookupOrCreateCanonicalIdentifier(i.namespace, i.value)

      if(id && id.ns.ns && id.value){
        canonical_ids.add(id)
        static_logger.debug("processing candidate identifier ${i} as ${id}");

        def io = IdentifierOccurrence.findAllByIdentifier(id)

        if ( io.size() > 0 ) {
          static_logger.debug("located existing title(s)");
          io.each { occ ->
            if(occ.ti && occ.ti.status?.value == 'Current' && !ti_candidates.contains(occ.ti)){
              ti_candidates.add(occ.ti)
            }
          }
        }
      }
      else {
        static_logger.debug("error processing candidate identifier ${i}");
      }
    }

    def valid_match = false

    if( ti_candidates.size() > 0 ) {
      static_logger.debug("Collected ${ti_candidates.size()} title candidates: ${ti_candidates}")

      def full_matches = []
      def origin_matches = []
      def name_matches = []

      ti_candidates.each { cti ->
        def intersection = 0
        boolean full_match = true
        boolean name_match = (cti.title == title ? true : false)
        boolean origin_match = false
        boolean origin_uri_match = false

        cti.ids.each { ctio ->

          if ( ctio.identifier.ns.ns == 'originediturl' && canonical_ids.contains(ctio.identifier) ){
            origin_match = true
          }
          else if ( !canonical_ids.contains(ctio.identifier) ){
            if ( ctio.identifier.ns.ns != 'originediturl' && ctio.identifier.ns.ns != 'uri'){
              full_match = false
            }
          }else{

            if ( ctio.identifier.ns.ns == 'uri' && canonical_ids.contains(ctio.identifier) ){
              origin_uri_match = true
            }
            intersection++;
          }
        }
        if ( origin_match ) {
          if( !result ) {
            result = cti
            valid_match = true
            log.debug("Matched TI by originediturl.")
          }else{
            log.warn("Multiple TIs with the same originediturl found: ${result} AND ${cti}! This should not be possible!")
          }
        }

        if ( full_match ){
          full_matches.add(cti)
        }

        if ( intersection >= 2 && origin_uri_match ){
          origin_matches.add(cti)
        }

        if ( name_match ){
          name_matches.add(cti)
        }
      }

      if ( !valid_match ){
        if ( full_matches.size() == 0 ){
          static_logger.debug("None of the matches is a full match. Looking for partial matches..")

          if ( origin_matches.size() == 1 ){
            static_logger.debug("Found one acceptable match! (By origin URI)")
            result = origin_matches[0]
            valid_match = true
          }
          else if ( name_matches.size() == 1 ){
            static_logger.debug("Found one acceptable match! (Name match and 1 identical ID)")
            result = name_matches[0]
            valid_match = true
          }
          else {
            static_logger.debug("Could not find an acceptable match..")
          }
        }
        else if ( full_matches.size() == 1 ) {
          static_logger.debug("Found exactly one full match.")
          result = full_matches[0]
          valid_match = true
        }
        else{
          static_logger.debug("Found multiple full matches, trying to narrow down the list..")

          if ( origin_matches.size() == 1 ){
            static_logger.debug("Matched title by origin uri");
            result = origin_matches[0]
            valid_match = true
          }
          else if ( name_matches.size() == 1 ) {
            static_logger.debug("Found one acceptable match! (Name match and 1 identical ID)")
            result = name_matches[0]
            valid_match = true
          }
          else{
            static_logger.debug("Could not match a candidate. Skipping title...")
            skip_creation = true
          }
        }
      }
    }

    if (!valid_match && !skip_creation) {
      static_logger.debug("No valid match - creating new title");
      def ti_status = RefdataValue.loc(RefdataCategory.TI_STATUS, [en: 'Current', de: 'Aktuell'])
      //result = new TitleInstance(title:title, impId:java.util.UUID.randomUUID().toString(), status:ti_status);
      result = ((titletyp=='BookInstance') ? new BookInstance(title:title, impId:java.util.UUID.randomUUID().toString(), status:ti_status, type: RefdataValue.loc(RefdataCategory.TI_TYPE, [en: 'EBook', de: 'EBook'])) :
              (titletyp=='DatabaseInstance' ? new DatabaseInstance(title:title, impId:java.util.UUID.randomUUID().toString(), status:ti_status, type: RefdataValue.loc(RefdataCategory.TI_TYPE, [en: 'Database', de: 'Database'])) : new JournalInstance(title:title, impId:java.util.UUID.randomUUID().toString(), status:ti_status, type: RefdataValue.loc(RefdataCategory.TI_TYPE, [en: 'Journal', de: 'Journal']))))
      result.save(flush:true, failOnError: true);

      result.ids=[]

      canonical_ids.each {
        def new_io = new IdentifierOccurrence(identifier:it, ti:result)
        if ( new_io.save(flush:true) ) {
          static_logger.debug("Created new IO");
        }
        else {
          static_logger.error("Problem creating new IO");
        }
        // result.ids.add(new IdentifierOccurrence(identifier:it, ti:result));
      }

      if ( ! result.save(flush:true) ) {
        throw new RuntimeException("Problem creating title instance : ${result.errors?.toString()}");
      }
    }

    if ( enrich && valid_match ) {
      static_logger.debug("enrich... current ids = ${result.ids}, import-ids = ${canonical_ids}");
      // static_logger.debug("Checking that all identifiers are already present in title");
      boolean modified = false;
      // Check that all the identifiers listed are present
      canonical_ids.each { cid ->
        if(cid.ns.ns.toLowerCase() != 'originediturl'){
          static_logger.debug("looking for identifier ${cid.ns.ns}:${cid.value}");

          def matched_io = IdentifierOccurrence.executeQuery("select io from IdentifierOccurrence as io where io.identifier = ? and io.ti = ?",[cid,result])

          if ( !matched_io || matched_io && matched_io.size() == 0){
            static_logger.debug("adding link to identifier ${cid}");
            def new_io = new IdentifierOccurrence(identifier:cid, ti:result).save(flush:true, failOnError: true);
            modified=true;
          }
        }
      }
      if ( modified ) {
        result.save(flush:true, failOnError: true);
      }
    }

    return result;

  }

  /**
   *  Caller passes in a map like {issn:'nnnn-nnnn',doi:'quyeihdj'} and expects to get back a new title
   *  or one matching any of the identifiers
   *  - assumes all identifiers to be unique -
   */
    //TODO: Wegen Überarbeitung von Titel Konzept muss dies hier nochmal überarbeitet werden by Moe
  static def lookupOrCreateViaIdMap(candidate_identifiers, title) {
    def result = null;
    def ids = []

    candidate_identifiers.each { i ->

      if ( ( i.key != null ) &&
           ( i.value != null ) &&
           ( i.key.length() > 0 ) &&
           ( i.value.length() > 0 ) ) {

        def id = Identifier.lookupOrCreateCanonicalIdentifier(i.key, i.value)
        if ( id != null ) {
          ids.add(id);

          // If the namespace is marked as non-unique, we allow repeats. otherwise check for uniqueness
          if ( id.ns.nonUnique == Boolean.TRUE ) {
            // Namespace is marked as non-unique, don't perform a uniqueness check
          }
          else {
            def io = IdentifierOccurrence.findByIdentifier(id)
            if ( io && io.ti ) {
              if ( result == null ) {
                result = io.ti;
              }
              else {
                if ( result != io.ti ) {
                  throw new RuntimeException("Identifiers(${candidate_identifiers}) reference multiple titles [Already located title with id ${result.id}, also located title with id ${io.ti.id}]");
                }
              }
            }
          }
        }
      }
    }

    if (!result) {
        //TODO: Konzept überdenken wegen imId.
      //result = new TitleInstance(title:title, impId:java.util.UUID.randomUUID().toString());
      result = ((title.type=='Serial') ? new JournalInstance(title:title, impId:java.util.UUID.randomUUID().toString(), type: RefdataValue.loc(RefdataCategory.TI_TYPE, [en: 'Journal', de: 'Journal'])) :
                (title.type=='Database' ? new DatabaseInstance(title:title, impId:java.util.UUID.randomUUID().toString(), type: RefdataValue.loc(RefdataCategory.TI_TYPE, [en: 'Database', de: 'Database'])) : new BookInstance(title:title, impId:java.util.UUID.randomUUID().toString(), type: RefdataValue.loc(RefdataCategory.TI_TYPE, [en: 'Book', de: 'Book']))))

      result.ids=[]
      ids.each {
        result.ids.add(new IdentifierOccurrence(identifier:it, ti:result).save(flush: true));
      }
      if ( ! result.save() ) {
        throw new RuntimeException("Problem creating title instance : ${result.errors?.toString()}");
      }
    }
    else {
      //static_logger.debug("Checking that all identifiers are already present in title (${ids})");
      boolean modified = false;
      // Check that all the identifiers listed are present
      ids.each { identifier ->
        // it == an ID
        // Does result.ids contain an identifier occurrence that matches this ID
        // def existing_id = result.ids.find { it -> it.identifier == identifier }
        def existing_id = IdentifierOccurrence.findByIdentifierAndTi(identifier,result)

        if ( existing_id == null ) {
          //static_logger.debug("Adding additional identifier ${identifier}");
          result.ids.add(new IdentifierOccurrence(identifier:identifier, ti:result).save(flush: true));
          modified=true;
        }
        else {
          //static_logger.debug("Identifier ${identifier} already present in existing title ${result}");
        }
      }

      if ( modified ) {
        result.save();
      }
    }

    return result;

  }

    @Override
    def beforeInsert() {
        if (title != null) {
            normTitle = generateNormTitle(title)
            keyTitle = generateKeyTitle(title)
            sortTitle = generateSortTitle(title)
        }
        
        if (impId == null) {
          impId = java.util.UUID.randomUUID().toString();
        }
        
        super.beforeInsert()
    }

    @Override
    def beforeUpdate() {
        if (title != null) {
            normTitle = generateNormTitle(title)
            keyTitle = generateKeyTitle(title)
            sortTitle = generateSortTitle(title)
        }
        super.beforeUpdate()
    }


  public static String generateSortTitle(String input_title) {
    if ( ! input_title ) return null;

    def s1 = Normalizer.normalize(input_title, Normalizer.Form.NFKD).trim().toLowerCase()
    s1 = s1.replaceFirst('^copy of ','')
    s1 = s1.replaceFirst('^the ','')
    s1 = s1.replaceFirst('^a ','')
    s1 = s1.replaceFirst('^der ','')
    
    return  s1.trim()  
  }

  public static String generateNormTitle(String input_title) {
    if (!input_title) return null;

    def result = input_title.replaceAll('&',' and ');
    result = result.trim();
    result = result.replaceAll("\\s+", " ");
    result = result.toLowerCase();
    result = alphanum.matcher(result).replaceAll("");
   
    return asciify(result)
  }

  public static String generateKeyTitle(String s) {
    def result = null
    if ( s != null ) {
        s = s.replaceAll('&',' and ');
        s = s.trim(); // first off, remove whitespace around the string
        s = s.replaceAll("\\s+", " ");
        s = s.toLowerCase(); // then lowercase it
        s = alphanum.matcher(s)?.replaceAll(''); // then remove all punctuation and control chars
        String[] frags = StringUtils.split(s); // split by whitespace
        TreeSet<String> set = new TreeSet<String>();
        for (String ss : frags) {
            set.add(ss); // order fragments and dedupe
        }
        StringBuffer b = new StringBuffer();
        Iterator<String> i = set.iterator();
        while (i.hasNext()) {  // join ordered fragments back together
            b.append(i.next());
            if ( i.hasNext() )
              b.append(' ');
        }
        result = asciify(b.toString()); // find ASCII equivalent to characters
    }

    return result;
  }

  protected static String asciify(String s) {
        char[] c = s.toCharArray();
        StringBuffer b = new StringBuffer();
        for (char element : c) {
            b.append(translate(element));
        }
        return b.toString();
  }

    /**
     * Translate the given unicode char in the closest ASCII representation
     * NOTE: this function deals only with latin-1 supplement and latin-1 extended code charts
     */
    private static char translate(char c) {
        switch(c) {
            case '\u00C0':
            case '\u00C1':
            case '\u00C2':
            case '\u00C3':
            case '\u00C4':
            case '\u00C5':
            case '\u00E0':
            case '\u00E1':
            case '\u00E2':
            case '\u00E3':
            case '\u00E4':
            case '\u00E5':
            case '\u0100':
            case '\u0101':
            case '\u0102':
            case '\u0103':
            case '\u0104':
            case '\u0105':
                return 'a';
            case '\u00C7':
            case '\u00E7':
            case '\u0106':
            case '\u0107':
            case '\u0108':
            case '\u0109':
            case '\u010A':
            case '\u010B':
            case '\u010C':
            case '\u010D':
                return 'c';
            case '\u00D0':
            case '\u00F0':
            case '\u010E':
            case '\u010F':
            case '\u0110':
            case '\u0111':
                return 'd';
            case '\u00C8':
            case '\u00C9':
            case '\u00CA':
            case '\u00CB':
            case '\u00E8':
            case '\u00E9':
            case '\u00EA':
            case '\u00EB':
            case '\u0112':
            case '\u0113':
            case '\u0114':
            case '\u0115':
            case '\u0116':
            case '\u0117':
            case '\u0118':
            case '\u0119':
            case '\u011A':
            case '\u011B':
                return 'e';
            case '\u011C':
            case '\u011D':
            case '\u011E':
            case '\u011F':
            case '\u0120':
            case '\u0121':
            case '\u0122':
            case '\u0123':
                return 'g';
            case '\u0124':
            case '\u0125':
            case '\u0126':
            case '\u0127':
                return 'h';
            case '\u00CC':
            case '\u00CD':
            case '\u00CE':
            case '\u00CF':
            case '\u00EC':
            case '\u00ED':
            case '\u00EE':
            case '\u00EF':
            case '\u0128':
            case '\u0129':
            case '\u012A':
            case '\u012B':
            case '\u012C':
            case '\u012D':
            case '\u012E':
            case '\u012F':
            case '\u0130':
            case '\u0131':
                return 'i';
            case '\u0134':
            case '\u0135':
                return 'j';
            case '\u0136':
            case '\u0137':
            case '\u0138':
                return 'k';
            case '\u0139':
            case '\u013A':
            case '\u013B':
            case '\u013C':
            case '\u013D':
            case '\u013E':
            case '\u013F':
            case '\u0140':
            case '\u0141':
            case '\u0142':
                return 'l';
            case '\u00D1':
            case '\u00F1':
            case '\u0143':
            case '\u0144':
            case '\u0145':
            case '\u0146':
            case '\u0147':
            case '\u0148':
            case '\u0149':
            case '\u014A':
            case '\u014B':
                return 'n';
            case '\u00D2':
            case '\u00D3':
            case '\u00D4':
            case '\u00D5':
            case '\u00D6':
            case '\u00D8':
            case '\u00F2':
            case '\u00F3':
            case '\u00F4':
            case '\u00F5':
            case '\u00F6':
            case '\u00F8':
            case '\u014C':
            case '\u014D':
            case '\u014E':
            case '\u014F':
            case '\u0150':
            case '\u0151':
                return 'o';
            case '\u0154':
            case '\u0155':
            case '\u0156':
            case '\u0157':
            case '\u0158':
            case '\u0159':
                return 'r';
            case '\u015A':
            case '\u015B':
            case '\u015C':
            case '\u015D':
            case '\u015E':
            case '\u015F':
            case '\u0160':
            case '\u0161':
            case '\u017F':
                return 's';
            case '\u0162':
            case '\u0163':
            case '\u0164':
            case '\u0165':
            case '\u0166':
            case '\u0167':
                return 't';
            case '\u00D9':
            case '\u00DA':
            case '\u00DB':
            case '\u00DC':
            case '\u00F9':
            case '\u00FA':
            case '\u00FB':
            case '\u00FC':
            case '\u0168':
            case '\u0169':
            case '\u016A':
            case '\u016B':
            case '\u016C':
            case '\u016D':
            case '\u016E':
            case '\u016F':
            case '\u0170':
            case '\u0171':
            case '\u0172':
            case '\u0173':
                return 'u';
            case '\u0174':
            case '\u0175':
                return 'w';
            case '\u00DD':
            case '\u00FD':
            case '\u00FF':
            case '\u0176':
            case '\u0177':
            case '\u0178':
                return 'y';
            case '\u0179':
            case '\u017A':
            case '\u017B':
            case '\u017C':
            case '\u017D':
            case '\u017E':
                return 'z';
        }
        return c;
    }



  static def refdataFind(params) {
    def result = [];
    def ql = null;
    if ( params.sort==null ) {
      params.sort="title"
      params.order="desc"
    }

    // If the search without a wildcard returns items, return those, otherwise if a case insensitive search
    // without a wildcard returns 0, add a wildcard and use that
    def num_titles = TitleInstance.countByTitleIlike("${params.q}",params)
    if ( num_titles == 0 ) {
      ql = TitleInstance.findAllByTitleIlike("${params.q}%",params)
    }
    else {
      ql = TitleInstance.findAllByTitleIlike("${params.q}",params)
    }

    int i = 1;
    if ( ql ) {
      ql.each { t ->
        result.add([id:"${t.class.name}:${t.id}",text:"[${i++}] ${t.title} (${t.identifiersAsString()})"])
      }
    }

    result
  }


  @Transient
  def identifiersAsString() {
    def result = new StringWriter()
    ids.each { id ->
      result.write("${id.identifier.ns.ns}:${id.identifier.value} ");
    }
    return result.toString()
  }

    /*
  @Transient
  def onChange = { oldMap,newMap ->

    // static_logger.debug("onChange")

    def changeNotificationService = grailsApplication.mainContext.getBean("changeNotificationService")
    def controlledProperties = ['title']

    controlledProperties.each { cp ->
      if ( oldMap[cp] != newMap[cp] ) {
        changeNotificationService.notifyChangeEvent([
                                                     OID:"${this.class.name}:${this.id}",
                                                     event:'TitleInstance.propertyChange',
                                                     prop:cp,
                                                     old:oldMap[cp],
                                                     new:newMap[cp]
                                                    ])
      }
    }
  }
*/

  @Transient
  def notifyDependencies(changeDocument) {
    // static_logger.debug("notifyDependencies(${changeDocument})");

    def changeNotificationService = grailsApplication.mainContext.getBean("changeNotificationService")
    tipps.each { tipp ->
      // Notify each package that a component title has changed
      changeNotificationService.broadcastEvent("${tipp.pkg.class.name}:${tipp.pkg.id}", changeDocument);
    }

    changeNotificationService.broadcastEvent("${this.class.name}:${this.id}", changeDocument);
  }

  def getInstitutionalCoverageSummary(institution, dateformat) {
    getInstitutionalCoverageSummary(institution, dateformat, null);
  }

  def getInstitutionalCoverageSummary(institution, dateformat, date_restriction) {
    def sdf = new java.text.SimpleDateFormat(dateformat)
    def qry = """
select ie from IssueEntitlement as ie JOIN ie.subscription.orgRelations as o 
  where ie.tipp.title = :title and o.org = :institution 
  AND (o.roleType.value = 'Subscriber' OR o.roleType.value = 'Subscriber_Consortial' OR o.roleType.value = 'Subscription Consortia') 
  AND ie.subscription.status.value != 'Deleted' 
  AND ie.status.value != 'Deleted'
"""
    def qry_params = ['title':this, institution:institution]

    if ( date_restriction ) {
      qry += " AND ie.subscription.startDate <= :date_restriction AND ie.subscription.endDate >= :date_restriction "
      qry_params.date_restriction = date_restriction
    }

    def ies = IssueEntitlement.executeQuery(qry,qry_params)
    def earliest = null
    def latest = null
    boolean open = false
    ies.each { ie ->
      if ( earliest == null ) { earliest = ie.startDate } else { if ( ie.startDate < earliest ) { earliest = ie.startDate } }
      if ( latest == null ) { latest = ie.endDate } else { if ( ie.endDate > latest ) { latest = ie.endDate } }
      if ( ie.endDate == null ) open = true;
    }

    [
      earliest:earliest?sdf.format(earliest):'',
      latest: open ? '': (latest?sdf.format(latest):''),
      ies:ies
    ]
  }

  def checkAndAddMissingIdentifier(ns,value) {
    boolean found = false
    static_logger.debug("Looking for identifier ${value} in title ids ${this.ids}");

    this.ids.each {
      if ( it.identifier.ns.ns.toLowerCase() == ns.toLowerCase() && it.identifier.value == value ) {
        found = true
      }
    }

    if ( !found ) {
      def id = Identifier.lookupOrCreateCanonicalIdentifier(ns, value)
      def id_occ = IdentifierOccurrence.executeQuery("select io from IdentifierOccurrence as io where io.identifier = ? and io.ti = ?", [id,this])

      static_logger.debug("Create new identifier occurrence for tid:${getId()} ns:${ns} value:${value}");

      if ( !id_occ || id_occ.size() == 0 ){
        new IdentifierOccurrence(identifier:id, ti:this).save(flush:true)
      }
    }
  }

  @Transient
  def distinctEventList() {
    def result = []
    historyEvents.each { he ->
      if ( result.find { it.event.id == he.event.id} ) {
      }
      else {
        result.add(he)
      }
    }
    result
  }

  @Transient
  def isInPackage(pkg) {
    def result = false
    def tipp = TitleInstancePackagePlatform.findByTitleAndPkg(this,pkg)
    if(tipp)
      result=true
    result
  }

  static def expunge(title_id) {
    try {
      log.debug("  -> IEs");
      TitleInstance.executeUpdate('delete from IssueEntitlement ie where ie.tipp in ( select tipp from TitleInstancePackagePlatform tipp where tipp.title.id = ? )',[title_id])
      log.debug("  -> TIPPs");
      TitleInstance.executeUpdate('delete from TitleInstancePackagePlatform tipp where tipp.title.id = ?',[title_id])
      log.debug("  -> IdentifierOccurrence");
      TitleInstance.executeUpdate('delete from IdentifierOccurrence io where io.ti.id = ?',[title_id])
      log.debug("  -> OrgRole");
      TitleInstance.executeUpdate('delete from OrgRole orl where orl.title.id = ?',[title_id])
      log.debug("  -> TitleHistoryEventParticipant");
      TitleInstance.executeUpdate('delete from TitleHistoryEventParticipant he where he.participant.id = ?',[title_id])
      log.debug("  -> CoreAssertion");
      TitleInstance.executeUpdate('delete from CoreAssertion ca where ca.tiinp in (select tip from TitleInstitutionProvider tip where tip.title.id = ?)',[title_id])
      log.debug("  -> TitleInstitutionProvider");
      TitleInstance.executeUpdate('delete from TitleInstitutionProvider tip where tip.title.id = ?',[title_id])
      log.debug("  -> Fact");
      TitleInstance.executeUpdate('delete from Fact fact where fact.relatedTitle.id = ?',[title_id])
      log.debug("  -> OrgTitleStats");
      TitleInstance.executeUpdate('delete from OrgTitleStats ots where ots.title.id = ?',[title_id])
      log.debug("  -> TI itself");
      TitleInstance.executeUpdate('delete from TitleInstance ti where ti.id = ?',[title_id])
      log.debug("  -> DONE");
    }
    catch ( Exception e ) {
      log.error("Problem expunging title",e);
    }
  }

  /**
   * Validate a tipp start and end date by ensuring that the date range lies within known dates when this Title
   * was published. Null start dates are only valid when there is no earliest published dates.
  */
  def isValidCoverage(start, end) {

    // Disabled 8-oct-2015 as requested by Magaly via OS
    return true

    def result = true;
    def published_from = null;
    def published_to = null;

    orgs.each { o ->
      if ( o.roleType?.value == 'Publisher' ) {
        if ( ( o.startDate != null ) && ( ( published_from == null ) || ( o.startDate < published_from ) ) ) {
          published_from = o.startDate
        }
        if ( ( o.endDate != null ) && ( ( published_to == null ) || ( o.endDate > published_to ) ) ) {
          published_to = o.endDate
        }
      }
    }

    // Start date checks - It's valid if published from and start are null, otherwise they must match
    if ( ( published_from == null ) && ( start == null ) ) {
      result = result & true
    }
    else if ( ( published_from == null ) && ( start != null ) ) {
      // We allow if the tipp has a date, but the publisher link does not.
      result = result & true
    }
    else if ( ( published_from == null ) || ( start == null ) ) {
      result = result & false
    }
    else { // We have a start date and a published from date - make sure the range matches
      result = result & ( start > published_from )
    }

    // End date checks
    if ( ( published_to == null ) && ( end == null ) ) {
      result = result & true
    }
    else if ( ( published_to == null ) && ( end != null ) ) {
      // We allow if the tipp has a date, but the publisher link does not.
      result = result & true
    }
    else if ( ( published_to == null ) || ( end == null ) ) {
      result = result & false
    }
    else { // We have a start date and a published from date - make sure the range matches
      result = result & ( end  < published_to )
    }

    result
  }

  @Transient
  static def oaiConfig = [
    id:'titles',
    textDescription:'Title repository for KBPlus',
    query:" from TitleInstance as o ",
    pageSize:20
  ]

  /**
   *  Render this title as OAI_dc
   */
  @Transient
  def toOaiDcXml(builder, attr) {
    builder.'dc'(attr) {
      'dc:title' (name)
    }
  }

  /**
   *  Render this Title as KBPlusXML
   */
  @Transient
  def toKBPlus(builder, attr) {

    def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    def pub = getPublisher()

    try {
      builder.'kbplus' (attr) {
        builder.'title' (['id':(id)]) {
          builder.'title' (title)
        }
        builder.'identifiers' () {
          ids?.each { id_oc ->
            builder.identifier([namespace:id_oc.identifier?.ns.ns, value:id_oc.identifier?.value])
          }
        }
        if ( pub ) {
          builder.'publisher' ([id:(pub.id)]) {
            builder.'name' (pub.name)
          }
        }
      }
    }
    catch ( Exception e ) {
      log.error(e);
    }

  }
}
