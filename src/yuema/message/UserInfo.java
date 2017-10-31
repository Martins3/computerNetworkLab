package yuema.message;

import java.util.LinkedList;

/**
 * Created by martin on 17-10-17.
 * @warning 只有message 的内容是公用的
 *
 */

public class UserInfo extends User {
    private LinkedList<String> allFriends;
    private String passwordQuestion;
    private String passwordAns;
    private String password;
    private boolean isActive;


    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LinkedList<String> getAllFriends() {
        return allFriends;
    }

    public void setAllFriends(LinkedList<String> allFriends) {
        this.allFriends = allFriends;
    }

    public String getPasswordQuestion() {
        return passwordQuestion;
    }

    public void setPasswordQuestion(String passwordQuestion) {
        this.passwordQuestion = passwordQuestion;
    }

    public String getPasswordAns() {
        return passwordAns;
    }

    public void setPasswordAns(String passwordAns) {
        this.passwordAns = passwordAns;
    }

    public String getPassword() {
        return password;
    }


    public UserInfo(String userID, String userListenPort, String userHostName) {
        super(userID, userListenPort, userHostName);
        this.password = null;
        this.isActive = false;
        allFriends = new LinkedList<>();
    }



    @Override
    public String toString() {
        return "UserInfo{" +
                "userID='" + userID + '\'' +
                ", userListenPort='" + userListenPort + '\'' +
                ", userHostName='" + userHostName + '\'' +
                ", allFriends=" + allFriends +
                ", passwordQuestion='" + passwordQuestion + '\'' +
                ", passwordAns='" + passwordAns + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

}
