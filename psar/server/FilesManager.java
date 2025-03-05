package psar.server;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class FilesManager {
	private Map<String, NFSFile> files; // name, file
	private Map<String, Integer> filesCounter; // name, counter
	private Registry registry;

	public FilesManager() throws RemoteException {
		super();
		this.files = new HashMap<>();
		this.filesCounter = new HashMap<>();
		this.registry = LocateRegistry.getRegistry("localhost", 1099);
		
	}
	
	public NFSFile getFile(String name) throws RemoteException {
		if (files.get(name)==null) {
            NFSFileImpl fileImpl = new NFSFileImpl(name); // create instance
            UnicastRemoteObject.exportObject(fileImpl, 0); // export object
            files.put(name, fileImpl); // add the stub 
            filesCounter.put(name, 0);
			// Save the stub in registry RMI
            registry.rebind(name, fileImpl);
		}
		filesCounter.put(name, filesCounter.get(name)+1);
		return files.get(name);
	}
	
	
	public void closeFile(String name) throws AccessException, RemoteException, NotBoundException {
		int c = filesCounter.get(name);
		if(c-1==0) {
			filesCounter.remove(name);
			NFSFile f = files.remove(name);
			registry.unbind(name);
			UnicastRemoteObject.unexportObject(f, true);
		} else {
			filesCounter.put(name, filesCounter.get(name)-1);
		}
	}
}