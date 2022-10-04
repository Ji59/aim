package cz.cuni.mff.kotal.frontend.menu.scenes.nodes;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.jetbrains.annotations.NotNull;

public class TitleMenuLabel extends Label {
   private static double x = 100, y = 200;
   private static final double RATIO_X = 0.05;
   private static final double RATIO_Y = 0.1;

   public TitleMenuLabel(String text) {
      super(text);
      setFont(Font.font(20));
      setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, new CornerRadii(5), BorderStroke.DEFAULT_WIDTHS)));
      setPadding(new Insets(0, 10, 0, 10));
   }

   public static double getXX() {
      return x;
   }

   public static void setXX(double x) {
      TitleMenuLabel.x = x * RATIO_X;
   }

   public static double getYY() {
      return y;
   }

   public static void setYY(double y) {
      TitleMenuLabel.y = y * RATIO_Y;
   }

   public @NotNull TitleMenuLabel addOnClickEvent(@NotNull EventHandler<MouseEvent> eventHandler) {
      addEventFilter(MouseEvent.MOUSE_CLICKED, eventHandler);
      return this;
   }
}
