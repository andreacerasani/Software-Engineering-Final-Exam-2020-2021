package it.polimi.ingsw.client.LocalModel;

import it.polimi.ingsw.server.model.enumerations.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Player {
    String nickname;
    ArrayList <Integer> leaderCards;
    FaithTrack faithTrack;
    DevelopmentCardSpace developmentCardSpace;
    WareHouseDepots wareHouseDepots;
    Strongbox strongbox;

    public void setRedCrossPosition(int redcrossPosition){

    }
    public void setPopeFavourTiles(ArrayList<Integer> popeFavourTiles){

    }
    public void setDevelopmentCardSpace(ArrayList<ArrayList<Integer>> developmentCardSpace){

    }
    public void setWareHouseDepots(List<Map<Resource, Integer>> depots){

    }
    public void setStrongbox(Map<Resource, Integer> strongbox){

    }
    public void setLeaderCards(ArrayList<Integer> id){

    }

    private void printPersonalBoards(){

    }


}
