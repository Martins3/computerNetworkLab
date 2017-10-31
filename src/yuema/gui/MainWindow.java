package yuema.gui;

import com.google.common.io.ByteStreams;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import yuema.local.Client;
import yuema.message.ConnectType;
import yuema.message.MessageContent;
import yuema.message.MessageType;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by martin on 17-10-11.
 * 封装说明:
 *      1. 发送消息直接发送,至于是不是离线, 由
 *      2.
 */
public class MainWindow implements Initializable {

    @FXML
    private ScrollPane chatLogContainer;

    @FXML
    private HBox onlineUsersHbox;
    //界面根容器
    @FXML private BorderPane borderPane;
    //用户头像, 暂时是一个装饰的作用
    @FXML private ImageView userImageView;
    //用户名, 根据最上面listView 确定
    @FXML private Label usernameLabel;
    //在线用户列表
    @FXML private ListView<String> onlineUserListView;
    //在线用户人数
    @FXML private Label userCountLabel;

    //消息显示列表
    @FXML private ListView<String> chatPaneListView;
    //消息发送框
    @FXML private TextArea messageBoxTextArea;
    //对话消息按钮
    @FXML private ListView<String> offlineUserListView;
    //聊天对象显示文本框
    @FXML
    private Label connectedUserName;

    // 所有的消息全部都是 : client to client
    // 服务器不会保存到之前的任何的消息, userHistoryMessage g关机之后消失
    private BlockingQueue<MessageContent> acceptedMessage;
    private ConcurrentHashMap<String, ListView<String>> userHistoryMessage;
    private Client client;
    private Task<Void> task;
    private String toWhom;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
            userHistoryMessage = new ConcurrentHashMap<>();
            acceptedMessage = new LinkedBlockingQueue<>();
            client = Client.getInstance();
            task = start();
            toWhom = null;
            messageBoxTextArea.setVisible(false);
    }

    // client 会把所有和主界面有关的消息通过此方法添加
    public void addMessage(MessageContent mess){
        acceptedMessage.add(mess);
    }


    private Task<Void> start() {
        Task<Void> listener = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("mainWindow background start !");
                while (!isCancelled()) {
                    MessageContent mess = acceptedMessage.take();
                    if(mess.messageType == MessageType.POISON) break;
                    switch (mess.messageType){
                        // listView 是一个显示控件, 同时还是数据结构
                        // 只是知道在有人下线区域, 但是不知道是不是本身在上线的位置下线的
                        // 似乎不是很清真
                        case SC_FRIEND_UP:
                            Platform.runLater(() ->{
                                Platform.setImplicitExit(true);
                                ObservableList<String> a = onlineUserListView.getItems();
                                if(!a.contains(mess.friendID)) a.add(mess.friendID);
                                offlineUserListView.getItems().remove(mess.friendID);
                            });
                            break;
                        case SC_FRIEND_DOWN:
                            Platform.runLater(() -> {
                                Platform.setImplicitExit(true);
                                onlineUserListView.getItems().remove(mess.friendID);
                                ObservableList<String> a = offlineUserListView.getItems();
                                if(!a.contains(mess.friendID)) a.add(mess.friendID);
                            });
                            break;
                        case SC_NO_SUCH_GUY:
                            System.out.println("提示没有哪一个用户");
                            break;
                        case COMMON_MESSAGE:
                            String friend = mess.myID;
                            String content = mess.content;
                            assert onlineUserListView.getItems().contains(friend)
                                    || offlineUserListView.getItems().contains(friend);


                            ListView<String> a = userHistoryMessage.computeIfAbsent(friend, k -> listViewCreator());
                            Platform.runLater(() -> {
                                a.getItems().add(content);
                            });
                            break;
                    }
                }
                System.out.println("mainWindow background over");
                return null;
            }
        };
        new Thread(listener).start();
        return listener;
    }


    public void setExited() {
        acceptedMessage.add(new MessageContent(MessageType.POISON));
        task.cancel();
    }





    /*
    * ------------------------------- view methods -----------------------------------------
    * */

    private ListView<String> listViewCreator(){
        ListView<String> listView = new ListView<>();
        listView.setStyle("-fx-control-inner-background: WHITE;");
        listView.setCellFactory(lst ->
                new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setPrefHeight(0);
                            setText(null);
                        } else {
                            String[] lines = item.split("\r\n|\r|\n");
                            int count = lines.length + 1;
                            setPrefHeight(count * 20);
                            setText(item);
                        }
                    }
                });
        return listView;
    }

    // 回到登录界面的位置
    // 不会保证文件传输没有问题, 如果不选择地址的话
    public void logoutImgViewPressedAction(MouseEvent mouseEvent) {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    File selectedDirectory =
                            directoryChooser.showDialog(new Stage());
                    client.setReceiveDir(selectedDirectory.toString());
    }





    /**
     * @bug 没有办法处理输入消换行的问题
     * @bug 修改布局文件,让布局文件得到的结果是下方含有镶边的
     *
     * */
    public void sendMethod(KeyEvent keyEvent) {
        KeyCombination c = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHIFT_DOWN);
        if(c.match(keyEvent)){
            messageBoxTextArea.appendText("\n");
        }else if(keyEvent.getCode() == KeyCode.ENTER){
            String message = messageBoxTextArea.getText();
            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            String time  = dateFormat.format(date);
            message = time + "\n" + message;
            chatPaneListView.getItems().add(message);

            // 清空消息
            keyEvent.consume();
            messageBoxTextArea.clear();

            // 同时消息发送到指定的用户上面去
            MessageContent toUser = new MessageContent(MessageType.COMMON_MESSAGE);
            toUser.connectType = ConnectType.CLIENT;
            toUser.connectedUserID = toWhom;
            toUser.content = message;
            client.send(toUser);
        }
    }



    // 点击人物的头像的结果
    public void clickImage(MouseEvent mouseEvent) {
        AlertBox.showMe(client.getMyName());
    }


    public void clickOfflineUser(MouseEvent mouseEvent) {
        String a = offlineUserListView.getSelectionModel().getSelectedItem();
        clickOnUser(a);
    }


    public void clickOnlineUser(MouseEvent mouseEvent) {

        String a = onlineUserListView.getSelectionModel().getSelectedItem();
        clickOnUser(a);
    }

   private void clickOnUser(String a){
       if(a == null) return;

       messageBoxTextArea.setVisible(true);
       if(a.equals(toWhom)) return;

       // 修改当前的对话人主题
       connectedUserName.setText(a);

       // 查询数据库
       chatPaneListView = userHistoryMessage.get(a);
       if(chatPaneListView == null){
           ListView<String> newAdded = listViewCreator();
           userHistoryMessage.put(a, newAdded);
           chatPaneListView = newAdded;
       }
       chatLogContainer.setContent(chatPaneListView);

       // 修改当前对话对象
       toWhom = a;
       System.out.println("click on user:" + a);
   }





    /*
    * ---------------------- 其他界面方法 ------------------------------------------
    * */


    // 跳出对话框出来, 可以直接修改
    public void setSecurityQuestionOrPassword(MouseEvent mouseEvent) {
        System.out.println("change security question or password !");
        new SetPPAP().display();
    }

    // 添加好友的界面
    public void addFriends(MouseEvent mouseEvent) {
        System.out.println("add friends");
        if(client == null) client = Client.getInstance();
        new AddFriends(client).display();
    }


    // 选择发送的文件
    public void chooseSendFile(MouseEvent mouseEvent) {
        if(toWhom == null) return;
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(new Stage());

        if (file != null) {
            MessageContent request = new MessageContent(MessageType.REQUEST_SEND_LOCAL);
            request.fileName = file.getName();
            request.filePath = file.getPath();
            request.byteSize = String.valueOf(file.length());
            request.connectedUserID = toWhom;
            request.connectType = ConnectType.CLIENT;
            client.send(request);
            System.out.println("maybe click twice !");
        }else{
            System.out.println("!!!!!!!!!!!!!!!! impossible !!!!!!!!!!!!!!!!!!!!!!!!!");
        }
    }





}

