package yuema.gui;

import com.jfoenix.controls.JFXTextField;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import yuema.local.Client;

import java.util.regex.Pattern;

/**
 * Created by martin on 17-10-29.
 */
public class AlertBox {
    static void display(String title, Client client) {
        Stage window = new Stage();

        //Block events to other windows
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);
        window.setWidth(250);
        window.setHeight(150);
        window.resizableProperty().setValue(Boolean.FALSE);

        JFXTextField ip = new JFXTextField();
        ip.setPromptText("ip");


        Button button = new Button("OK");
        button.setOnMouseClicked(e -> {
            String ipString = ip.getText();
            Pattern PATTERN = Pattern.compile(
                    "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
            if(PATTERN.matcher(ipString).matches()){
                client.setServerIP(ipString);
                window.close();
            }else{
                ip.setText("");
                ip.setPromptText("please input an valid ip address");
            }
        });

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(ip, button);

        //Display window and wait for it to be closed before returning
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();
    }
}
