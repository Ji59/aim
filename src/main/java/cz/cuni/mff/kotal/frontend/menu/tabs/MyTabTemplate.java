package cz.cuni.mff.kotal.frontend.menu.tabs;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;

import java.util.Arrays;
import java.util.Iterator;

public abstract class MyTabTemplate extends Tab {
   private final GridPane grid = new GridPane();

   public MyTabTemplate(String text) {
      super(text);
      
      // TODO zavest konstanty
      grid.setHgap(20);
      grid.setVgap(50);
      grid.setPadding(new Insets(30, 30, 30, 30));
      
      setContent(grid);
   }

   public GridPane getGrid() {
      return grid;
   }

   public MyTabTemplate addRow(int index, Node... children) {
      grid.addRow(index, children);
      Iterator<Node> iterator = Arrays.stream(children).iterator();
      for (int i = 0; iterator.hasNext(); i++) {
         GridPane.setConstraints(iterator.next(), i, index);
      }
      return this;
   }

   public MyTabTemplate addInvisibleRow(int index, Node... children) {
      grid.addRow(index, children);
      Iterator<Node> iterator = Arrays.stream(children).iterator();
      for (int i = 0; iterator.hasNext(); i++) {
         Node child = iterator.next();
         GridPane.setConstraints(child, i, index);
         child.setVisible(false);
      }
      return this;
   }
}
