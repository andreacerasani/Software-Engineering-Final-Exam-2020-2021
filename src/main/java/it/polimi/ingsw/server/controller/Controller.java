package it.polimi.ingsw.server.controller;

import it.polimi.ingsw.common.messages.messagesToClient.ErrorMessage;
import it.polimi.ingsw.common.messages.messagesToClient.FirstConnectedUpdate;
import it.polimi.ingsw.common.View;
import it.polimi.ingsw.common.messages.messagesToClient.MainTurnActionDoneUpdate;
import it.polimi.ingsw.common.utils.observe.MessageObservable;
import it.polimi.ingsw.server.Lobby;
import it.polimi.ingsw.server.model.Match;
import it.polimi.ingsw.server.model.PersonalBoard;
import it.polimi.ingsw.server.model.SoloMatch;
import it.polimi.ingsw.server.model.enumerations.MatchPhase;
import it.polimi.ingsw.server.model.enumerations.PersonalBoardPhase;
import it.polimi.ingsw.server.model.enumerations.Resource;
import it.polimi.ingsw.server.model.exceptions.*;
import java.util.Map;

/**
 * Class that receives messages from the view and modifies the state of the state of the model
 */
public class Controller extends MessageObservable{
    private Match match;
    private Boolean firstConnected;
    private final Boolean demo;
    private Lobby lobby;

    /**
     * Constructor for the online game implementing multiple matches
     * @param demo true to enable the demo mode
     * @param lobby handles the players that connects and the creation of multiple matches
     */
    public Controller(Boolean demo, Lobby lobby) {
        this(demo);
        this.lobby = lobby;
    }

    /**
     * Constructor for the solo game controller
     * @param demo true to enable the demo mode
     */
    public Controller(Boolean demo){
        this.demo = demo;
        firstConnected = false;
    }

    /**
     * Handles the connections from the clients
     * @param view the view associated with the new client
     */
    public synchronized void newConnection(View view){
        this.addObserver(view);
        if(!firstConnected){
            firstConnected = true;
            view.update(new FirstConnectedUpdate(true));
        }
        else if (match != null)
            view.update(new FirstConnectedUpdate(false));
        else {
            assert false;
            view.update(new ErrorMessage(null, "The first connected player is choosing the number of players. Wait..."));
        }
    }

    /**
     * Method to know if the first player of a match already connected
     * @return true if the first player of a match already connected
     */
    public Boolean hasFirstConnected() {
        return firstConnected;
    }

    /**
     * Method to know if a match waiting for players already exist
     * @return true if a match waiting for players already exist
     */
    public Boolean isNumOfPlayerChosen(){
        return match != null;
    }

    /**
     * Handles a nicknameReplyMessage
     * @param nickname is the nickname to add to the match
     * @param view is the view of the client that sends the message
     */
    public synchronized void handleNicknameReplyMessage(String nickname, View view){
        try {
            if(match.getMatchPhase() == MatchPhase.SETUP) {
                match.addPlayer(nickname,view);
            }
            else{
                view.update(new ErrorMessage(nickname, "Match has already started"));
            }
        } catch (InvalidNickName invalidNickName) {
            view.update(new ErrorMessage(nickname, invalidNickName.getMessage()));
        }
    }

    /**
     * Handles a createMatchReplyMessage
     * @param numOfPlayers is the number of players that the new match will require to start
     * @param nickname is the nickname of the first player
     * @param view is the view of the client that sends the message
     */
    public synchronized void handleCreateMatchReplyMessage(int numOfPlayers,String nickname, View view){
        try {
            if(match == null){
                if(numOfPlayers == 1)
                    match = new SoloMatch(1,demo);
                else
                    match = new Match(1,numOfPlayers,demo);
                //creates a player
                handleNicknameReplyMessage(nickname,view);
                firstConnected = true;

                //Notify waiting clients that the number of players has been chosen
                if(lobby!= null) lobby.notifyNumOfPlayers(numOfPlayers);
            }
            else{
                view.update(new ErrorMessage(nickname, "Invalid command"));
            }
        }catch (InvalidParameterException exception) {
            view.update(new ErrorMessage(nickname, exception.getMessage()));
        }
    }

