package cz.cuni.mff.kotal.frontend.intersection;

import cz.cuni.mff.kotal.frontend.MyApplication;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;

public class IntersectionScene extends Scene {
   public static final double PADDING = 30;

   private static final VBox MENU = new IntersectionMenu(PADDING);
   private static final IntersectionGraph GRAPH = new IntersectionGraph(Screen.getPrimary().getBounds().getHeight() * MyApplication.getHEIGHT_RATIO());
   private static final HBox ROOT = new HBox(PADDING, MENU, GRAPH);

   public IntersectionScene(double width, double height) {
      super(ROOT, width, height);
      ROOT.setPadding(new Insets(PADDING, PADDING, PADDING, PADDING));

      heightProperty().addListener((observable, oldValue, newValue) -> {
         if (newValue.doubleValue() < getWidth() - MENU.getWidth() - PADDING) {
            IntersectionGraph.setPreferredHeight(newValue.doubleValue() - PADDING * 2);
            GRAPH.redraw();
         }
      });

      widthProperty().addListener((observable, oldValue, newValue) -> {
         if (newValue.doubleValue() < IntersectionGraph.getPreferredHeight() + MENU.getWidth() + 3 * PADDING ||
            newValue.doubleValue() > oldValue.doubleValue() && newValue.doubleValue() < getHeight() + MENU.getWidth() + PADDING) {
            IntersectionGraph.setPreferredHeight(newValue.doubleValue() - MENU.getWidth() - 3 * PADDING);
            GRAPH.redraw();
         }
      });


      MENU.setPrefWidth(width - height + PADDING);
      MENU.setMinWidth(width - height + PADDING);
   }

   public static IntersectionGraph getIntersectionGraph() {
      return GRAPH;
   }
}
