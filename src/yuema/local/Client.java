package yuema.local;

import com.google.gson.Gson;
import javafx.scene.Scene;
import javafx.stage.Stage;
import yuema.FileTransfer.FileReceiver;
import yuema.FileTransfer.FileSender;
import yuema.gui.LoginWindow;
import yuema.gui.MainWindow;
import yuema.message.ConnectType;
import yuema.message.MessageContent;
import yuema.message.MessageType;
import yuema.message.User;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by martin on 17-10-18.
 * 希望的设计原则是: ClientReceive  接受所有的消息, 根据message 或者 connection tye 进行发送到不同的
 *  总共含有四个容器处理在线和非在线用户
 */
public class Client implements Runnable {

    // 有可能使用, 但是有可能不适用的东西 ?
    // 所有的发送全部添加到这一队列中间,

    private volatile boolean closeCheckThread = false;
    private Gson jsonHelper;
    private static volatile  Client instance;
    private ConcurrentHashMap<String, User> activeUser; // 用于得到可以发送消息的用户的ip 和 hostName
    private ConcurrentSkipListSet<String> nonActiveUser; // 非活跃的用户, 暂时无用, 只是
    private BlockingQueue<MessageContent> allReceivedMessage;
    private ClientSend clientSend;
    private ClientReceive clientReceive;
    private int receivePort; // reserved!

    // 直接负责处理界面
    private MainWindow mainWindowLoader;
    private LoginWindow loginWindowLoader;
    private Scene loginScene;

    // 文件处理类
    private FileSender fileSender;
    private FileReceiver fileReceiver;
    private Scene mainScene;
    private Stage window;



    public int getReceivePort() {
        return clientReceive.getReceivePort();
    }

    public ConcurrentHashMap<String, User> getActiveUser() {
        return activeUser;
    }

    public void setExited(){
        allReceivedMessage.add(new MessageContent(MessageType.POISON));
        clientReceive.setExited();
        clientSend.setExited();
        loginWindowLoader.setExited();
        mainWindowLoader.setExited();
        if(fileReceiver != null) fileReceiver.setExited();
        if(fileSender != null) fileSender.setExited();
    }

    private Client(){
        jsonHelper = new Gson();
        activeUser = new ConcurrentHashMap<>();
        allReceivedMessage = new LinkedBlockingQueue<>();
        clientSend = ClientSend.getInstance(activeUser);
        clientReceive = ClientReceive.getInstance(allReceivedMessage);
        new Thread(clientReceive).start();

    }

    public static Client getInstance(){
        if (instance == null) {
            synchronized (Client.class) {
                if (instance == null) {
                    instance = new Client();
                }
            }
        }
        return instance;
    }



    @Override
    public void run() {
        // 进行数据的交换
        loginWindowLoader.setClient(instance);


        while (true){
            try {
                MessageContent mess = allReceivedMessage.take();
                // 作为所有的消息的中心, 实现消息的处理
                if(mess.messageType == MessageType.POISON) break;
                switch (mess.messageType){
                    case SC_SIGN_UP_FAIL:
                        loginWindowLoader.addMessage(mess);
                        break;
                    case SC_SIGN_UP_OK:
                        loginWindowLoader.addMessage(mess);
                        break;
                    case SC_LOGIN_FAIL:
                        loginWindowLoader.addMessage(mess);
                        break;
                    case SC_LOGIN_OK:

                        System.out.println("login ok added !");
                        loginWindowLoader.addMessage(mess);
                        // server 发送的时候不会清除 connectedUserID
                        // by this way, we
                        clientSend.setUserId(mess.myID);


                        // 只有登录完成之后才可以实现文件的传输

                        fileReceiver = FileReceiver.getInstance(getReceivePort());
                        new Thread(fileReceiver).start();

                        fileSender = FileSender.getInstance();
                        new Thread(fileSender).start();

                        break;
                    case SC_FRIEND_DOWN:
                        boolean active = activeUser.keySet().contains(mess.friendID);
                        if(!active){
                            System.out.println("adding friends");
                        }else{
                            System.out.println(mess.friendID + "is out of connection !");
                            // 回收发送的线程, 上线的用户未必含有发送的线程
                            MessageContent destroySender = new MessageContent(MessageType.POISON);
                            destroySender.connectedUserID = mess.friendID;
                            destroySender.connectType = ConnectType.CLIENT;
                            clientSend.send(destroySender);

                            // 客户不需要保存任何 non-active User的信息
                            User user = activeUser.remove(mess.friendID);
                            assert user != null;
                        }
                        mainWindowLoader.addMessage(mess);
                        break;
                    case SC_FRIEND_UP:
                        // 对于上线的用户, 需要持有起数据库
                        activeUser.put(mess.friendID, new User(mess.friendID, mess.listenPort, mess.listenHostname));
                        mainWindowLoader.addMessage(mess);
                        break;
                    case SC_NO_SUCH_GUY:
                        mainWindowLoader.addMessage(mess);
                        break;
                    case COMMON_MESSAGE:
                        mainWindowLoader.addMessage(mess);
                        break;
                    case REQUEST_SEND_FILE:
                        // 使用相同的收听的端口
                        // 只有在线的用户才是可以发送文件的
                        fileReceiver.add(mess);
                    case PERMIT_SEND_FILE:
                        fileSender.begin(mess);
                    case SC_CHECK_OK:
                        loginWindowLoader.addMessage(mess);
                        break;
                    case SC_CHECK_FAIL:
                        loginWindowLoader.addMessage(mess);
                        break;
                    case SC_RESET_OK:
                        loginWindowLoader.addMessage(mess);
                        break;

                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    // 获取界面参数
    public void setLayout(LoginWindow loginWindowLoader, MainWindow mainWindowLoader,
                          Scene loginScene, Scene mainScene, Stage window) {
        this.loginWindowLoader = loginWindowLoader;
        this.mainWindowLoader = mainWindowLoader;
        this.window = window;
        this.loginScene = loginScene;
        this.mainScene = mainScene;
    }

    public Stage getWindow() {
        return window;
    }


    public int send(MessageContent a) {
        // 发送文件请求时候, 需要添加请求的队列中间
        // 保证只有发送完毕的时候才可以发送新的请求
        if(a.messageType == MessageType.REQUEST_SEND_LOCAL){
            fileSender.newSendingFileRequest(a);
            System.out.println("add to sending request !");
            return 1;
        }
        return clientSend.send(a);
    }

    public void loginOk() {
        System.out.println("转换界面");
        window.setScene(mainScene);
    }

    public void setServerIP(String text) {
        clientSend.setServerHostname(text);
    }
}
