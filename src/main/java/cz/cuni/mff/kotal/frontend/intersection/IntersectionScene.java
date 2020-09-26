package cz.cuni.mff.kotal.frontend.intersection;

import cz.cuni.mff.kotal.frontend.MyApplication;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

public class IntersectionScene extends Scene {
   static final double PADDING = 30;

   private static final HBox root = new HBox(PADDING);
   private static final IntersectionGraph graph = new IntersectionGraph(Screen.getPrimary().getBounds().getHeight() * MyApplication.getHEIGHT_RATIO());

   public IntersectionScene(double width, double height) {
      super(root, width, height);
      VBox menu = new VBox(PADDING);

      heightProperty().addListener((observable, oldValue, newValue) -> {
         if (newValue.doubleValue() < getWidth() - menu.getWidth() - PADDING) {
            IntersectionGraph.setPreferredHeight(newValue.doubleValue() - PADDING * 2);
            graph.redraw();
         }
      });

      widthProperty().addListener((observable, oldValue, newValue) -> {
         if (newValue.doubleValue() < IntersectionGraph.getPreferredHeight() + menu.getWidth() + 3 * PADDING ||
            newValue.doubleValue() > oldValue.doubleValue() && newValue.doubleValue() < getHeight() + menu.getWidth() + PADDING) {
               IntersectionGraph.setPreferredHeight(newValue.doubleValue() - menu.getWidth() - 3 * PADDING);
               graph.redraw();
         }
      });

      root.setPadding(new Insets(PADDING, PADDING, PADDING, PADDING));

      menu.setPrefWidth(width - height - PADDING);
      TextField test = new TextField("Test");
      test.setPrefWidth(Double.MAX_VALUE);
      test.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.DEFAULT_WIDTHS)));
      menu.getChildren().addAll(test, new Label("Test1"));

      root.getChildren().addAll(menu, graph);
   }

   public static IntersectionGraph getIntersectionGraph() {
      return graph;
   }
}
