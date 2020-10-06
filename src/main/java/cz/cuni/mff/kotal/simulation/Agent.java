package cz.cuni.mff.kotal.simulation;

import cz.cuni.mff.kotal.simulation.graph.Edge;

public class Agent {
   private final int id;
   private Edge location = null;

   public Agent(int id) {
      this.id = id;
   }

   public Agent(int id, Edge v) {
      this.id = id;
      location = v;
   }

   public int getId() {
      return id;
   }

   public Edge getLocation() {
      return location;
   }

   public Agent setLocation(Edge location) {
      this.location = location;
      return this;
   }
}