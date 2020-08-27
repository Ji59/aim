package cz.cuni.mff.kotal.backend;

import cz.cuni.mff.kotal.backend.graph.Vertex;

public class Agent {
   private final int id;
   private Vertex location = null;

   public Agent(int id) {
      this.id = id;
   }

   public Agent(int id, Vertex v) {
      this.id = id;
      location = v;
   }

   public int getId() {
      return id;
   }

   public Vertex getLocation() {
      return location;
   }

   public Agent setLocation(Vertex location) {
      this.location = location;
      return this;
   }
}