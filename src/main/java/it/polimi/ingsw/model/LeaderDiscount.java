package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enumerations.Resource;
import it.polimi.ingsw.model.exceptions.NotEnoughWhiteMarblesException;

import java.util.HashMap;

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
    public void abilityDiscount(HashMap<Resource, Integer> resources) {
        Integer newValue = resources.get(resourceDiscounted) - discount;
        if(newValue >= 0)
            resources.replace(resourceDiscounted,newValue);
        else
            resources.replace(resourceDiscounted,0);
    }

    @Override
    public void abilityDepot() {

    }

    @Override
    public void abilityMarble(HashMap<Marble, Integer> temporaryMapMarble, int numOfTransformation) throws NotEnoughWhiteMarblesException {

    }

    @Override
    public HashMap<Resource,Integer> abilityProduction() {
        return null;
    }
}