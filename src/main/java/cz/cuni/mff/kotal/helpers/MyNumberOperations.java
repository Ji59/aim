package cz.cuni.mff.kotal.helpers;


import cz.cuni.mff.kotal.frontend.simulation.Point;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Class with special number operations.
 */
public class MyNumberOperations {

	/**
	 * Private constructor to mask implicit one.
	 */
	private MyNumberOperations() {
	}

	/**
	 * Compare if two double values are similar enough.
	 *
	 * @param d0        First number
	 * @param d1        Second number
	 * @param proximity Maximal allowed difference between the numbers
	 * @return True if difference between the numbers are within specified range
	 */
	public static boolean doubleAlmostEqual(double d0, double d1, double proximity) {
		double difference = Math.abs(d0 - d1);
		return difference <= proximity;
	}

	/**
	 * Compute angle of line between specified two points.
	 *
	 * @param startX Starting X coordinate of the first point
	 * @param startY Starting Y coordinate of the first point
	 * @param endX   Ending X coordinate of the first point
	 * @param endY   Ending Y coordinate of the first point
	 * @return Angle of line
	 */
	public static double computeRotation(double startX, double startY, double endX, double endY) {
		double xDiff = endX - startX;
		double yDiff = endY - startY;
		if (yDiff == 0) {
			if (xDiff == 0) {
				return -1;
			} else {
				if (xDiff > 0) return 90;
				return 270;
			}
		}

		// TODO WTF?
		double angle = Math.toDegrees(Math.atan(Math.abs(xDiff / yDiff)));
		if (xDiff >= 0 && yDiff < 0) {
			angle = 180 - angle;
		} else if (xDiff < 0 && yDiff >= 0) {
			angle = 360 - angle;
		} else if (xDiff < 0 && yDiff < 0) {
			angle = 180 + angle;
		}
		return 360 - angle;
	}

	/**
	 * Computes modulo and convert result to non-negative numbers.
	 *
	 * @param number Input number to modify
	 * @param modulo Number taken as base
	 * @return Positive modulo of number
	 */
	public static int myModulo(int number, int modulo) {
		int result = number % modulo;
		return result >= 0 ? result : modulo + result;
	}

	/**
	 * Computes modulo and convert result to non-negative numbers.
	 *
	 * @param number Input number to modify
	 * @param modulo Number taken as base
	 * @return Positive modulo of number
	 */
	public static long myModulo(long number, long modulo) {
		long result = number % modulo;
		return result >= 0 ? result : modulo + result;
	}


	// TODO
	public static double distance(double x0, double y0, double x1, double y1) {
		double xDiff = x1 - x0;
		double yDiff = y1 - y0;
		return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
	}

	public static double distance(Point p0, Point p1) {
		return distance(p0.getX(), p0.getY(), p1.getX(), p1.getY());
	}

	public static double perimeter(double l, double w) {
		return Math.sqrt(l * l + w * w) / 2;
	}

	public static <T> T[][] cartesianProduct(T[][] arrays, Class<T> tClass) {
		int size = 1;
		for (T[] array : arrays) {
			size *= array.length;
		}

		T[][] cartesian = (T[][]) Array.newInstance(tClass, size, arrays.length);

		for (int i = 0, period = size; i < arrays.length; i++) {
			period /= arrays[i].length;
			T[] array = arrays[i];
			int j = 0;
			while (j < size) {
				for (T id : array) {
					for (int l = 0; l < period; l++, j++) {
						cartesian[j][i] = id;
					}
				}
			}
		}

		return cartesian;
	}

	public static <T> Stream<List<T>> combinations(final T[] arr) {
		final long N = (long) Math.pow(2, arr.length);
		return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(N, Spliterator.SIZED) {
			long i = N - 1;

			@Override
			public boolean tryAdvance(Consumer<? super List<T>> action) {
				if (i > 0) {
					List<T> out = new ArrayList<>(Long.bitCount(i));
					for (int bit = 0; bit < arr.length; bit++) {
						if ((i & (1 << bit)) != 0) {
							out.add(arr[bit]);
						}
					}
					action.accept(out);
					i--;
					return true;
				} else {
					return false;
				}
			}
		}, false);
	}

	public static <T> Stream<Collection<T>> combinations(final Collection<T> arr) {
		final long N = (long) Math.pow(2, arr.size());
		return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(N, Spliterator.SIZED) {
			int i = arr.size();
			long j = 0;
			long nCi = combinationNumber(arr.size(), i);

			@Override
			public boolean tryAdvance(Consumer<? super Collection<T>> action) {
				if (i >= 0) {
					Set<T> out = new HashSet<>(i);
					action.accept(out);
					i--;
					// TODO
					return true;
				} else {
					return false;
				}
			}
		}, false);
	}

	/**
	 * nCk = (n-1)Ck + (n-1)C(k-1)
	 *
	 * @param arr
	 * @param size
	 * @param <T>
	 * @return
	 */
	public static <T> Collection<Collection<T>> combinations(final Collection<T> arr, int size) {
		if (size <= 0) {
			return Collections.singleton(new HashSet<>());
		} else if (arr.size() <= size) {
			return Collections.singleton(arr);
		}

		Collection<T> arrCopy = new ArrayList<>(arr.size() - 1);
		Iterator<T> it = arr.iterator();
		T element = it.next();
		while (it.hasNext()) {
			arrCopy.add(it.next());
		}

		Collection<Collection<T>> combinations = new HashSet<>(combinations(arrCopy, size));

		for (Collection<T> smaller : combinations(arrCopy, size - 1)) {
			smaller.add(element);
			combinations.add(smaller);
		}

		return combinations;
	}

	public static long combinationNumber(long n, long r) {
		if (n < r || n == 0)
			return 1;

		long num = 1;
		long den = 1;
		for (long i = r; i >= 1; i--) {
			num = num * n--;
			den = den * i;
		}

		return num / den;

	}

	// TODO move to test
	public static void main(String... args) {
		String[] arr = new String[]{"0", "1", "2", "3", "4"};
		String result = combinations(arr).map(l -> String.join(" ", l)).collect(Collectors.joining("\n"));
		System.out.println(result);

		List<String> arr1 = new LinkedList<>(Arrays.asList(arr));
		for (int i = 0; i <= arr1.size(); i++) {
			Collection<Collection<String>> combinations = combinations(arr1, i);
			for (Collection<String> result1 : combinations) {
				System.out.println(String.join(" ", result1));
			}
			System.out.println(combinationNumber(arr1.size(), i) + ", " + combinations.size() + '\n');
		}
	}
}
