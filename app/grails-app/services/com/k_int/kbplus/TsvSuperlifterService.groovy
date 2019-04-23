package com.k_int.kbplus

import org.apache.poi.ss.formula.functions.T
import org.grails.datastore.mapping.model.types.Simple
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy

import java.text.ParseException
import java.text.SimpleDateFormat
import org.springframework.transaction.annotation.*
import au.com.bytecode.opencsv.CSVReader


class TsvSuperlifterService {

  def genericOIDService
  def executorService
  def messageSource

  def load(input_stream, config, testRun, defaultLocatedObjects = [:]) {
    def result = [:]
    result.log = []

    def ctr = 0;
    def start_time = System.currentTimeMillis()

    CSVReader r = new CSVReader( new InputStreamReader(input_stream, java.nio.charset.Charset.forName('UTF-8') ), '\t' as char )
    String[] nl;
    String[] columns;
    def colmap = [:]
    def first = true

    while ((nl = r.readNext()) != null) {

     def row_information = [ messages:[], error:false]

     def elapsed = System.currentTimeMillis() - start_time

      if ( first ) {
        first = false; // header
        columns=nl
        result.columns = columns;
        log.debug('Header :'+columns);

        if ( columns?.length == 1 ) {
          throw new RuntimeException("Only one column in tsv file - Is it possible your tabs have been removed by an editor?");
        }

        // Set up colmap
        int i=0;
        columns.each {
          colmap[it] = new Integer(i++);
        }
      }
      else {
        row_information.rownum=ctr;
        row_information.rawValues = nl;
        result.log.add(row_information)

        // The process of matching column values to domain objects can result in objects being located or not. Sometimes, several columns may
        // be needed to look up a domain object. Once identified domain objects are put into locatedObjects
        // locatedObjects
        def locatedObjects = [:]
        locatedObjects << defaultLocatedObjects

        // We need to see if we can identify any existing domain objects which match the current row in the TSV.
        // We do this using the config.header.targetObjectIdentificationHeuristics list which contains a list of
        // column conjunctions.

        // Cycle through config.header.targetObjectIdentificationHeuristics
        config.header.targetObjectIdentificationHeuristics.each { toih ->
          // For this type [Instances of toih.cls] which will go into locatedObjects with key [toih.ref] iterate over all the
          // diferent ways we have to try and locate such instances. Stop when we have a match -- OR -- carry on and see if we have a conflict?
          def located_objects = []
          toih.heuristics.each { toih_heuristic ->
            // Each heuristic is a conjunction of properties
            log.debug("Trying to look up instance of ${toih.cls}");
            def o = locateDomainObject(config, toih, toih_heuristic, nl, locatedObjects, colmap);
            if ( ( o != null ) && ( o.size() == 1 ) ) {
              row_information.messages.add("Located instance of ${toih.cls} : ${o[0]}");
              located_objects.add(o[0]);
            }
          }

          if ( located_objects.size() == 1 ) {
            if ( locatedObjects[toih.ref] == null ) {
              row_information.messages.add("Located unique item for ${toih.ref} :: ${located_objects[0]}");
              locatedObjects[toih.ref] = located_objects[0]
            }
            else {
              // We already have an entry - what does config tell us - No special config == overwrite
              switch ( toih.onOverride ) {
                case 'reject':
                  break;
                case 'mustEqual':
                  // Converting Hibernate proxy to real entity object
                  // (Source: https://stackoverflow.com/questions/2216547/converting-hibernate-proxy-to-real-entity-object)
                  if (located_objects[0] == 0) {
                      throw new NullPointerException("Entity passed for initialization is null")
                  }
                  Hibernate.initialize(located_objects[0])
                  if (located_objects[0] instanceof  HibernateProxy) {
                      located_objects[0] = located_objects[0].getHibernateLazyInitializer().getImplementation()
                  }
                  if ( locatedObjects[toih.ref] == located_objects[0] ) {
                    log.debug("Located object matches existing object for ${toih.ref} - continue")
                  }
                  else {
                    row_information.messages.add("Row tried to set a different value for ${toih.ref} - but it is already set to ${locatedObjects[toih.ref]}");
                    row_information.error = true;
                  }
                  break;
                default:
                  break;
              }
            }
          }
          else if ( located_objects.size() > 1 ) {
            row_information.messages.add("Multiple items located for ${toih.ref}. ERROR");
            row_information.error = true;
          }
          else {
            row_information.messages.add("No domain objects located for ${toih.ref} - Check for create instruction");
            if ( toih.creation?.onMissing &&
                 meetsCriteria(config, toih.creation, locatedObjects,  nl, colmap, testRun, row_information ) ) {
              createDomainObject(config, toih, locatedObjects,  nl, colmap, testRun,row_information  );
            }
          }
        }

        log.debug("About to start creation rules :: pre flight")
        locatedObjects.each { key, value ->
          log.debug("Located ${key} -> ${value}")
        }

        // We have completed looking up any reference data, and perhaps created refdata along the way, now do the main
        // work of creating domain objects for this row
        config.header.creationRules.each { creation_rule ->
          if ( meetsCriteria(config, creation_rule, locatedObjects,  nl, colmap, testRun, row_information )) {
            createDomainObject(config, creation_rule, locatedObjects, nl, colmap, testRun,row_information  )
          }
          else {
          }
        }
      }
      ctr++
    }

    result
  }

