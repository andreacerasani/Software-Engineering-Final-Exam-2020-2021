package it.polimi.ingsw.server.model;

import it.polimi.ingsw.common.messages.messagesToClient.WarehouseUpdate;
import it.polimi.ingsw.common.messages.messagesToClient.AddSpecialDepotUpdate;
import it.polimi.ingsw.common.utils.observe.MessageObservable;
import it.polimi.ingsw.server.model.enumerations.Resource;
import it.polimi.ingsw.server.model.exceptions.InvalidAdditionException;
import it.polimi.ingsw.server.model.exceptions.InvalidMoveException;
import it.polimi.ingsw.server.model.exceptions.InvalidSwapException;
import it.polimi.ingsw.server.model.exceptions.InvalidRemovalException;

import java.util.*;
import java.util.Map;

/**
 * Class that represents the warehouse of the personal board.
 * This class coordinates the depots where resources from the market are stored
 */
public class WarehouseDepots extends MessageObservable {
    private final int STANDARDDEPOTS = 3;
    private final ArrayList<Depot> depots = new ArrayList<>();

    public WarehouseDepots() {
        //Filling WarehouseDepots with depots of progressively increasing size
        for(int i = 0; i < STANDARDDEPOTS; i++){
            depots.add(new Depot(i+1));
        }
    }

    //Method used to perform checks before adding resources
    private void checkAdd(int depotNumber, Map<Resource, Integer> singleResourceMap) throws InvalidAdditionException{
        if (depotNumber > depots.size()) {
            throw new InvalidAdditionException("Invalid depot");
        }
        //Checking if the request is correct
        if (singleResourceMap.size() != 1) {
            throw new InvalidAdditionException("Not one resource");
        }
        Resource resource = singleResourceMap.keySet().iterator().next();
        //Checks to do if the depot is a standard depot
        if (depotNumber <= STANDARDDEPOTS){
            //Making sure the same resource is not in another depot that is not special
            for (Depot depot : depots.subList(0, STANDARDDEPOTS)){
                //Skipping control for the depot to which we want to add the resource
                if (depot != depots.get(depotNumber-1)){
                    if (depot.checkResource(resource)) {
                        throw new InvalidAdditionException("Same resource in other depots");
                    }
                }
            }
        }
    }

    /**
     * Method used to add a given quantity of a resource to a specified depot
     * @param depotNumber The depot to which the resource has to be added, the specified depot must exist
     * @param singleResourceMap The map must contain only one resource and the value associated with the resource is the quantity that will be added
     * @throws InvalidAdditionException "Invalid Depot" : the depot does not exist
     * "Not one resource": singleResourceMap does not contain only one resource
     * "Same resource in other depots": when trying to add a resource to a standard depot and the same resource is in another standard depot
     * The exception can be thrown by the depot itself when there is not enough space or there is already another resource in the depot
     */
    public void add(int depotNumber, Map<Resource, Integer> singleResourceMap) throws InvalidAdditionException {
        //Performing checks
        checkAdd(depotNumber,singleResourceMap);
        //Adding the resource
        depots.get(depotNumber-1).add(singleResourceMap);

        doNotify();
    }

    /**
     * Method to add a special depot
     * @param resource The only type of resource that can be added to the special depot
     */
    public void addSpecialDepot(Resource resource){
        depots.add(new SpecialDepot(resource));
        notifyObservers(new AddSpecialDepotUpdate(this.getNickname(), resource));
    }

    /**
     * Method to get how many depots there are in warehouse depots
     * @return The number of depots in warehouse depots
     */
    public int getNumDepots(){
        return depots.size();
    }

