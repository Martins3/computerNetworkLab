package yuema.server;



import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import yuema.message.MessageContent;
import yuema.message.MessageType;
import yuema.message.User;
import yuema.message.UserInfo;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Stack;
import java.util.concurrent.*;


/**
 * Created by martin on 17-10-11.
 * server 需要保存的信息, 所有的
 * https://www.veracode.com/blog/research/encryption-and-decryption-java-cryptography 加密的内容
 * 接口说明:
 *  1. 向上提供上线的人的数据
 *  2. 上面可以控制服务器维护的做法
 *
 *
 * 内部实现:
 *  1. 所有的线程的创建都是根据接受socket 而得到
 *  2. 得到的socket 告诉线程的名称: 所有的上线的时候含有用户的名称和用户名, 同时设置的线程的名称
 *  3. 使用TreeSet 管理线程, 拒接使用 ThreadPool 管理(文档不清楚)
 *  4. 通信的协议:
 *      1. 所有自动状态机来处理登录过程, 密码找回
 *          1. 可以使用的方法是: 在未确认的登陆过程中间的,所有的消息都是添加首部消息的, 在通信确认之后
 *          ,发送的消息含有目标的消息, 也就是所有的行为添加标注
 *      2. 用户名 -u
 *         密码 -p
 *         消息 -m
 *         找回密码 -f -问题标号 -答案
 *         查找好友 -ff
 *
 *         根据当前的环境确定到底是设置还是确认
 *      3. 服务发送回去的消息
 *          1. 所有上线好友的 hostName sendId 账号
 *          2. 离线消息
 *  5. 服务器和客户的通信: FSM的客户登录, 离线消息保存
 *
 *
 *
 *
 * 1. 有没有必要添加一个是谁在线的问题:
 *      1. 可以遍历userInfos 来确定
 *      2. 可以使用线程 TreeSet 分析, 那么就是需要进行进行销毁线程的时候, 将线程去除, 在线程
 *      3. TreeSet 使用 thread 还是 client, 使用client 分析
 *
 *
 *
 * 处理所有的消息内容:
 *
 */

public class Server implements Runnable{
    // 保存用户的消息, 用户的消息也是需要使用使用文件保存


    // 用户的信息

    private ConcurrentHashMap<String, UserInfo> userInfos; // 只有用户的永久信息,而不是登录的消息
    private ConcurrentSkipListSet<String> activeUsers; // 在线用户
    private ConcurrentHashMap<String, Stack<MessageContent>> storeMessage; // 存储的离线消息,根据到底是谁建立的消息联系
    private static volatile Server instance;
    private BlockingQueue<MessageContent> receivedMessage;
    private ServerSend serverSend;
    private ServerReceive serverReceive;
    private final String userInfoData;
    private final String storeMessageData;
    private Gson gson;
    private BlockingQueue<MessageContent> serverGuiBlockingQueue;
    private Encryption e;



    private void loadStoreMessage(){
        storeMessage = new ConcurrentHashMap<>();
        System.out.println("we do create a storeMessage");
    }

    private void dumpStoreMessage(){

    }

