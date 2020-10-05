package cz.cuni.mff.kotal.frontend.intersection;

import cz.cuni.mff.kotal.frontend.menu.tabs.SimulationMenuTab3;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class IntersectionMenu extends VBox {

   private static boolean abstractMode = false;

   // TODO extract constants
   private static final Slider SPEED_SLIDER = new Slider(0, 1, 0),
      TIMELINE_SLIDER = new Slider(0, 1, 0);
   private static final Label stepsLabel = new Label("#n"),
      delayLabel = new Label("#n"),
      rejectionsLabel = new Label("#n"),
      collisionsLabel = new Label("#n"),
      remainingLabel = new Label(),
      speedLabel = new Label("Speed"),
      timelineLabel = new Label("Timeline");

   private static final Button PLAY_BUTTON = new Button("Play"),
      RESTART_BUTTON = new Button("Restart"),
      SAVE_AGENTS_BUTTON = new Button("Save agents");

   public IntersectionMenu(double padding) {
      super(padding);

      Button intersectionMode = new Button("Real");
      intersectionMode.setPrefWidth(Double.MAX_VALUE);
      intersectionMode.setOnMouseClicked(event -> {
         abstractMode = !abstractMode;
         intersectionMode.setText(abstractMode ? "Abstract" : "Real");
         IntersectionScene.getIntersectionGraph().redraw();
      });

      getChildren().add(intersectionMode);

      createControlNodes(padding);
   }

   private void createControlNodes(double padding) {
      // TODO extract constants

      GridPane sliders = new GridPane();
      sliders.addRow(0, speedLabel, SPEED_SLIDER);
      sliders.addRow(1, timelineLabel, TIMELINE_SLIDER);
      sliders.setVgap(padding);

      SPEED_SLIDER.valueProperty().addListener(getSliderValueListener(SimulationMenuTab3.getSpeed()));
      TIMELINE_SLIDER.valueProperty().addListener(getSliderValueListener(SimulationMenuTab3.getTimeline()));
      SimulationMenuTab3.getSpeed().valueProperty().addListener(getSliderValueListener(SPEED_SLIDER));
      SimulationMenuTab3.getTimeline().valueProperty().addListener(getSliderValueListener(TIMELINE_SLIDER));

      PLAY_BUTTON.setOnMouseClicked(event -> {
         String newText = PLAY_BUTTON.getText().equals("Play") ? "Stop" : "Play";
         PLAY_BUTTON.setText(newText);
         SimulationMenuTab3.getPlayButton().setText(newText);
      });
      SimulationMenuTab3.getPlayButton().setOnMouseClicked(PLAY_BUTTON.getOnMouseClicked());
      SimulationMenuTab3.getRestartButton().setOnMouseClicked(RESTART_BUTTON.getOnMouseClicked());
      SimulationMenuTab3.getSaveAgentsButton().setOnMouseClicked(SAVE_AGENTS_BUTTON.getOnMouseClicked());
      SAVE_AGENTS_BUTTON.setPrefWidth(Double.MAX_VALUE);

      GridPane statistics = new GridPane();
      SimulationMenuTab3.createStatisticsGrid(statistics);

      HBox buttons = new HBox(PLAY_BUTTON, RESTART_BUTTON);
      buttons.setPrefWidth(Double.MAX_VALUE);
      getChildren().addAll(sliders, buttons, SAVE_AGENTS_BUTTON, new Label("Statistics"), statistics);

   }

   private ChangeListener<Number> getSliderValueListener(Slider affectedSlider) {
      return (observable, oldValue, newValue) -> affectedSlider.setValue(newValue.doubleValue());
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