    /**
     * Method used to swap resources between two standard depots
     * @param depotNumber1 Cannot be a special depot
     * @param depotNumber2 Cannot be a special depot
     * @throws InvalidSwapException This exception is thrown when the depot is a special depot or when the resource of at least one depot do not fit the other depot
     */
    public void swap(int depotNumber1, int depotNumber2) throws InvalidSwapException {
        //Making sure that both shelves are standard depots
        if (depotNumber1 > STANDARDDEPOTS || depotNumber2 > STANDARDDEPOTS) {
            throw new InvalidSwapException("Can't swap between special depots");
        }

        Depot depot1 = depots.get(depotNumber1-1);
        Depot depot2 = depots.get(depotNumber2-1);

        //Checking if the number of resources of each depot fit the other depot
        if (depot1.getNumberResources() > depot2.getSIZE()||depot2.getNumberResources() > depot1.getSIZE()) {
            throw new InvalidSwapException("Not enough space in the depots");
        }

        //Swapping the resources
        Map<Resource, Integer> tempmap1  = depot1.getMapResource();
        depot1.setMapResource(depot2.getMapResource());
        depot2.setMapResource(tempmap1);

        doNotify();
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
    public void moveToFromSpecialDepot(int sourceDepotNumber, int destinationDepotNumber, int quantity) throws InvalidRemovalException, InvalidAdditionException, InvalidMoveException {
        //Throw exception if the condition that exactly one of the two depots can be a special depot is not respected
        if(sourceDepotNumber <= STANDARDDEPOTS == destinationDepotNumber <= STANDARDDEPOTS) {
            throw new InvalidMoveException("Not exactly one of the two depots is a special depot");
        }
        //Throw exception if the depot does not exist
        if (sourceDepotNumber > depots.size() || destinationDepotNumber > depots.size()) {
            throw new InvalidMoveException("The depot does not exist");
        }

        Depot sourceDepot = depots.get(sourceDepotNumber-1);
        Depot destinationDepot = depots.get(destinationDepotNumber-1);

        //Instantiating the resource map that has to be moved
        Map<Resource, Integer> resourceMap = new HashMap<>();
        //Throws exception if the source depot is empty
        if (!sourceDepot.getMapResource().keySet().iterator().hasNext()){
            throw new InvalidRemovalException("Not enough resources in the first depot");
        }
        Resource resourceToMove = sourceDepot.getMapResource().keySet().iterator().next();
        resourceMap.put(resourceToMove, quantity);
        //Checking if the move can be performed
        Map<Resource, Integer> resourceToCheckMap = new HashMap<>(resourceMap);
        sourceDepot.checkAvailability(resourceToCheckMap);
        if (!resourceToCheckMap.isEmpty()) {
            throw new InvalidRemovalException("Move can't be performed");
        }
        checkAdd(destinationDepotNumber, resourceMap);
        destinationDepot.checkAdd(resourceMap);

        //Moving the resource
        Map<Resource, Integer> resourceToRemove = new HashMap<>(resourceMap);
        sourceDepot.uncheckedRemove(resourceToRemove);

        destinationDepot.uncheckedAdd(resourceMap);
        doNotify();
    }



    /**
     * Checks that all the resources and their quantities in resourceMap are stored in WarehouseDepot
     * @param resourceMap The resources and their quantities whose availability in WarehouseDepot is verified
     * @return true if all the resources in resourceMap are in WareHouseDepot, false otherwise
     */
    public boolean isAvailable(Map<Resource, Integer> resourceMap) {
        return resourcesNotAvailable(resourceMap).isEmpty();
    }

    /**
     * @param resourceMap Resources whose presence in WarehouseDepots is checked
     * @return a map of the resources not available in WarehouseDepots among those in resourceMap
     */
    public Map<Resource, Integer> resourcesNotAvailable(Map<Resource, Integer> resourceMap){
        Map<Resource, Integer> resourceToCheckMap = new HashMap<>(resourceMap);
        for (Depot depot: depots){
            depot.checkAvailability(resourceToCheckMap);
        }
        return resourceToCheckMap;
    }


    /**
     * Method to remove a resource from a depot, must be called only after performing checks with checkAvailability
     * @param resourceMap The map that contains the resources and the quantity of the resources to remove
     */
    public void uncheckedRemove(Map<Resource, Integer> resourceMap){
        Map<Resource, Integer> resourceToRemoveMap = new HashMap<>(resourceMap);
        for (Depot depot: depots){
            depot.uncheckedRemove(resourceToRemoveMap);
        }
        doNotify();
    }

    /**
     * Method to get the total quantity of resources in the warehouse depots
     * @return The total quantity of resources in every depot
     */
    public int getTotalResources(){
        int total = 0;
        for (Depot depot : depots){
            total += depot.getNumberResources();
        }
        return total;
    }

    /**
     * Method that returns the depot associated with the passed number.
     * Method to use only for testing purpose
     * @param numberDepot The number of the depot that has to be returned
     * @return The depot that is associated with the passed number
     */
    public Depot getDepot(int numberDepot){
        return depots.get(numberDepot-1);
    }

    /**
     * Notifies the view with a clone of the model data of the warehouse depots
     */
    public void doNotify(){
        List<Map<Resource, Integer>> warehouseState = new ArrayList<>();
        for (Depot depot : depots){
            warehouseState.add(depot.getMapResource());
        }

        notifyObservers(new WarehouseUpdate(this.getNickname(), warehouseState));
    }
}
