package com.k_int.kbplus.abstract_domain

import com.k_int.kbplus.RefdataCategory
import com.k_int.properties.PropertyDefinition
import com.k_int.kbplus.License
import com.k_int.kbplus.RefdataValue
import javax.persistence.Transient

abstract class AbstractProperty implements Serializable{

    @Transient
    def controlledProperties = ['stringValue','intValue','decValue','refValue','paragraph','note', 'date']
    String          stringValue
    Integer         intValue
    BigDecimal      decValue
    RefdataValue    refValue
    String          note = ""
    Date            date
    
    static mapping = {
        note         type: 'text'
    }

    static constraints = {
        stringValue(nullable: true)
        intValue(nullable: true)
        decValue(nullable: true)
        refValue(nullable: true)
        note(nullable: true)
        date(nullable: true)
    }

    @Transient
    def getValueType(){
        if(stringValue) return "stringValue"
        if(intValue) return "intValue"
        if(decValue) return "decValue"
        if(refValue) return "refValue"
        if(date) return "refValue"
    }

    @Override
    public String toString(){
        if(stringValue) return stringValue
        if(intValue) return intValue.toString()
        if(decValue) return decValue.toString()
        if(refValue) return refValue.toString()
        if(date) return date.toString()
    }

    def copyValueAndNote(newProp){
        if(stringValue) newProp.stringValue = stringValue
        else if(intValue) newProp.intValue = intValue
        else if(decValue) newProp.decValue = decValue
        else if(refValue) newProp.refValue = refValue
        else if(date) newProp.refValue = date
        newProp.note = note
        newProp
    }

    def parseValue(value, type){

        def result
        switch (type){
            case Integer.toString():
                result = Integer.parseInt(value)
                break;
            case String.toString():
                result = value
                break;
            case BigDecimal.toString():
                result = new BigDecimal(value)
                break;
            case org.codehaus.groovy.runtime.NullObject.toString():
                result = null
                break;
            default:
                result = "AbstractProperty.parseValue failed"
        }
        return result
    }

    def setValue(value, type, rdc) {

        if (type == Integer.toString()) {
            intValue = parseValue(value, type)
        }
        else if (type == BigDecimal.toString()) {
            decValue = parseValue(value, type)
        }
        else if (type == String.toString()) {
            stringValue = parseValue(value, type)
        }
        else if (type == RefdataValue.toString()) {
            refValue = RefdataValue.findByOwnerAndValue(RefdataCategory.findByDesc(rdc), value.toString())
        }
    }

    public String getValue() {
        return toString()
    }
}
