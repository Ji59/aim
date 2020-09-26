package cz.cuni.mff.kotal.frontend;

import cz.cuni.mff.kotal.frontend.intersection.IntersectionGraph;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
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

   @Override
   public void start(Stage primaryStage) throws Exception {
      primaryStage.setTitle("Autonomous Intersection Management");

      // TODO udelat velikost poradne

      double width = Screen.getPrimary().getBounds().getWidth();
      double height = Screen.getPrimary().getBounds().getHeight() * HEIGHT_RATIO;

      menuStage = new MenuStage(width - height - 300, height);

      Scene scene = new IntersectionScene(height + 220, height);
      primaryStage.setScene(scene);
      primaryStage.setX(width - height - 300);
      primaryStage.setY(0);
      primaryStage.show();

      IntersectionScene.getIntersectionGraph().redraw();
   }

   public static Window getMenuStage() {
      return menuStage;
   }

   public static double getHEIGHT_RATIO() {
      return HEIGHT_RATIO;
   }
}