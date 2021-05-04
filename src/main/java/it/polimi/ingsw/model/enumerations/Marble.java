package it.polimi.ingsw.model.enumerations;

import it.polimi.ingsw.model.FaithTrack;
import it.polimi.ingsw.model.enumerations.Resource;
import it.polimi.ingsw.model.exceptions.InvalidParameterException;

import java.util.Map;

/**
 * This class represents the abstract marbles
 */
public enum Marble {


    WHITEMARBLE{

        /**
         * White marble doesn't transform into anything
         * @param resourceMap map of resources from personal board
         * @param faithTrack references of FaithTrack
         */
        @Override
        public void transform (Map<Resource,Integer> resourceMap, FaithTrack faithTrack){

        }
    },
    PURPLEMARBLE{

        /**
         * Implementation of transform method, it transforms the marble into resource
         * @param resourceMap map of resources from personal board
         * @param faithTrack references of FaithTrack
         */
        @Override
        public void transform (Map<Resource,Integer> resourceMap, FaithTrack faithTrack){

            resourceMap.merge(Resource.SERVANT, value, Integer::sum);

        }
    },
    BLUEMARBLE{

        /**
         * Implementation of transform method, it transforms the marble into resource
         * @param resourceMap map of resources from personal board
         * @param faithTrack references of FaithTrack
         */
        @Override
        public void transform (Map<Resource,Integer> resourceMap, FaithTrack faithTrack){

            resourceMap.merge(Resource.SHIELD, value, Integer::sum);

        }
    },
    YELLOWMARBLE{

        /**
         * Implementation of transform method, it transforms the marble into resource
         * @param resourceMap map of resources from personal board
         * @param faithTrack references of FaithTrack
         */
        @Override
        public void transform (Map<Resource,Integer> resourceMap, FaithTrack faithTrack){

            resourceMap.merge(Resource.COIN, value, Integer::sum);

        }
    },
    GREYMARBLE{

        /**
         * Implementation of transform method, it transforms the marble into resource
         * @param resourceMap map of resources from personal board
         * @param faithTrack references of FaithTrack
         */
        @Override
        public void transform (Map<Resource,Integer> resourceMap, FaithTrack faithTrack){

            resourceMap.merge(Resource.STONE, value, Integer::sum);

        }
    },
    REDMARBLE{

        /**
         * Implementation of transform method, it moves the faithMarker of one position
         * @param resourceMap map of resources from personal board
         * @param faithTrack references of FaithTrack
         */
        @Override
        public void transform (Map<Resource,Integer> resourceMap, FaithTrack faithTrack) throws InvalidParameterException {
            try {
                faithTrack.moveFaithMarker(value);
            }catch (Exception exception){
                throw new InvalidParameterException();
            }
        }
    };

    private static final Integer value = 1;
    public abstract void transform (Map<Resource,Integer> temporaryMapResource, FaithTrack faithTrack) throws InvalidParameterException;

}
