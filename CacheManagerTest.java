import java.io.File;

public class CacheManagerTest {

    public static void main(String[] args) {
        String cacheDir = "./cache"; // Remplacez ce chemin si nécessaire
        CacheManager cacheManager = new CacheManager(cacheDir);

        // Liste les fichiers dans le cache
        System.out.println("Test : Lister les fichiers dans le cache");
        cacheManager.listCacheFiles();
        String fileToDelete = "style.css"; // Remplacez par le nom du fichier à supprimer
        System.out.println("\nÉtape 2 : Suppression d'un fichier");
        cacheManager.removeFromCache(fileToDelete);
        String[] filesToDelete = {"logo.png", "sortie.png"}; // Remplacez par les noms des fichiers à supprimer
        System.out.println("\nÉtape 3 : Suppression de plusieurs fichiers");
        cacheManager.removeMultipleFromCache(filesToDelete);
        cacheManager.listCacheFiles();

    }
}
