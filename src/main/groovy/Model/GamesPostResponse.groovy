package Model

/**
 * Created by super on 27/11/2016.
 */
class GamesPostResponse {
    private List<String> newGameIDs = new LinkedList<>();

    List<String> getNewGameIDs() {
        return newGameIDs
    }

    void setNewGameIDs(List<String> newGameIDs) {
        this.newGameIDs = newGameIDs
    }

    public void add(String gameId) {
        newGameIDs.add(gameId);
    }
}
