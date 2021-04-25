package cz.cuni.mff.kotal;


public class MyGenerator {
	public static long generateRandomLong(long maximum) {
		return generateRandomLong(0, maximum);
	}

	public static int generateRandomInt(int maximum) {
		return (int) generateRandomLong(maximum);
	}

	public static long generateRandomLong(long minimum, long maximum) {
		double random = Math.random();
		if (random == 0) {
			return 0;
		} else return Math.round(random * (maximum - minimum + 1) + minimum - 0.5);
	}
}
