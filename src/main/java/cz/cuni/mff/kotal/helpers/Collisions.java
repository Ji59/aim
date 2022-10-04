package cz.cuni.mff.kotal.helpers;


import cz.cuni.mff.kotal.frontend.simulation.AgentPane;
import cz.cuni.mff.kotal.frontend.simulation.AgentPolygon;
import cz.cuni.mff.kotal.frontend.simulation.LineSegment;
import cz.cuni.mff.kotal.frontend.simulation.Point;
import cz.cuni.mff.kotal.simulation.timer.AgentBoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Class containing methods for collision checking.
 */
public class Collisions {
	// TODO refactor

	/**
	 * Hide implicit constructor.
	 */
	private Collisions() {
	}

	/**
	 * Check bounding boxes of agents for collisions.
	 *
	 * @param agents Map of agents and their ID to check
	 * @return Set of agents with overlapping agent bounding boxes
	 */
	public static @NotNull Set<Pair<AgentPane, AgentPane>> getBoundingBoxesOverlaps(@NotNull Set<AgentPane> agents) {
		List<AgentBoundingBox> boundingBoxes;
		boundingBoxes = agents.stream().map(a -> new AgentBoundingBox(a, a.getBoundingBox())).sorted().toList();

		@NotNull Set<Pair<AgentPane, AgentPane>> overlappingAgents = new HashSet<>();
		@NotNull List<AgentBoundingBox> actualBoundingBoxes = new LinkedList<>();

		@NotNull SortedSet<AgentBoundingBox> stopValues = new TreeSet<>((a0, a1) -> {
			int c0 = Double.compare(a0.getEndX(), a1.getEndX());
			if (c0 != 0) {
				return c0;
			}
			int c1 = Double.compare(a0.getStartY(), a1.getStartY());
			if (c1 != 0) {
				return c1;
			}
			return Long.compare(a0.getAgentPane().getAgentID(), a1.getAgentPane().getAgentID());
		});

		for (@NotNull AgentBoundingBox agentBoundingBox : boundingBoxes) {
			double boundingBoxStartX = agentBoundingBox.getStartX();
			@NotNull Iterator<AgentBoundingBox> iterator = stopValues.iterator();
			AgentBoundingBox next;
			while (iterator.hasNext() && boundingBoxStartX > (next = iterator.next()).getEndX()) {
				assert (next.getAgentPane().getAgentID() == actualBoundingBoxes.get(actualBoundingBoxes.indexOf(next)).getAgentPane().getAgentID());
				actualBoundingBoxes.remove(next);
				iterator.remove();
			}
			stopValues.add(agentBoundingBox);

			double boundingBoxStartY = agentBoundingBox.getStartY();
			double boundingBoxEndY = agentBoundingBox.getEndY();
			int neighbourIndex;

			// skip all ending before right Y
			for (neighbourIndex = 0; neighbourIndex < actualBoundingBoxes.size(); neighbourIndex++) {
				AgentBoundingBox neighbour = actualBoundingBoxes.get(neighbourIndex);
				if (neighbour.getEndY() >= boundingBoxStartY) {
					break;
				}
			}

			// check others
			for (; neighbourIndex < actualBoundingBoxes.size(); neighbourIndex++) {
				AgentBoundingBox neighbour = actualBoundingBoxes.get(neighbourIndex);
				if (neighbour.getStartY() > boundingBoxEndY) {
					// can't collide and the next can't to
					break;
				}

				if (neighbour.getEndY() >= boundingBoxStartY) {
					// collision
					overlappingAgents.add(new Pair<>(neighbour.getAgentPane(), agentBoundingBox.getAgentPane()));
				}
			}

			actualBoundingBoxes.add(neighbourIndex, agentBoundingBox);
		}

		return overlappingAgents;
	}

	/**
	 * Check two agents if they are overlapping each other.
	 *
	 * @param agent0 First agent to check
	 * @param agent1 Second agent to check
	 * @return True if they are overlapping, otherwise False
	 */
	public static boolean inCollision(@NotNull AgentPane agent0, @NotNull AgentPane agent1) {
		List<Point> cornerPoints0 = agent0.getCornerPoints();
		List<Point> cornerPoints1 = agent1.getCornerPoints();

		if (existsSeparatingLine(cornerPoints0, cornerPoints1)) {
			return false;
		}

		return !existsSeparatingLine(cornerPoints1, cornerPoints0);
	}

