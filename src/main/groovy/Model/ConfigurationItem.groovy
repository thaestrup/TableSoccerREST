package Model

import com.fasterxml.jackson.annotation.JsonProperty
import groovy.sql.GroovyRowResult

/**
 * Created by NIKL on 2016-01-15.
 */
public class ConfigurationItem {
    private final String name;
    private final String value;

    public ConfigurationItem(@JsonProperty("name") String name,
                  @JsonProperty("value") String value) {
        this.name = name;
        this.value = value;
    }

    public ConfigurationItem(GroovyRowResult row) {
        if (row.containsKey("name")) {
            this.name = row.getProperty("name");
        }
        if (row.containsKey("value")) {
            this.value = row.getProperty("value");
        }

    }

    String getName() {
        return name
    }

    String getValue() {
        return value
    }


}
