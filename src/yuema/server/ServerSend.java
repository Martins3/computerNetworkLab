package yuema.server;

import yuema.message.MessageContent;
import yuema.message.UserInfo;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by martin on 17-10-17.
 * 1. 所有发送的消息进入此位置的, 向下实现消息的分拣
 * 2. 仅仅允许初始化一个对象的
 * 3. 相比客户端简单的就是: 所有消息发送之前全部通过查询 UserInfo 来取得 socket, 客户对于的发送的过称是:
 *       -- 询问当前是否含有发送的线程
 *       -- 此人是否已经上线
 *       -- 我是否已经创建了和该对象传送的 socket
 *    方案:   1. 创建socket 的时候, 首先询问是否含有socket,
 *           2. 创建了socket 的时候添加UserInfo中间
 *           3. 对象下线的时候的, 对应的字符串 null
 *           4. 如果持有的消息是过时的消息的时候, 在异常捕获的位置进行清除
 *
 */


public class ServerSend {
    private static volatile ServerSend instance = null;
    // 消息分拣的位置添加
    private ConcurrentHashMap<String, UserInfo> userInfos;
    private ConcurrentHashMap<String, Sender> senderController;
    private ExecutorService executor;



    private ServerSend(ConcurrentHashMap<String, UserInfo> userInfos) {
        this.userInfos = userInfos;
        this.senderController = new ConcurrentHashMap<>();
        this.executor = Executors.newScheduledThreadPool(20);
    }

    // 提供好友信息数据库
    // 待发送消息队列

    public static ServerSend getInstance(ConcurrentHashMap<String, UserInfo> receiverInfos) {
        if (instance == null) {
            synchronized (ServerSend.class) {
                if (instance == null) {
                    instance = new ServerSend(receiverInfos);
                }
            }
        }
        return instance;
    }


    // by this method, close all the connections !
    public void setExited() {
        for(Map.Entry<String, Sender> entry: senderController.entrySet()){
            entry.getValue().setExited();
        }
        executor.shutdown();
        System.out.println("all sender closed !");
    }

    // 处理消息分拣
    void sendMessage(MessageContent message){
        Sender sender = senderController.get(message.connectedUserID);
        UserInfo userInfo = userInfos.get(message.connectedUserID);
        // 发送消息的位置一定userInfo 绝对不可以为空

        // 首先询问当前是否含有发送的线程, 没有当前的发送的线程也就是意味着当前 发送任务的socket 是没有创建的
        if(sender == null){
            Socket socket;
            try {
                socket = new Socket(userInfo.getUserHostName(),
                        Integer.parseInt(userInfo.getUserListenPort()));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("服务器创建发送消息socket失败");
                return;
            }

            sender = new Sender(socket, userInfo.getUserID(), senderController);
            senderController.put(userInfo.getUserID(), sender);
            executor.execute(sender);
            message.connectedUserID = null; // connectedUserID 的作用是对于发送信息的分类,现在已经灭有意义了
            sender.sendMessage(message);

            System.out.println("send message added !");
        }else {
            message.connectedUserID = null;
            sender.sendMessage(message);
            System.out.println("send message added");
        }
    }
}
