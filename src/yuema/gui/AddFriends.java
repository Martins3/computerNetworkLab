package yuema.gui;

/**
 * Created by martin on 17-10-20.
 *
 */

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import yuema.local.Client;
import yuema.message.ConnectType;
import yuema.message.MessageContent;
import yuema.message.MessageType;


/**
 * Created by martin on 17-10-10.
 * 要么是函数有问题, Viper 没有办法读取,很难受的
 */
public class AddFriends{
    private Client client;
    AddFriends(Client client){
        this.client = client;
    }
    void display(){
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.resizableProperty().setValue(Boolean.FALSE);
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);


        Label nameLabel = new Label("Username:");
        nameLabel.setId("bold-label");
        GridPane.setConstraints(nameLabel,0,0);


        TextField nameInput = new TextField();
        GridPane.setConstraints(nameInput, 0, 1);

        Label resultLabel = new Label();
        GridPane.setConstraints(resultLabel, 0, 2);

        Button loginButton = new Button("find");
        GridPane.setConstraints(loginButton, 0, 3);
        loginButton.setOnAction(e -> {
            // 发送数据而已, 体现为 user 的变化而已
            String name = nameInput.getText();
            if (name.length() == 0) {
                resultLabel.setText("please input the right name !");
            } else {
                resultLabel.setText("request has been send to server !");
                MessageContent mess = new MessageContent(MessageType.CS_FIND_FRIEND);
                mess.friendID = name;
                client.send(mess);
            }
        });
        grid.getChildren().addAll(nameLabel, nameInput, resultLabel, loginButton);
        Scene scene = new Scene(grid, 300, 200);
        scene.getStylesheets().add(getClass().getResource("rsc/css/Viper.css").toExternalForm());
        window.setScene(scene);
        window.show();
    }
}
