package it.polimi.ingsw.server.model;

import it.polimi.ingsw.server.model.enumerations.Resource;

import java.util.Map;

/**
 * Leader card with the discount ability
 */
public class LeaderDiscount extends LeaderCard{
    private int discount;
    private Resource resourceDiscounted;

    public LeaderDiscount(int victoryPoints, Requirement requirement, int discount, Resource resourceDiscounted) {
        super(victoryPoints, requirement);
        this.discount = discount;
        this.resourceDiscounted = resourceDiscounted;
    }

    /**
     * this method applies a discount to a map of resources
     * @param resources is the map of resources to discount
     */
    @Override
    public void abilityDiscount(Map<Resource, Integer> resources) {
        if (resources.containsKey(resourceDiscounted)) {
            Integer newValue = resources.get(resourceDiscounted) - discount;
            if (newValue > 0)
                resources.replace(resourceDiscounted, newValue);
            else
                resources.remove(resourceDiscounted);
        }
    }

    /**
     * Getter for the discounted resource
     * @return the resource discounted by the leader power
     */
    public Resource getResourceDiscounted() {
        return resourceDiscounted;
    }
}