    /**
     * Handles an ActivateLeaderMessage
     * @param numLeaderCard the number of the leader card to activate
     * @param nickname the nickname of the player that wants to activate the card
     * @param view is the view of the client that sends the message
     */
    public synchronized void handleActivateLeader(int numLeaderCard, String nickname, View view){
        try {
            if(match.getCurrentPlayer().getNickname().equals(nickname)
                && (match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.MAIN_TURN_ACTION_AVAILABLE
                    || match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.MAIN_TURN_ACTION_DONE)){
                match.getCurrentPlayer().getPersonalBoard().activateLeader(numLeaderCard);
            }
            else
                view.update(new ErrorMessage(getNickname(),"wrong phase"));
        } catch (InvalidParameterException | RequirementNotMetException exception) {
            view.update(new ErrorMessage(nickname, exception.getMessage()));
        }
    }

    /**
     * Handles a TakeFromMarketMessage
     * @param rowOrColumn 0 if row, 1 if column
     * @param value from 0 to 2 if row, from 0 to 3 if column
     * @param nickname is the nickname of the player that sends the message
     * @param view is the view of the client that sends the message
     */
    public synchronized void handleTakeFromMarketMessage(int rowOrColumn, int value,String nickname, View view){
        try{
            if((match.getMatchPhase() == MatchPhase.STANDARDROUND || match.getMatchPhase() == MatchPhase.LASTROUND)
                    && match.getCurrentPlayer().getNickname().equals(nickname)
                    && match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.MAIN_TURN_ACTION_AVAILABLE){
                match.getCurrentPlayer().getPersonalBoard().takeFromMarket(rowOrColumn,value);
            }
            else{
                view.update(new ErrorMessage(nickname, "Invalid command"));
            }
        } catch (InvalidParameterException exception) {
            view.update(new ErrorMessage(nickname, exception.getMessage()));
        }
    }

    /**
     * Handles a TransformWhiteMarblesMessage
     * @param leaderCard is the number of the card to use to transform marbles
     * @param numOfTransformations is the number of marbles that needs to be transformed
     * @param nickname is the nickname of the player that sends the message
     * @param view is the view of the client that sends the message
     */
    public synchronized void handleTransformWhiteMarblesMessage(int leaderCard,int numOfTransformations, String nickname, View view){
        try{
            if((match.getMatchPhase() == MatchPhase.STANDARDROUND || match.getMatchPhase() == MatchPhase.LASTROUND)
                    && match.getCurrentPlayer().getNickname().equals(nickname)
                    && match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.TAKE_FROM_MARKET){
                match.getCurrentPlayer().getPersonalBoard().transformWhiteMarble(leaderCard,numOfTransformations);
            }
            else{
                view.update(new ErrorMessage(nickname, "Invalid command"));
            }
        } catch (InvalidLeaderAction | InvalidParameterException | NotEnoughWhiteMarblesException invalidLeaderAction) {
            view.update(new ErrorMessage(nickname, invalidLeaderAction.getMessage()));
        }
    }

    /**
     * Handles a TransformMarblesMessage
     * @param view is the view of the client that sends the message
     * @param nickname is the nickname of the player that sends the message
     */
    public synchronized void handleTransformMarblesMessage(View view, String nickname){
        if((match.getMatchPhase() == MatchPhase.STANDARDROUND || match.getMatchPhase() == MatchPhase.LASTROUND)
                && match.getCurrentPlayer().getNickname().equals(nickname)
                && match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.TAKE_FROM_MARKET){
            match.getCurrentPlayer().getPersonalBoard().transformMarbles();
        }
        else{
            view.update(new ErrorMessage(nickname, "Invalid command"));
        }
    }

    /**
     * Method used to discard the two leader card selected by the player
     * @param view The view of the player whose leader cards have to be discarded
     * @param nickname The nickname of the player
     * @param indexLeaderCard1 The index of one of the two leader card to discard
     * @param indexLeaderCard2 The index of one of the two leader card to discard
     */
    public synchronized void handleDiscardInitialLeaderMessage(View view, String nickname, int indexLeaderCard1, int indexLeaderCard2){
        if (match.getMatchPhase() != MatchPhase.LEADERCHOICE){
            view.update(new ErrorMessage(nickname, "Invalid command"));
            return;
        }

        PersonalBoard personalBoard = null;
        try {
            personalBoard = match.getPlayer(nickname).getPersonalBoard();
        } catch (InvalidNickName invalidNickName) {
            view.update(new ErrorMessage(nickname, invalidNickName.getMessage()));
        }

        if(personalBoard.getPersonalBoardPhase() != PersonalBoardPhase.LEADER_CHOICE){
            view.update(new ErrorMessage(nickname, "Invalid command"));
            return;
        }

        try {
            personalBoard.discardInitialLeader(indexLeaderCard1, indexLeaderCard2);
        } catch (InvalidParameterException e) {
            view.update(new ErrorMessage(nickname, "Invalid command"));
        }
    }

