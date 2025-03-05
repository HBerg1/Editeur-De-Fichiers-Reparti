package psar.client;

import java.net.ServerSocket;
import java.io.IOException;

public class FreePortFinder {
    public static void main(String[] args) {
        int freePort = findFreePort();
        System.out.println("Port libre trouvé : " + freePort);
    }

    // find a free port
    public static int findFreePort() {
        int startPort = 1024; 
        int endPort = 65535; 
        
        for (int port = startPort; port <= endPort; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        throw new IllegalStateException("Aucun port libre disponible.");
    }

    public static boolean isPortAvailable(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
