package cz.cuni.mff.kotal.frontend.menu.tabs.myNodes;

import javafx.geometry.Pos;
import javafx.scene.control.Label;

public class MenuLabel extends Label {
   public MenuLabel(String text) {
      super(text);
      setAlignment(Pos.BASELINE_LEFT);
   }
}
