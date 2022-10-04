package cz.cuni.mff.kotal.helpers;


import java.util.Random;

/**
 * Class for generating random numbers.
 */
public class MyGenerator {

	/**
	 * Private constructor to mask implicit one.
	 */
	private MyGenerator() {
	}

	/**
	 * Generate random long number between 0 and specified maximum.
	 *
	 * @param maximum Maximal generated number
	 * @return Random number
	 */
	public static long generateRandomLong(long maximum) {
		return generateRandomLong(0, maximum);
	}

	/**
	 * Generate random integer between 0 and specified maximum.
	 *
	 * @param maximum Maximal generated number
	 * @return Random number
	 */
	public static int generateRandomInt(int maximum) {
		return (int) generateRandomLong(maximum);
	}

	/**
	 * Generate random long number between specified minimum and maximum.
	 *
	 * @param minimum Minimal generated number
	 * @param maximum Maximal generated number
	 * @return Random number
	 */
	public static long generateRandomLong(long minimum, long maximum) {
		double random = Math.random();
		if (random == 0) {
			return 0;
		} else return Math.round(random * (maximum - minimum + 1) + minimum - 0.5);
	}

	/**
	 * Generate random decimal number between specified minimum and maximum, add deviation.
	 *
	 * @param minimum      Minimal generated number
	 * @param maximum      Maximal generated number
	 * @param maxDeviation Maximal deviation of the generated number
	 * @return Random number
	 */
	public static double generateWithDeviation(long minimum, long maximum, double maxDeviation) {
		long baseValue = generateRandomLong(minimum, maximum);
		if (maxDeviation == 0) {
			return baseValue;
		}

		double deviation = Math.random() * maxDeviation;
		boolean deviationSmaller = Math.random() < 0.5;
		deviation = 1 + (deviationSmaller ? -1 : 1) * deviation;
		return baseValue * deviation;
	}

	public static double generateRandom(double minimum, double maximum) {
		return Math.random() * (maximum - minimum) + minimum;
	}
}
