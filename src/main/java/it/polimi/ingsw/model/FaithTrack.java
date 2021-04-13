package it.polimi.ingsw.model;

import java.util.ArrayList;

/**
 *this class represents the faith track of a player with the positions of the red cross and the tiles status
*/
public class FaithTrack {
    /**
     * tiles values:
     * 0 - value before vatican report
     * 1 - value after vatican report if the tile doesn't turn
     * 2 - value after vatican report if the tile turn     * */
    private int faithTrackPosition;
    private final int numOfPopeFavourTiles = 3;
    private final int nonActive = 0;
    private final int nonActivated = 1;
    private final int active = 2;
    public ArrayList<Integer> popeFavourTiles;

    /**
     * this constructor sets the position of the red cross to 0 and each tiles as nonActive
     */
    public FaithTrack() {
        faithTrackPosition = 0;
        popeFavourTiles = new ArrayList<>(numOfPopeFavourTiles);
        for(int tile = 0; tile < numOfPopeFavourTiles; tile++)
            popeFavourTiles.add(nonActive);
    }

    /**
     * this method returns the position in the faith track
     * @return an int representing the position of the red cross in the faith track
     */
    public int getFaithTrackPosition(){
        return faithTrackPosition;
    }

    /**
     * this method moves forward the red cross
     * @param numOfSteps represents the amount of movements that the red cross has to do on the faith track
     */
    public void moveFaithMarker(int numOfSteps){
        assert  numOfSteps >= 0;
        faithTrackPosition = faithTrackPosition + numOfSteps;
        if (faithTrackPosition > 20)        // the maximum amount of space in the track is 20
            faithTrackPosition = 20;
    }

    /**
     * this method is implemented for testing purpose
     * @param tileNumber represents the number of the tile
     * @return an int representing the value of the tile
     */
    public int getPopeFavourTileValue(int tileNumber){
        assert tileNumber > 0 && tileNumber < 4;
        tileNumber = tileNumber - 1;
        return popeFavourTiles.get(tileNumber);
    }


    /**
     * this method returns the status of a give tile
     * @param tileNumber represents the number of the tile
     * @return true if the tile value is 1 o 2, false if the tile value is 0
     */
    public boolean getPopeFavourTile(int tileNumber){
        assert tileNumber > 0 && tileNumber < 4;
        tileNumber = tileNumber - 1;
        boolean isTileActive = false;
        int tileValue = popeFavourTiles.get(tileNumber);
        if(tileValue ==  1 || tileValue == 2)
            isTileActive = true;
        return isTileActive;
    }

    /**
     * this method represents the activation of the vatican report on a specific tile,
     * it checks the position of the red cross on the faith path
     * @param tileNumber represents the number of the tile
     */
    public void setPopeFavourTiles(int tileNumber){
        assert tileNumber > 0 && tileNumber < 4;
        tileNumber = tileNumber - 1;
        int firstValueInterval = 4 + (7 * tileNumber);
        int secondValueInterval = 9 + (8 * tileNumber);
        if(popeFavourTiles.get(tileNumber) == nonActive) {
            if (faithTrackPosition > firstValueInterval && faithTrackPosition < secondValueInterval)
                popeFavourTiles.set(tileNumber, active);
            else
                popeFavourTiles.set(tileNumber, nonActivated);
        }
    }

    /**
     * This method calculates victory points from the position of the red cross and the tiles status
     * @return an int representing the VPs associated to the faith track
     */
    public int calculateVictoryPoints(){
        return calculateVPfromFaithPoints() + calculateVPfromTiles();
    }

    private int calculateVPfromFaithPoints(){
        //rounds down faithpoints to the maximum multiple of 3 and then assings victory points with a switch case
        int vp;
        int roundedFaithPoints = faithTrackPosition - (faithTrackPosition % 3);
        switch (roundedFaithPoints){
            case 3: vp = 1;
                break;
            case 6: vp = 2;
                break;
            case 9: vp = 4;
                break;
            case 12: vp = 6;
                break;
            case 15: vp = 9;
                break;
            case 18: vp = 12;
                break;
            case 21: vp = 16;
                break;
            case 24: vp = 20;
                break;
            default: vp = 0;
        }
        return vp;
    }

    private int calculateVPfromTiles(){
        int vp=0;
        //iterating all the tiles
        for(int tileNumber = 0; tileNumber < 3; tileNumber++){
            if(popeFavourTiles.get(tileNumber) == active)   //only active tiles contributes to the victory points calculations
                vp = vp + (tileNumber + 2);     //(tileNumber + 2) are VP that you get from an active tile
        }
        return vp;
    }

}