  private def meetsCriteria(config, creation_rule, locatedObjects,  nl, colmap, testRun,row_information ) {
    def passed = true;
    def missingProps = []

    creation_rule.whenPresent?.each { rule ->
      log.debug("Checking rule ${rule}")
      switch( rule.type ) {
        case 'val':
          def theval = getColumnValue(config,colmap,nl,rule.colname)
          try {
            if ( ( theval == null ) || ( theval.trim().length() == 0 ) ) {
              passed = false
              missingProps.add("Column[${colmap[rule.colname]}] ${rule.colname} :: ${nl[colmap[rule.colname]]}")
            }
          }
          catch (NullPointerException e) {
            missingProps.add("Column ${rule.colname} entirely missing!")
          }
          break
        case 'ref':
          if ( locatedObjects[rule.refname] == null ) {
            passed = false;
            missingProps.add("Reference "+rule.refname+"::"+locatedObjects[rule.refname])
          }
          break
        case 'checkForOrgRoles':
          rule.values.each { v ->
            if (v.colname instanceof List) {
              v.colname.each { colname ->
                try {
                  if ( nl[colmap[colname]] != null ) {
                    qry_params[k] = getColumnValue(config,colmap,nl,colname)
                  }
                }
                catch (Exception e) {
                  log.info("Alternative column name ${colname} not present in map, skipping ...")
                }
              }
            }
            else if ( v.colname instanceof String && nl[colmap[v.colname]] != null ) {
              qry_params[k] = getColumnValue(config,colmap,nl,v.colname) // nl[colmap[v.colname]]
            }
            else {
              log.error("Missing parameter ${v.colname}");
              passed = false
            }
          }

          break
      }
      if ( ( !passed ) && ( rule.errorOnMissing ) ) {
        row_information.error = true;
      }
    }

    if ( passed )
      row_information.messages.add("Row passed whenPresent Check for ${creation_rule.ref} ")
    else {
      row_information.messages.add("Row failed whenPresent check for ${creation_rule.ref} - ${missingProps} not present")
    }

    return passed
  }

  /** Create a new domain object based on the config, values from the row,
  and other objects already located for this row */
  private def createDomainObject(config, toih, locatedObjects, nl, colmap, testRun, row_information ) {

    row_information.messages.add("Attempt to create instance of ${toih.cls} for ${toih.ref} ${testRun?'[Test Run]':'[Save]'}");
    def new_obj_cls = Class.forName(toih.cls)
    def new_obj = new_obj_cls.newInstance();
    def create_msg = "new(${toih.cls})("

    toih.creation.properties.each { pd ->
      switch ( pd.type ) {
        case 'ref':
          log.debug("Setting ${pd.property} on new ${toih.ref} to ${locatedObjects[pd.refname]}");
          def vl = locatedObjects[pd.refname];
          if ( vl != null ) {
            new_obj[pd.property] = vl;
            create_msg += pd.property + ":" + vl+' ';
          }
          break;
        case 'val':
          log.debug("Setting ${pd.property} on new ${toih.ref} to ${nl[colmap[pd.colname]]}");
          if ( ( nl[colmap[pd.colname]] != null ) && ( nl[colmap[pd.colname]].length() > 0 ) ) {
            def vl = convertString(getColumnValue(config,colmap,nl,pd.colname), pd.datatype);
            new_obj[pd.property] = vl;
            create_msg += pd.property + ":" + vl+' ';
          }
          break;
        case 'valueClosure':
          def created_value = pd.closure(colmap,nl,locatedObjects);
          new_obj[pd.property] = created_value
          create_msg += pd.property + ":" + created_value+' ';
          break;
        case 'closure':
          log.debug("Closure");
          pd.closure(new_obj, nl, colmap, pd.property, locatedObjects)
          break;
        defualt:
          log.debug("Unhandled create rule type ${pd.type}");
          break;
      }
    }

    create_msg += ")";

    if ( testRun ) {
      log.debug("Not saving - test run")
    }
    else {
      new_obj.save(flush:true, failOnError:true);
    }

    locatedObjects[toih.ref] = new_obj;

    row_information.messages.add(create_msg);

    return new_obj;
  }

