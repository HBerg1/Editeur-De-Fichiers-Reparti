package psar.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.Map.Entry;

import psar.client.Client;

public class NFSFileImpl implements NFSFile {
	private final List<String> lines;
	private final List<Client> clients;
	private final Map<Integer, Queue<Client>> managerLines;
	private final String path;

	public NFSFileImpl(String name) {
		this.path = name;
		this.lines = new ArrayList<>();
		this.clients = new ArrayList<Client>();
		this.managerLines = new HashMap<>();

		// get lines of the file
		try (BufferedReader br = new BufferedReader(new FileReader(name))) {
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean save(int line, String content, Client c) throws RemoteException {
		synchronized (lines) {

			// update current file
			if(lines.isEmpty()) {
				lines.add(content);
			} else {
				lines.set(line, content);
			}
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.path))) {
				for (String l : lines) {
					writer.write(l);
					writer.newLine(); // add a new line after each line
				}
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}

			// update lines of client that open the file
			this.sendLineToClients(c, line, "update", content);
			Queue<Client> manager = managerLines.get(line);


			if(manager!=null && manager.size() > 1) {
				this.removeClientFromManagerLines(line);
			} else {
				c.accessLine(line);
			}
		}

		return true;
	}

	public List<String> getLines() {
		synchronized (lines) {
			return lines;
		}
	}

	@Override
    public boolean accessLine(int line, Client c, boolean currentAccess, int currentLine, int awaitingLine) throws RemoteException {
        synchronized (managerLines) {
            Queue<Client> queue = managerLines.get(line);
            boolean access = (queue == null) || queue.size() == 0;
            if(currentLine == line&&queue!=null && queue.peek().equals(c)) return true;

            if(awaitingLine >= 0) {
            	this.removeClientFromManagerLinesAwaitingLine(awaitingLine, c);
            }
            // we suppose that we trust currentAccess
            if (currentAccess) {
                this.removeClientFromManagerLines(currentLine);
            }

            // Prevent everyone for the all new accesses
            if (access) {
                System.out.println("accès");
                Queue<Client> q = new LinkedList<>();
                q.add(c);
                managerLines.put(line, q);
                this.sendLineToClients(c, line, "lock", "");
            } else {
                System.out.println("pas accès");
                queue.add(c);
            }
            return access;
        }
    }

	public void unlockLine(int line, Client c) throws RemoteException {
		synchronized (managerLines) {
			Queue<Client> queue = managerLines.get(line);
			queue.remove(c);
			this.removeClientFromManagerLines(line);
			this.sendLineToClients(c, line, "unlock", "");

		}
	}
	
	public void unlockLine(int line) throws RemoteException {
		synchronized (clients) {
			for (Client client : clients) {
				client.unlockLine(line);
			}
		}
	}
	
	

	@Override
	public boolean registerNewAccess(Client c) throws RemoteException {
		synchronized (clients) {
			clients.add(c);
			return true;
		}
	}


	
	@Override
    public boolean addLine(int line, Client c) throws RemoteException {
        synchronized (lines) {
            synchronized (managerLines) {
                // update lines of the file
                List<String> start = null;
                List<String> last;

                // it is new line
                if (line > lines.size()) {
                    start = new ArrayList<>(lines);
                    start.add(""); // Add an empty line
                    lines.clear();
                    lines.addAll(start);
                } else {
                    start = new ArrayList<>(lines.subList(0, line));
                    last = lines.subList(line, lines.size());
                    start.add(""); // Add an empty line
                    start.addAll(last);
                    lines.clear();
                    lines.addAll(start);
                }


                writeLines();
                // update access line of clients
                Map<Integer, Queue<Client>> managerTemp = new HashMap<>();
                for (Iterator<Entry<Integer, Queue<Client>>> iterator = managerLines.entrySet().iterator(); iterator
                        .hasNext();) {
                    Entry<Integer, Queue<Client>> ml = iterator.next();
                    Integer newline = ml.getKey();
                    if (newline + 1 > line) {
                    	Queue<Client> clients = ml.getValue();
                        iterator.remove(); // Safely remove entry from the map
                        managerTemp.put(newline+1, clients);
                    }
                }
                managerLines.putAll(managerTemp);
            }
            this.sendLineToClients(c, line, "add", "");
            this.removeClientFromManagerLines(line);
        }

        return true;
    }


	
	@Override
    public boolean deleteLine(int line, Client c) throws RemoteException {
        synchronized (lines) {
            synchronized (managerLines) {
                // update lines of the file
                lines.remove(line);
                managerLines.remove(line);
                writeLines();
                Map<Integer,Queue<Client>> managerTemp = new HashMap<>();
                // update access line of clients
                for (Iterator<Entry<Integer, Queue<Client>>> iterator = managerLines.entrySet().iterator(); iterator
                        .hasNext();) {
                    Entry<Integer, Queue<Client>> ml = iterator.next();
                    Integer newline = ml.getKey() - 1;
                    if (newline + 1 > line) {
                        Queue<Client> clients = ml.getValue();
                        iterator.remove(); // Safely remove entry from the map
                        managerTemp.put(newline, clients);
                    }
                }
                managerLines.putAll(managerTemp);
            }
            this.sendLineToClients(c, line, "delete", "");
        }
        return true;

    }

	private void removeClientFromManagerLines(int line) throws RemoteException {
		synchronized (managerLines) {
			Queue<Client> manager = managerLines.get(line);
			if (manager != null && manager.size()!=0) {
				Client noAccess = manager.remove();
				Client access = manager.peek();
				if (access != null) {
					System.out.println("accessLine");
					access.accessLine(line);
					noAccess.cantAccessLine(line);
				} else {
					unlockLine(line);
				}
			}
		}
	}
	
	private void removeClientFromManagerLinesAwaitingLine(int line, Client c) throws RemoteException{
		synchronized (managerLines) {
			Queue<Client> manager = managerLines.get(line);
			if (manager != null && manager.size()!=0) {
				 manager.remove(c);
			}
		}
		
	}

	private void sendLineToClients(Client c, int line, String how, String content) throws RemoteException {
		synchronized (clients) {
			for (Client client : clients) {
				if (!client.equals(c)) {
					switch (how) {
					case "add":
						client.addLine(line);
						break;
					case "delete":
						client.deleteLine(line);
						break;
					case "lock":
						client.lockLine(path, line);
						break;
					case "update":
						client.updateLine(line, content);
						break;
					case "unlock":
						client.unlockLine(line);
						break;
					default:
					}
				}
			}
		}
	}

	@Override
	public boolean unregisterClient(Client c) throws RemoteException {
		synchronized (managerLines) {
			synchronized (clients) {
				clients.remove(c);
				System.out.println("client qui se desenregistre");
				Iterator<Map.Entry<Integer, Queue<Client>>> iterator = managerLines.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<Integer, Queue<Client>> entry = iterator.next();
					Integer line = entry.getKey();
					Queue<Client> manager = entry.getValue();

					// Remove the client from the queue
					if (!manager.contains(c)) {
						continue;
					}
					manager.remove(c);

					// If the queue is empty, remove the line
					if (manager.isEmpty()) {
						iterator.remove();
						unlockLine(line);
						System.out.println("ligne file retiré");
					} else {
						// Grant access to the next
						Client access = manager.peek();
						if (access != null) {
							access.accessLine(line);
							System.out.println("donne client acces");
						}
					}
				}
			}
		}
		return true;
	}

	private void writeLines() {
		synchronized (lines) {
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.path))) {
				for (String l : lines) {
					writer.write(l);
					writer.newLine(); // add new line after each line
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public Set<Integer> getLockedLines() throws RemoteException {
		HashSet<Integer> lockedLines = new HashSet<>();

        for (Integer key : managerLines.keySet()) {
        	if(managerLines.get(key).size() != 0) {        		
        		lockedLines.add(key);
        	}
        }

        return lockedLines;
	}

}