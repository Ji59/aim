package cz.cuni.mff.kotal.frontend.menu.tabs;

import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MenuLabel;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MyComboBox;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MySlider;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class IntersectionMenuTab0 extends MyTabTemplate {

   private static final MyComboBox model = new MyComboBox(Arrays.stream(Parameters.Models.values()).map(Parameters.Models::getText).collect(Collectors.toList())),
      restriction = new MyComboBox(Arrays.stream(Parameters.Restrictions.values()).map(Parameters.Restrictions::getText).collect(Collectors.toList()));
   private static final MySlider granularity = new MySlider(2, 65, 4),
      entries = new MySlider(1, granularity.getValue() - 1, 1),
      exits = new MySlider(1, granularity.getValue() - entries.getValue(), 1);
   private static long roads = 4;

   public IntersectionMenuTab0() {
      super(Tabs.T0.getText());

      addGranularityActions();
      addEntriesActions();
      addExitsActions();
      addModelActions();

      Iterator<Parameters> iterator = Arrays.stream(Parameters.values()).iterator();
      for (int index = 0; iterator.hasNext(); index++) {
         Parameters parameter = iterator.next();
         addRow(index, new MenuLabel(parameter.text), parameter.parameter);
      }

      // TODO pridat tlacitko na ulozeni konfigurace
   }

   private void addModelActions() {
      model.valueProperty().addListener((observable, oldValue, newValue) -> {
         Parameters.Models selected = null;
         for (Parameters.Models model : Parameters.Models.values()) {
            if (model.getText().equals(newValue)) {
               selected = model;
            }
         }
         assert selected != null;
         roads = selected.getDirections() == null ? 4 : selected.getDirections().size();
         AgentsMenuTab1.createDirectionsMenuAndAddActions(selected);

         AgentsMenuTab1.getNewAgentsMinimum().setMax(roads * entries.getValue());
         AgentsMenuTab1.getNewAgentsMaximum().setMax(roads * entries.getValue());
      });
   }

   private void addGranularityActions() {
      granularity.addAction((observable, oldValue, newValue) -> {
         long newVal = newValue.longValue() - 1;
         entries.setMax(newVal);
         exits.setMax(newVal);
         if (entries.getValue() + exits.getValue() > newVal) {
            exits.setValue(newVal - entries.getValue());
         }
         AgentParametersMenuTab4.getMaximalSizeLength().setMax(newVal);
         AgentParametersMenuTab4.getMinimalSizeLength().setMax(newVal);
      });
   }

   private void addEntriesActions() {
      entries.addAction((observable, oldValue, newValue) -> {
         if (newValue.longValue() + exits.getValue() > granularity.getValue()) {
            exits.setValue(granularity.getValue() - newValue.longValue());
         }
         AgentParametersMenuTab4.getMinimalSizeWidth().setMax(Math.min(newValue.longValue(), exits.getValue()));
         AgentParametersMenuTab4.getMaximalSizeWidth().setMax(Math.min(newValue.longValue(), exits.getValue()));
         AgentsMenuTab1.getNewAgentsMaximum().setMax(roads * newValue.longValue());
         AgentsMenuTab1.getNewAgentsMinimum().setMax(roads * newValue.longValue());
      });
   }

   private void addExitsActions() {
      exits.addAction((observable, oldValue, newValue) -> {
         if (newValue.longValue() + entries.getValue() > granularity.getValue()) {
            entries.setValue(granularity.getValue() - newValue.longValue());
         }
         AgentParametersMenuTab4.getMinimalSizeWidth().setMax(Math.min(newValue.longValue(), exits.getValue()));
         AgentParametersMenuTab4.getMaximalSizeWidth().setMax(Math.min(newValue.longValue(), exits.getValue()));
      });
   }

   public static MyComboBox getModel() {
      return model;
   }

   public static MyComboBox getRestriction() {
      return restriction;
   }

   public static MySlider getGranularity() {
      return granularity;
   }

   public static MySlider getEntries() {
      return entries;
   }

   public static MySlider getExits() {
      return exits;
   }

   public static long getRoads() {
      return roads;
   }

   public enum Parameters {
      MODEL("Model: ", model),
      GRANULARITY("Granularity: ", granularity),
      ENTRY("Entries: ", entries),
      EXIT("Exits: ", exits),
      RESTRICTION("Restriction: ", restriction),
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

      public enum Models {
         SQUARE("Square grid", Arrays.asList('N', 'S', 'W', 'E')),
         HEXAGONAL("Hexagonal grid", Arrays.asList('A', 'B', 'C', 'D', 'E', 'F')),
         OCTAGONAL("Octagonal grid", Arrays.asList('N', 'S', 'W', 'E')),
         CUSTOM("Custom intersection", new ArrayList<>()),
         ;

         private final String text;
         private final List<Character> directions;

         Models(String text, List<Character> directions) {
            this.text = text;
            this.directions = directions;
         }

         public String getText() {
            return text;
         }

         public List<Character> getDirections() {
            return directions;
         }
      }

      public enum Restrictions {
         NO_RESTRICTION("Without restriction"),
         ROUNDABOUT("Roundabout"),
         LANES("Lanes"),
         CUSTOM("Custom restriction"),
         ;

         private final String text;

         Restrictions(String text) {
            this.text = text;
         }

         public String getText() {
            return text;
         }
      }
   }
}
