package cz.cuni.mff.kotal.frontend.intersection;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class IntersectionMenu extends VBox {
   private static boolean abstractMode = false;

   public IntersectionMenu(double padding) {
      super(padding);

      TextField test = new TextField("Test");
      test.setPrefWidth(Double.MAX_VALUE);
      test.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.DEFAULT_WIDTHS)));

      Button intersectionMode = new Button("Real");
      intersectionMode.setPrefWidth(Double.MAX_VALUE);
      intersectionMode.setOnMouseClicked(event -> {
         abstractMode = !abstractMode;
         intersectionMode.setText(abstractMode ? "Abstract" : "Real");
         IntersectionScene.getIntersectionGraph().redraw();
      });

      getChildren().addAll(test, intersectionMode);
   }

   public static boolean isAbstract() {
      return abstractMode;
   }
}
