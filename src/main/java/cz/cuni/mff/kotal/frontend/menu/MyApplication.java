package cz.cuni.mff.kotal.frontend.menu;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.swing.*;

public class MyApplication extends Application {

   private static Window menuStage;

   private static final double HEIGHT = 9. / 10;

   @Override
   public void start(Stage primaryStage) throws Exception {
//      Parent root = FXMLLoader.load(getClass().getResource("scene.fxml"));

//      Group root = new Group();
//
//      Scene scene = new Scene(root, 666, 540);
//      scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
//
//      Label label = new Label("Label");
//      label.setBorder(new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.DASHED, new CornerRadii(2.), BorderStroke.DEFAULT_WIDTHS)));
//
//      root.getChildren().add(label);
//
//      Button b = new Button("Button");
//      b.setOnMouseClicked(new EventHandler<MouseEvent>() {
//         @Override
//         public void handle(MouseEvent event) {
//            Scene s = new Scene(new Group(), 200, 200);
//            primaryStage.setScene(s);
//         }
//      });
//
//      root.getChildren().add(b);


      primaryStage.setTitle("Autonomous Intersection Management");

      // TODO udelat velikost poradne

      double width = Screen.getPrimary().getBounds().getWidth();
      double height = Screen.getPrimary().getBounds().getHeight() * HEIGHT;

      menuStage = new MenuStage(width - height, height);

      Scene scene = new Scene(new Group(), height, height);
      primaryStage.setScene(scene);
      primaryStage.setX(height);
      primaryStage.setY(0);
      primaryStage.show();
   }

   public static Window getMenuStage() {
      return menuStage;
   }

   public static double getHEIGHT() {
      return HEIGHT;
   }
}
