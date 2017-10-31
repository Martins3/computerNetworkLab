package yuema.gui;

/**
 * Created by martin on 17-10-20.
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
public class SetPPAP{
    void display(){
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.resizableProperty().setValue(Boolean.FALSE);
        window.setTitle("Reset");
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);


        Label notification = new Label();
        GridPane.setConstraints(notification,0, 0);


        //Name Label - constrains use (child, column, row)
        Label nameLabel = new Label("Question:");
        nameLabel.setId("bold-label");
        GridPane.setConstraints(nameLabel, 0, 1);
        TextField nameInput = new TextField();
        nameInput.setPromptText("what is your first hero ?");
        GridPane.setConstraints(nameInput, 1, 1);

        Label ansLabel = new Label("Answer:");
        GridPane.setConstraints(ansLabel, 0, 2);
        TextField ansInput = new TextField();
        ansInput.setPromptText("Jinx");
        GridPane.setConstraints(ansInput, 1, 2);

        Label passLabel = new Label("Password:");
        GridPane.setConstraints(passLabel, 0, 3);
        TextField passInput = new TextField();
        passInput.setPromptText("password");
        GridPane.setConstraints(passInput, 1, 3);


        Button loginButton = new Button("Ok");
        GridPane.setConstraints(loginButton, 1, 4);


        loginButton.setOnAction(e ->{
            MessageContent mc = new MessageContent(MessageType.CS_CHECK_SET);
            mc.connectType = ConnectType.SERVER;
            mc.myPassword = passInput.getText();
            mc.securityAns = ansInput.getText();
            mc.securityQue = nameInput.getText();
            // 检查密码的长度
            int p = mc.myPassword.length();
            int a = mc.securityAns.length();
            int b = mc.securityQue.length();
            System.out.println(mc.toString());
            if((a == 0 && b != 0) ||(a != 0 && b == 0)){
                notification.setText("security question exception");
            }else if(a == 0){
                mc.securityAns = null;
                mc.securityQue = null;
                if(p != 0){
                    if(p < 3){
                        notification.setText("password too short !");
                    }else if(p > 20){
                        notification.setText("password too long !");
                    }else {
                        Client.getInstance().send(mc); // 其他的情况表示只有所有都是空
                    }
                }
            }else{
                if(p == 0){
                    mc.myPassword = null; // 清除password 选项
                    Client.getInstance().send(mc);
                }else if (p < 3){
                    notification.setText("password too short !");
                }else if(p > 20) {
                    notification.setText("password too long !");
                }else {
                    Client.getInstance().send(mc);
                }
            }
            window.close();
        });

        //Add everything to grid
        grid.getChildren().addAll(nameLabel, nameInput, passLabel, passInput,
                loginButton, ansInput, ansLabel, notification);
        Scene scene = new Scene(grid, 300, 200);
        scene.getStylesheets().add(getClass().getResource("rsc/css/Viper.css").toExternalForm());
        window.setScene(scene);
        window.show();
    }
}
