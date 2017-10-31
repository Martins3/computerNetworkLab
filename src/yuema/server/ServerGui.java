package yuema.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import yuema.message.MessageContent;
import yuema.message.MessageType;
import yuema.message.NetInterfaceIP;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by martin on 17-10-17.
 * the gui of server: init the essential data structure !
 */
public class ServerGui extends Application {
    private Server server;
    private Task<Void> task;
    private BlockingQueue<MessageContent> message;
    private ListView<String> a;
    private ShowUser showUser;
    @Override
    public void start(Stage window) throws Exception {
        // 初始化必要的变量
        server = Server.getInstance();
        new Thread(server).start();
        a = new ListView<>();
        message = server.getBlockingQueue();
        showUser = ShowUser.getInstance();

        a.setOnMouseClicked( e ->{
              String name = a.getSelectionModel().getSelectedItem();
              if(name != null){
                  showUser.display(name);
              }
        });


        // 展示本机ip
        Label showIP = new Label(NetInterfaceIP.localHostname());
        showIP.minHeight(30);
        showIP.setAlignment(Pos.CENTER);

        VBox layout = new VBox();
        layout.getChildren().addAll(showIP, a);
        Scene scene = new Scene(layout, 300, 300);

        window.getIcons().add(new Image(ServerGui.class.getResourceAsStream("rsc/server.png")));
        window.setTitle("同城夜约会");
        window.setScene(scene);
        window.show();

        task = start();
    }

    @Override
    public void stop(){
        server.setExited();
        task.cancel();
    }


    private Task<Void> start() {
        Task<Void> listener = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("loginWindow background start !");
                while (!isCancelled()) {
                    MessageContent mess = message.take();
                    if(mess.messageType == MessageType.POISON) break;
                    switch (mess.messageType){
                        case SC_FRIEND_DOWN:
                            Platform.runLater(() -> a.getItems().remove(mess.connectedUserID));
                            break;
                        case SC_FRIEND_UP:
                            Platform.runLater(() -> a.getItems().add(mess.connectedUserID));
                            break;
                        default:
                            System.out.println("server gui bug !");
                    }
                }
                System.out.println("loginWindow background over");
                return null;
            }
        };
        new Thread(listener).start();
        return listener;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
