# JADBExample

Ce programme Java montre l'utilisation de JADB (Java ADB) pour interagir avec un appareil Android connecté via ADB (Android Debug Bridge).

## Prérequis
Java 18 ou Java Temurin-18.0.2.1+1 (https://adoptium.net/)  
ADB (Android Debug Bridge) installé et accessible dans le PATH  
L'utilisation de IntelliJ IDEA est recommandée

## Utilisation
1. Cloner le projet
2. Ouvrir le projet dans IntelliJ IDEA
3. Connecter un appareil Android via USB ou utilser un émulateur
4. Activer le mode développeur sur l'appareil (https://developer.android.com/studio/debug/dev-options) sauf sur les émulateurs
5. Dé commenter les appels de méthodes que vous souhaitez tester dans la classe `JADBExample`
6. Exécuter la classe `JADBExample` (Shift+F10)
7. Observer les résultats dans la console

## Attention
La méthode addContact est en cours de développement et ne fonctionne pas encore.  
Ne pas mettre en commentaire les méthode startADBServer et closeADBServer.  
Si vous avez beaucoup de SMS, la méthode extractSMS peut prendre du temps à s'exécuter.  
Si vous avez beaucoup de contacts, la méthode extractContacts peut prendre du temps à s'exécuter.  