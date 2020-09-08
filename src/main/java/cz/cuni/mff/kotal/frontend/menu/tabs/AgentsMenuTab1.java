package cz.cuni.mff.kotal.frontend.menu.tabs;

import cz.cuni.mff.kotal.frontend.menu.MenuApp;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MenuLabel;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MyComboBox;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MySlider;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class AgentsMenuTab1 extends MyTabTemplate {

   private static final MyComboBox inputType = new MyComboBox(Arrays.stream(Parameters.Input.values()).map(Parameters.Input::getText).collect(Collectors.toList()));
   //TODO extract constants
   private static final TextField steps = new TextField("0 ~ infinite simulation"),
      filePath = new TextField("Enter file path here");
   private static final MySlider newAgentsMinimum = new MySlider(0, IntersectionMenuTab0.getRoads() * IntersectionMenuTab0.getEntries().getValue(), 0),
      newAgentsMaximum = new MySlider(0, IntersectionMenuTab0.getRoads() * IntersectionMenuTab0.getEntries().getValue(), IntersectionMenuTab0.getRoads());

   public AgentsMenuTab1() {
      super(Tabs.T1.getText());

      addInputActions();

      createFileMenuAndAddActions();

      addStepsActions();

      createAmountMenuAndAddActions();

      createDirectionsMenuAndAddActions(IntersectionMenuTab0.Parameters.Models.SQUARE);

      Iterator<Parameters> iterator = Arrays.stream(Parameters.values()).iterator();
      for (int index = 0; iterator.hasNext(); index++) {
         Parameters parameter = iterator.next();
         if (parameter.equals(Parameters.FILE)) {
            addInvisibleRow(1, new MenuLabel(parameter.text), parameter.parameter);
            continue;
         }
         addRow(index, new MenuLabel(parameter.text), parameter.parameter);
      }

      // TODO pridat tlacitko na ulozeni konfigurace
   }

   private void addInputActions() {
      inputType.valueProperty().addListener((observable, oldValue, newValue) -> {
         for (Node child : getGrid().getChildren()) {
            child.setVisible(!child.isVisible() || child.equals(inputType) || (child instanceof Label && ((Label) child).getText().equals(Parameters.INPUT.getText())));
         }
      });
   }

   private void addStepsActions() {
      steps.focusedProperty().addListener((observable, oldValue, newValue) -> {
         if (newValue) {
            if (steps.getText().startsWith("0")) {
               steps.setText("");
            }
         } else {
            if (steps.getText().isEmpty()) {
               steps.setText(String.valueOf(0));
            } else {
               try {
                  long value = Long.parseLong(steps.getText());
                  if (value < 0) {
                     throw new Exception("Entered number: " + value);
                  }
               } catch (Exception e) {
                  new Alert(Alert.AlertType.ERROR, "Please enter non-negative number. " + e.getMessage(), ButtonType.OK).showAndWait();
                  steps.setText(String.valueOf(0));
               }
            }
            if (steps.getText().startsWith("0")) {
               SimulationMenuTab3.getRemainingLabel().setText("");
            } else {
               // TODO do something with that
               SimulationMenuTab3.getRemainingLabel().setText("n s");
            }
         }
      });
   }

   private void createFileMenuAndAddActions() {
      //TODO extract constants
      filePath.setPrefWidth(200);
      filePath.focusedProperty().addListener((observable, oldValue, newValue) -> {
         if (newValue) {
            if (filePath.getText().equals("Enter file path here")) {
               filePath.setText("");
            }
         } else {
            if (filePath.getText().isEmpty()) {
               filePath.setText("Enter file path here");
               return;
            }
            try {
               File file = new File(filePath.getText());
               if (!file.isFile()) {
                  //TODO extract constant
                  throw new Exception("File does not exist. Entered file: " + file.getPath());
               }
            } catch (Exception e) {
               new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
               filePath.requestFocus();
            }
         }
      });
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Select agents file input");
      Button fileButton = new Button("Select file");
      fileButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
         File file = fileChooser.showOpenDialog(MenuApp.getStage());
         if (file != null && file.isFile()) {
            filePath.setText(file.getAbsolutePath());
         }
      });
      HBox fileBox = (HBox) Parameters.FILE.parameter;
      fileBox.getChildren().addAll(filePath, fileButton);
   }

   private void createAmountMenuAndAddActions() {
      newAgentsMinimum.addAction((observable, oldValue, newValue) -> {
         if (newAgentsMinimum.getValue() > newAgentsMaximum.getValue()) {
            newAgentsMaximum.setValue(newValue.longValue());
         }
      });
      newAgentsMaximum.addAction((observable, oldValue, newValue) -> {
         if (newAgentsMaximum.getValue() < newAgentsMinimum.getValue()) {
            newAgentsMinimum.setValue(newValue.longValue());
         }
      });
      Label minLabel = new Label("Minimum: ");
      Label maxLabel = new Label("Maximum:");
      ((GridPane) Parameters.AMOUNT.getParameter()).getChildren().addAll(minLabel, newAgentsMinimum, maxLabel, newAgentsMaximum);
      GridPane.setConstraints(minLabel, 0, 0);
      GridPane.setConstraints(maxLabel, 0, 1);
      GridPane.setConstraints(newAgentsMinimum, 1, 0);
      GridPane.setConstraints(newAgentsMaximum, 1, 1);
   }

   static void createDirectionsMenuAndAddActions(IntersectionMenuTab0.Parameters.Models model) {
      if (model == null || model.getDirections() == null) {
         return;
      }

      VBox vBox;
      (vBox = ((VBox) Parameters.DIRECTION.getParameter())).getChildren().clear();


      List<DirectionSlider> sliders = new ArrayList<>();

      int row = 0, remains = 100;
      for (Character direction : model.getDirections()) {
         long value = remains / (model.getDirections().size() - row);
         DirectionSlider directionSlider = new DirectionSlider(direction, value);
         sliders.add(directionSlider);

         directionSlider.addSliderAction((observable, oldValue, newValue) -> {
            if (directionSlider.sliderActionOn) {
               double sum = 0;
               for (DirectionSlider sl : sliders) {
                  sum += sl.getSliderValue();
               }

               int remainingPercentages = 100;
               for (DirectionSlider sl : sliders) {
                  double oldVal = sl.getSliderValue();
                  remainingPercentages -= sl.setValue(Math.round(sl.getSliderValue() / sum * remainingPercentages));
                  sum -= oldVal;
               }
            }
         });

         directionSlider.addValueLabelFocusedAction((observable, oldValue, newValue) -> {
            if (newValue) {
               // if focused
               TextField valueText = directionSlider.valueText;
               directionSlider.valueText.setText(valueText.getText(0, valueText.getText().lastIndexOf('%')));
               directionSlider.oldValue = Long.parseLong(directionSlider.valueText.getText());
            } else {
               // if not focused anymore
               try {
                  int newVal = Integer.parseInt(directionSlider.valueText.getText());
                  if (newVal < Math.round(directionSlider.slider.getMin() - 0.5) || newVal > directionSlider.slider.getMax()) {
                     directionSlider.setValue(directionSlider.oldValue + '%');
                     throw new Exception("Please enter integer between 0 and 100. Entered value: " + newVal);
                  } else {
                     double sum = 0, available = 100 - newVal;
                     for (DirectionSlider sl : sliders) {
                        if (sl != directionSlider) {
                           sum += sl.getValue();
                        }
                     }
                     for (DirectionSlider sl : sliders) {
                        long newSliderValue;
                        if (sl != directionSlider) {
                           sl.setValue(newSliderValue = Math.round(sl.getValue() / sum * available));
                        } else {
                           newSliderValue = newVal;
                        }
                        sl.sliderActionOn = false;
                        sl.setSliderValue(newSliderValue);
                        sl.sliderActionOn = true;
                     }
                     directionSlider.valueText.setText(newVal + "%");
                  }
               } catch (Exception e) {
                  new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
               }
            }
         });

         remains -= value;
         row++;
      }

      vBox.getChildren().addAll(sliders);
   }

   public static MyComboBox getInputType() {
      return inputType;
   }

   public static TextField getSteps() {
      return steps;
   }

   public static TextField getFilePath() {
      return filePath;
   }

   public static MySlider getNewAgentsMinimum() {
      return newAgentsMinimum;
   }

   public static MySlider getNewAgentsMaximum() {
      return newAgentsMaximum;
   }

   private enum Parameters {
      INPUT("Agents input type: ", inputType),
      STEPS("Number of steps: ", steps),
      AMOUNT("Amount of new agents:", new GridPane()),
      DIRECTION("Direction distribution: ", new VBox()),
      // TODO make spacing constant
      FILE("File name: ", new HBox(5)),
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

      private enum Input {
         RNG("Randomly generated"),
         FILE("From file"),
         ;

         private final String text;

         Input(String text) {
            this.text = text;
         }

         public String getText() {
            return text;
         }
      }
   }

   private static class DirectionSlider extends HBox {
      private final Slider slider = new Slider(0.002, 100, 50);
      private final TextField valueText;
      private long oldValue = 0;
      private boolean sliderActionOn = true;

      private DirectionSlider(char directionName, long value) {
         // TODO introduce constant
         super(10);

         valueText = new TextField(String.valueOf(value) + '%');
         valueText.setPrefWidth(45);

         Label directionLabel = new Label(String.valueOf(directionName));
         directionLabel.setPrefWidth(15);
         getChildren().addAll(directionLabel, slider, valueText);
      }

      public long getValue() throws NumberFormatException {
         return Long.parseLong(valueText.getText(0, valueText.getText().lastIndexOf('%')));
      }

      long setValue(long value) {
         valueText.setText(String.valueOf(value) + '%');
         return value;
      }

      double getSliderValue() {
         return slider.getValue();
      }

      DirectionSlider setSliderValue(double value) {
         slider.setValue(value);
         return this;
      }

      DirectionSlider addSliderAction(ChangeListener<? super Number> listener) {
         slider.valueProperty().addListener(listener);
         return this;
      }

      DirectionSlider addValueLabelFocusedAction(ChangeListener<? super Boolean> listener) {
         valueText.focusedProperty().addListener(listener);
         return this;
      }
   }
}
