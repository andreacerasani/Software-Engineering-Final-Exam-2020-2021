package it.polimi.ingsw.server.model;

import it.polimi.ingsw.common.InitialLeaderCardsUpdate;
import it.polimi.ingsw.common.utils.observe.MessageObservable;
import it.polimi.ingsw.server.model.enumerations.Marble;
import it.polimi.ingsw.server.model.enumerations.PersonalBoardPhase;
import it.polimi.ingsw.server.model.enumerations.Resource;
import it.polimi.ingsw.server.model.exceptions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PersonalBoard extends MessageObservable {
    private Player myPlayer;
    private final int TOTPOWERPRODUCTIONS = 6;
    private int victoryPoints;
    private HashMap<Marble,Integer> temporaryMarbles;
    private HashMap<Resource, Integer> temporaryMapResource;
    private ArrayList<LeaderCard> leaderCards;
    private FaithTrack faithTrack;
    private Match match;
    private Strongbox strongbox;
    private WarehouseDepots warehouseDepots;
    private Market market;
    private CardGrid cardGrid;
    private DevelopmentCardSpace developmentCardSpace;
    //Array of booleans used to check if a powerOfProduction has already been used in the same turn
    private Boolean[] powerOfProductionUsed = new Boolean[TOTPOWERPRODUCTIONS];
    private PersonalBoardPhase personalBoardPhase;
    private int numOfResourcesToChoose;

    public PersonalBoard(ArrayList<LeaderCard> leaderCards, Match match) {
        this.victoryPoints = 0;
        this.market = match.getMarket();
        this.cardGrid = match.getCardGrid();
        this.leaderCards = leaderCards;
        this.faithTrack = new FaithTrack();
        faithTrack.addObserver(match);
        this.developmentCardSpace = new DevelopmentCardSpace();
        developmentCardSpace.addObserver(match);
        this.match = match;
        this.strongbox = new Strongbox();
        this.warehouseDepots = new WarehouseDepots();
        this.temporaryMapResource = new HashMap<>();
        this.temporaryMarbles = new HashMap<>();
        Arrays.fill(this.powerOfProductionUsed, false);
        //TODO when the turn ends set power of production to null again
        numOfResourcesToChoose = 0;
        personalBoardPhase = PersonalBoardPhase.LEADER_CHOICE;
    }

    //Method used to check if the merged maps of cost strongbox and cost warehouseDepot are equal to costToPay
    private void mergeCostsAndVerify(HashMap<Resource,Integer> costStrongbox, HashMap<Resource,Integer> costWarehouseDepot, HashMap<Resource,Integer> costToPay) throws InvalidCostException {
        //Merging maps in a temporary cost map and checking it is equal to cost of power of production
        HashMap<Resource,Integer> totalCostResourceMap =  new HashMap<>(costStrongbox);
        costWarehouseDepot.forEach(
                (key, value) -> totalCostResourceMap.merge(key, value, Integer::sum)
        );

        if(!totalCostResourceMap.equals(costToPay)){
            throw new InvalidCostException();
        }
    }

    //Method used to remove resources from strongbox and warehouseDepot
    private void pay(HashMap<Resource,Integer> costStrongbox, HashMap<Resource,Integer> costWarehouseDepot) throws InvalidRemovalException {
        //Checking resource availability
        if(!strongbox.isAvailable(costStrongbox) || !warehouseDepots.isAvailable(costWarehouseDepot)){
            throw new InvalidRemovalException();
        }
        //Removing price paid from strongbox and/or warehouse
        strongbox.uncheckedRemove(costStrongbox);
        warehouseDepots.uncheckedRemove(costWarehouseDepot);
    }

    //Method used to pay and add production to faithTrack and/or strongbox
    private void produce(HashMap<Resource,Integer> costStrongbox, HashMap<Resource,Integer> costWarehouseDepot, HashMap<Resource,Integer> production) throws InvalidRemovalException {
        pay(costStrongbox, costWarehouseDepot);
        //Adding production to strongbox and/or faithTrack
        dispatch(production);
        strongbox.add(production);
    }

    //Method that is used to remove faith from temporaryMapResource and to add it to faithTrack
    private void dispatch(Map<Resource,Integer> production){
        for (Resource resource : production.keySet()){
            resource.dispatch(production, faithTrack);
        }
    }

    //Returns true if all the resources in resourceRequirement are present in strongbox and/or warehouse
    private boolean checkResourceRequirement(HashMap<Resource,Integer> resourceRequirement){
        HashMap<Resource,Integer> resourcesNotAvailable = warehouseDepots.resourcesNotAvailable(resourceRequirement);
        resourcesNotAvailable = strongbox.resourcesNotAvailable(resourcesNotAvailable);
        return resourcesNotAvailable.isEmpty();
    }

    /**
     * Method used to activate the production using the power of production of a development card
     * @param costStrongbox The cost of power of production paid with the resources located in the strongbox
     * @param costWarehouseDepot The cost of power of production paid with the resources located in the warehouse
     * @param indexDevelopmentCardSpace  The number of the development card slot used for the production. Ranges from 1 to 3
     * @throws InvalidProductionException If the same production has already been done in the same turn
     * @throws InvalidRemovalException If the payment can't be made
     * @throws InvalidCostException If the specified costs do not match the cost required by the power of production
     */
    public void activateCardProduction(HashMap<Resource,Integer> costStrongbox, HashMap<Resource,Integer> costWarehouseDepot, int indexDevelopmentCardSpace) throws InvalidProductionException, InvalidRemovalException, InvalidCostException, InvalidParameterException {
        //Checking that this production has not already been used in this turn
        if (powerOfProductionUsed[indexDevelopmentCardSpace]) {
            throw new InvalidProductionException();
        }
        PowerOfProduction powerOfProduction = developmentCardSpace.getPowerOfProduction(indexDevelopmentCardSpace);
        //Checking the correctness of costs
        mergeCostsAndVerify(costStrongbox, costWarehouseDepot, powerOfProduction.getCost());
        //Activating production
        produce(costStrongbox, costWarehouseDepot, powerOfProduction.getProduction());
        //Marking that the production has been done in this turn
        powerOfProductionUsed[indexDevelopmentCardSpace] = true;
        personalBoardPhase = PersonalBoardPhase.PRODUCTION;
    }

    /**
     * Method used to activate the production using the basic production power
     * @param costStrongbox The cost of power of production paid with the resources located in the strongbox
     * @param costWarehouseDepot The cost of power of production paid with the resources located in the warehouse
     * @param resource The resource that is going to be produced
     * @throws InvalidProductionException If the same production has already been done in the same turn or if the specified resource is faith
     * @throws InvalidRemovalException If the payment can't be made
     * @throws InvalidCostException If the total resources are not two
     */
    public void activateBasicProduction(HashMap<Resource,Integer> costStrongbox, HashMap<Resource,Integer> costWarehouseDepot, Resource resource) throws InvalidProductionException, InvalidRemovalException, InvalidCostException {
        //Checking that this production has not already been used in this turn
        if (powerOfProductionUsed[0]) {
            throw new InvalidProductionException();
        }
        //Checking that resource is not faith
        if (resource == Resource.FAITH){
            throw new InvalidProductionException();
        }
        //Checking that there is a total of two resources in both costs
        int totalResources = 0;
        for (int singleQuantity : costStrongbox.values()){
            totalResources += singleQuantity;
        }
        for (int singleQuantity : costWarehouseDepot.values()){
            totalResources += singleQuantity;
        }
        if (totalResources != 2){
            throw new InvalidCostException();
        }
        //Creating a resource map with the single resource
        HashMap<Resource, Integer> resourceToAdd = new HashMap<>();
        resourceToAdd.put(resource, 1);
        //Activating production
        produce(costStrongbox, costWarehouseDepot, resourceToAdd);
        //Marking that the production has been done in this turn
        powerOfProductionUsed[0] = true;
        personalBoardPhase = PersonalBoardPhase.PRODUCTION;
    }

    /**
     * Method used to activate the production using the power of production of a leader card with the proper power
     * @param costStrongbox The cost of power of production paid with the resources located in the strongbox
     * @param costWarehouseDepot The cost of power of production paid with the resources located in the warehouse
     * @param numLeaderCard The number of the leader card to use, must be > 0 and < leader cards not discarded in PlayerBoard
     * @param resource The resource that is going to be produced (together with a faith point already provided by the leader card)
     * @throws InvalidProductionException If the specified leader card does not exist or if the same production has already been done in the same turn
     * @throws InvalidRemovalException If the payment can't be made
     * @throws InvalidLeaderAction If the chosen leader card does not have the proper power
     * @throws InvalidCostException If the specified costs do not match the cost required by the power of production
     */
    public void activateLeaderProduction(HashMap<Resource,Integer> costStrongbox, HashMap<Resource,Integer> costWarehouseDepot, int numLeaderCard, Resource resource) throws InvalidProductionException, InvalidRemovalException, InvalidLeaderAction, InvalidCostException {
        //Checking that the specified leader card exists
        if (numLeaderCard <= 0 || numLeaderCard > leaderCards.size()){
            throw new InvalidProductionException();
        }
        //Checking that the power of production has not been already used in this turn
        if (powerOfProductionUsed[3+numLeaderCard]){
            throw new InvalidProductionException();
        }
        LeaderCard leaderCard = leaderCards.get(numLeaderCard-1);
        //Checking that leader card is active
        if(!leaderCard.isActive()){
            throw new InvalidProductionException();
        }
        //Checking that resource is not faith
        if (resource == Resource.FAITH) {
            throw new InvalidProductionException();
        }
        //Retrieving powerOfProduction of leaderCard, if not a production leader card an exception is thrown
        PowerOfProduction powerOfProduction = leaderCard.abilityProduction();
        if(powerOfProduction == null){
            throw new InvalidProductionException();
        }
        //Add resource chosen by player
        HashMap<Resource, Integer> production = powerOfProduction.getProduction();
        production.put(resource, 1);

        //checking that the resources the specified cost are right for this production
        mergeCostsAndVerify(costStrongbox, costWarehouseDepot, powerOfProduction.getCost());
        produce(costStrongbox, costWarehouseDepot, production);
        //Changing boolean of power of production for this turn
        powerOfProductionUsed[3+numLeaderCard] = true;
        personalBoardPhase = PersonalBoardPhase.PRODUCTION;
    }

    /**
     * Method used to acquire marbles form market
     * @param rowOrColumn 0 if row, 1 if column
     * @param value from 0 to 2 if row, from 0 to 3 if column Todo ricontrollare
     */
    public void takeFromMarket(int rowOrColumn, int value) throws InvalidParameterException{
        if((rowOrColumn == 0 && value >= 0 && value <= 2) || (rowOrColumn == 1 && value >= 0 && value <= 3))
            temporaryMarbles = new HashMap<>(market.takeBoughtMarbles(rowOrColumn, value));
        else{
            throw new InvalidParameterException();
        }
        personalBoardPhase = PersonalBoardPhase.TAKE_FROM_MARKET;
    }

    /**
     * This method transform a number of white marbles in the temporaryMarbles map
     * @param leaderCard is the number of the card to use to transform marbles
     * @param numOfTransformations is the number of marbles that needs to be transformed
     * @throws NotEnoughWhiteMarblesException this exception is thrown when there are not enough white marbles in the given map of marbles
     */
    public void transformWhiteMarble(int leaderCard, int numOfTransformations) throws InvalidParameterException,NotEnoughWhiteMarblesException, InvalidLeaderAction {
        if(leaderCard > 0 && leaderCard <=leaderCards.size() && numOfTransformations > 0)
            leaderCards.get(leaderCard - 1).abilityMarble(temporaryMarbles,numOfTransformations);
        else
            throw new InvalidParameterException();
    }

    /**
     * Method used to transform marbles taken from market into resources stored in temporary map resources
     */
    public void transformMarbles(){
        for (Marble marble : temporaryMarbles.keySet()){
            for(int value = 0; value < temporaryMarbles.get(marble); value++){
                try {
                    marble.transform(temporaryMapResource, faithTrack);
                } catch (InvalidParameterException e) {
                    e.printStackTrace();
                }
            }
        }
        temporaryMarbles.clear();
    }

    /**
     * Method used to add a single resource type in a specified quantity from the temporary resource map to a depot
     * @param depotLevel The depot to add the resource to
     * @param singleResourceMap The map which contains the single resource type and its quantity
     * @throws InvalidAdditionException If there is not a single resource type, if the resource is not in the temporary resource map or is not enough,
     * or if the rules of the warehouse depot are not followed
     */
    public void addToWarehouseDepots(int depotLevel, HashMap<Resource,Integer> singleResourceMap) throws InvalidAdditionException {
        //Checking if the request is correct
        if (singleResourceMap.size() != 1) {
            throw new InvalidAdditionException("Not one resource");
        }
        Resource resource = singleResourceMap.keySet().iterator().next();
        //Checking if there are enough resources in temporary map
        if(temporaryMapResource.get(resource) == null){
            throw new InvalidAdditionException("Not enough resources in temporary map");
        }
        if (temporaryMapResource.get(resource) < singleResourceMap.get(resource)){
            throw new InvalidAdditionException("Not enough resources in temporary map");
        }
        warehouseDepots.add(depotLevel, singleResourceMap);
        //Removing or subtracting from temporaryResourceMap
        if (temporaryMapResource.get(resource).equals(singleResourceMap.get(resource))){
            temporaryMapResource.remove(resource);
        }
        else{
            temporaryMapResource.put(resource, temporaryMapResource.get(resource) - singleResourceMap.get(resource));
        }
    }

    /**
     * Method used to swap resources between two standard depots
     * @param depot1 Cannot be a special depot
     * @param depot2 Cannot be a special depot
     * @throws InvalidSwapException This exception is thrown when the depot is a special depot or when the resource of at least one depot do not fit the other depot
     */
    public void swapResourceStandardDepot(int depot1, int depot2) throws InvalidSwapException {
        warehouseDepots.swap(depot1, depot2);
    }

    /**
     * Method to move a resource between a standard depot and a special depot, only one of the two depots can be a special depot
     * @param sourceDepotNumber Number of the depot from which the resource has to be moved, if this is a special depot the other depot cannot be a special depot
     * @param destinationDepotNumber Number of the depot to which the resource has to be moved, if this is a special depot the other depot cannot be a special depot
     * @param quantity How much of the resource in the source depot has to be moved
     * @throws InvalidRemovalException If there is not enough of the resource in the source depot
     * @throws InvalidAdditionException If the resource cannot be moved to the destination depot
     * @throws InvalidMoveException If the condition that only one of the two depots can be a special depot is not respected or if at least one of the two depots does not exist
     */
    public void moveResourceSpecialDepot(int sourceDepotNumber, int destinationDepotNumber, int quantity) throws InvalidAdditionException, InvalidRemovalException, InvalidMoveException {
        warehouseDepots.moveToFromSpecialDepot(sourceDepotNumber, destinationDepotNumber, quantity);
    }

    //se index è 0 non attiva nessuna leader card, valori possibili 0,1 o 2

    /**
     * Method to buy a development card from card grid
     * @param row Row of the card grid of the chosen card, ranges from 0 to 2
     * @param column Column of the card grid of the chosen card, ranges from 0 to 3
     * @param costStrongbox The cost of power of production paid with the resources located in the strongbox
     * @param costWarehouseDepots The cost of power of production paid with the resources located in the warehouse
     * @param numLeaderCard The number of the leader card to use to discount the price, if 0 then no leader card will be used, otherwise must be > 0 and < leader cards not discarded in PlayerBoard
     * @param cardPosition The development card space slot in which the bought card will be placed
     * @throws NoCardException If there is no card in the specified coordinates of card grid
     * @throws InvalidCostException If the specified price does not match the one of the chosen card
     * @throws InvalidLeaderAction If the specified leader card does not have the proper power
     * @throws InvalidRemovalException If the payment can't be made
     * @throws InvalidDevelopmentCardException If the card cannot be placed in the chosen development card space slot
     * @throws InvalidParameterException If the specified development card space slot does not exist
     */
    public void buyDevelopmentCard(int row, int column,HashMap<Resource, Integer> costStrongbox, HashMap<Resource, Integer> costWarehouseDepots, int numLeaderCard, int cardPosition) throws NoCardException, InvalidCostException, InvalidLeaderAction, InvalidRemovalException, InvalidDevelopmentCardException, InvalidParameterException {
        DevelopmentCard cardToBuy = cardGrid.getCard(row, column);
        HashMap<Resource, Integer> price = new HashMap<>(cardToBuy.getPrice());
        //if numLeaderCard is 1 or 2 method tries to discount price
        if (numLeaderCard != 0){
            if (numLeaderCard < 0 || numLeaderCard > leaderCards.size())
                throw new InvalidLeaderAction();
            //if not the correct leader throws InvalidLeaderAction()
            leaderCards.get(numLeaderCard-1).abilityDiscount(price);
            //TODO make sure ability discount manages if resource to discount is not in price
        }
        //Verifying that the provided costs are correct
        mergeCostsAndVerify(costStrongbox, costWarehouseDepots, price);
        //Checking resource availability
        if(!strongbox.isAvailable(costStrongbox) || !warehouseDepots.isAvailable(costWarehouseDepots)){
            throw new InvalidRemovalException();
        }
        developmentCardSpace.addCard(cardToBuy, cardPosition);
        //Removing price paid from strongbox and/or warehouse
        strongbox.uncheckedRemove(costStrongbox);
        warehouseDepots.uncheckedRemove(costWarehouseDepots);
        cardGrid.buyCard(row, column);
        personalBoardPhase = PersonalBoardPhase.BUY_DEV_CARD;
    }

    /**
     * Method to activate a leader
     * @param numLeaderCard The number of the leader card to activate, must be > 0 and < leader cards not discarded in PlayerBoard
     * @throws RequirementNotMetException if the requirements of the leader card are not met
     * @throws InvalidParameterException if the specified numLeaderCard is not correct, or if the leader card is already active
     */
    public void activateLeader(int numLeaderCard) throws RequirementNotMetException, InvalidParameterException {
        if (numLeaderCard <= 0 || numLeaderCard > leaderCards.size())
            throw new InvalidParameterException();
        LeaderCard leaderCard = leaderCards.get(numLeaderCard-1);
        if (leaderCard.isActive()){
            throw new InvalidParameterException();
        }

        Requirement requirement = leaderCard.getRequirement();
        //Checking resource requirements if the field not set to null
        if (requirement.getResourceRequirement() != null){
            if(!checkResourceRequirement(requirement.getResourceRequirement())){
                throw new RequirementNotMetException();
            }
        }
        //Checking card requirements if the field is not set to null
        if (requirement.getCardsRequirement() != null){
            if(!developmentCardSpace.checkRequirement(requirement.getCardsRequirement())){
                throw new RequirementNotMetException();
            }
        }
        //Activate leader card
        leaderCard.activate();
    }

    /**
     * Method used to discard a leader card during the game and gain a faith point as a consequence
     * @param numLeaderCard The number of the leader card to activate, must be > 0 and < leader cards not discarded in PlayerBoard
     * @throws InvalidParameterException if numLeaderCard is incorrect or the leader does not exist
     */
    public void removeLeader(int numLeaderCard) throws InvalidParameterException {
        if (numLeaderCard <= 0 || numLeaderCard > leaderCards.size())
            throw new InvalidParameterException();
        LeaderCard leaderCard = leaderCards.get(numLeaderCard-1);
        //Checking if leader card is active
        if (leaderCard.isActive()){
            throw new InvalidParameterException();
        }
        //Moving faith
        moveFaithMarkerInternally(1);
        //Removing leader card
        leaderCards.remove(numLeaderCard-1);
    }

    /**
     * Method used to discard the two leader cards at the beginning of the match
     * @param indexLeaderCard1 Index of one of the two leader cards to discard, must range from 1 to 4 and must be different from the other index
     * @param indexLeaderCard2 Index of one of the two leader cards to discard, must range from 1 to 4 and must be different from the other index
     * @throws InvalidParameterException If the conditions specified in the parameters are not met
     */
    public void discardInitialLeader(int indexLeaderCard1, int indexLeaderCard2) throws InvalidParameterException {
        if (indexLeaderCard1 <= 0 || indexLeaderCard1 > leaderCards.size()
                || indexLeaderCard2 <= 0 || indexLeaderCard2 > leaderCards.size()
                || indexLeaderCard1 == indexLeaderCard2 )
            throw new InvalidParameterException();
        leaderCards.remove(indexLeaderCard1-1);
        leaderCards.remove(indexLeaderCard2-1);
        match.addPlayerReady();
        personalBoardPhase = PersonalBoardPhase.RESOURCE_CHOICE;
    }

    /**
     * Method used to add initial resources chosen by player according to its order
     * @param initialResources Resources to add to the temporary resources
     * @throws InvalidParameterException If the number of resources chosen doesn't match the possible number of resources the player can choose
     */
    public void addInitialResources(Map<Resource, Integer> initialResources) throws InvalidParameterException {
        int totalResources = 0;
        for(int singleResourceQuantity : initialResources.values()){
            totalResources+= singleResourceQuantity;
        }
        if (totalResources != numOfResourcesToChoose){
            throw new InvalidParameterException();
        }
        temporaryMapResource = new HashMap<>(initialResources);
    }

    /**
     * This method checks the conditions to activate the vatican report and activates the report for all the players.
     */
    public void checkVaticanReport() {
        if (faithTrack.getPopeFavourTile(1) && 4 < faithTrack.getFaithTrackPosition() && faithTrack.getFaithTrackPosition() < 9){
            //calls the vatican report for all the other players
            match.vaticanReport(1);
        }
        else if(faithTrack.getPopeFavourTile(2) && 11 < faithTrack.getFaithTrackPosition() && faithTrack.getFaithTrackPosition() < 17){
            //calls the vatican report for all the other players
            match.vaticanReport(2);
        }
        else if(faithTrack.getPopeFavourTile(3) && 18 < faithTrack.getFaithTrackPosition() && faithTrack.getFaithTrackPosition() < 25){
            //calls the vatican report for all the other players
            match.vaticanReport(3);
        }
    }

    /**
     * Calculates the victory points for this personalBoard
     */
    public void sumVictoryPoints(){
        //Points from development cards
        victoryPoints += developmentCardSpace.getVictoryPoints();
        //Points from faith points and Pope's favour tiles
        victoryPoints += faithTrack.calculateVictoryPoints();
        //Points from active leader cards
        for (LeaderCard leaderCard : leaderCards){
            if (leaderCard.isActive()){
                victoryPoints += leaderCard.getVictoryPoints();
            }
        }
        //Points from resources in Warehouse or Strongbox
        victoryPoints += Math.floorDiv(strongbox.getTotalResources() + warehouseDepots.getTotalResources(), 5);
    }

    /**
     * @return The victory points of this personalBoard
     */
    public int getVictoryPoints() {
        return victoryPoints;
    }

    /**
     * This method moves the faith marker and activates the vatican report if necessary
     * @param numOfSteps is the number of steps to make on the faith track
     * @throws InvalidParameterException when numOfSteps is negative
     */
    private void moveFaithMarkerInternally(int numOfSteps) throws InvalidParameterException {
        //moves the faith marker
        faithTrack.moveFaithMarker(numOfSteps);
        //activates checks vatican report
        checkVaticanReport();
    }

    /**
     * This method moves the faith marker without checking for the vatican report
     * @param numOfSteps is the number of steps to make on the faith track
     * @throws InvalidParameterException when numOfSteps is negative
     */
    public void moveFaithMarker(int numOfSteps) throws InvalidParameterException {
        //moves the faith marker
        faithTrack.moveFaithMarker(numOfSteps);
    }

    /**
     * This method activates the vatican report on a specific tile
     * @param tileNumber is the tile to activate
     */
    public void activateVaticanReport(int tileNumber){
        faithTrack.setPopeFavourTiles(tileNumber);
    }

    /**
     * @return The temporary resource map of the resources not yet stored
     */
    public HashMap<Resource, Integer> getTemporaryMapResource() {
        return temporaryMapResource;
    }
    /**
     * @return The temporary marble map of the marbles not yet transformed
     */
    public HashMap<Marble, Integer> getTemporaryMarbles() {
        return temporaryMarbles;
    }

    public Strongbox getStrongbox() {
        return strongbox;
    }

    public WarehouseDepots getWarehouseDepots() {
        return warehouseDepots;
    }

    public CardGrid getCardGrid() {
        return cardGrid;
    }

    public DevelopmentCardSpace getDevelopmentCardSpace() {
        return developmentCardSpace;
    }

    public PersonalBoardPhase getPersonalBoardPhase() {
        return personalBoardPhase;
    }

    public void setNumOfResourcesToChoose(int numOfResourcesToChoose) {
        this.numOfResourcesToChoose = numOfResourcesToChoose;
    }

    /**
     * this method is used only for testing purpose
     * @return the faith track of teh personal board
     */
    public FaithTrack getFaithTrack() {
        return faithTrack;
    }

    public void setPlayer(Player myPlayer){
        this.myPlayer = myPlayer;
    }

    public void doNotifyLeaders() {
        ArrayList<Integer> initialLeaderCardsID = new ArrayList<>();
        leaderCards.forEach(x->initialLeaderCardsID.add(x.getId()));
        if(myPlayer.getView() != null)
            myPlayer.getView().update(new InitialLeaderCardsUpdate(myPlayer.getNickname(),initialLeaderCardsID));
    }
}