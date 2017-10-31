package yuema.local;

import com.google.gson.Gson;
import yuema.message.MessageContent;
import yuema.message.MessageType;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by martin on 17-10-16.
 * 所有的发送消息都是没有没有区别的, 服务器的名称为 "1"
 *
 * 忽然发现 exit 的消息没有意义
 */

class Sender implements  Runnable{
    private Socket socket;
    private BlockingQueue<MessageContent> send;
    private ConcurrentHashMap<String, Sender> sendController;
    private String name; // 发送方的名称
    private Gson json;


    public void setExited() {
        send.add(new MessageContent(MessageType.POISON));
    }

    Sender(Socket socket, String name, ConcurrentHashMap<String, Sender> sendController){
        this.socket = socket;
        this.name = name;
        this.send = new LinkedBlockingQueue<>();
        this.sendController = sendController;
        json = new Gson();
    }


    @Override
    public void run() {
        DataOutputStream outToServer = null;
        sendController.put(name, this);
        try {
            outToServer = new DataOutputStream(socket.getOutputStream());
        }catch (IOException e) {
           e.printStackTrace();
        }


        if(outToServer == null){
            System.out.println("time out failed, over !");
            sendController.remove(name);
            return;
        }

        // 成功的建立了消息机制之后, 需要添加确定而接受消息的端口
        // 也许如果在多机器环境的时候,可以简单的处理

        // 时刻小心连接重置之后的得到东西, 貌似还有更加复杂问题, 向上层发送关闭进程的指令
        System.out.println("client send thread begin ---------------");
        try {
            while (true){
                // 发送消息只是无脑的发送消息, 和和接受消息没有关系, 由于所有的消息都是含有标签的,消息的顺序不重要
                // 传递消息, 全部都是需要使用 blocking queue, 至于如何阻塞线程 ??
                MessageContent mess = send.take();
                if(mess.messageType == MessageType.POISON) break;
                System.out.println("发送出去的的消息:" + json.toJson(mess));
                assert mess.connectedUserID == null;
                outToServer.writeUTF(json.toJson(mess));
            }
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }

        System.out.println("client send thread over ----------------");
        // 告知received 的线程关闭
        //
        sendController.remove(name);
    }

    public void addMessage(MessageContent mess) {
        send.add(mess);
    }
}
