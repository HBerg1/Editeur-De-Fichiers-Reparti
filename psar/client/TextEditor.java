package psar.client;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import psar.server.NFSFile;
import psar.server.NFSServer;


public class TextEditor implements Client {
	private boolean currentAccess = false;
	private int currentLine = -1;
	private int awaitingLine = -1;
	private int cursorPosition;
	private List<String> lines;
	private NFSServer server;
	private NFSFile currentFile;
	private String num;
	Set<Integer> lockedLines = new HashSet<>();
	private final String defaultFilename = "Files/file1";
	
	
	// Elements JavaFX 
	private TextArea textArea;
	private VBox lineNumbers;
	private Label status;

	
	public void setServer() {
    	Registry registry;
		try {
			registry = LocateRegistry.getRegistry("localhost", 1099);
			this.server = (NFSServer) registry.lookup("NFSServer");
			int port = FreePortFinder.findFreePort();
            UnicastRemoteObject.exportObject(this, port);
            this.num = String.valueOf(port);
            registry.rebind(this.num, this);
            
        	this.currentFile = server.getFile(defaultFilename);
        	this.currentFile.registerNewAccess(this);
        	this.lockedLines = currentFile.getLockedLines();
        	lockedLines = currentFile.getLockedLines().stream()
                    .map(element -> element + 1)
                    .collect(Collectors.toSet()); 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            System.err.println("Erreur, le serveur n'est peut être pas encore lancé" + e.getMessage());
		}
    	
	}
	