    /**
     * Method used to communicate to the model the resources chosen by player
     * @param view View of the player who has chosen the resources
     * @param nickname Nickname of the player
     * @param resourceIntegerMap Map of the resources and their quantities chosen by player
     */
    public synchronized void handleChooseInitialResourcesMessage(View view, String nickname, Map<Resource, Integer> resourceIntegerMap){
        if (match.getMatchPhase() != MatchPhase.RESOURCECHOICE){
            view.update(new ErrorMessage(nickname, "Invalid command"));
            return;
        }
        PersonalBoard personalBoard = null;
        try {
            personalBoard = match.getPlayer(nickname).getPersonalBoard();
        } catch (InvalidNickName invalidNickName) {
            view.update(new ErrorMessage(nickname, invalidNickName.getMessage()));
        }

        if(personalBoard.getPersonalBoardPhase() != PersonalBoardPhase.RESOURCE_CHOICE){
            view.update(new ErrorMessage(nickname, "Invalid command"));
            return;
        }

        try {
            personalBoard.addInitialResources(resourceIntegerMap);
        } catch (InvalidParameterException e) {
            view.update(new ErrorMessage(nickname, e.getMessage()));
        }
    }

    /**
     * Handles an AddToWarehouseMessage
     * @param view is the view of the client that sends the message
     * @param nickname the nickname of the player
     * @param depotLevel the depot to add the resources
     * @param singleResourceMap the map with the resource and the quantity to add to the depot
     */
    public synchronized void handleAddToWarehouseMessage(View view, String nickname, int depotLevel, Map<Resource,Integer> singleResourceMap){
        PersonalBoard personalBoard;
        try {
            personalBoard = match.getPlayer(nickname).getPersonalBoard();
        } catch (InvalidNickName invalidNickName) {
            view.update(new ErrorMessage(nickname, invalidNickName.getMessage()));
            return;
        }

        if(match.getMatchPhase() == MatchPhase.STANDARDROUND
                || match.getMatchPhase() == MatchPhase.LASTROUND
                || (match.getMatchPhase() == MatchPhase.RESOURCECHOICE && personalBoard.getPersonalBoardPhase()== PersonalBoardPhase.ADD_INITIAL_RESOURCES)){
            try {
                personalBoard.addToWarehouseDepots(depotLevel, singleResourceMap);
            } catch (InvalidAdditionException e) {
                view.update(new ErrorMessage(nickname, e.getMessage()));
            }
        }
        else{
            view.update(new ErrorMessage(nickname, "Invalid phase"));
        }
    }

    /**
     * Handles a SwapMessage
     * @param view is the view of the client that sends the message
     * @param nickname nickname of the player
     * @param depot1 the first depot to swap
     * @param depot2 the second depot to swap
     */
    public synchronized void handleSwapMessage(View view, String nickname, int depot1, int depot2){
        if(match.getMatchPhase() == MatchPhase.STANDARDROUND || match.getMatchPhase() == MatchPhase.LASTROUND){
            try {
                match.getPlayer(nickname).getPersonalBoard().swapResourceStandardDepot(depot1,depot2);
            } catch (InvalidSwapException | InvalidNickName e) {
                view.update(new ErrorMessage(nickname, e.getMessage()));
            }
        }
        else{
            view.update(new ErrorMessage(nickname, "Invalid command in this phase of the match"));
        }

    }

    /**
     * Handles the MoveMessage
     * @param view is the view of the client that sends the message
     * @param nickname the nickname of the player
     * @param sourceDepotNumber the depot to use to take resources
     * @param destinationDepotNumber the depot used to deposit the resources
     * @param quantity the quantity of resources to move
     */
    public synchronized void handleMoveMessage(View view, String nickname, int sourceDepotNumber, int destinationDepotNumber, int quantity ){
        if(match.getMatchPhase() == MatchPhase.STANDARDROUND || match.getMatchPhase() == MatchPhase.LASTROUND){
            try {
                match.getPlayer(nickname).getPersonalBoard().moveResourceSpecialDepot(sourceDepotNumber,destinationDepotNumber,quantity);
            } catch (InvalidNickName | InvalidAdditionException | InvalidRemovalException | InvalidMoveException e) {
                view.update(new ErrorMessage(nickname, e.getMessage()));
            }
        }
        else{
            view.update(new ErrorMessage(nickname, "Invalid command in this phase of the match"));
        }
    }

