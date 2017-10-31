package yuema.message;

import java.net.Socket;

/**
 * Created by martin on 17-10-17.
 */
public class User {
    String userID;
    String userListenPort;
    String userHostName;


    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserListenPort() {
        return userListenPort;
    }

    public void setUserListenPort(String userListenPort) {
        this.userListenPort = userListenPort;
    }

    public String getUserHostName() {
        return userHostName;
    }

    public void setUserHostName(String userHostName) {
        this.userHostName = userHostName;
    }




    @Override
    public String toString() {
        return "User{" +
                "userID='" + userID + '\'' +
                ", userListenPort='" + userListenPort + '\'' +
                ", userHostName='" + userHostName + '\'' +
                '}';
    }

    public User(String userID, String userListenPort, String userHostName) {
        this.userID = userID;
        this.userListenPort = userListenPort;
        this.userHostName = userHostName;
    }
}