	/**
	 * Try to find separating line between two lists of points.
	 *
	 * @param points0 List of points creating first polygon
	 * @param points1 List of points creating second polygon
	 * @return True if there is separating line between the polygons, otherwise False
	 */
	public static boolean existsSeparatingLine(List<Point> points0, List<Point> points1) {
		points0 = new LinkedList<>(points0);
		points1 = new LinkedList<>(points1);

		int points0Size = points0.size();

		Point point0 = points0.remove(0);
		Point point1;

		for (int i = 0; i < points0Size; i++) {
			point1 = points0.remove(0);
			@NotNull LineSegment line = new LineSegment(point0, point1);

			double side = line.getSide(points0.get(0));
			assert side != 0;
			int sign = side > 0 ? 1 : -1;
			assert points0.stream().allMatch(p -> line.getSide(p) * sign >= 0);

			if (points1.stream().allMatch(p -> line.getSide(p) * sign < 0)) {
				return true;
			}

			points0.add(point0);
			point0 = point1;
		}

		return false;
	}

	/**
	 * Compute collisions between agents.
	 *
	 * @param lastStates Map of agent polygons and their ID in last state
	 * @param newStates  Map of agent polygons and their ID in new state
	 * @return Set of collisions between agents
	 */
	public static @NotNull Set<Collision> getTimeShiftCollisions(@NotNull Map<Long, AgentPolygon> lastStates, @NotNull Map<Long, AgentPolygon> newStates) {
		@NotNull Set<Pair<LineSegmentWithID, LineSegmentWithID>> collidedSegments = new HashSet<>();

		@NotNull Queue<LineSegmentWithID> lineSegments = new PriorityQueue<>(lastStates.size());

		lastStates.forEach((id, polygon) -> {
			if (newStates.containsKey(id)) {
				List<Point> lastCorners = polygon.getCorners();
				List<Point> newCorners = newStates.get(id).getCorners();
				assert lastCorners.size() == newCorners.size();

				for (int i = 0; i < lastCorners.size(); i++) {
					lineSegments.add(new LineSegmentWithID(lastCorners.get(i), newCorners.get(i), id));
				}
			}
		});

		@NotNull Queue<LineSegmentWithY> newVerticalSegmentQueue = new PriorityQueue<>();

		@NotNull Queue<LineSegmentWithID> segmentEnds = new PriorityQueue<>(Comparator.comparingDouble(LineSegment::getX1));

		LineSegmentWithID nextSegment;
		while ((nextSegment = lineSegments.poll()) != null) {
			LineSegmentWithID nextEnd;
			while ((nextEnd = segmentEnds.peek()) != null && nextEnd.getX1() < nextSegment.getX0()) {
				segmentEnds.remove();
				newVerticalSegmentQueue = checkLineSegmentsCollisions(collidedSegments, newVerticalSegmentQueue, nextEnd.getX1());
			}

			newVerticalSegmentQueue = checkLineSegmentsCollisions(collidedSegments, newVerticalSegmentQueue, nextSegment.getX0());

			final double startingY = nextSegment.getY0();
			for (@NotNull LineSegmentWithY lineSegmentWithY : newVerticalSegmentQueue) {
				if (lineSegmentWithY.getY() == startingY && lineSegmentWithY.getLineSegment().getId() != nextSegment.getId()) {
					collidedSegments.add(new Pair<>(lineSegmentWithY.getLineSegment(), nextSegment));
				} else if (lineSegmentWithY.getY() > startingY) {
					break;
				}
			}

			newVerticalSegmentQueue.add(new LineSegmentWithY(startingY, nextSegment));
			segmentEnds.add(nextSegment);
		}

		//		for (Pair<LineSegmentWithID, LineSegmentWithID> lineSegmentPair: collidedSegments) {
//			LineSegmentWithID line0 = lineSegmentPair.getKey();
//			LineSegmentWithID line1 = lineSegmentPair.getValue();
//			Collision collision = new Collision(line0.getId(), line1.getId(), line0.getIntersectionRatio(line1));
//			if (collisionsSet.contains(collision)) {
//				collisionsSet.remove(collision);
//			}
//		}
		return collidedSegments.stream()
			.map(lineSegmentPair -> {
				LineSegmentWithID line0 = lineSegmentPair.getVal0();
				LineSegmentWithID line1 = lineSegmentPair.getVal1();
				double intersectionRatio = line0.getIntersectionRatio(line1);
				return new Collision(line0.getId(), line1.getId(), intersectionRatio);
			})
			.collect(Collectors.toSet());
	}