    private String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
    private void loadUserInfo(){
        // 本应该从文件中间得到, 如何确定
        String content;
        try {
            content = readFile(userInfoData, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        System.out.println(content);
        content = e.vulnerableDecrypt(content);
        System.out.println("加载的内容" + "\n" + content);
        if(content.length() != 0) {
            Type type = new TypeToken<ConcurrentHashMap<String, UserInfo>>() {}.getType();
            userInfos = gson.fromJson(content, type);
        }else {
            userInfos = new ConcurrentHashMap<>();
        }
    }

    private void dumpUserInfo(){
        String jsonString = gson.toJson(userInfos);
        System.out.println("保存的消息" + "\n" +jsonString);
        try{
            BufferedWriter out = new BufferedWriter(new FileWriter(userInfoData));
            out.write(e.vulnerableEncrypt(jsonString));
            System.out.println(e.vulnerableEncrypt(jsonString));
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Server(){
        // 加载预先的数据
        URL url = getClass().getResource("rsc/userInfo");
        userInfoData = url.getPath().replaceFirst("^/(.:/)", "$1");
        url = getClass().getResource("rsc/userStoredMessage");
        storeMessageData = url.getPath();
        System.out.println(userInfoData);
        // 初始化加密算法类
        e = Encryption.getInstance();

        gson = new Gson();
        serverGuiBlockingQueue = new LinkedBlockingQueue<>();
        activeUsers = new ConcurrentSkipListSet<>();
        loadUserInfo();
        loadStoreMessage();
        // 初始化接受消息队列
        receivedMessage = new LinkedBlockingQueue<>();
        // 初始化sendTool and receiveTool
        serverReceive = ServerReceive.getInstance(receivedMessage, userInfos);
        new Thread(serverReceive).start();
        serverSend = ServerSend.getInstance(userInfos);
    }

    UserInfo query(String name){
        UserInfo a = userInfos.get(name);
        assert a != null;
        return  a;
    }

    static Server getInstance(){
        if (instance == null) {
            synchronized (Server.class) {
                if (instance == null) {
                    instance = new Server();
                }
            }
        }
        return instance;
    }



    // 如果需要产生UI, 那么就是 只是需要从 instance  中间获取数据
    // 本class 就是所有数据的集合
    // run 方法需要处理的事情为: 从receivedMessage 中间取出所有的消息, 进行一一回复
    @Override
    public void run() {
        while (true){
            MessageContent c;
            MessageContent send;
            UserInfo userInfo;
            try {
                c = receivedMessage.take();
                if(c.messageType == MessageType.POISON){
                    // 主线程需要关闭的时候, 将所有的线程进行回收
                    serverSend.setExited();
                    serverReceive.setExited();
                    break;
                }
                // 查询该消息来自于谁, 一定不为空
                userInfo = userInfos.get(c.connectedUserID);

                switch (c.messageType){
                    case CS_SIGN_UP:
                        if(userInfo.getPassword() == null) {
                            send = new MessageContent(MessageType.SC_SIGN_UP_OK);
                            userInfo.setPassword(c.myPassword);
                        }else {
                            send = new MessageContent(MessageType.SC_SIGN_UP_FAIL);
                        }
                        send.connectedUserID = c.connectedUserID; // 告诉下一层到底发送谁
                        serverSend.sendMessage(send);
                        break;
                    case CS_LOGIN:
                        // 虽然所有的尝试都是会添加到userInfo 中间, 但是默认password 为 null
                        if(userInfo.getPassword().equals(c.myPassword)){
                            send = new MessageContent(MessageType.SC_LOGIN_OK);
                            // 登陆成功, 添加在线用户中间
                            activeUsers.add(c.connectedUserID);

                            // 告知服务器的显示
                            MessageContent toServerGui = new MessageContent(MessageType.SC_FRIEND_UP);
                            toServerGui.connectedUserID = c.connectedUserID;
                            serverGuiBlockingQueue.add(toServerGui);

                            // 标记自己的状态为上线
                            userInfo.setActive(true);

                            // 告知他的所有的在线好友, 这一个人已经上线 同时告知他 他的好友的状态
                            userGetFriendsInfo(userInfo);

                            // 写入离线的消息
                            Stack<MessageContent> offlineMessage = storeMessage.get(c.connectedUserID);
                            if(offlineMessage != null){
                                while (!offlineMessage.isEmpty()){
                                    serverSend.sendMessage(offlineMessage.pop());
                                }
                            }
                        }else{
                            send = new MessageContent(MessageType.SC_LOGIN_FAIL);
                        }
                        send.myID = c.myID;
                        send.connectedUserID = c.myID; // 告诉下一层到底发送谁
                        serverSend.sendMessage(send);
                        break;
                    case CS_CHECK_SET:
                        // check's meaning is dynamic and becoming confusing
                        // here it's password and ans and que but other place password is excluded
                        if(c.myPassword != null){
                            userInfo.setPassword(c.myPassword);
                        }
                        if(c.securityAns != null){
                            userInfo.setPasswordAns(c.securityAns);
                            userInfo.setPasswordQuestion(c.securityQue);
                        }
                        break;
                    case CS_CHECK_QUERY:
                        // 查询是否含有该用户

                        // 查询是否含有该问题

                        // 查询答案是否正确
                        MessageContent mc;
                        if(userInfo.getPassword() != null &&
                                c.securityQue.equals(userInfo.getPasswordQuestion()) &&
                                c.securityAns.equals(userInfo.getPasswordAns())){
                            mc = new MessageContent(MessageType.SC_CHECK_OK);
                        }else {
                            mc = new MessageContent(MessageType.SC_CHECK_FAIL);
                        }
                        mc.connectedUserID = c.connectedUserID;
                        serverSend.sendMessage(mc);
                        break;
                    case CS_LOGOUT:
                        System.out.println("&&&&  " + c.connectedUserID + "logout  &&&&");
                        activeUsers.remove(c.connectedUserID);
                        userInfo.setActive(false);

                        // 告知服务器的显示
                        MessageContent toServerGui = new MessageContent(MessageType.SC_FRIEND_DOWN);
                        toServerGui.connectedUserID = c.connectedUserID;
                        serverGuiBlockingQueue.add(toServerGui);

                        // 告诉好友下线了
                        offlineNotify(userInfo);


                        // 告知服务器的收听可以关闭
                        MessageContent closeServer = new MessageContent(MessageType.POISON);
                        closeServer.connectedUserID = c.connectedUserID;
                        serverSend.sendMessage(closeServer);
                        break;

                    case COMMON_MESSAGE:
                        // 会不会有客户已经上线, 但是还是收到了离线消息 ?
                        // 服务器发送离线消息触发: 上线

                        Stack<MessageContent> messStack = storeMessage.computeIfAbsent(c.friendID, k -> new Stack<>());
                        c.connectedUserID = c.friendID;
                        c.friendID = null;
                        messStack.add(c);
                        System.out.println("add a common message to server:\n" + c.toString());
                        break;
                    case CS_FIND_FRIEND:
                        // 检查服务器中间是否含有该人
                        // 消息类型 + friendId
                        System.out.println("server get the request for friends !");
                        String friend = c.friendID;
                        UserInfo friend_info = userInfos.get(friend);
                        if(friend_info == null){
                            send = new MessageContent(MessageType.SC_NO_SUCH_GUY);
                        }else if(activeUsers.contains(friend)) {
                            send = new MessageContent(MessageType.SC_FRIEND_UP);
                            // 如果activeUsers 包含friend, 那么一定hostname port 一定有效
                            send.listenHostname = friend_info.getUserHostName();
                            send.listenPort = friend_info.getUserListenPort();
                            // 修改双方的好友列表
                            userInfo.getAllFriends().add(friend);
                            friend_info.getAllFriends().add(c.connectedUserID);

                            // 告知被添加方有人加好友
                            MessageContent notifyFriends = new MessageContent(MessageType.SC_FRIEND_UP);
                            notifyFriends.connectedUserID = friend;
                            notifyFriends.friendID = c.connectedUserID;
                            notifyFriends.listenHostname = userInfo.getUserHostName();
                            notifyFriends.listenPort = userInfo.getUserListenPort();
                            serverSend.sendMessage(notifyFriends);

                        }else {
                            send = new MessageContent(MessageType.SC_FRIEND_DOWN);
                        }
                        send.friendID = friend;
                        send.connectedUserID = c.connectedUserID; // 告诉下一层到底发送谁
                        serverSend.sendMessage(send);
                        break;
                    case CS_NEW_PASSWORD:
                        // 之前发送用户名称的时候已经持有该用户的hostName port
                        // 此处需要修改的为用户的密码 并不执行 其他的操作, 帮助用户回到主界面
                        userInfo.setPassword(c.myPassword);
                        send = new MessageContent(MessageType.SC_RESET_OK);
                        send.connectedUserID = c.connectedUserID;
                        serverSend.sendMessage(send);
                        break;

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void offlineNotify(UserInfo userInfo){
        LinkedList<String> userFriends = userInfo.getAllFriends();
        MessageContent sendToFriend;
        for(String i:userFriends) {
            UserInfo friend = userInfos.get(i);
            if (friend.isActive()) {
                // 上线用户告知 和 被告知
                sendToFriend = new MessageContent(MessageType.SC_FRIEND_DOWN);
                sendToFriend.friendID = userInfo.getUserID();
                sendToFriend.connectedUserID = i;
                serverSend.sendMessage(sendToFriend);
            }
        }
    }


    private void userGetFriendsInfo(UserInfo userInfo){
        LinkedList<String> userFriends = userInfo.getAllFriends();
        MessageContent sendToFriend;
        MessageContent sendToMe;
        for(String i:userFriends){
            UserInfo friend = userInfos.get(i);
            if(friend.isActive()){
                // 上线用户告知 和 被告知
                sendToFriend = new MessageContent(MessageType.SC_FRIEND_UP);
                sendToFriend.listenPort = userInfo.getUserListenPort();
                sendToFriend.listenHostname = userInfo.getUserHostName();
                sendToFriend.friendID = userInfo.getUserID();
                sendToFriend.connectedUserID = i;

                sendToMe = new MessageContent(MessageType.SC_FRIEND_UP);
                sendToMe.listenPort = friend.getUserListenPort();
                sendToMe.listenHostname = friend.getUserHostName();
                sendToMe.friendID = i;
                sendToMe.connectedUserID = userInfo.getUserID();


                serverSend.sendMessage(sendToFriend);
                serverSend.sendMessage(sendToMe);
            }else{
                // 非上线的用户 被告知
                sendToMe = new MessageContent(MessageType.SC_FRIEND_DOWN);
                sendToMe.friendID = i;
                sendToMe.connectedUserID = userInfo.getUserID();
                serverSend.sendMessage(sendToMe);
            }
        }
    }



    public void setExited() {
        receivedMessage.add(new MessageContent(MessageType.POISON));
        dumpStoreMessage();
        dumpUserInfo();
    }


    public BlockingQueue<MessageContent> getBlockingQueue() {
        return serverGuiBlockingQueue;
    }
}




