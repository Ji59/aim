module aim.main {
	// Require JavaFX
	requires javafx.base;
	requires javafx.graphics;
	requires javafx.controls;
	requires javafx.fxml;
	requires org.jetbrains.annotations;
	requires com.google.gson;
	requires java.desktop;

	// Export the "cz.cuni.mff.kotal" package (needed by JavaFX to start the Application)
	exports cz.cuni.mff.kotal;

	//  allow to access your classes via reflections
	opens cz.cuni.mff.kotal.frontend to javafx.graphics;
	opens cz.cuni.mff.kotal.simulation to com.google.gson;
}