  private def convertString(value,type) {
    def result = value;

    if ( type && value ) {
      switch ( type ) {
        case 'Double':
          result = Double.parseDouble(value);
          break;
        case 'Long':
          // result = Double.parseDouble(value);
          result = Long.parseLong(value)
          break;
        case 'date':
          result = parseDate(value)
          break;
        default:
          break;
      }
    }

    return result;
  }

  private static Date parseDate(String input) {
      List<SimpleDateFormat> possibleFormats = [new SimpleDateFormat('dd.MM.yyyy'),
                                                new SimpleDateFormat('dd.MM.yyyy HH:mm'),
                                                new SimpleDateFormat('dd/MM/yyyy'),
                                                new SimpleDateFormat('dd/MM/yyyy HH:mm'),
                                                new SimpleDateFormat('dd-MM-yyyy'),
                                                new SimpleDateFormat('dd-MM-yyyy HH:mm')]
      Date result
      possibleFormats.each { sdf ->
          try {
              result = sdf.parse(input)
          }
          catch (ParseException e) {
              log.info("Format ${sdf.toPattern()} not work, trying next one ...")
          }
      }
      result
  }

  private def locateDomainObject(config,toih, toih_heuristic, nl, locatedObjects, colmap) {
    // try to look up instances of toih.cls using the given heuristic
    def result = null;

    switch ( toih_heuristic.type ) {
      case 'simpleLookup' :
        def error = false;
        def qry_params = [:]
        def base_qry = "select i from ${toih.cls} as i where "
        boolean fc = true
        toih_heuristic.criteria.each { clause ->
          // iterate through each clause in the conjunction of clauses that might identify a domain object
          if ( fc ) { fc = false; } else { base_qry += " and " }

          switch ( clause.srcType ) {
            case 'col' :
              base_qry += "i.${clause.domainProperty} = :${clause.colname}"
              log.debug("${base_qry} ${colmap[clause.colname]}");
              qry_params.put(clause.colname,getColumnValue(config,colmap,nl,clause.colname))    //  nl[colmap[clause.colname]]);
              break;
            case 'ref' :
              if ( locatedObjects[clause.refname] != null ) {
                base_qry += "i.${clause.domainProperty} = :${clause.refname}"
                qry_params.put(clause.refname,locatedObjects[clause.refname]);
              }
              else {
                error = true;
              }
              break;
          }
        }
        if ( ! error ) {
          result = TitleInstance.executeQuery(base_qry,qry_params)
          log.debug("Lookup ${toih.ref} using ${base_qry} and params ${qry_params} result:${result}");
        }
        break;

      case 'hql' :
        //  hql: 'select o from Org as o join o.ids as id where id.ns.ns = :jcns and id.value = :orgId',
        // values : [ jcns : [type:'static', value:'JC'], orgId: [type:'column', colname:'InstitutionId'] ]
        def error = false;
        log.debug("HQL Lookup");
        def qry_params=[:]
        toih_heuristic.values.each { k, v ->
          switch ( v.type ) {
            case 'static':
              qry_params[k] = v.value
              break
            case 'column':
              if (v.colname instanceof List) {
                  v.colname.each { colname ->
                      try {
                          if ( nl[colmap[colname]] != null ) {
                            qry_params[k] = getColumnValue(config,colmap,nl,colname)
                          }
                      }
                      catch (Exception e) {
                          log.info("Alternative column name ${colname} not present in map, skipping ...")
                      }
                  }
              }
              else if ( v.colname instanceof String && nl[colmap[v.colname]] != null ) {
                qry_params[k] = getColumnValue(config,colmap,nl,v.colname) // nl[colmap[v.colname]]
              }
              else {
                log.error("Missing parameter ${v.colname}");
                error = true
              }
              break
            case 'defaultLookupObject':
              qry_params[k] = locatedObjects[v.key]
              break
          }
        }

        if ( !error ) {
          log.debug("HQL : ${toih_heuristic.hql}, ${qry_params}");
          result = TitleInstance.executeQuery(toih_heuristic.hql, qry_params);
        }
        break;

      default:
        log.debug("Unhandled heuristic type");
        break;
    }

    return result;
  }

  private def getColumnValue(config,colmap,nl,colname) {
    def col_cfg = config.cols.find { it.colname==colname }

    // Find out where this column appears in the uploaded document
    def position = -1
    position = colmap[colname]
    def value = null

    if ( position >= 0)
      value = nl[position]

    log.debug("getColumnValue cfg:${col_cfg} val:${value} colname:${colname}")

    if ( col_cfg ) {
      switch (col_cfg.type) {
        case 'vocab':
          def mapped_value = col_cfg.mapping[value]
          if ( mapped_value ) {
            log.debug("Mapping ${value} to ${mapped_value}")
            value = mapped_value
          }
          break;

      }
    }
    return value
  }
}