	ScrollPane setTextLine(TextArea textArea) {
        lineNumbers = new VBox();
        lineNumbers.setPadding(new Insets(5, 5, 5, 5));

        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
        	Platform.runLater(() -> {
        		updateLineNumbers();        		
        	});

        });
        
        HBox hbox = new HBox(lineNumbers, textArea);
        
        updateLineNumbers();
        lineNumbers.setPrefHeight(textArea.getHeight());
        
        return new ScrollPane(hbox);
	}
	
	public boolean endOfLine() {
		return cursorPosition == textArea.getText().length() || 
				cursorPosition < textArea.getText().length() && textArea.getText().charAt(cursorPosition) == '\n';
	}
	
	public boolean startOfLine() {
		return cursorPosition == 0 || 
				cursorPosition < textArea.getText().length() && textArea.getText().charAt(cursorPosition-1) == '\n';
	}
	


    public TextEditor(Stage primaryStage) throws RemoteException {

    	setServer();

        lines = this.currentFile.getLines();
    	
        // Création d'un TextArea
        this.textArea = new TextArea(String.join("\n", lines));
        this.textArea.setPrefSize(780, 600);
        
        textArea.setOnMouseClicked(event->{
        	cursorPosition = textArea.getCaretPosition();
            int lineNumber = getLineNumber(textArea.getText(), cursorPosition);
        });
         
        textArea.setOnKeyReleased(event->{
        	//TODO Faire en sorte de désactiver l'éditeur quand c'est pas la même ligne
        	cursorPosition = textArea.getCaretPosition();
            int lineNumber = getLineNumber(textArea.getText(), cursorPosition);

            
        });
        
        // Empêche de sauter ou d'effacer une ligne
        textArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if ( event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.BACK_SPACE && startOfLine()) {
                event.consume();
            }
        });
        
        
        ScrollPane scrollPane = setTextLine(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        
        this.status = new Label("Veuillez selectionner une ligne à modifier");
  		
  		
        // Bouton pour sélectionner une ligne
        Button selectButton = new Button("Selectionner une ligne");
        selectButton.setOnAction(event -> {
        	cursorPosition = textArea.getCaretPosition();

        	int selectedLine = getLineNumber(textArea.getText(), cursorPosition);

        	try {
        		currentAccess = 
						this.currentFile.accessLine(selectedLine-1, 
								this, 
								currentAccess,
								currentLine-1, 
								awaitingLine-1);
				if(currentAccess) {
					currentLine = selectedLine;
					awaitingLine = -1;
					setStatusText();	
				} else {
					currentLine = currentLine != -1 ? -1 : currentLine;
					awaitingLine = selectedLine;
					status.setText("La ligne " + selectedLine + " est actuellement en cours d'utilisation, attente de l'accès ou sélectionnez une autre ligne");
				}
				updateLineNumbers();

			} catch (RemoteException e) {
				e.printStackTrace();
			}
        	

        });
        
        // Bouton pour envoyer une ligne
        Button sendButton = new Button("Envoyer la modification de la ligne");
        sendButton.setOnAction(event -> {
        	try {
        		if(currentLine != -1) {
        			sendLine(textArea.getText(), currentLine);
        		} else {
        			status.setText("Veuillez selectionner une ligne avant d'envoyer");
        		}
        		
        		currentLine = -1;
        		updateLineNumbers();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        });
        
        // Bouton pour supprimer une ligne
        Button deleteLineButton = new Button("Suppression de la ligne");
        deleteLineButton.setOnAction(event -> {
    		try {
    			int cursorLine = getLineNumber(textArea.getText(), cursorPosition);
    			if(currentLine != cursorLine) {
    				status.setText("Veuillez selectionner une ligne à supprimer");
    				return;
    			}
    			if(textArea.getText().length() == 0 || lockedLines.contains(cursorLine)) return;
    			if(cursorLine == currentLine) {
    				currentLine = -1;
    				awaitingLine = -1;
    				setStatusText();
    			}
        		currentFile.deleteLine(cursorLine-1, this);
        		deleteTextLine(cursorLine);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
    		updateLineNumbers();
        	
        });
        
        // Bouton pour sauter une ligne
        Button lineFallButton = new Button("Saut de ligne");
        lineFallButton.setOnAction(event -> {
        	try {
        		int cursorLine = getLineNumber(textArea.getText(), cursorPosition);
        		if(currentLine != cursorLine) {
        			status.setText("Veuillez selectionner une ligne avant de sauter une ligne");
        			return;
        		}
        		addLineFall(cursorLine+1);
        		currentFile.addLine(cursorLine, this);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        });
        
  		
        
        Separator separator = new Separator();
        HBox hbox2 = new HBox(selectButton, sendButton, deleteLineButton, lineFallButton);
        
        VBox root = new VBox(scrollPane, hbox2, separator, status);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Scene scene = new Scene(root, 800, 800);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Editeur de texte");
        primaryStage.show();
        
        // fermeture de la fenetre
        primaryStage.setOnCloseRequest(event -> {
            Registry registry;
            boolean ok = false;
            
            try {
				this.currentFile.unregisterClient(this);
				this.server.closeFile(defaultFilename);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
            while(!ok) {
                try {
                    registry = LocateRegistry.getRegistry("localhost", 1099);
                    registry.unbind(this.num);
                    UnicastRemoteObject.unexportObject(this, true);
                    ok = true;
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Fermeture de l'application");
        });
    }
    
    public void addLineFall(int line) {
    	currentLine = currentLine >=line ? currentLine+1 :currentLine;
    	setStatusText();	
    	String text = textArea.getText();
    	String[] textToLine = text.split("\n",-1);
    	int index = line-1;
    	
    	String[] res = new String[textToLine.length + 1];        

        System.arraycopy(textToLine, 0, res, 0, index);

        res[index] = "";

        System.arraycopy(textToLine, index, res, index + 1, textToLine.length - index);
        
        textArea.setText(String.join("\n", res)); 
		Set<Integer> newSet = new HashSet<>();
		for (Integer i : lockedLines) {
			if(i >= line) {
				i++;
			}
			newSet.add(i);
		}
		lockedLines = newSet;
		updateLineNumbers();
    }
    
    private void setStatusText() {
    	String text = currentLine == -1 ? "Sélectionner une ligne" : "Ligne sélectionnée : " + currentLine;
    	status.setText(text);	
    }
    
    private void deleteTextLine(int line) {
    	currentLine = currentLine >=line ? currentLine-1 : currentLine;
    	setStatusText();	
    	String text = textArea.getText();
    	String[] textToLine = text.split("\n",-1);
    	int index = line-1;
    	
    	ArrayList<String> list = new ArrayList<String>(Arrays.asList(textToLine));

        list.remove(index);

        String[] res = new String[list.size()];
        res = list.toArray(res);
        
        textArea.setText(String.join("\n", res)); 
        

		Set<Integer> newSet = new HashSet<>();
		for (Integer i : lockedLines) {
			if(i >= line) {
				i--;
			}
			newSet.add(i);
		}
		lockedLines = newSet;

		updateLineNumbers();
    }

    private int getLineNumber(String text, int caretPosition) {
        String[] lines = text.substring(0, caretPosition).split("\n", -1);
        return lines.length;
    }
    
    
    private void sendLine(String text, int lineNumber) throws RemoteException {
    	int index = lineNumber-1;
    	String[] textToLine = text.split("\n",-1);
    	if(index >= 0 && index <textToLine.length) {
    		String content = textToLine[index];
    		this.currentFile.save(index, content, this);
    	}
    	
    }
    
    private void updateLineNumbers() {
        this.lineNumbers.getChildren().clear();
        String[] lines = textArea.getText().split("\n", -1);
        int totalLines = lines.length;
        int maxWidth = String.valueOf(totalLines).length();
        for (int i = 1; i <= totalLines; i++) {
            Label lineNumber = new Label(String.format("%" + maxWidth + "d", i));
            lineNumber.setPrefWidth(40);
            lineNumber.setAlignment(Pos.CENTER_RIGHT);
            if (lockedLines.contains(i) && i!= currentLine) {
                lineNumber.setTextFill(Color.RED);
            } 
            if(i == currentLine) {
            	lineNumber.setTextFill(Color.GREEN);
            	lineNumber.setStyle("-fx-font-weight: bold");
            }
            lineNumbers.getChildren().add(lineNumber);
        }
    }
    
	@Override
	public void lockLine(String fileName, int line) throws RemoteException {
		Platform.runLater(() -> {
	        lockedLines.add((line+1));
	        updateLineNumbers();
	    });
		
	}
	@Override
	public void unlockLine(int line) throws RemoteException {
		Platform.runLater(() -> {
	        lockedLines.remove(line+1);
	        currentLine = currentLine == (line+1) ? -1 : currentLine;
	        updateLineNumbers();
	    });
	}
	

	@Override
	public void accessLine(int line) throws RemoteException {
		Platform.runLater(() -> {

			currentLine = line+1;
			this.currentAccess = true;
			awaitingLine = -1;
			
			setStatusText();
			lockedLines.remove(line+1);
			updateLineNumbers();
	    });
	}

	@Override
	public void cantAccessLine(int line) throws RemoteException {
		Platform.runLater(() -> {
	        lockedLines.add(line+1);
	        updateLineNumbers();
	    });

	}

	@Override
	public void updateLine(int line, String content) throws RemoteException {
		Platform.runLater(() -> {
			this.textArea.getText();
			String[] textToLine = this.textArea.getText().split("\n", -1);
			textToLine[line] = content;
			this.textArea.setText(String.join("\n", textToLine));
			
			updateLineNumbers();
	    });
	}

	@Override
	public void addLine(int line) throws RemoteException {
		Platform.runLater(() -> {
			// Mettre à jour tous les variables en rapport aux lignes 
			// car ça décale
			addLineFall(line+1);
	    });
	}

	@Override
	public void deleteLine(int line) throws RemoteException {
		lockedLines.remove(line+1);
		Platform.runLater(() -> {
			deleteTextLine(line+1);
	    });
	}
}