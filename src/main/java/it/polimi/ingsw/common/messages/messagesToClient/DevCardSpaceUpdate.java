package it.polimi.ingsw.common.messages.messagesToClient;

import it.polimi.ingsw.client.CLI.LocalModel.LocalPhase;
import it.polimi.ingsw.client.ClientView;
import it.polimi.ingsw.common.messages.MessageType;

import java.util.ArrayList;

public class DevCardSpaceUpdate extends MessageToClient {
    private final ArrayList<ArrayList<Integer>> cardsState;
    public DevCardSpaceUpdate(String nickname, ArrayList<ArrayList<Integer>> cardsState) {
        super(nickname, MessageType.DEV_CARD_SPACE_UPDATE);
        this.cardsState = cardsState;
    }

    @Override
    public void handleMessage(ClientView clientView) {
        clientView.showUpdatedDevCardSpace(getNickname(), cardsState);
    }
}
