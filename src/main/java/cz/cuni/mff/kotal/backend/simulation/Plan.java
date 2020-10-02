package cz.cuni.mff.kotal.backend.simulation;

import cz.cuni.mff.kotal.backend.simulation.graph.Vertex;
import cz.cuni.mff.kotal.simulation.event.Action;
import cz.cuni.mff.kotal.simulation.event.Event;
import cz.cuni.mff.kotal.simulation.event.EventCollision;

import java.math.BigInteger;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

public class Plan {
   private Queue<Event> heap = new PriorityQueue<>();

   public boolean collision(BigInteger time, Agent a0, Agent a1, Vertex v) {
      heap = heap.parallelStream().filter(event -> !(event.getAgent().equals(a0) && event.getAgent().equals(a1))).collect(Collectors.toCollection(PriorityQueue::new));
      return heap.add(new EventCollision(Action.COLLISION, time, a0, a1, v));
   }

   public Queue<Event> getHeap() {
      return heap;
   }
}