package yuema.server;

import com.google.gson.Gson;
import yuema.message.ConnectType;
import yuema.message.MessageContent;
import yuema.message.MessageType;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by martin on 17-10-17.
 * Server sender 处理的事情(不需要userInfo , 因为创建对象的时候就是已经得到了):
 *      1. 线程管理的Table
 *      3. socket
 *      4. thread id(is the user id):
 *
 */


class Sender implements Runnable {

    private Socket socket;
    private BlockingQueue<MessageContent> send;
    private ConcurrentHashMap<String, Sender> senderController;
    private Gson json;

    private String userID; // 区分线程


    Sender(Socket socket, String userID, ConcurrentHashMap<String, Sender> senderController){
        this.socket = socket;
        this.userID = userID;
        this.send = new LinkedBlockingQueue<>();
        this.senderController = senderController;
        json = new Gson();
    }


    // 收到的消息 和 接受的消息机器相关的, 首先分析类型, 然后确定回复

    void sendMessage(MessageContent message){
        send.add(message);
    }
    @Override
    public void run() {
        DataOutputStream outToClient;
        try{
            outToClient = new DataOutputStream(socket.getOutputStream());
        }catch (IOException e){
            // 客户端主动断开的时候, 需要进行处理
            e.printStackTrace();
            outToClient = null;
        }

        if(outToClient == null){
            System.out.println(userID + "已经掉线");
            senderController.remove(userID);
            return;
        }

        System.out.println("start sender --------------");
        while (true){
            try {
                MessageContent mess = send.take();
                assert mess.connectedUserID == null;
                if(mess.messageType == MessageType.POISON) break;

                outToClient.writeUTF(json.toJson(mess));
                System.out.println("发送到" + userID + ": " + json.toJson(mess));
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }


        try {
            socket.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("close sender --------------");
        senderController.remove(userID); // 线程结束的时候, 从线程池子重建去除
    }

    void setExited() {
        send.add(new MessageContent(MessageType.POISON));
    }
}
