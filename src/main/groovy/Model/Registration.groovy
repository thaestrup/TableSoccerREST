package Model

import com.fasterxml.jackson.annotation.JsonProperty
import groovy.sql.GroovyRowResult

/**
 * Created by NIKL
 */
public class Registration {
    private final String RFIDTag;

    Registration(@JsonProperty("RFIDTag") String RFIDTag) {
       this.RFIDTag = RFIDTag
    }

    public Registration(GroovyRowResult row) {
        this.RFIDTag = row.getProperty("RFIDTag")
    }

    String getRFIDTag() {
        return RFIDTag
    }
}
