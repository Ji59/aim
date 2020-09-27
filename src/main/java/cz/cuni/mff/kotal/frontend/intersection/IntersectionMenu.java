package cz.cuni.mff.kotal.frontend.intersection;

import cz.cuni.mff.kotal.frontend.menu.tabs.SimulationMenuTab3;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class IntersectionMenu extends VBox {

   private static boolean abstractMode = false;

   // TODO extract constants
   private static final Slider speed = new Slider(0, 1, 0),
      timeline = new Slider(0, 1, 0);
   private static final Label stepsLabel = new Label("#n"),
      delayLabel = new Label("#n"),
      rejectionsLabel = new Label("#n"),
      collisionsLabel = new Label("#n"),
      remainingLabel = new Label();
   private static final Button PLAY_BUTTON = new Button("Play"),
      RESTART_BUTTON = new Button("Restart"),
      SAVE_AGENTS_BUTTON = new Button("Save agents");

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

      createControlNodes(padding);
   }

   private void createControlNodes(double padding) {
      // TODO extract constants
      PLAY_BUTTON.setOnMouseClicked(event -> {
         String newText = PLAY_BUTTON.getText().equals("Play") ? "Stop" : "Play";
         PLAY_BUTTON.setText(newText);
         SimulationMenuTab3.getPlayButton().setText(newText);
      });

      SimulationMenuTab3.getPlayButton().setOnMouseClicked(PLAY_BUTTON.getOnMouseClicked());
      SimulationMenuTab3.getRestartButton().setOnMouseClicked(RESTART_BUTTON.getOnMouseClicked());
      SimulationMenuTab3.getSaveAgentsButton().setOnMouseClicked(SAVE_AGENTS_BUTTON.getOnMouseClicked());
      SAVE_AGENTS_BUTTON.setPrefWidth(Double.MAX_VALUE);

      VBox buttons = new VBox(padding, new HBox(PLAY_BUTTON, RESTART_BUTTON), SAVE_AGENTS_BUTTON);
      getChildren().add(buttons);

   }

   public static boolean isAbstract() {
      return abstractMode;
   }

   public static Button getPlayButton() {
      return PLAY_BUTTON;
   }

   public static Button getRestartButton() {
      return RESTART_BUTTON;
   }

   public static Button getSaveAgentsButton() {
      return SAVE_AGENTS_BUTTON;
   }
}
