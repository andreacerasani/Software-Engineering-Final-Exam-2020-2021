package it.polimi.ingsw.model;

import it.polimi.ingsw.model.enumerations.Resource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class LeaderDiscountTest {

    @Test
    void abilityDiscount() {
        Resource resourcediscount = Resource.SHIELD;
        Requirement requirement = new Requirement(new ArrayList<>(),new HashMap<>());
        LeaderCard leaderCardTest = new LeaderDiscount(10,requirement,2,resourcediscount);

        HashMap<Resource,Integer> resourcesTest = new HashMap<>();
        resourcesTest.put(Resource.SHIELD,1);

        leaderCardTest.abilityDiscount(resourcesTest);
        assertTrue(resourcesTest.get(Resource.SHIELD) == 0);

        leaderCardTest.abilityDiscount(resourcesTest);
        assertTrue(resourcesTest.get(Resource.SHIELD) == 0);

        resourcesTest.put(Resource.COIN, 4);
        resourcesTest.put(Resource.SHIELD,3);
        leaderCardTest.abilityDiscount(resourcesTest);
        assertTrue(resourcesTest.get(Resource.SHIELD) == 1);
        assertTrue(resourcesTest.get(Resource.COIN) == 4);
    }
}