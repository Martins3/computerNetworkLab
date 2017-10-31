package yuema.gui;/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import yuema.local.Client;
import yuema.message.ConnectType;
import yuema.message.MessageContent;
import yuema.message.MessageType;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author hubachelar
 *
 * 尽可能的利用已有界面:
 *  1. loginWindow 很有三总场景
 *      1. 登录界面, 提交和反馈信息        signup login
 *      2. 验证密保                     确定 取消
 *      3. 设置密保阶段 Security Question : 确定, 取消
 *
 *  2. 创建线程仅仅在此类中间创建, 所以创建之后的
 *
 *  3. 服务器的消息如何传递出来
 *  https://stackoverflow.com/questions/18880455/task-vs-service-for-database-operations
 */
public class LoginWindow implements Initializable{


    @FXML
    private Label regain;

    @FXML
    private Label theme;

    @FXML
    private  Label notification;

    @FXML
    private JFXTextField password;

    @FXML
    private JFXButton signUpButton;


    @FXML
    private JFXTextField userID;


    @FXML
    private JFXButton loginButton;


    private Client client;
    private volatile BlockingQueue<MessageContent> message;
    private Task<Void> task;
    private LoginStage loginStage;
    private String secureID;


    // 含有风险
    public void setClient(Client client) {
        this.client = client;
    }

    public void addMessage(MessageContent m){
        message.add(m);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        message = new LinkedBlockingQueue<>();
        task = start();
        loginStage = LoginStage.LOGIN_SINGUP;
    }

