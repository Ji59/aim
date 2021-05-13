package cz.cuni.mff.kotal.frontend.menu.tabs;

import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MenuLabel;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;

import java.util.Map;


public class SimulationMenuTab3 extends MyTabTemplate {

   // TODO extract constants and rename constants
   private static final Slider SPEED_SLIDER = new Slider(0, 1000, 815),
      timeline = new Slider(0, 1, 0);
   private static final Label stepsLabel = new Label("#n"),
      delayLabel = new Label("#n"),
      rejectionsLabel = new Label("#n"),
      collisionsLabel = new Label("#n"),
      remainingLabel = new Label();
   private static final Button PLAY_BUTTON = new Button("Play"),
      RESTART_BUTTON = new Button("Restart"),
      SAVE_AGENTS_BUTTON = new Button("Save agents");

   public SimulationMenuTab3() {
      super(Tabs.T3.getText());

      createStatisticsGrid((GridPane) Parameters.STATISTICS.getParameter());

      addSpeedSliderActions();
      // TODO pridat akce na tlacitka a slidery

      int row = 0;
      for (Parameters parameter : Parameters.values()) {
         if (parameter != Parameters.BUTTONS) {
            addRow(row++, new MenuLabel(parameter.getText()), parameter.getParameter());
         } else {
            getGrid().getChildren().add(parameter.getParameter());
            GridPane.setConstraints(parameter.getParameter(),0,  row++, 3, 1);
         }
      }
   }

   public static void createStatisticsGrid(GridPane grid) {
      grid.setHgap(20);
      int row = 0;
      for (Parameters.Statistics statistic : Parameters.Statistics.values()) {
         Label label = new Label(statistic.getText());
         Label labelValue = statistic.getValue();
         grid.getChildren().addAll(label, labelValue);
         GridPane.setConstraints(label, 0, row);
         GridPane.setConstraints(labelValue, 1, row++);
      }
   }

   public static void createStatisticsGrid(GridPane grid, Map<Parameters.Statistics, Label> labels) {
      grid.setHgap(20);
      int row = 0;
      for (Parameters.Statistics statistic : Parameters.Statistics.values()) {
         Label label = new Label(statistic.getText());
         Label labelValue = labels.get(statistic);
         grid.getChildren().addAll(label, labelValue);
         GridPane.setConstraints(label, 0, row);
         GridPane.setConstraints(labelValue, 1, row++);
      }
   }

   private void addSpeedSliderActions() {
      SPEED_SLIDER.valueProperty().addListener((observable, oldValue, newValue) -> IntersectionScene.changeSimulation());
   }

   public static Slider getSpeedSlider() {
      return SPEED_SLIDER;
   }

   public static Slider getTimeline() {
      return timeline;
   }

   public static Label getStepsLabel() {
      return stepsLabel;
   }

   public static Label getDelayLabel() {
      return delayLabel;
   }

   public static Label getRejectionsLabel() {
      return rejectionsLabel;
   }

   public static Label getCollisionsLabel() {
      return collisionsLabel;
   }

   public static Label getRemainingLabel() {
      return remainingLabel;
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

   public enum Parameters {
      SPEED("Speed:", SimulationMenuTab3.SPEED_SLIDER),
      TIMELINE("Timeline:", timeline),
      // TODO remove constants
      BUTTONS(null, new TilePane(Orientation.HORIZONTAL, 20, 0, PLAY_BUTTON, RESTART_BUTTON, SAVE_AGENTS_BUTTON)),
      STATISTICS("Statistics:", new GridPane()),
      ;

      private final String text;
      private final Node parameter;

      Parameters(String text, Node parameter) {
         this.text = text;
         this.parameter = parameter;
      }

      public String getText() {
         return text;
      }

      public Node getParameter() {
         return parameter;
      }

      public enum Statistics {
         STEPS("Steps:", stepsLabel),
         DELAY("Total delay:", delayLabel),
         REJECTIONS("Rejections:", rejectionsLabel),
         COLLISIONS("Collisions:", collisionsLabel),
         REMAINS("Remaining time:", remainingLabel),
         ;

         private final String text;
         private final Label value;

         Statistics(String text, Label value) {
            this.text = text;
            this.value = value;
         }

         public String getText() {
            return text;
         }

         public Label getValue() {
            return value;
         }
      }
   }
}
