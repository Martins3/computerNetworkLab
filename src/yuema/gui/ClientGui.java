package yuema.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import yuema.local.Client;

/**
 * Created by martin on 17-10-11.
 * 1. 重拍消息的顺序是不可能的吗 ?
 * 2. 服务器只有被动的建立连接, 但是客户端含有主动的连接和被动的连接
 *
 * 在此处的打开的线程分析的问题是什么: 系统消息, 如果涉及到的界面的时候, 界面使用concurrent data structure 处理
 *  1. 上线好友 和 下线好友
 *
 *
 *  含有一个大问题: 子线程的while 循环的速度实在是太快了, 应该对于消息队列的检查应该是含有 sleep 的
 *
 *
 */
public class ClientGui extends Application {

    // 有可能使用, 但是有可能不适用的东西 ?
    // 所有的发送全部添加到这一队列中间,
    private Client client;
    @Override
    public void start(Stage window) throws Exception {

        // 启动的时候首先进入登录的界面
        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("resource/FXMLDocument.fxml"));
        Parent root = loginLoader.load();
        Scene scene = new Scene(root);

        FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("resource/MainWindow.fxml"));
        Parent mainRoot = mainLoader.load();
        Scene mainScene = new Scene(mainRoot);

        LoginWindow loginController = loginLoader.getController();
        MainWindow mainWindow = mainLoader.getController();

        window.resizableProperty().setValue(Boolean.FALSE);
        window.getIcons().add(new Image(ClientGui.class.getResourceAsStream("resource/user.jpg")));
        window.setScene(scene);
        window.show();

        // 处理上线用户的消息
        client =  Client.getInstance();
        client.setLayout(loginController, mainWindow, scene, mainScene, window);
        new Thread(client).start();
    }


    @Override
    public void stop(){
        client.setExited();
    }

    public static void main(String[] args) {
        launch(args);
    }


}


