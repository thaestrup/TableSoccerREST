package Model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by super on 04/12/2016.
 */
public class PointsPrPlayerRequest {
    private int winnerPoints;
    private int loserPoints;
    private int evenPoints;

    public PointsPrPlayerRequest(@JsonProperty("winnerPoints")  int winnerPoints,
                                 @JsonProperty("loserPoints") int loserPoints,
                                 @JsonProperty("evenPoints") int evenPoints) {
        this.winnerPoints = winnerPoints;
        this.loserPoints = loserPoints;
        this.evenPoints = evenPoints;
    }

    public int getWinnerPoints() {
        return winnerPoints;
    }

    public void setWinnerPoints(int winnerPoints) {
        this.winnerPoints = winnerPoints;
    }

    public int getLoserPoints() {
        return loserPoints;
    }

    public void setLoserPoints(int loserPoints) {
        this.loserPoints = loserPoints;
    }

    public int getEvenPoints() {
        return evenPoints;
    }

    public void setEvenPoints(int evenPoints) {
        this.evenPoints = evenPoints;
    }
}
