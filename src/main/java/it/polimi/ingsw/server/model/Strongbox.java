package it.polimi.ingsw.server.model;

import it.polimi.ingsw.common.messages.messagesToClient.StrongboxUpdate;
import it.polimi.ingsw.common.utils.observe.MessageObservable;
import it.polimi.ingsw.server.model.enumerations.Resource;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that represents the strongbox of a personal board
 */
public class Strongbox extends MessageObservable {
    private Map<Resource, Integer> strongbox;

    /**
     * Constructor that initialize an empty Map
     */
    public Strongbox() {
        strongbox = new HashMap<>();
    }

    /**
     * Stores the given map of resources in the strongbox
     * @param resourceMap The map of resources to add
     */
    public void add(Map<Resource, Integer> resourceMap) {
        resourceMap.forEach((resource, quantity) -> strongbox.merge(resource, quantity, Integer::sum));
        notifyObservers(new StrongboxUpdate(this.getNickname(), new HashMap<>(strongbox)));
    }

    /**
     * Checks that all the resources and their quantities in resourceMap are stored in Strongbox
     * @param resourceMap The resources and their quantities whose availability in Strongbox is verified
     * @return true when every resource in resourceMap is in Strongbox, false otherwise
     */
    public boolean isAvailable(Map<Resource, Integer> resourceMap) {
        //Checking if there are enough resources in strongbox to remove
        for (Resource resource : resourceMap.keySet())  {
            if (strongbox.get(resource) == null){
                return false;
            }
            else if (strongbox.get(resource) < resourceMap.get(resource)){
                return false;
            }
        }
        return true;
    }

    /**
     * @param resourceMap Resources whose presence in Strongbox is checked
     * @return a map of the resources not available in Strongbox among those in resourceMap
     */
    public Map<Resource, Integer> resourcesNotAvailable(Map<Resource, Integer> resourceMap){
        Map<Resource, Integer> resourceToCheckMap = new HashMap<>(resourceMap);
        for (Resource resource : resourceMap.keySet())  {
            if (strongbox.get(resource) != null){
                if (strongbox.get(resource) >= resourceToCheckMap.get(resource)){
                    resourceToCheckMap.remove(resource);
                }
                else {
                    resourceToCheckMap.put(resource, resourceToCheckMap.get(resource)-strongbox.get(resource));
                }
            }
        }
        return resourceToCheckMap;
    }

    /**
     * Removes the given map of resources from the strongbox, must be called only after performing checks with checkAvailability
     * @param resourceMap The map of resources to remove
     */
    public void uncheckedRemove(Map<Resource, Integer> resourceMap){
        for (Resource resource : resourceMap.keySet())  {
            if (strongbox.get(resource).equals(resourceMap.get(resource)))
                strongbox.remove(resource);
            else
                strongbox.merge(resource, -resourceMap.get(resource), Integer::sum);
        }
        notifyObservers(new StrongboxUpdate(this.getNickname(), new HashMap<>(strongbox)));
    }

    /**
     * Method to get the quantity of a given resource in strongbox
     * @param resource The resource whose quantity is requested
     * @return The quantity of the specified resource if present, else 0
     */
    public int getResourceQuantity(Resource resource) {
        return strongbox.getOrDefault(resource, 0);
    }

    /**
     * Method to calculate the sum of all resources in the strongbox
     * @return The sum of all resources in the strongbox
     */
    public int getTotalResources(){
        int total = 0;
        //Iterating in the Collection of values of the Map
        for (int singleQuantity : strongbox.values()){
            total += singleQuantity;
        }
        return total;
    }
}
