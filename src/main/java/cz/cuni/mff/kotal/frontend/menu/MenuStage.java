package cz.cuni.mff.kotal.frontend.menu;

import cz.cuni.mff.kotal.frontend.menu.scenes.MenuScene;
import javafx.stage.Stage;

 class MenuStage extends Stage {
   MenuStage(double width, double height) {
      setTitle("Settings");

      // TODO udelat velikost poradne
      setScene(new MenuScene(width, height));
      setX(0);
      setY(0);
      show();
   }
}