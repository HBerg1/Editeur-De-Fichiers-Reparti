package psar.client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Client extends Remote {
	void lockLine(String fileName, int line) throws RemoteException;
	void updateLine(int line, String content) throws RemoteException; 
	void addLine(int line) throws RemoteException;
	void deleteLine(int line) throws RemoteException;
	void accessLine(int line) throws RemoteException;
	void cantAccessLine(int line) throws RemoteException;
	void unlockLine(int line) throws RemoteException;
	
}