    // 返回对象,消息中间含有 interrupt, 可能情况有问题 ?
    private Task<Void> start(){
        Task<Void> listener = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("loginWindow background start !");
                while (!isCancelled()) {
                    // 一旦切换界面之后, 可以关掉此线程
                    MessageContent mess = message.take();
                    if(mess.messageType == MessageType.POISON) break;
                    switch (mess.messageType){
                        case SC_LOGIN_OK:
                            Platform.runLater(() -> client.loginOk());
                            break;
                        case SC_LOGIN_FAIL:
                            Platform.runLater(() ->{
                                System.out.println("注册失败");
                                notification.setText("Wrong userInfo or username !");
                            });
                            break;
                        case SC_SIGN_UP_OK:
                            System.out.println("注册成功");
                            break;
                        case SC_SIGN_UP_FAIL:
                            System.out.println("注册失败");
                            break;
                        case SC_CHECK_FAIL:
                            System.out.println("密保错误");
                            Platform.runLater(() -> notification.setText("Unaccepted"));
                            break;
                        case SC_CHECK_OK:
                            System.out.println("密保正确");
                            Platform.runLater(() -> {
                                // 显示出来密码框
                                // 同时隐藏其他的框, 一单按下确定的按键的时候, 发送密码 同时装换主界面
                                loginStage = LoginStage.RESET;
                                userID.setPromptText("new password");
                                password.setPromptText("set again");
                                userID.clear();
                                password.clear();
                            });
                            break;
                        case SC_RESET_OK:
                            Platform.runLater(() -> goBack());
                            break;
                    }
                }
                System.out.println("loginWindow background over");
                return null;
            }
        };
        new Thread(listener).start();
        return listener;
    }





    public void login(MouseEvent mouseEvent) {
        switch (loginStage){
            case LOGIN_SINGUP:
                loginAndSignUp(MessageType.CS_LOGIN);
                break;
            case INPUT_ID:
                // 获取用户的id
                boolean validUsername = regainPasswordHelp();
                if(validUsername){
                    loginStage = LoginStage.CHECK;
                    password.setVisible(true);
                    // 切换界面
                    theme.setText("Security Question");
                    userID.setPromptText("your security question");
                    password.setPromptText("your security answer");
                    loginButton.setText("set");
                    signUpButton.setText("cancel");
                    secureID = userID.getText();
                    userID.clear();
                    password.clear();
                }
                break;
            case CHECK:
                // 用户输入密保问题 和 密保答案, 发送消息到服务器
                MessageContent a = new MessageContent(MessageType.CS_CHECK_QUERY);
                a.myID = secureID;
                a.connectType = ConnectType.SERVER;
                a.securityQue = userID.getText();
                a.securityAns = password.getText();
                if(a.securityAns.length() > 0 && a.securityQue.length() > 0) {
                    if (client.send(a) == -1) {
                        notification.setText("Network is blocked !");
                    }
                }else {
                    notification.setText("Wrong security format !");
                }
                break;
            case RESET:
                String userId = userID.getText();
                String pass = password.getText();
                if(resetPasswordHelp(userId, pass)){
                    MessageContent mc = new MessageContent(MessageType.CS_NEW_PASSWORD);
                    mc.myPassword = userId;
                    client.send(mc);
                }
                break;
        }

    }



    public void signUp(MouseEvent mouseEvent) {
        switch (loginStage){
            case LOGIN_SINGUP:
                loginAndSignUp(MessageType.CS_SIGN_UP);
                break;
            default:
                goBack();
                break;
        }
    }

    private void loginAndSignUp(MessageType messageType){
        String passwords = password.getText();
        String userId = userID.getText();
        if(!checkKV(userId, passwords)) return;
        MessageContent a = new MessageContent(messageType);
        a.myPassword = passwords;
        a.myID = userId;
        a.connectType = ConnectType.SERVER;
        if(client.send(a) == -1){
            notification.setText("Network is blocked !");
        }
    }


    private boolean checkKV(String userId, String passwords){
        if (!userId.matches("[a-zA-Z0-9]*")) {
            notification.setText("Illegal character in userInfo");
            return false;
        }
        if(userId.length() > 20){
            notification.setText("username is too long !");
            return false;
        }else if(userId.length() < 3){
            notification.setText("username is too short");
            return false;
        }

        if(passwords.length() > 50){
            notification.setText("Password is too long !");
            return false;
        }else if(passwords.length() < 3){
            notification.setText("Password is too short !");
            return false;
        }
        return true;
    }




    public void setExited() {
        message.add(new MessageContent(MessageType.POISON));
        task.cancel();
    }



    // 点击 forget password, 进入到填写用户名称的位置
    public void regainPassword(MouseEvent mouseEvent) {
        switch (loginStage) {
            case LOGIN_SINGUP:
                theme.setText("Input your username");
                loginButton.setText("set");
                signUpButton.setText("cancel");
                regain.setText("");
                password.setVisible(false);
                loginStage = LoginStage.INPUT_ID;
            default:
                // 仅仅在login signUp 的时候响应
                break;
        }
    }

    // 还原所有的场景, 进入到开始的阶段
    private void goBack(){
        theme.setText("");
        notification.setText("");
        loginButton.setText("login");
        signUpButton.setText("sign up");
        regain.setText("forget password");
        password.setVisible(true);
        userID.setPromptText("username");
        password.setPromptText("password");
        userID.clear();
        password.clear();
        loginStage = LoginStage.LOGIN_SINGUP;
    }

    private boolean regainPasswordHelp(){
        String userId = userID.getText();
        if (!userId.matches("[a-zA-Z0-9]*")) {
            notification.setText("Illegal character in userInfo");
            return false;
        }
        if(userId.length() > 20){
            notification.setText("username is too long !");
            return false;
        }else if(userId.length() < 3){
            notification.setText("username is too short");
            return false;
        }
        return true;
    }

    private boolean resetPasswordHelp(String userId, String pass){
        if(!userId.equals(pass)){
            notification.setText("two password different !");
            return false;
        }
        if(pass.length() > 20){
            notification.setText("new password is too long !");
            return false;
        }else if(pass.length() < 3){
            notification.setText("new password is too short");
            return false;
        }
        return true;
    }

    public void setServerIP(MouseEvent mouseEvent) {
        // 设置新ip
        AlertBox.display("Set IP", client);
    }
}


