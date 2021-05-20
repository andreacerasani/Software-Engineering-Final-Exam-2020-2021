package it.polimi.ingsw.common.messages.messagesToClient;

import it.polimi.ingsw.client.ClientView;
import it.polimi.ingsw.common.messages.MessageType;

import java.util.List;

public class PlayersOrderUpdate extends MessageToClient{
    List<String> playerNicknames;

    public PlayersOrderUpdate(String nickname, List<String> playerNicknames) {
        super(nickname, MessageType.PLAYERS_ORDER_UPDATE);
        this.playerNicknames = playerNicknames;
    }

    @Override
    public void handleMessage(ClientView clientView) {
        clientView.showUpdatePlayersOrder(playerNicknames);
    }
}