package Model;

/**
 * Created by super on 04/12/2016.
 */
public class PointsPrPlayerPlayer {
    private int position;
    private int points;
    private int numberOfGames;
    private String name;

    public PointsPrPlayerPlayer(int position, int points, int numberOfGames, String name) {
        this.position = position;
        this.points = points;
        this.numberOfGames = numberOfGames;
        this.name = name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getNumberOfGames() {
        return numberOfGames;
    }

    public void setNumberOfGames(int numberOfGames) {
        this.numberOfGames = numberOfGames;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
