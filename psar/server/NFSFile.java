package psar.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import psar.client.Client;

public interface NFSFile extends Remote {
	boolean registerNewAccess(Client c) throws RemoteException;
	boolean unregisterClient(Client c) throws RemoteException;
	boolean save(int line, String content, Client c) throws RemoteException;
	boolean deleteLine(int line, Client c) throws RemoteException;
	boolean addLine(int line, Client c) throws RemoteException; // correspond à la ligne où a lieu l'insertion
	boolean accessLine(int line, Client c, boolean currentAccess, int currentLine, int awaitingLine) throws RemoteException;
	List<String> getLines() throws RemoteException;
	Set<Integer> getLockedLines() throws RemoteException;
}