package yuema.server;


import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import yuema.message.UserInfo;

/**
 * Created by martin on 17-10-30.
 */
public class ShowUser {
    private static volatile ShowUser instance;
    private Server server;
    static ShowUser getInstance(){
        if (instance == null) {
            synchronized (ShowUser.class) {
                if (instance == null) {
                    instance = new ShowUser();
                }
            }
        }
        return instance;
    }

    void display(String userName){
        if(server == null) server = Server.getInstance();
        UserInfo userInfo = server.query(userName);

        Stage window = new Stage();
        window.resizableProperty().setValue(Boolean.FALSE);
        window.setTitle(userName);
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        Label a = new Label("port:");
        GridPane.setConstraints(a, 0, 0);
        Label b = new Label(String.valueOf(userInfo.getUserListenPort()));
        GridPane.setConstraints(b, 1, 0);

        //Name Label - constrains use (child, column, row)
        Label c = new Label("hostname:");
        GridPane.setConstraints(c, 0, 1);
        Label d = new Label(String.valueOf(userInfo.getUserHostName()));
        GridPane.setConstraints(d, 1, 1);

        Label e = new Label("security que:");
        GridPane.setConstraints(e, 0, 2);
        Label f = new Label(String.valueOf(userInfo.getPasswordQuestion()));
        GridPane.setConstraints(f, 1, 2);

        Label g = new Label("security ans");
        GridPane.setConstraints(g, 0, 4);
        Label h = new Label(String.valueOf(userInfo.getPasswordAns()));
        GridPane.setConstraints(h, 1, 4);


        Label i = new Label("password:");
        GridPane.setConstraints(i, 0, 5);
        Label j = new Label(String.valueOf(userInfo.getPassword()));
        GridPane.setConstraints(j, 1, 5);


        //Add everything to grid
        grid.getChildren().addAll(a, b, c, d, e, f, g, h, i, j);
        Scene scene = new Scene(grid, 300, 200);
        scene.getStylesheets().add(getClass().getResource("rsc/Viper.css").toExternalForm());
        window.setScene(scene);
        window.show();
    }
}
