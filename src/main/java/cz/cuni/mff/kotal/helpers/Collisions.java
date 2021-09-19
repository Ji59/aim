package cz.cuni.mff.kotal.helpers;

import cz.cuni.mff.kotal.frontend.simulation.AgentPane;
import cz.cuni.mff.kotal.frontend.simulation.AgentPolygon;
import cz.cuni.mff.kotal.frontend.simulation.LineSegment;
import cz.cuni.mff.kotal.frontend.simulation.Point;
import cz.cuni.mff.kotal.frontend.simulation.timer.AgentBoundingBox;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class Collisions {
	// TODO refactor
	public static Set<Pair<AgentPane, AgentPane>> getBoundingBoxesOverlaps(Map<Long, AgentPane> agents) {
		List<AgentBoundingBox> boundingBoxes;
		synchronized (agents) {
			boundingBoxes = agents.values().stream().filter(a -> !a.isDisabled()).map(a -> new AgentBoundingBox(a, a.getBoundingBox())).sorted().collect(Collectors.toList());
		}

		Set<Pair<AgentPane, AgentPane>> overlappingAgents = new HashSet<>();
		List<AgentBoundingBox> actualBoundingBoxes = new LinkedList<>();

		SortedSet<AgentBoundingBox> stopValues = new TreeSet<>((a0, a1) -> {
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

		for (AgentBoundingBox agentBoundingBox : boundingBoxes) {
			double boundingBoxStartX = agentBoundingBox.getStartX();
			Iterator<AgentBoundingBox> iterator = stopValues.iterator();
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

	public static boolean inCollision(AgentPane agent0, AgentPane agent1) {
		List<Point> cornerPoints0 = agent0.getCornerPoints();
		List<Point> cornerPoints1 = agent1.getCornerPoints();

		if (existsSeparatingLine(cornerPoints0, cornerPoints1)) {
			return false;
		}

		return !existsSeparatingLine(cornerPoints1, cornerPoints0);
	}

	public static boolean existsSeparatingLine(List<Point> points0, List<Point> points1) {
		points0 = new LinkedList<>(points0);
		points1 = new LinkedList<>(points1);

		int points0Size = points0.size();

		Point point0 = points0.remove(0);
		Point point1 = null;

		for (int i = 0; i < points0Size; i++) {
			point1 = points0.remove(0);
			LineSegment line = new LineSegment(point0, point1);

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

	public static Set<Collision> getTimeShiftCollisions(Map<Long, AgentPolygon> lastStates, Map<Long, AgentPolygon> newStates) {
		Set<Pair<LineSegmentWithID, LineSegmentWithID>> collidedSegments = new HashSet<>();

		Queue<LineSegmentWithID> lineSegments = new PriorityQueue<>(lastStates.size());

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

		Queue<LineSegmentWithY> newVerticalSegmentQueue = new PriorityQueue<>();

		Queue<LineSegmentWithID> segmentEnds = new PriorityQueue<>(Comparator.comparingDouble(LineSegment::getX1));

		LineSegmentWithID nextSegment;
		while ((nextSegment = lineSegments.poll()) != null) {
			LineSegmentWithID nextEnd;
			while ((nextEnd = segmentEnds.peek()) != null && nextEnd.getX1() < nextSegment.getX0()) {
				newVerticalSegmentQueue = checkLineSegmentsCollisions(collidedSegments, newVerticalSegmentQueue, nextEnd.getX1());
			}

			newVerticalSegmentQueue = checkLineSegmentsCollisions(collidedSegments, newVerticalSegmentQueue, nextSegment.getX0());

			final double startingY = nextSegment.getY0();
			for (LineSegmentWithY lineSegmentWithY : newVerticalSegmentQueue) {
				if (lineSegmentWithY.getY() == startingY) {
					collidedSegments.add(new Pair<>(lineSegmentWithY.getLineSegment(), nextSegment));
				} else if (lineSegmentWithY.getY() > startingY) {
					break;
				}
			}

			newVerticalSegmentQueue.add(new LineSegmentWithY(startingY, nextSegment));
			segmentEnds.add(nextSegment);
		}

		return collidedSegments.stream()
			.map(lineSegmentPair -> {
				LineSegmentWithID line0 = lineSegmentPair.getKey();
				LineSegmentWithID line1 = lineSegmentPair.getValue();
				return new Collision(line0.getId(), line1.getId(), line0.getIntersectionRatio(line1));
			})
			.collect(Collectors.toSet());
	}

	@NotNull
	private static Queue<LineSegmentWithY> checkLineSegmentsCollisions(Set<Pair<LineSegmentWithID, LineSegmentWithID>> collidedSegments, Queue<LineSegmentWithY> newVerticalSegmentList, double endingX) {
		Queue<LineSegmentWithY> lastVerticalSegmentList;
		lastVerticalSegmentList = newVerticalSegmentList;
		newVerticalSegmentList = getSegmentsAtX(newVerticalSegmentList, endingX);

		Iterator<LineSegmentWithY> lastIt = lastVerticalSegmentList.iterator();
		Iterator<LineSegmentWithY> newIt = newVerticalSegmentList.iterator();

		List<LineSegmentWithY> mismatchingSegments = new LinkedList<>();
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

	@NotNull
	private static PriorityQueue<LineSegmentWithY> getSegmentsAtX(Queue<LineSegmentWithY> segmentList, double x) {
		return segmentList.stream()
			.map(lineSegmentWithY -> {
				LineSegmentWithID lineSegmentWithID = lineSegmentWithY.getLineSegment();
				return new LineSegmentWithY(lineSegmentWithID.getYAtX(x), lineSegmentWithID);
			})
			.collect(Collectors.toCollection(PriorityQueue::new));
	}

	private static boolean checkMismatchingSegments(Set<Pair<LineSegmentWithID, LineSegmentWithID>> collidedSegments, List<LineSegmentWithY> mismatchingSegments, LineSegmentWithID lastVerticalSegment) {
		Iterator<LineSegmentWithY> mismatchingSegmentsIt = mismatchingSegments.iterator();
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

	private static class LineSegmentWithY implements Comparable<LineSegmentWithY> {
		private final double y;
		private final LineSegmentWithID lineSegment;

		private LineSegmentWithY(double y, LineSegmentWithID lineSegment) {
			this.y = y;
			this.lineSegment = lineSegment;
		}

		@Override
		public int compareTo(@NotNull Collisions.LineSegmentWithY lineSegment) {
			int yDiff = Double.compare(y, lineSegment.y);
			return yDiff == 0 ? this.compareTo(lineSegment) : yDiff;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			LineSegmentWithY that = (LineSegmentWithY) o;
			return Double.compare(that.y, y) == 0 && lineSegment.equals(that.lineSegment);
		}

		@Override
		public int hashCode() {
			return Objects.hash(y, lineSegment);
		}

		public double getY() {
			return y;
		}

		public LineSegmentWithID getLineSegment() {
			return lineSegment;
		}
	}
}
