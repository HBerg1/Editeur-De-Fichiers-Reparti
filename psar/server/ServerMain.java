package psar.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class ServerMain {
    public static void main(String[] args) {
        try {
        	NFSServerImpl server = new NFSServerImpl();

            // Création du registre RMI sur le port 1099
            Registry registry = LocateRegistry.createRegistry(1099);
            // Liaison du serveur au registre
            registry.rebind("NFSServer", server);
            UnicastRemoteObject.exportObject(server, 1099);

            // On est à la racine du projet donc on peut directement accéder à Files
            	// Enfaite dépends des paramètres de lancement de l'IDE, voir le workspace
            server.getFile("Files/File1");



            System.out.println("Serveur RMI démarré avec succès.");
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du serveur RMI : " + e.getMessage());
        }
    }
}
