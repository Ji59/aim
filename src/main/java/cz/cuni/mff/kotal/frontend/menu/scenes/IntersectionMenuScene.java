package cz.cuni.mff.kotal.frontend.menu.scenes;

import cz.cuni.mff.kotal.frontend.menu.scenes.nodes.TitleMenuLabel;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

public class IntersectionMenuScene extends MenuScene {
   private static final AnchorPane L1 = new AnchorPane();
   private static final String TITLE_TEXT = "Intersection creation";
   private static final TitleMenuLabel TITLE_MENU = new TitleMenuLabel(TITLE_TEXT);

   public IntersectionMenuScene(double width, double height) {
      super(width, height);

//      VBox iMenuL2 = new VBox(0);
//      iMenuL2.setLayoutX(300);
//      iMenuL2.setLayoutY(300);
//      iMenuL2.setSpacing(50);
//      iMenuL2.getChildren().add(new Label("VBox0"));
//      iMenuL2.getChildren().add(new Label("VBox1"));
//
//      HBox box = new HBox(20);
//      box.getChildren().add(iMenuL2);
//      box.getChildren().add(new Label("Test"));
//      box.setLayoutX(200);
//      box.setLayoutY(200);
//
//      L1.getChildren().add(box);
      getLeftMenu().getChildren().addAll(new Label("test"), new Label("test1"), new Label("test2"));
   }
}
