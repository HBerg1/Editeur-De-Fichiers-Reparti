package psar.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NFSServer extends Remote{
	NFSFile getFile(String name) throws RemoteException ;
	void closeFile(String name) throws RemoteException ;	
}
