package cz.cuni.mff.kotal.frontend.menu;

import cz.cuni.mff.kotal.frontend.menu.scenes.MenuScene;
import javafx.application.Application;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

public class MenuApp extends Application {

   private static Window stage;

   @Override
   public void start(Stage stage) throws Exception {
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
//            stage.setScene(s);
//         }
//      });
//
//      root.getChildren().add(b);

      MenuApp.stage = stage;

      stage.setTitle("Autonomous Intersection Management");

      // TODO udelat velikost poradne

      double width = Screen.getPrimary().getBounds().getWidth();
      double height = Screen.getPrimary().getBounds().getHeight();

      stage.setScene(new MenuScene(width * (1./4), height * (9./10)));
      stage.setX(0);
      stage.setY(0);
      stage.show();
   }

   public static Window getStage() {
      return stage;
   }
}
