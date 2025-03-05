package psar.server;

import java.rmi.RemoteException;

public class NFSServerImpl implements NFSServer {
	private FilesManager manager;
	
	
	public NFSServerImpl() throws RemoteException {
		super();
		this.manager = new FilesManager();
	}
	
	@Override
	public NFSFile getFile(String name) throws RemoteException {
		// we suppose that the path is valid
		return manager.getFile(name);

	}

	@Override
	public void closeFile(String name) throws RemoteException {
		try {
			manager.closeFile(name);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}