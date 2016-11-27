package Model

/**
 * Created by super on 27/11/2016.
 */
class GamesPostResponse {
    private String generationMethod;
    private List<String> newGameIDs = new LinkedList<>();

    String getGenerationMethod() {
        return generationMethod
    }

    void setGenerationMethod(String generationMethod) {
        this.generationMethod = generationMethod
    }

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
