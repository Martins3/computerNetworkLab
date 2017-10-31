package yuema.FileTransfer;

import yuema.local.Client;
import yuema.message.ConnectType;
import yuema.message.MessageContent;
import yuema.message.MessageType;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by martin on 17-10-21.
 * 1. 第一个包需要持有的内容:
 *      1. 第一个包使用长度特殊
 *      2. 告知文件的长度, 从而计算出来
 * 接受的数据写入的位置是什么 ?
 *
 * 2. 实现的模式为: 对方发送, 只有准备好了之后才会允许发送方进行发送
 *  仅仅实现简单的版本, 也就是只有
 *
 *  3. 直接开辟一个很大的空间(应该可以优化)
 *  4. 不处理最后的一个文件返回ack 可能丢失的问题
 *  5. 不添加baseNum 只是进行写入, 尽管可能发生对于相同的位置的多次写入
 *  6. 相对路径, 不知道如何处理
 *
 *
 */
public class FileReceiver implements Runnable{
    private static volatile FileReceiver instance;
    private DatagramSocket serverSocket;

    private String dir = "/home/martin/Network/download/";
    private byte [] sendData; // 发送的数据一定是一个ack,直接为int
    private byte[] receiveData;
    private DatagramPacket receivePacket; // 防止反复创建新的对象
    private final int MSS = 1024; // 其实可以靠Tcp 实现制定MSS

    private BlockingQueue<MessageContent> fileTOReceive; // 接受的文件,
    private Client client;


    public void add(MessageContent mc){
        fileTOReceive.add(mc);
    }


    public void setExited(){
        // 发送毒素
        fileTOReceive.add(new MessageContent(MessageType.POISON));
    }


    public static FileReceiver getInstance(int port){
        if (instance == null) {
            synchronized (FileReceiver.class) {
                if (instance == null) {
                    instance = new FileReceiver(port);
                }
            }
        }
        return instance;
    }

    private FileReceiver(int port){
        // 创建接受的文件, 所有的接受的文件全部默认的写入位置
        receiveData = new byte[MSS + 32 / 8];
        fileTOReceive = new LinkedBlockingQueue<>();
        client = Client.getInstance();
        try {
            serverSocket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


/*
* -------------------------------- kernel part -------------------------------------------------------
* */

    @Override
    public void run() {
        sendData = new byte[32 / 8];
        while (true) {
            MessageContent file;
            System.out.println("----------- file receive thread start --------------------");
            try {
                // 同时告知文件的发送方可以开始发送文件了, 可以开始发送消息了
                file =  fileTOReceive.take();
                if(file.messageType == MessageType.POISON) break;

                // 弹出选择框图来,告诉接受方选择
                // 可以进行打断的
                if(client.getReceiveDir() != null){
                    dir = client.getReceiveDir();
                }

                MessageContent mc = new MessageContent(MessageType.PERMIT_SEND_FILE);
                mc.connectType = ConnectType.CLIENT;
                mc.connectedUserID = file.myID;
                mc.filePath = file.filePath;
                client.send(mc);

            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }


            int byteSize = Integer.parseInt(file.byteSize);
            int totalPackageNumber = byteSize / MSS;
            int lastPkgSize = byteSize % MSS;
            if(lastPkgSize !=0) totalPackageNumber ++;

            String fileName = file.fileName;
            ByteBuffer fileInRam = ByteBuffer.allocate(byteSize);
            System.out.println("start receive\n receive dir :" + dir+'\n');


            while (true) {
                // 接受数据
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                try {
                    serverSocket.receive(receivePacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] allData = receivePacket.getData();
                sendData = Arrays.copyOfRange(allData, MSS, allData.length);
                int seqNum =  ByteBuffer.wrap(sendData).getInt();

                byte[] data;
                if(seqNum == totalPackageNumber - 1){
                    // 最后的一个包的大小可变
                    data = Arrays.copyOfRange(allData, 0, lastPkgSize);
                }else {
                    data = Arrays.copyOfRange(allData, 0, MSS);
                }
                fileInRam.put(data);




                // 回执ack
                InetAddress IPAddress = receivePacket.getAddress();
                int port = receivePacket.getPort();
                DatagramPacket sendPacket =
                        new DatagramPacket(sendData, sendData.length, IPAddress, port);
                try {
                    serverSocket.send(sendPacket);
                    System.out.printf("发送ACK" + seqNum);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 如果是最后的一个文件, 那么写入磁盘中间
                if(seqNum == totalPackageNumber - 1){
                    try {
                        FileOutputStream out = new FileOutputStream(dir + fileName);
                        out.write(fileInRam.array());
                        out.close();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        System.out.println("------------  file receive thread over      -------------------");
    }
}

