package yuema.server;

import com.google.gson.Gson;
import yuema.message.MessageContent;
import yuema.message.MessageType;
import yuema.message.UserInfo;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by martin on 17-10-17.
 * 处理的模式为:
 *  1. 使用相同的管线上传所有的消息
 *  2. 使用Set 管理线程:
 *      收听线程不需要进行消息处理, 只有添加 和 全部的删除
 *  3. 所有客户的第一次消息都是一定需要携带id的, 然后写入到线程的局部
 *      变量中间, 该线程上传的时候全部携带该消息
 */
class Receiver implements Runnable {

    private volatile boolean exited = false; // 似乎是没有用的
    private Socket socket;
    private BlockingQueue<MessageContent> sendToAbove;
    private ConcurrentHashMap<Integer, Receiver> receivers;
    private Gson gson;
    private boolean firstMessage;
    // 查询 和 修改 userInfo的内容
    private ConcurrentHashMap<String, UserInfo> userInformations;
    private String userId;
    private int num;




    Receiver(int num, BlockingQueue<MessageContent> sendToAbove, Socket socket,
             ConcurrentHashMap<Integer, Receiver> receiversController, ConcurrentHashMap<String, UserInfo> userInfomations){
        this.socket = socket;
        this.num = num;
        this.sendToAbove = sendToAbove;
        this.receivers = receiversController;
        this.userInformations = userInfomations;
        gson = new Gson();
        firstMessage = true;
    }


    void setExited() {
        exited = true;
        try {
            socket.close();
            System.out.println("socket closed !");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        System.out.println("run的开始的");
        System.out.println("查看已有对象: ");
        DataInputStream inFromServer = null;
        receivers.put(num, this);
        System.out.println("new receive thread begin run !");
        try {
            inFromServer = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(inFromServer == null) return;
        System.out.println("start receiver ***********************");
        try {
            while (!exited){
                String getFromClient = inFromServer.readUTF();
                MessageContent mess = gson.fromJson(getFromClient, MessageContent.class);
                if(firstMessage) {
                    firstMessage = false;
                    // 第一次 的时候, 标记现成的名称, 告诉回复的方法
                    userId = mess.myID;
                    UserInfo query = userInformations.get(userId);
                    if(query == null) {
                        query = new UserInfo(mess.myID, mess.listenPort, mess.listenHostname);
                        userInformations.put(mess.myID, query);
                    }
                    query.setUserListenPort(mess.listenPort);
                    query.setUserHostName(mess.listenHostname);
                }

                mess.connectedUserID = userId; // 上传消息的时候之处来自于哪一个
                System.out.println("收到的消息 : " + getFromClient);
                sendToAbove.add(mess);
            }
        } catch (IOException e){
            MessageContent a = new MessageContent(MessageType.CS_LOGOUT);
            a.connectedUserID = userId;
            sendToAbove.add(a);
        }
        Receiver me = receivers.remove(num);
        System.out.println("close receiver ********************");
        assert  me != null;
    }
}
