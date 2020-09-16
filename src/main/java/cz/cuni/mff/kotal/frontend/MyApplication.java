package cz.cuni.mff.kotal.frontend;

import cz.cuni.mff.kotal.frontend.intersection.IntersectionGraph;
import cz.cuni.mff.kotal.frontend.menu.MenuStage;
import cz.cuni.mff.kotal.frontend.menu.tabs.IntersectionMenuTab0;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

public class MyApplication extends Application {
   private static final double HEIGHT_RATIO = 9. / 10;

   private static Window menuStage;
   private static final IntersectionGraph graph = new IntersectionGraph(Screen.getPrimary().getBounds().getHeight() * HEIGHT_RATIO);

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
      double height = Screen.getPrimary().getBounds().getHeight() * HEIGHT_RATIO;

      menuStage = new MenuStage(width - height - 300, height);

      Scene scene = new Scene(graph, height, height);
      primaryStage.setScene(scene);
      primaryStage.setX(width - height - 220);
      primaryStage.setY(0);
      primaryStage.show();

      graph.drawSquareModel(IntersectionMenuTab0.getGranularity().getValue(), IntersectionMenuTab0.getEntries().getValue(), IntersectionMenuTab0.getExits().getValue());
   }

   public static Window getMenuStage() {
      return menuStage;
   }

   public static IntersectionGraph getIntersectionGraph() {
      return graph;
   }

   public static double getHEIGHT_RATIO() {
      return HEIGHT_RATIO;
   }
}