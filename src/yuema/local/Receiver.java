package yuema.local;

import com.google.gson.Gson;
import yuema.message.MessageContent;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by martin on 17-10-15.
 * 线程自动回收, 根据上层负责
 *
 * 接受消息,
 *
 */

class Receiver implements Runnable{

    private volatile boolean exited;
    private Socket socket;
    private BlockingQueue<MessageContent> sendToAbove;
    private ConcurrentHashMap<Integer, Receiver> receivers;
    private Gson gson;
    private int num;


    Receiver(int num, BlockingQueue<MessageContent> sendToAbove, Socket socket, ConcurrentHashMap<Integer, Receiver> receiversController){
        this.socket = socket;
        this.sendToAbove = sendToAbove;
        this.receivers = receiversController;
        this.num = num;
        gson = new Gson();
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
        DataInputStream inFromServer = null;
        receivers.put(num, this);
        try {
            inFromServer = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(inFromServer == null) return;
        System.out.println("client receive thread begin --------------------------");
        try {
            while (!exited){
                String getFormServer = inFromServer.readUTF();
                System.out.println("收到消息" + getFormServer);
                sendToAbove.add(gson.fromJson(getFormServer, MessageContent.class));
            }
        } catch (IOException e){
            System.out.println("end the thread result from other close socket");
        }
        Receiver receiver = receivers.remove(num);
        assert receiver !=null;
        System.out.println("client receive thread over ----------------------------");

    }
}
