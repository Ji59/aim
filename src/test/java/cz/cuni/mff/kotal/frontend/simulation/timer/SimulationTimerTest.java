package cz.cuni.mff.kotal.frontend.simulation.timer;

import cz.cuni.mff.kotal.frontend.simulation.Point;
import cz.cuni.mff.kotal.helpers.Collisions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class SimulationTimerTest {

	@BeforeEach
	void setUp() {
	}

	@Test
	void existsSeparatingLine() {
		@NotNull Point a0 = new Point(0, 0);
		@NotNull Point a1 = new Point(0, 1);
		@NotNull Point a2 = new Point(1, 1);
		@NotNull Point a3 = new Point(1, 0);

		@NotNull Point b0 = new Point(1, 1.1);
		@NotNull Point b1 = new Point(0, 1.1);
		@NotNull Point b2 = new Point(0, 2);
		@NotNull Point b3 = new Point(1, 2);

		@NotNull List<Point> points0 = getLinkedList(a0, a1, a2, a3);
		@NotNull List<Point> points1 = getLinkedList(b0, b1, b2, b3);

		assert Collisions.existsSeparatingLine(points0, points1);
		assert Collisions.existsSeparatingLine(points1, points0);

		points1.remove(0);
		points1.add(0, new Point(1, 0));


		assert !Collisions.existsSeparatingLine(points0, points1);
		assert !Collisions.existsSeparatingLine(points1, points0);

		points1 = getLinkedList(0.5, 1.1,
			1.2, 0.9,
			1.5, 1.7,
			0., 2.);

		assert !Collisions.existsSeparatingLine(points0, points1);
		assert !Collisions.existsSeparatingLine(points1, points0);


		points0 = getLinkedList(
			0., 0.,
			0., 1.,
			1., 0.9,
			1., 0.5
		);

		assert Collisions.existsSeparatingLine(points0, points1);
		assert Collisions.existsSeparatingLine(points1, points0);

		double @NotNull [] pointDoubles0 = {100.01, 150.7,
			150.7, 100.01,
			201.42, 150.7,
			150.7, 201.42};
		points0 = getLinkedList(
			pointDoubles0
		);

		double @NotNull [] pointDoubles1 = {70.5, 100.,
			180., 50.,
			300., 100.,
			180., 120.};
		points1 = getLinkedList(
			pointDoubles1
		);

		assert !Collisions.existsSeparatingLine(points0, points1);
		assert !Collisions.existsSeparatingLine(points1, points0);

		points0.remove(1);
		points0.add(1, new Point(150, 115));

		assert !Collisions.existsSeparatingLine(points0, points1);
		assert Collisions.existsSeparatingLine(points1, points0);
	}

	@NotNull
	private List<Point> getLinkedList(Point @NotNull ... points) {
		return Arrays.stream(points).collect(Collectors.toList());
	}

	@NotNull
	private List<Point> getLinkedList(double @NotNull ... pointsCoordinates) {
		@NotNull List<Point> points = new ArrayList<>(pointsCoordinates.length / 2);
		for (int i = 0; i < pointsCoordinates.length / 2; i++) {
			points.add(new Point(pointsCoordinates[2 * i], pointsCoordinates[2 * i + 1]));
		}
		return points;
	}
}