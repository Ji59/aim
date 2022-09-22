open module aim.main {
	// Require JavaFX
	requires javafx.base;
	requires javafx.graphics;
	requires javafx.controls;
	requires javafx.fxml;
	requires org.jetbrains.annotations;
	requires com.google.gson;
	requires java.desktop;
	requires org.ow2.sat4j.core;
	requires org.ow2.sat4j.maxsat;
	requires org.ow2.sat4j.pb;

	// Export the "cz.cuni.mff.kotal" package (needed by JavaFX to start the Application)
	exports cz.cuni.mff.kotal;
}
