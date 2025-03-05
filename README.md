# Nécessité
- JavaFX 17 ou 19
- un JDK 17
- Eclipse

# Installation JavaFX
[Site pour l'installation de JavaFX](https://gluonhq.com/products/javafx/)
- Choisir la bonne architecture de votre machine et bon OS
- Choisir la bonne version et le type SDK
- Dézipper l'archive dans le dossier javafx-sdk-<version> de votre choix

## JavaFX sur Eclipse
- Pour ajouter JavaFX dans Eclipse, bien  l'avoir installer
- sur Eclipse, aller dans Windows -> Preference -> 
- Java ->  BuildPath -> User Libraries ->
- New -> Le nommer comme on veut, par exemple "JavaFX" 
- Add Externals JAR -> Ajouter tous les jar qui sont dans chemin\javafx-sdk-17.0.11\lib
- Ensuite, dans notre projet
- Clic droit -> Build Path -> Configure Build Path
- Onglet Libraries
- Dans Modulepath, Add Libraries -> User Libraries -> "JavaFX" ou l'autre nom que vous avez donné

# Importation du projet sur Eclipse
- Dézipper le dossier dans votre workspace eclipse
- Aller dans l'onglet File -> Import -> General Existing Projects into Workspace -> 
- Sélectionner le dossier que vous venez de dézipper.

# Pour lancer l'application:
- Spécifier le dossier source du projet avant de lancer
	- Pour ce faire clic droit sur le projet, Run As -> Run Configurations.. ->
		- Arguments -> en bas dans Working directory, sélectionner Other -> 
		- Workspace... -> sélectionner le dossier source src -> Apply -> Close
- Lancez en premier le serveur avec ServerMain
- Lancez des clients avec ClientMain 

# Pour lancer l'application dans le terminal
- Se placer dans le dossier `src`
- Lancer d'abord le serveur avec : `java -cp ../bin psar.server.ServerMain `
- Lancer les clients avec : `java --module-path $MODULEPATH:bin --add-modules javafx.base,javafx.controls,javafx.graphics -cp ../bin psar.client.ClientMain`
$MODULEPATH est le chemin vers la librairie de JavaFX qui est soit à déterminer soit à remplacer.