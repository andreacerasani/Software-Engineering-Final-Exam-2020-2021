package it.polimi.ingsw.client.GUI.Controller;

import it.polimi.ingsw.client.GUI.SceneManager;
import it.polimi.ingsw.common.messages.messagesToServer.TransformMarblesMessage;
import it.polimi.ingsw.common.messages.messagesToServer.TransformWhiteMarblesMessage;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class WhiteMarbleLeaderPowerController extends AbstractController {
    @FXML
    private Button noButton;
    @FXML
    private Spinner<Integer> whiteMarbleSpinner;
    @FXML
    private Button okButton;
    @FXML
    private ImageView leaderTwoImgView;
    @FXML
    private ImageView leaderOneImgView;

    private ImageView selectedLeaderCardImageView = null;

    private final DropShadow highlightEffect = new DropShadow(6, Color.web("#f9dc52"));

    @FXML
    public void initialize(){
        GameInterfaceController gameInterfaceController = (GameInterfaceController) SceneManager.getInstance().getController("gameInterface");

        String nickname = SceneManager.getInstance().getGui().getNickname();
        PersonalBoardController personalBoardController = gameInterfaceController.getPersonalBoardControllerMap().get(nickname);
        //Load leader card images
        leaderOneImgView.setImage(personalBoardController.getLeaderCard1Image());
        leaderTwoImgView.setImage(personalBoardController.getLeaderCard2Image());

        SpinnerValueFactory<Integer> spinnerValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1,4,1);
        whiteMarbleSpinner.setValueFactory(spinnerValueFactory);
        leaderOneImgView.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onLeaderCardClick);
        leaderTwoImgView.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onLeaderCardClick);
        okButton.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onOkClick);
        noButton.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onNoClick);
    }

    private void onLeaderCardClick(Event event){
        ImageView selectedCard = (ImageView)event.getSource();
        highlightCard(selectedCard);
    }

    private void onOkClick(MouseEvent e){
        if(selectedLeaderCardImageView != null){
            int leaderCardInt = Integer.parseInt(selectedLeaderCardImageView.getId());
            int numOfTransformationsInt = whiteMarbleSpinner.getValue();
            getGui().getMessageSender().sendMessage(new TransformWhiteMarblesMessage(getGui().getNickname(),leaderCardInt,numOfTransformationsInt));
            Stage stage = (Stage) okButton.getScene().getWindow();
            stage.close();
        }
    }

    private void onNoClick(MouseEvent e){
        getGui().getMessageSender().sendMessage(new TransformMarblesMessage(getGui().getNickname()));
        Stage stage = (Stage) noButton.getScene().getWindow();
        stage.close();
    }

    private void highlightCard(ImageView selectedCard){
        if (selectedLeaderCardImageView != selectedCard){
            if (selectedLeaderCardImageView != null){
                selectedLeaderCardImageView.setEffect(null);
            }
            selectedLeaderCardImageView = selectedCard;
            selectedLeaderCardImageView.setEffect(highlightEffect);
        }
    }
}