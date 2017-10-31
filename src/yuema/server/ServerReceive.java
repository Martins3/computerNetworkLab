package yuema.server;

import yuema.local.ClientReceive;
import yuema.message.MessageContent;
import yuema.message.UserInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by martin on 17-10-17.
 * 发送数据使用一条线,下层来分拣:
 * 接受的数据不需要多线, 无需分拣:
 *  1. normal message => store message
 *  2. user login/signUp message => handle now
 *
 *  => copy from ClientReceive is just ok !
 *  => fix a bug, receive action creates a socket, these sockets should send to others,
 *
 *  提供的作用: 提供一个Blocking Queue 上传消息
 *              修改收到的user的hostname and id
 *
 *
 */
public class ServerReceive implements Runnable {
    private BlockingQueue<MessageContent> sendToAbove;
    private volatile static ServerReceive instance;
    private ExecutorService executor;
    private ConcurrentHashMap<Integer, Receiver> receiversController;
    private ConcurrentHashMap<String, UserInfo> userInformation;
    private ServerSocket welcomeSocket;
    private int num;

    public static ServerReceive getInstance(BlockingQueue<MessageContent> sendToAbove,
                                            ConcurrentHashMap<String, UserInfo> userInformation) {
        if (instance == null) {
            synchronized (ClientReceive.class) {
                if (instance == null) {
                    instance = new ServerReceive(sendToAbove, userInformation);
                }
            }
        }
        return instance;
    }

    private void setWelcomeSocket() {
        try {
            welcomeSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ServerReceive(BlockingQueue<MessageContent> sendToAbove,
                          ConcurrentHashMap<String, UserInfo> userInformation){
        this.sendToAbove = sendToAbove;
        this.userInformation = userInformation;
        executor = Executors.newScheduledThreadPool(20);
        receiversController = new ConcurrentHashMap<>();
        welcomeSocket = null;
        num = 0;
    }

    public void setExited(){
        for(Map.Entry<Integer, Receiver> entry:receiversController.entrySet()){
            entry.getValue().setExited();
        }
        executor.shutdown();
        setWelcomeSocket();
        System.out.println("server receiver all closed !");
    }



    // 打开收听的端口, 是不是需要使用多线程的方法


    @Override
    public void run() {
        try {
            welcomeSocket = new ServerSocket(6789);
            System.out.println("收听开启");
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
                System.out.println("主动关闭WelcomeSocket");
                return;
            }
            System.out.println("检查到一个新的发送者");
            Receiver a = new Receiver(num, sendToAbove, connectionSocket, receiversController, userInformation);
            num ++;
            executor.execute(a);
            System.out.println("开始执行");
        }
    }
}
