package cz.cuni.mff.kotal.frontend.intersection;

import cz.cuni.mff.kotal.frontend.MyApplication;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;

public class IntersectionScene extends Scene {
   public static final double PADDING = 15;

   private static final VBox MENU = new IntersectionMenu(PADDING);
   private static final IntersectionGraph GRAPH = new IntersectionGraph(Screen.getPrimary().getBounds().getHeight() * MyApplication.getHEIGHT_RATIO());
   private static final HBox ROOT = new HBox(MENU, GRAPH);

   public IntersectionScene(double width, double height) {
      super(ROOT, width, height);

      heightProperty().addListener((observable, oldValue, newValue) -> {
         double newHeight = Math.min(newValue.doubleValue(), getWidth() - MENU.getMinWidth());
         IntersectionGraph.setPreferredHeight(newHeight);
         MENU.setPrefWidth(getWidth() - newHeight);
         GRAPH.redraw();
      });

      widthProperty().addListener((observable, oldValue, newValue) -> {
         double newHeight = Math.min(getHeight(), newValue.doubleValue() - MENU.getMinWidth());
         IntersectionGraph.setPreferredHeight(newHeight);
         MENU.setPrefWidth(newValue.doubleValue() - newHeight - 1);
         GRAPH.redraw();
      });


      double menuWidth = width - height;
      MENU.setPrefWidth(menuWidth);
      MENU.setMinWidth(menuWidth);
      MENU.setPadding(new Insets(PADDING, PADDING, PADDING, PADDING));

//      IntersectionMenu.getPlayButton().setPrefWidth(menuWidth / 2 - PADDING / 2);
      IntersectionMenu.getPlayButton().setMinWidth(menuWidth / 2 - PADDING);
      IntersectionMenu.getPlayButton().setPrefWidth(Double.MAX_VALUE);
//      IntersectionMenu.getRestartButton().setPrefWidth(menuWidth / 2 - PADDING / 2);
      IntersectionMenu.getRestartButton().setMinWidth(menuWidth / 2 - PADDING);
      IntersectionMenu.getRestartButton().setPrefWidth(Double.MAX_VALUE);
   }

   public static IntersectionGraph getIntersectionGraph() {
      return GRAPH;
   }

   public static VBox getMENU() {
      return MENU;
   }
}
