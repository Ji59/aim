package cz.cuni.mff.kotal.frontend.menu.scenes;

import cz.cuni.mff.kotal.frontend.menu.tabs.*;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MySlider;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MenuScene extends Scene {
   private static final double PADDING = 20;
   private static final double WIDTH = 720;
   private static final double HEIGHT = 405;
   private final VBox L2 = new VBox(PADDING);
   private final HBox L1 = new HBox(PADDING, L2);
   private final TabPane L0;

   public MenuScene(double width, double height) {
      super(new TabPane(), width, height);
      L0 = (TabPane)getRoot();
//      AnchorPane.setTopAnchor(L1, PADDING);
//      AnchorPane.setBottomAnchor(L1, PADDING);
//      AnchorPane.setLeftAnchor(L1, PADDING);
//      AnchorPane.setRightAnchor(L1, PADDING);

//      L0.setPadding(new Insets(200, 200, 200, 200));

//      L1.getChildren().add(new TitleMenuLabel("L1"));
//
//      VBox L3_0 = new VBox(), L3_1 = new VBox();
//      L3_0.getChildren().add(new TitleMenuLabel(text));
//      L3_1.getChildren().add(new TitleMenuLabel("Agents"));
//      L2.getChildren().addAll(L3_0, L3_1);
//
//      Label t = new TitleMenuLabel("Test");
//      t.setMinWidth(1200);
//      L3_0.getChildren().add(t);

      L0.getTabs().addAll(new IntersectionMenuTab0(), new AgentsMenuTab1(), new AlgorithmMenuTab2(), new SimulationMenuTab3(), new AgentParametersMenuTab4());
   }

   public VBox getLeftMenu() {
      return L2;
   }
}
