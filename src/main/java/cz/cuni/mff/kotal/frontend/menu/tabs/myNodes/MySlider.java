package cz.cuni.mff.kotal.frontend.menu.tabs.myNodes;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class MySlider extends HBox {
   private final Slider slider;
   private final TextField textField;

   public MySlider(double min, double max, long value) {

      // TODO remove constant
      setSpacing(20);

      slider = new Slider(min, max, value);
      slider.setShowTickLabels(true);
      slider.setShowTickMarks(true);
      long count = (long) (max - min + 1);
      int tickUnit = getSliderMajorTicks(count);

      if (tickUnit == 0) {
         slider.setMinorTickCount((int) (count - 2));
         slider.setBlockIncrement(1);
      } else {
         slider.setBlockIncrement(1);
         slider.setMajorTickUnit(tickUnit);
         slider.setMinorTickCount(getSliderMinorTicks(tickUnit));
      }

      textField = new TextField(String.valueOf(value));
      textField.setPrefWidth(40);

      slider.valueProperty().addListener((observable, oldValue, newValue) -> {
         setDisable(true);

         long roundedValue = Math.round(newValue.doubleValue());
         slider.setValue(roundedValue);
         textField.setText(String.valueOf(roundedValue));

         setDisable(false);
      });

      textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
         setDisable(true);

         if (!newValue) {
            String sliderValue = String.valueOf(Math.round(slider.getValue()));
            if (textField.getText().isEmpty()) {
               textField.setText(sliderValue);
               return;
            }
            try {
               long newVal = Long.parseLong(textField.getText());
               if (newVal > slider.getMax() || newVal < slider.getMin()) {
                  throw new Exception("Entered number is out of range.");
               }
               slider.setValue(newVal);
            } catch (NumberFormatException e) {
               new Alert(Alert.AlertType.ERROR, "Cannot parse entered value to integer: " + e.getMessage(), ButtonType.OK).showAndWait();
               textField.setText(sliderValue);
            } catch (Exception e) {
               new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
               textField.setText(sliderValue);
            }
         }

         setDisable(false);
      });

      getChildren().addAll(slider, textField);
   }

   public MySlider addAction(ChangeListener<? super Number> listener) {
      slider.valueProperty().addListener(listener);
      return this;
   }

   public long getValue() {
      return Math.round(slider.getValue());
   }

   public MySlider setValue(long value) {
      slider.setValue(value);
      return this;
   }

   public long getMax() {
      return (long) slider.getMax();
   }

   public MySlider setMax(long value) {
      slider.setMax(value);
      return this;
   }

   public MySlider setMin(long value) {
      slider.setMin(value);
      return this;
   }

   public long getMin() {
      return (long) slider.getMin();
   }

   private int getSliderMajorTicks(long count) {
      if (count <= 5) {
         return 0;
      } else {
         return getGoodNumber(count);
      }
   }

   private int getSliderMinorTicks(int majorTick) {
      if (majorTick == 0) {
         return 0;
      }

      return getGoodNumber(majorTick);
   }

   private int getGoodNumber(long number) {
      for (long i = number / 4; i >= number / 20; i--) {
         if (number % i == 0) {
            return (int) i;
         }
      }
      return (int) (number / 2);
   }
}
