package cz.cuni.mff.kotal.frontend.menu.tabs;

import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MenuLabel;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MyComboBox;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MySlider;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.stream.Collectors;

public class AgentParametersMenuTab4 extends MyTabTemplate {
   private static final MySlider minimalSizeLength = new MySlider(1, IntersectionMenuTab0.getGranularity().getValue() - 1, 1),
      minimalSizeWidth = new MySlider(1, Math.min(IntersectionMenuTab0.getEntries().getValue(), IntersectionMenuTab0.getExits().getValue()), 1),
      maximalSizeLength = new MySlider(1, IntersectionMenuTab0.getGranularity().getValue() - 1, 1),
      maximalSizeWidth = new MySlider(1, Math.min(IntersectionMenuTab0.getEntries().getValue(), IntersectionMenuTab0.getExits().getValue()), 1),
      minimalSpeed = new MySlider(1, IntersectionMenuTab0.getGranularity().getValue(), 1),
      maximalSpeed = new MySlider(1, IntersectionMenuTab0.getGranularity().getValue(), 1);
   private static final Slider speedDeviation = new Slider(0, 1, 0);

   public AgentParametersMenuTab4() {
      super(Tabs.T4.getText());

      // TODO pridat akce
      createSizeSlidersAndAddActions();

      createSpeedSlidersAndAddActions();

      int row = 0;
      for (Parameters parameter : Parameters.values()) {
         addRow(row++, new MenuLabel(parameter.getText()), parameter.getParameter());
      }
   }

   private void createSizeSlidersAndAddActions() {      
      minimalSizeLength.addAction((observable, oldValue, newValue) -> {
         if (maximalSizeLength.getValue() < newValue.longValue()) {
            maximalSizeLength.setValue(newValue.longValue());
         }
      });
      maximalSizeLength.addAction((observable, oldValue, newValue) -> {
         if (minimalSizeLength.getValue() > newValue.longValue()) {
            minimalSizeLength.setValue(newValue.longValue());
         }
      });      
      minimalSizeWidth.addAction((observable, oldValue, newValue) -> {
         if (maximalSizeWidth.getValue() < newValue.longValue()) {
            maximalSizeWidth.setValue(newValue.longValue());
         }
      });
      maximalSizeWidth.addAction((observable, oldValue, newValue) -> {
         if (minimalSizeWidth.getValue() > newValue.longValue()) {
            minimalSizeWidth.setValue(newValue.longValue());
         }
      });
      
      GridPane grid = (GridPane) Parameters.SIZE.getParameter();
      // TODO pryc s tim
      Label l0 = new Label("L"), l1 = new Label("L"),
         w0 = new Label("W"), w1 = new Label("W");
      l0.setPrefWidth(20);
      l1.setPrefWidth(20);
      w0.setPrefWidth(20);
      w1.setPrefWidth(20);
      // TODO odebrat konstanty
      grid.addRow(0, new Label("Minimum:"), new VBox(new HBox(10, l0, minimalSizeLength), new HBox(10, w0, minimalSizeWidth)));
      grid.addRow(1, new Label("Maximum:"), new VBox(new HBox(10, l1, maximalSizeLength), new HBox(10, w1, maximalSizeWidth)));
      grid.setHgap(20);
      grid.setVgap(20);
   }

   private void createSpeedSlidersAndAddActions() {
      Label speedDeviationLabel = new Label("0%");
      
      minimalSpeed.addAction((observable, oldValue, newValue) -> {
         if (maximalSpeed.getValue() < newValue.longValue()) {
            maximalSpeed.setValue(newValue.longValue());
         }
      });
      maximalSpeed.addAction((observable, oldValue, newValue) -> {
         if (minimalSpeed.getValue() > newValue.longValue()) {
            minimalSpeed.setValue(newValue.longValue());
         }
      });
      speedDeviation.valueProperty().addListener((observable, oldValue, newValue) -> {
         speedDeviationLabel.setText(new DecimalFormat("###.##%").format(newValue));
      });
      speedDeviation.setBlockIncrement(0.05);
      speedDeviation.setMajorTickUnit(0.2);
      speedDeviation.setMinorTickCount(3);
      speedDeviation.setShowTickLabels(true);
      speedDeviation.setShowTickMarks(true);

      // TODO odstranit konstanty
      GridPane grid = (GridPane) Parameters.SPEED.getParameter();
      grid.addRow(0, new Label("Minimum:"), minimalSpeed);
      grid.addRow(1, new Label("Maximum:"), maximalSpeed);
      grid.addRow(2, new Label("Deviation:"), speedDeviation, speedDeviationLabel);
      grid.setHgap(20);
      grid.setVgap(20);
   }

   public static MySlider getMinimalSizeLength() {
      return minimalSizeLength;
   }

   public static MySlider getMinimalSizeWidth() {
      return minimalSizeWidth;
   }

   public static MySlider getMaximalSizeLength() {
      return maximalSizeLength;
   }

   public static MySlider getMaximalSizeWidth() {
      return maximalSizeWidth;
   }

   public static MySlider getMinimalSpeed() {
      return minimalSpeed;
   }

   public static MySlider getMaximalSpeed() {
      return maximalSpeed;
   }

   public static Slider getSpeedDeviation() {
      return speedDeviation;
   }

   private enum Parameters {
      SIZE("Size:", new GridPane()),
      SPEED("Speed:", new GridPane()),
      RNG("RNG distribution:", new MyComboBox(Arrays.stream(Random.values()).map(Random::getText).collect(Collectors.toList()))),
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

      private enum Random {
         // TODO pridat funkcni predpis
         GAUSS("Gauss"),
         LINEAR("Linear"),
         ;

         private final String text;

         Random(String text) {
            this.text = text;
         }

         public String getText() {
            return text;
         }
      }
   }
}
