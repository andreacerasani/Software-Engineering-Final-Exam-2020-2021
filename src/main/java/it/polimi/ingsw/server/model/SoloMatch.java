package it.polimi.ingsw.server.model;

import it.polimi.ingsw.common.messages.messagesToClient.RankUpdate;
import it.polimi.ingsw.server.model.comparators.CustomPlayerComparator;
import it.polimi.ingsw.server.model.enumerations.MatchPhase;
import it.polimi.ingsw.server.model.exceptions.InvalidParameterException;

import java.util.ArrayList;

/**
 * The match to handle a game with only one player that plays against Lorenzo
 */
public class SoloMatch extends Match{
    private Lorenzo lorenzo;
    private ArrayList<RankPosition> finalRank;
    private boolean win;

    /**
     * This constructor creates a match deserializing all the leader cards and creating the market and the card grid
     * @param matchID an int that identifies the match
     */
    public SoloMatch(int matchID, boolean demo) throws InvalidParameterException {
        super(matchID, 1,demo);
        lorenzo = new Lorenzo(this);
        lorenzo.addObserver(this);
        lorenzo.addObserverList(this.getMessageObservers());
    }

    /**
     * Activates the action of Lorenzo
     */
    @Override
    public void nextPlayer() {
        if(getMatchPhase() == MatchPhase.STANDARDROUND)
            lorenzo.draw();
        super.nextPlayer();
    }

    /**
     * Move the faithMarker of Lorenzo
     * @param positions is the number of steps to make on the faith track for each player
     */
    @Override
    public void moveFaithMarkerAll(int positions) {
        lorenzo.moveFaithMarker(positions);
    }

    /**
     * Method called when the conditions to end the match are verified
     * @param message true if lorenzo won the match, false otherwise
     */
    @Override
    public void update(boolean message) {
        super.update(message);
        win = message;
    }

    /**
     * This method ends the game sending to client if the winner is lorenzo or the player
     */
    @Override
    public void endGame() {
        if(win){
            finalRank = new ArrayList<>();
            finalRank.add(new RankPosition("Lorenzo",0));
            notifyObservers(new RankUpdate(finalRank));
        }
        else {
            super.endGame();
        }
    }

    /**
     * Getter for testing purpose
     * @return lorenzo
     */
    public Lorenzo getLorenzo() {
        return lorenzo;
    }
}
