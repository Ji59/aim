package cz.cuni.mff.kotal.simulation.event;

public enum Action {
   ON_WAY(2),
   ARRIVAL(3),
   LEAVING(1),
   COLLISION(0),
   ;

   private final int priority;

   Action(int priority) {
      this.priority = priority;
   }

   public int getPriority() {
      return priority;
   }
}