    /**
     * Handle a discardLeaderMessage
     * @param view is the view of the client that sends the message
     * @param nickname the nickname of the player
     * @param numLeaderCard the number of the leader card to discard
     */
    public synchronized void handleDiscardLeaderMessage(View view, String nickname, int numLeaderCard){
        if((match.getMatchPhase() == MatchPhase.STANDARDROUND || match.getMatchPhase() == MatchPhase.LASTROUND)
            && match.getCurrentPlayer().getNickname().equals(nickname)
            && (match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.MAIN_TURN_ACTION_AVAILABLE
                || match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.MAIN_TURN_ACTION_DONE)){
            try {
                match.getCurrentPlayer().getPersonalBoard().removeLeader(numLeaderCard);
            } catch (InvalidParameterException e) {
                view.update(new ErrorMessage(nickname, e.getMessage()));
            }
        }
        else{
            view.update(new ErrorMessage(nickname, "Invalid command in this phase of the match"));
        }
    }

    /**
     * Handles an ActivateCardProductionMessage
     * @param view is the view of the client that sends the message
     * @param nickname the nickname of the player
     * @param costStrongbox a map of resources to get from the strongbox
     * @param costWarehouseDepot a map of resources to get from the warehouse depots
     * @param indexDevelopmentCardSpace the space of the development card space to take the power of production from. Ranges from 1 to 3
     */
    public synchronized void handleActivateCardProductionMessage(View view, String nickname, Map<Resource,Integer> costStrongbox, Map<Resource,Integer> costWarehouseDepot, int indexDevelopmentCardSpace){
        if((match.getMatchPhase() == MatchPhase.STANDARDROUND || match.getMatchPhase() == MatchPhase.LASTROUND)
                && match.getCurrentPlayer().getNickname().equals(nickname)
                && match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.MAIN_TURN_ACTION_AVAILABLE
                || match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.PRODUCTION){
            try {
                match.getCurrentPlayer().getPersonalBoard().activateCardProduction(costStrongbox,costWarehouseDepot,indexDevelopmentCardSpace);
            } catch (InvalidProductionException | InvalidParameterException | InvalidCostException | InvalidRemovalException e) {
                view.update(new ErrorMessage(nickname, e.getMessage()));
            }
        }
        else {
            view.update(new ErrorMessage(nickname, "Invalid command in this phase of the match"));
        }
    }

    /**
     * Handles a ActivateBasicProductionMessage
     * @param view is the view of the client that sends the message
     * @param nickname the nickname of the player
     * @param costStrongbox a map of resources to get from the strongbox
     * @param costWarehouseDepot a map of resources to get from the warehouse depots
     * @param resource the type of the resources to produce
     */
    public synchronized void handleActivateBasicProductionMessage(View view, String nickname, Map<Resource,Integer> costStrongbox, Map<Resource,Integer> costWarehouseDepot, Resource resource){
        if((match.getMatchPhase() == MatchPhase.STANDARDROUND || match.getMatchPhase() == MatchPhase.LASTROUND)
                && match.getCurrentPlayer().getNickname().equals(nickname)
                && (match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.MAIN_TURN_ACTION_AVAILABLE
                || match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.PRODUCTION)){
            try {
                match.getCurrentPlayer().getPersonalBoard().activateBasicProduction(costStrongbox,costWarehouseDepot,resource);
            } catch (InvalidProductionException | InvalidCostException | InvalidRemovalException e) {
                view.update(new ErrorMessage(nickname, e.getMessage()));
            }
        }
        else {
            view.update(new ErrorMessage(nickname, "Invalid command in this phase of the match"));
        }
    }

    /**
     * Handles an ActivateLeaderProductionMessage
     * @param view is the view of the client that sends the message
     * @param nickname the nickname of the player
     * @param costStrongbox a map of resources to get from the strongbox
     * @param costWarehouseDepot a map of resources to get from the warehouse depots
     * @param numLeaderCard the number of the leader card to use
     * @param resource the type of resource to produce
     */
    public synchronized void handleActivateLeaderProductionMessage(View view, String nickname, Map<Resource,Integer> costStrongbox, Map<Resource,Integer> costWarehouseDepot, int numLeaderCard, Resource resource){
        if((match.getMatchPhase() == MatchPhase.STANDARDROUND || match.getMatchPhase() == MatchPhase.LASTROUND)
                && match.getCurrentPlayer().getNickname().equals(nickname)
                && (match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.MAIN_TURN_ACTION_AVAILABLE
                || match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.PRODUCTION)){
            try {
                match.getCurrentPlayer().getPersonalBoard().activateLeaderProduction(costStrongbox,costWarehouseDepot,numLeaderCard,resource);
            } catch (InvalidProductionException | InvalidCostException | InvalidLeaderAction | InvalidRemovalException e) {
                view.update(new ErrorMessage(nickname, e.getMessage()));
            }
        }
        else {
            view.update(new ErrorMessage(nickname, "Invalid command in this phase of the match"));
        }
    }

