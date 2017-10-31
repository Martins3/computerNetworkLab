package yuema.message;

import com.google.gson.Gson;

/**
 * Created by martin on 17-10-16.
 * 设计的原则是: 所有特殊内容全部都是含有自己的名称的, 普通消息就是content
 */
public class MessageContent {

   // common
   public ConnectType connectType; // 使用的位置是什么:
   public MessageType messageType;
   public String myPassword;
   public String securityAns;
   public String securityQue;
   public String myID;        // 发送离线消息的时候需要, 如此之后就是直接进入离线消息队列之中

   // client to server
   public String connectedUserID; // 只有发送普通的消息的时候才会添加
   public String listenPort;
   public String listenHostname;
   public String friendID;
   public String content;

   // file
   public String filePath; // windows unix compatible
   public String fileName;
   public String byteSize;


    @Override
    public String toString() {
        return "MessageContent{" +
                "connectType=" + connectType +
                ", messageType=" + messageType +
                ", myPassword='" + myPassword + '\'' +
                ", securityAns='" + securityAns + '\'' +
                ", securityQue='" + securityQue + '\'' +
                ", myID='" + myID + '\'' +
                ", connectedUserID='" + connectedUserID + '\'' +
                ", listenPort='" + listenPort + '\'' +
                ", listenHostname='" + listenHostname + '\'' +
                ", friendID='" + friendID + '\'' +
                ", content='" + content + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileName='" + fileName + '\'' +
                ", byteSize='" + byteSize + '\'' +
                '}';
    }

    public MessageContent(MessageType messageType){
      this.messageType = messageType;
   }
}
