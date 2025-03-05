package psar.client;

import java.rmi.RemoteException;

import javafx.application.Application;

import javafx.stage.Stage;

public class ClientMain extends Application{
	@Override
	public void start(Stage stage){
		try {	
			new TextEditor(stage);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
	    try {
	        launch(args);            
	    } catch (Exception e) {
	        System.err.println("Erreur, le serveur n'est peut être pas encore lancé : " + e.getMessage());
	    }
    }
}
