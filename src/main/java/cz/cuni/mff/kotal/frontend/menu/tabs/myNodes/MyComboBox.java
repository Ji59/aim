package cz.cuni.mff.kotal.frontend.menu.tabs.myNodes;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;

import java.util.List;

public class MyComboBox extends ComboBox<String> {
   public MyComboBox(List<String> items) {
      super(FXCollections.observableList(items));
      getSelectionModel().selectFirst();
   }
}
