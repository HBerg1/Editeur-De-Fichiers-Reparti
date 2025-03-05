module psar {
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.base;
	requires java.rmi;
	exports psar.server;
	exports psar.client;
	opens psar.client to javafx.graphics, javafx.fxml;
}