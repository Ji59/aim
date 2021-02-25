package cz.cuni.mff.kotal;


import cz.cuni.mff.kotal.frontend.MyApplication;
import javafx.application.Application;


public class AIM {

	/**
	 * Main function.
	 * Start application GUI.
	 *
	 * @param args Input arguments, unused.
	 */
	public static void main(String[] args) throws InterruptedException {
		// TODO remove hello world output
		System.out.println("Hello there.");

		Thread gui = new Thread(() -> Application.launch(MyApplication.class, args));
		gui.start();

		gui.join();
	}
}
