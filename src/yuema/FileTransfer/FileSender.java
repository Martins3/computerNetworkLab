package yuema.FileTransfer;


import com.google.common.io.ByteStreams;
import yuema.local.Client;
import yuema.message.MessageContent;
import yuema.message.MessageType;
import yuema.message.User;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by martin on 17-10-21.
 * 需要应对随时变化的package的长度的变化的处理
 * InetAddress IPAddress = InetAddress.getByName("127.0.0.1");
 *
 *
 * 什么时候会超时:
 *      1. 之后的数据全部都是没有办法到达, 全部都是超时的, 此时的计算时间 有 偏差
 *      2. 出现了repeat ack
 *
 * 重复的ack的设置 原理需要核对一下 !
 *
 *
 *
 *File file =new File("c:\\java_xml_logo.jpg");

 if(file.exists())
 */
public class FileSender implements Runnable{
    private static volatile FileSender instance;
    private int MSS = 1024; // 不知道如何合理的设置windowSize
    private int timeOut = 1000;
    private DatagramSocket sendSocket;
    private int windowSize;
    private byte[] array;
    private int lastPackageNum;
    private BlockingQueue<MessageContent> fileToSend;
    private BlockingQueue<MessageContent> requestSending;
    private Client client;
    private int port;
    private InetAddress address;



    public void setExited() {
        // 现在仅仅分析了理想的情况, 发送文件, 结束, 然后等待
        requestSending.add(new MessageContent(MessageType.POISON));
    }


    public static FileSender getInstance(){
        if (instance == null) {
            synchronized (FileSender.class) {
                if (instance == null) {
                    instance = new FileSender();
                }
            }
        }
        return instance;
    }

    private FileSender(){
        fileToSend = new LinkedBlockingQueue<>();
        requestSending = new LinkedBlockingQueue<>();
        windowSize = 100; // maybe window size too small
        client = Client.getInstance();
        try {
            sendSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    // client send method 被重定向到此处
    public void newSendingFileRequest(MessageContent mc) {
        requestSending.add(mc);
        System.out.println("we can file added ! !");
    }

    public void begin(MessageContent mc) {
        fileToSend.add(mc);
    }

    /**
     * 如何计算的rtt :
     *      1. 对于超时 官方的处理是什么 ?
     *      2. 如何计算rtt, 或者近似的rtt
     * */
    @Override
    public void run() {
        System.out.println("--------- file send thread start -------");
        while (true){
            MessageContent file;
            try {
                MessageContent  request = requestSending.take();
                if(request.messageType ==MessageType.POISON) break;

                // 去掉中间的标志
                request.messageType = MessageType.REQUEST_SEND_FILE;
                client.send(request);


                file = fileToSend.take();
                User user = client.getActiveUser().get(file.myID);
                address = InetAddress.getByName(user.getUserHostName());
                port = Integer.parseInt(user.getUserListenPort());
                System.out.println("start sending file:");
                System.out.println("address ---------->" + address.toString());
                System.out.println("port ------------->" + port);
            } catch (InterruptedException | UnknownHostException e) {
                e.printStackTrace();
                return;
            }


            try {
                InputStream inputStream = new DataInputStream(
                        new FileInputStream(file.filePath));
                array = ByteStreams.toByteArray(inputStream);
                lastPackageNum = array.length / MSS;
                if(array.length % MSS > 0) lastPackageNum ++;


                // 创建通信的连接
                int baseNum = 0; // 期待到达序号
                int nextSegNum = 0; // window的右侧第一个数据
                Queue<Integer> tobeSend = new ArrayDeque<>();

                // 初始化基本额数据添加
                while (nextSegNum < baseNum + windowSize){
                    tobeSend.add(nextSegNum);
                    nextSegNum ++;
                }


                // 指出接受数据放置的位置
                byte[] receiveAck = new byte[32 / 8];
                DatagramPacket receivePacket =
                        new DatagramPacket(receiveAck, receiveAck.length);
                // we need a better data structure too label the acks
                // 当前的ack 支持的数据含有上限
                PriorityQueue<Integer> labeledACK = new PriorityQueue<>();
                int repeatACK = 0;
                boolean transferOver = false;



                // 开始进入到时间的循环之中
                while (true) {
                    // 发送数据窗口之下的数据
                    while (!tobeSend.isEmpty()){
                        sendPackage(tobeSend.poll());
                    }


                    // 接受数据, 只要接收到baseNum 的 ack
                    while (true) {
                        sendSocket.setSoTimeout(timeOut); // 留下修改的接口
                        timeOut += 100;
                        try {
                            sendSocket.receive(receivePacket);
                        }catch (SocketTimeoutException e){
                            // 发现超时, 重发
                            tobeSend.add(baseNum);
                            e.printStackTrace();
                            break;
                        }

                        // if window size is dynamic , tobeSend.add() should be used careful !
                        int ack = ByteBuffer.wrap(receivePacket.getData()).getInt();
                        if (ack == baseNum) {
                            // 顺序的到达
                            baseNum ++;
                            tobeSend.add(nextSegNum);
                            nextSegNum ++;

                            // 接受到最后一个 ack
                            if(baseNum == lastPackageNum - 1){
                                transferOver = true;
                                break;
                            }

                            if(!labeledACK.isEmpty()){
                                // 终于填补上来的了
                                while (true) {
                                    Integer min = labeledACK.peek();
                                    if(min == null) break;

                                    // 正好可以连接起来
                                    if (min == baseNum){
                                        baseNum ++;
                                        tobeSend.add(nextSegNum);
                                        labeledACK.poll();
                                        nextSegNum ++;
                                    }else{
                                        break;
                                    }
                                }
                            }
                            break; // 窗口向右移动, 可以发送数据


                        } else {
                            if(ack < baseNum){
                                System.out.println("we received a weired ack : " + ack);
                                continue;
                            }
                            // 添加标记数据
                            labeledACK.add(ack);
                            repeatACK ++;
                            if(repeatACK == 3){ // 实现快速的重传数据
                                tobeSend.add(baseNum);
                                repeatACK = 0;
                                System.out.println("repeat ack!");
                                break;
                            }

                        }
                    }
                    if(transferOver) break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("--------- file send thread over -------------------------");
    }

    // 可能添加拥塞控制
    private int adjustWindowSize(){
        return windowSize;
    }



    public int  adjustTimeOut() {
        return timeOut;
    }


    // 发送区: 实现普通的发送 特定package 的重发
    // 计数 从 0 开始
    private void sendPackage(int sendPkgNum){
        // 判断是否会越界, 有没有必要的
        if(sendPkgNum >= lastPackageNum) return;
        // 取出 byte
        int deist = Math.min(array.length, (sendPkgNum + 1)* MSS);


        byte[] sendData = Arrays.copyOfRange(array, sendPkgNum * MSS, deist);  // 仅仅添加一个序号
        // 添加 ack
        sendData = makePackage(sendData, sendPkgNum);
        // 发送数据
        DatagramPacket sendPacket =
                new DatagramPacket(sendData, sendData.length, address, port);
        try {
            sendSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] makePackage(byte[] data, int seqNum){
        // 保持发送的大小稳定
        ByteBuffer u = ByteBuffer.allocate(MSS + 32 / 8);
        u.put(data);
        u.putInt(MSS, seqNum);
        return u.array();
    }
}

