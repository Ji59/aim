package cz.cuni.mff.kotal.helpers;


import java.util.Random;

/**
 * Class for generating random numbers.
 */
public class MyGenerator {
	private static final Random randomGenerator = new Random();

	/**
	 * Private constructor to mask implicit one.
	 */
	private MyGenerator() {
	}

	/**
	 * Generate random integer between 0 and specified maximum.
	 *
	 * @param maximum Maximal generated number
	 *
	 * @return Random number
	 */
	public static int generateRandomInt(int maximum) {
		return (int) generateRandomLong(maximum);
	}

	/**
	 * Generate random long number between 0 and specified maximum.
	 *
	 * @param maximum Maximal generated number
	 *
	 * @return Random number
	 */
	public static long generateRandomLong(long maximum) {
		return generateRandomLong(0, maximum);
	}

	/**
	 * Generate random long number between specified minimum and maximum inclusively with uniform distribution.
	 *
	 * @param minimum Minimal generated number
	 * @param maximum Maximal generated number
	 *
	 * @return Random number
	 */
	public static long generateRandomLong(long minimum, long maximum) {
		return randomGenerator.nextLong(minimum, maximum + 1);
	}

	/**
	 * Generate random decimal number between specified minimum and maximum inclusive with linear distribution, add deviation.
	 *
	 * @param minimum      Minimal generated number
	 * @param maximum      Maximal generated number
	 * @param maxDeviation Maximal deviation of the generated number
	 *
	 * @return Random number
	 */
	public static double generateRandomWithDeviation(final long minimum, final long maximum, final double maxDeviation) {
		long baseValue = generateRandomLong(minimum, maximum);
		if (maxDeviation == 0) {
			return baseValue;
		}

		double deviation = randomGenerator.nextDouble(maxDeviation);
		deviation = 1 + (randomGenerator.nextBoolean() ? -1 : 1) * deviation;
		return baseValue * deviation;
	}

	/**
	 * Generate random decimal number between specified minimum and maximum inclusive with gaussian distribution, add deviation.
	 * Mean of distribution is in the middle between minimum and maximum, standard deviation is `(maximum - minimum) / 2`.
	 *
	 * @param minimum      Minimal generated number
	 * @param maximum      Maximal generated number
	 * @param maxDeviation Maximal deviation of the generated number
	 *
	 * @return Random number
	 */
	public static double generateRandomGaussianWithDeviation(long minimum, long maximum, double maxDeviation) {
		long baseValue = generateRandomLongGaussian(minimum, maximum);
		if (maxDeviation == 0) {
			return baseValue;
		}

		double deviation;
		do {
			deviation = randomGenerator.nextGaussian(0, maxDeviation / 2);
		}while (deviation < -maxDeviation || deviation > maxDeviation);
		deviation++;
		return baseValue * deviation;
	}

	/**
	 * Generate random long number between specified minimum and maximum inclusively with gaussian distribution.
	 * Mean of distribution is in the middle between minimum and maximum, standard deviation is `(maximum - minimum) / 2`.
	 *
	 * @param minimum Minimal generated number
	 * @param maximum Maximal generated number
	 *
	 * @return Random number
	 */
	public static long generateRandomLongGaussian(long minimum, long maximum) {
		return generateRandomLongGaussian(minimum, maximum, (minimum + maximum) / 2., (maximum - minimum) / 4.);
	}

	/**
	 * Generate random long number between specified minimum and maximum inclusively with gaussian distribution.
	 *
	 * @param minimum Minimal generated number
	 * @param maximum Maximal generated number
	 * @param mean
	 * @param stddev
	 *
	 * @return Random number
	 */
	public static long generateRandomLongGaussian(long minimum, long maximum, double mean, double stddev) {
		assert minimum <= maximum;
		long randomLong;

		do {
			double random = randomGenerator.nextGaussian(mean, stddev);
			randomLong = Math.round(random);
		} while (randomLong < minimum || randomLong > maximum);
		return randomLong;
	}

	public static double generateRandom(double minimum, double maximum) {
		return randomGenerator.nextDouble(minimum, maximum);
	}

	public static double generateRandomGaussian(double minimum, double maximum) {
		return generateRandomGaussian(minimum, maximum, (minimum + maximum) / 2, (maximum - minimum) / 4);
	}

	public static double generateRandomGaussian(double minimum, double maximum, double mean, double stddev) {
		double random;
		do {
			random = randomGenerator.nextGaussian(mean, stddev);
		} while (random < minimum || random > maximum);
		return random;
	}
}
