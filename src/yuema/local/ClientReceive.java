package yuema.local;

import yuema.message.MessageContent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by martin on 17-10-16.
 *
 *
 *
 */
public class ClientReceive implements Runnable{

    private BlockingQueue<MessageContent> sendToAbove;
    private volatile static ClientReceive instance;
    private ExecutorService executor;
    private ConcurrentHashMap<Integer, Receiver> receiversController;
    private ServerSocket welcomeSocket;
    private int receivePort;
    private int num;

    public int getReceivePort() {
        assert receivePort != 0;
        return receivePort;
    }

    private void setWelcomeSocket() {
        try {
            welcomeSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ClientReceive getInstance(BlockingQueue<MessageContent> sendToAbove) {
        if (instance == null) {
            synchronized (ClientReceive.class) {
                if (instance == null) {
                    instance = new ClientReceive(sendToAbove);
                }
            }
        }
        return instance;
    }

    private ClientReceive(BlockingQueue<MessageContent> sendToAbove){
        this.sendToAbove = sendToAbove;
        executor = Executors.newScheduledThreadPool(20);
        receiversController = new ConcurrentHashMap<>();
        welcomeSocket = null;
        num = 0;
    }

    public void setExited(){
        for(Map.Entry<Integer,Receiver> receiver:receiversController.entrySet()){
            receiver.getValue().setExited();
        }
        executor.shutdown();
        setWelcomeSocket();
    }



    @Override
    public void run() {
        try {
            welcomeSocket = new ServerSocket(0);
            receivePort = welcomeSocket.getLocalPort();
        } catch (IOException e) {
            System.out.println("端口占用");
            e.printStackTrace();
            return;
        }
        Socket connectionSocket;
        while (true) {
            try {
                connectionSocket = welcomeSocket.accept(); // 检测到新的执行
            } catch (IOException e) {
                System.out.println("主动的关闭了WelcomeSocket");
                return;
            }
            Receiver a = new Receiver(num, sendToAbove, connectionSocket, receiversController);
            num ++;
            executor.execute(a);
        }
    }
}
