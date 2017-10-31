package yuema.local;

import yuema.message.*;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by martin on 17-10-12.
 * 处理和server的通信, 单独处理, 所以只是不需要两层的架构
 * 如何放置反复添加线程 : 也就是快速的点击login
 *
 *  发送消息流程: 对于分拣消息 转换消息 发送消息
 *  想要发送消息, 那么就是一定需要持有
 *  服务器的通信, 线程单独的管理
 *
 */
public class ClientSend{

    private static volatile ClientSend instance = null;
    private ConcurrentHashMap<String, User> receiverInfos; // 仅仅在于获取发送消息的时候,到底添加到谁, 不包含服务器
    private ConcurrentHashMap<String, Sender> senderController;
    private ExecutorService executor;
    private String userId;
    private String serverHostname;


    // 登陆成功设置自己的 id 是什么
    void setUserId(String userId) {
        System.out.println("lock the user id is " + userId);
        this.userId = userId;
    }

    // 设置 serverHostName 需要保证ClientSend 的初始化的位置
    void setServerHostname(String ip){
        serverHostname = ip;
    }

    private ClientSend(ConcurrentHashMap<String, User> receiverInfos) {
        this.receiverInfos = receiverInfos;
        this.senderController = new ConcurrentHashMap<>();
        this.executor = Executors.newScheduledThreadPool(10);
        serverHostname = "127.0.0.1"; // 默认连接本机
    }

    // 提供好友信息数据库
    // 待发送消息队列

    public static ClientSend getInstance(ConcurrentHashMap<String, User> receiverInfos) {
        if (instance == null) {
            synchronized (ClientSend.class) {
                if (instance == null) {
                    instance = new ClientSend(receiverInfos);
                }
            }
        }
        return instance;
    }


    // by this method, close all the connections !
    public void setExited() {
        for(Map.Entry<String,Sender> entry: senderController.entrySet()){
            entry.getValue().setExited();
        }
        executor.shutdown();
    }

    // client can send message only by this method
    /**
     * @param message 对于message 不会添加任何修改, 直接发送*/
    public int send(MessageContent message){
        // 分拣消息的, 添加到对应的Sender 的对象中间
        // 如果不含有到达该, 创建线程. 接受或者创建 socket
        // 本层次负责处理添加
        if(message.connectType == ConnectType.CLIENT){
            message.connectType = null;
            Sender sender;
            String anotherClient = message.connectedUserID;
            message.myID = userId; // 无论发送到谁, 都是需要告知自己的id


            // 没有该线程的情况下: 第一次发送消息 对面的人没有上线
            Socket socket;
            User userInfo = receiverInfos.get(anotherClient);

            if (userInfo == null) {
                // 该用户没有上线,使用离线的消息
                // 重新包装消息
                message.friendID = anotherClient;
                message.connectedUserID = null;
                return sendTOServer(message);
            } else {
                if (!senderController.keySet().contains(anotherClient)) {
                    // 防止还是没有创建过该线程,POISON 成为第一条消息
                    if(message.messageType == MessageType.POISON) return 1;

                    try {
                        socket = new Socket(userInfo.getUserHostName(),
                                Integer.parseInt(userInfo.getUserListenPort()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        // 查询到该用户, 但是却没有办法发送消息
                        System.out.println("--------------- out date socket exception -----------");
                        System.out.println("--------------- this is impossible -------------------");
                        return sendTOServer(message);
                    }
                    sender = new Sender(socket, message.connectedUserID, senderController);
                    executor.execute(sender);
                    message.connectedUserID = null;
                    sender.addMessage(message);
                } else {
                    // 直接发送
                    sender = senderController.get(anotherClient);
                    message.connectedUserID = null;
                    sender.addMessage(message);
                }
            }
        }else{
            return sendTOServer(message);
        }
        return 0;
    }

    private int  sendTOServer(MessageContent message){
        // 首先查询服务器在不在, 没有区分server 和 client的线程
        Sender sender;
        message.connectType = null;
        if(!senderController.keySet().contains("1")) {
            int maxTryTime = 3;
            Socket socket = null;
            int tryTimes = 0;
            boolean inception = false;
            while (tryTimes < maxTryTime) {
                try {
                    socket = new Socket(serverHostname, 6789);
                } catch (IOException e) {
                    System.out.println("failed connected to server :" + tryTimes + " time");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                        tryTimes = maxTryTime;
                    }
                    tryTimes++;
                    inception = true;
                }
                if(!inception) break;
            }
            if(tryTimes == maxTryTime){
                return -1;
            }
            System.out.println("和服务器建立第一次连接");
            sender = new Sender(socket, "1", senderController);
            executor.execute(sender);
            try {
                // 需要知道到底使用哪一个网卡
                message.listenHostname = NetInterfaceIP.localHostname();
                message.listenPort = Client.getInstance().getReceivePort() + "";
            } catch (SocketException e) {
                e.printStackTrace();
            }
            sender.addMessage(message);
        }else {
            sender = senderController.get("1");
            sender.addMessage(message);
        }
        return 0;
    }
}


