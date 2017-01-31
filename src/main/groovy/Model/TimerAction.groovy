package Model

import com.fasterxml.jackson.annotation.JsonProperty
import groovy.sql.GroovyRowResult

/**
 * Created by NIKL
 */
public class TimerAction {
    private final int id;
    private final String lastRequestedTimerStart;

    TimerAction(@JsonProperty("id") int id,
         @JsonProperty("lastRequestedTimerStart") String lastRequestedTimerStart) {
       this.id = Integer.valueOf(id)
       this.lastRequestedTimerStart = lastRequestedTimerStart
    }

    public TimerAction(GroovyRowResult row) {
        this.id = Integer.valueOf(row.getProperty("id"))
        this.lastRequestedTimerStart = row.getProperty("lastRequestedTimerStart")
    }

    String getLastRequestedTimerStart() {
        return lastRequestedTimerStart
    }

    int getId() {
        return id
    }
}