	/**
	 * Check intersections between line segments with different IDs.
	 *
	 * @param collidedSegments       Set of already collided segments; used for adding new collisions
	 * @param newVerticalSegmentList Last X queue of segments sorted by Y
	 * @param endingX                Actual processing X
	 * @return New X queue of segments sorted by Y
	 */
	@NotNull
	private static Queue<LineSegmentWithY> checkLineSegmentsCollisions(@NotNull Set<Pair<LineSegmentWithID, LineSegmentWithID>> collidedSegments, Queue<LineSegmentWithY> newVerticalSegmentList, double endingX) {
		Queue<LineSegmentWithY> lastVerticalSegmentList;
		lastVerticalSegmentList = newVerticalSegmentList;
		newVerticalSegmentList = getSegmentsAtX(newVerticalSegmentList, endingX);

		@NotNull Iterator<LineSegmentWithY> lastIt = lastVerticalSegmentList.iterator();
		@NotNull Iterator<LineSegmentWithY> newIt = newVerticalSegmentList.iterator();

		@NotNull List<LineSegmentWithY> mismatchingSegments = new LinkedList<>();
		LineSegmentWithY lastVerticalSegment;
		LineSegmentWithY newVerticalSegment;
		while (lastIt.hasNext()) {
			lastVerticalSegment = lastIt.next();
			LineSegmentWithID lastLineSegmentWithID = lastVerticalSegment.getLineSegment();
			long id = lastLineSegmentWithID.getId();
			if (checkMismatchingSegments(collidedSegments, mismatchingSegments, lastLineSegmentWithID)) {
				continue;
			}

			while (newIt.hasNext() && (newVerticalSegment = newIt.next()).getLineSegment() != lastLineSegmentWithID) {
				mismatchingSegments.add(newVerticalSegment);
				long newSegmentID = newVerticalSegment.getLineSegment().getId();
				if (id != newSegmentID) {
					collidedSegments.add(new Pair<>(lastVerticalSegment.getLineSegment(), newVerticalSegment.getLineSegment()));
				}
			}
		}
		return newVerticalSegmentList;
	}

	/**
	 * Recompute and sort segments at given X.
	 *
	 * @param segmentList List of segments to reorder
	 * @param x           X value to compute Y at
	 * @return Sorted segments by Y coordination at X
	 */
	@NotNull
	private static PriorityQueue<LineSegmentWithY> getSegmentsAtX(@NotNull Queue<LineSegmentWithY> segmentList, double x) {
		return segmentList.stream()
			.map(lineSegmentWithY -> {
				LineSegmentWithID lineSegmentWithID = lineSegmentWithY.getLineSegment();
				return new LineSegmentWithY(lineSegmentWithID.getYAtX(x), lineSegmentWithID);
			})
			.collect(Collectors.toCollection(PriorityQueue::new));
	}

	/**
	 * Check if mismatching segments contain actually processing segment.
	 *
	 * @param collidedSegments    Already collided segments
	 * @param mismatchingSegments List of segments with switched order
	 * @param lastVerticalSegment Actually processing segment in last state
	 * @return True if mismatching segments contain the segment, otherwise False
	 */
	private static boolean checkMismatchingSegments(@NotNull Set<Pair<LineSegmentWithID, LineSegmentWithID>> collidedSegments, @NotNull List<LineSegmentWithY> mismatchingSegments, @NotNull LineSegmentWithID lastVerticalSegment) {
		@NotNull Iterator<LineSegmentWithY> mismatchingSegmentsIt = mismatchingSegments.iterator();
		while (mismatchingSegmentsIt.hasNext()) {
			LineSegmentWithID mismatchingSegment = mismatchingSegmentsIt.next().getLineSegment();
			long mismatchingSegmentID = mismatchingSegment.getId();
			if (mismatchingSegment == lastVerticalSegment) {
				mismatchingSegmentsIt.remove();
				return true;
			} else {
				long id = lastVerticalSegment.getId();
				if (id != mismatchingSegmentID) {
					collidedSegments.add(new Pair<>(lastVerticalSegment, mismatchingSegment));
				}
			}
		}
		return false;
	}

	/**
	 * Local class representing segment with saved Y value.
	 */
	private record LineSegmentWithY(double y, LineSegmentWithID lineSegment) implements Comparable<LineSegmentWithY> {

		/**
		 * Compare two line segments by Y value.
		 *
		 * @param lineSegment Compared line segment
		 * @return Double difference if Y differs, otherwise line segment comparison
		 */
		@Override
		public int compareTo(@NotNull Collisions.LineSegmentWithY lineSegment) {
			int yDiff = Double.compare(y, lineSegment.y);
			return yDiff == 0 ? this.lineSegment.compareTo(lineSegment.lineSegment) : yDiff;
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			@NotNull LineSegmentWithY that = (LineSegmentWithY) o;
			return Double.compare(that.y, y) == 0 && lineSegment.equals(that.lineSegment);
		}

		@Override
		public int hashCode() {
			return Objects.hash(y, lineSegment);
		}

		/**
		 * @return Saved Y value
		 */
		public double getY() {
			return y;
		}

		/**
		 * @return Representing line segment
		 */
		public LineSegmentWithID getLineSegment() {
			return lineSegment;
		}
	}
}