    /**
     * Handles an EndProductionMessage
     * @param view is the view of the client that sends the message
     * @param nickname the nickname of the player
     */
    public synchronized void handleEndProductionMessage(View view, String nickname){
        if((match.getMatchPhase() == MatchPhase.STANDARDROUND || match.getMatchPhase() == MatchPhase.LASTROUND)
                && match.getCurrentPlayer().getNickname().equals(nickname)
                && match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.PRODUCTION) {
            match.getCurrentPlayer().getPersonalBoard().endProduction();
            view.update(new MainTurnActionDoneUpdate(nickname));
        }
        else{
            view.update(new ErrorMessage(nickname, "Invalid command in this phase of the match"));
        }
    }

    /**
     * Handle a BuyDevelopmentCardMessage
     * @param view is the view of the client that sends the message
     * @param nickname the nickname of the player
     * @param row the row of the card in the card grid
     * @param column the column of the card in the card grid
     * @param costStrongbox a map of resources to take from the strongbox
     * @param costWarehouse a map of resources to take from the warehouse depot
     * @param numLeaderCard the number of the leader card used to discount the cost of the development card
     * @param cardPosition the position of the development card space to put the bought card
     */
    public synchronized void handleBuyDevelopmentCardMessage(View view, String nickname, int row, int column, Map<Resource, Integer> costStrongbox, Map<Resource, Integer> costWarehouse, int numLeaderCard, int cardPosition){
        try {
            PersonalBoard personalBoard = match.getPlayer(nickname).getPersonalBoard();
            if((match.getMatchPhase() == MatchPhase.STANDARDROUND || match.getMatchPhase() == MatchPhase.LASTROUND)
                    && match.getCurrentPlayer().getNickname().equals(nickname)
                    && match.getCurrentPlayer().getPersonalBoard().getPersonalBoardPhase() == PersonalBoardPhase.MAIN_TURN_ACTION_AVAILABLE){
                personalBoard.buyDevelopmentCard(row, column, costStrongbox, costWarehouse, numLeaderCard, cardPosition);
            }
            else{
                view.update(new ErrorMessage(nickname, "Invalid command"));
            }
        } catch (NoCardException | InvalidParameterException | InvalidCostException | InvalidLeaderAction | InvalidRemovalException | InvalidDevelopmentCardException | InvalidNickName e) {
            view.update(new ErrorMessage(nickname, e.getMessage()));
        }
    }

    /**
     * Handles an EndTurnMessage
     * @param view is the view of the client that sends the message
     * @param nickname the nickname of the player
     */
    public synchronized void handleEndTurnMessage(View view, String nickname){
        if (!match.getCurrentPlayer().getNickname().equals(nickname)){
            view.update(new ErrorMessage(nickname, "Not your turn"));
            return;
        }
        PersonalBoard personalBoard = null;
        try {
            personalBoard = match.getPlayer(nickname).getPersonalBoard();
        } catch (InvalidNickName invalidNickName) {
            view.update(new ErrorMessage(nickname, invalidNickName.getMessage()));
        }
        if (!personalBoard.getPersonalBoardPhase().equals(PersonalBoardPhase.MAIN_TURN_ACTION_DONE)){
            view.update(new ErrorMessage(nickname, "You haven't done your main turn action yet"));
            return;
        }
        personalBoard.endTurn();
        match.nextPlayer();
    }

    /**
     * Handles a DiscardResourcesFromMarketMessage
     * @param view is the view of the client that sends the message
     * @param nickname the nickname of the player
     */
    public synchronized void handleDiscardResourcesFromMarket(View view, String nickname) {
        if(match.getCurrentPlayer().getNickname().equals(nickname)){
            try {
                match.getPlayer(nickname).getPersonalBoard().discardResourcesFromMarket();
            } catch (InvalidNickName invalidNickName) {
                view.update(new ErrorMessage(nickname, invalidNickName.getMessage()));
            }
        }
        else
            view.update(new ErrorMessage(nickname, "Not your turn"));
    }
}
