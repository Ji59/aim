package cz.cuni.mff.kotal.frontend.menu.tabs;

enum Tabs {
   T0("Intersection"),
   T1("Agents"),
   T2("Algorithm"),
   T3("Simulation"),
   T4("Agents parameters"),
   ;

   private final String text;

   Tabs(String text) {
      this.text = text;
   }

   public String getText() {
      return text;
   }
}
