package it.polimi.ingsw.server.view;
import com.google.gson.Gson;
import it.polimi.ingsw.common.View;
import it.polimi.ingsw.common.messages.messagesToClient.DisconnectedUpdate;
import it.polimi.ingsw.common.messages.messagesToClient.MessageToClient;
import it.polimi.ingsw.common.messages.messagesToClient.ErrorMessage;
import it.polimi.ingsw.common.messages.messagesToServer.MessageToServer;
import it.polimi.ingsw.server.Lobby;
import it.polimi.ingsw.server.controller.Controller;
import it.polimi.ingsw.server.model.ObservableGameEnder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class VirtualView implements Runnable,View {
    private Socket socket;
    private Controller controller;
    private InetAddress clientAddress;
    private int clientPort;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;
    private static final MessageToServerDeserializer messageDeserializer = new MessageToServerDeserializer();
    private String nickname;
    private Lobby lobby;
    private String line = "";
    private final int TIMEOUT_TIME = 5000;
    private final String PING = "ping";


    public VirtualView(Socket socket, Lobby lobby) {
        this.controller = null;
        this.lobby = lobby;
        this.socket = socket;
        clientAddress = socket.getInetAddress();
        clientPort = socket.getPort();
        gson = new Gson();
        nickname = null;
    }

    public void run() {
        System.out.println("Thread created");
        try {
            socket.setSoTimeout(TIMEOUT_TIME);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            controller = lobby.getAvailableMatch(this);
            if(controller != null){
                controller.newConnection(this);
            }
            else{
                this.update(new ErrorMessage(null, "A player is choosing the number of players. Wait..."));
            }

            while ((line = in.readLine()) != null) {
                if(!line.equals(PING)){
                    synchronized (this){
                        if(controller == null){
                            this.update(new ErrorMessage(null, "The first connected player is choosing the number of players. Wait..."));
                        }
                        else{
                            System.out.println("Received: " + line);
                            MessageToServer messageToServer = messageDeserializer.deserializeMessage(line);

                            if (nickname!= null){
                                if(!nickname.equals(messageToServer.getNickname())){
                                    this.update(new ErrorMessage(nickname, "This message cannot be sent by this client"));
                                }
                                else{
                                    messageToServer.handleMessage(controller,this);
                                }
                            }
                            else{
                                messageToServer.handleMessage(controller,this);
                            }
                        }
                    }
                }
                else System.out.println("ping received");
            }
            disconnect();
        } catch (IOException e) {
            disconnect();

            /*if(nickname == null){
                System.err.println(e.getMessage());
                System.out.println("Stream di rete terminato");
                try {
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException | NullPointerException ioException) {
                    ioException.printStackTrace();
                }
            } else {
                System.err.println(e.getMessage());
                System.out.println("Stream di rete terminato");
                controller.removeObserver(this);
                try {
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException | NullPointerException ioException) {
                    ioException.printStackTrace();
                }
                controller.notifyObservers(new DisconnectedUpdate(nickname));
                //controller.notifyObservers(new ); // creare messaggio di disconnessione client
            }
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.out.println("stream di rete terminato");*/
        }
    }

    private void disconnect(){
        try {
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Could not disconnect");
        }
        //if a controller is assigned the player is inside a match
        if(controller!= null){
            lobby.advanceQueue();
            controller.removeObserver(this);
            controller.notifyObservers(new DisconnectedUpdate(nickname));
        }
        //else the player was waiting and is removed from the queue
        else lobby.removeFromQueue(this);
    }

    @Override
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Sends message to client
     *
     * @param message Message notified by {@link ObservableGameEnder}
     */
    @Override
    public void update(MessageToClient message) {
        out.println(gson.toJson(message));
        System.out.println("Sent:" + gson.toJson(message));
    }

    public void setController(Controller controller){
        synchronized (this){
            this.controller = controller;
            controller.newConnection(this);
        }
    }


    /*public void enableHeartbeat(boolean enable) {
        if (enabled) {
            pinger.scheduleAtFixedRate(() -> sendMessage(new PingMessage()), 0, 1000, TimeUnit.MILLISECONDS);
        } else {
            pinger.shutdownNow();
        }
    }*/

}
