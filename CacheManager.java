import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class CacheManager {
    private String cacheDir;

    public CacheManager(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public boolean isCached(String fileName) {
        File cacheFile = new File(cacheDir + File.separator + fileName);
        if (!cacheFile.exists()) {
            return false;
        }
        long lastModified = cacheFile.lastModified();
        long currentTime = System.currentTimeMillis();
        boolean isExpired = currentTime - lastModified > Config.CACHE_EXPIRATION_TIME_MS;
        if (isExpired) {
            System.out.println("Le cache du fichier " + fileName + " a expiré.");
        }
        return !isExpired;
    }

    public String getFromCache(String fileName) {
        try {
            Path cachePath = Paths.get(cacheDir, fileName);
            return new String(Files.readAllBytes(cachePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveToCache(String fileName, String content) {
        try {
            Path cachePath = Paths.get(cacheDir, fileName);
            Files.createDirectories(cachePath.getParent());
            Files.write(cachePath, content.getBytes(StandardCharsets.UTF_8));
            System.out.println("Fichier " + fileName + " mis en cache.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] getBinaryFromCache(String fileName) {
        try {
            Path cachePath = Paths.get(cacheDir, fileName);
            return Files.readAllBytes(cachePath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveBinaryToCache(String fileName, byte[] content) {
        try {
            Path cachePath = Paths.get(cacheDir, fileName);
            Files.createDirectories(cachePath.getParent());
            Files.write(cachePath, content);
            System.out.println("Fichier binaire " + fileName + " mis en cache.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearExpiredCache() {
        File cacheFolder = new File(cacheDir);
        File[] files = cacheFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (System.currentTimeMillis() - file.lastModified() > Config.CACHE_EXPIRATION_TIME_MS) {
                    if (file.delete()) {
                        System.out.println("Fichier expiré supprimé : " + file.getName());
                    }
                }
            }
        }
    }

    public void removeFromCache(String fileName) {
        File cacheFile = new File(cacheDir + File.separator + fileName);
        if (cacheFile.exists() && cacheFile.delete()) {
            System.out.println("Cache du fichier " + fileName + " supprimé.");
        }
    }

    public void listCacheFiles() {
        File cacheFolder = new File(cacheDir);
        File[] files = cacheFolder.listFiles(File::isFile); // Filtre uniquement les fichiers
        if (files != null && files.length > 0) {
            System.out.println("Fichiers présents dans le cache :");
            for (File file : files) {
                System.out.println("- " + file.getName() + " (Dernière modification : " + new java.util.Date(file.lastModified()) + ")");
            }
        } else {
            System.out.println("Le cache est vide.");
        }
    }
    

    public void removeMultipleFromCache(String[] fileNames) {
        for (String fileName : fileNames) {
            File cacheFile = new File(cacheDir + File.separator + fileName);
            if (cacheFile.exists() && cacheFile.delete()) {
                System.out.println("Cache du fichier " + fileName + " supprimé.");
            } else {
                System.out.println("Impossible de supprimer le fichier " + fileName + ". Peut-être qu'il n'existe pas dans le cache.");
            }
        }
    }
}
