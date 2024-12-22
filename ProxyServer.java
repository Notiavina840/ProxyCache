import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class ProxyServer {

    public static void main(String[] args) {
        
        // Validation du répertoire Web Root
        System.out.println("Répertoire Web Root : " + new File(Config.WEB_ROOT).getAbsolutePath());
        createDirectoriesIfNotExist(Config.WEB_ROOT);
        createDirectoriesIfNotExist(Config.CACHE_DIR);

        CacheManager cacheManager = new CacheManager(Config.CACHE_DIR);

        try (ServerSocket serverSocket = new ServerSocket(Config.PORT)) {
            System.out.println("Serveur proxy démarré sur le port " + Config.PORT);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> {
                        try {
                            handleClientRequest(clientSocket, cacheManager);
                        } catch (IOException e) {
                            System.err.println("Erreur de traitement de la requête client : " + e.getMessage());
                        } finally {
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                                System.err.println("Erreur de fermeture de la socket : " + e.getMessage());
                            }
                        }
                    }).start();
                } catch (IOException e) {
                    System.err.println("Erreur de connexion client : " + e.getMessage());
                }
                cacheManager.clearExpiredCache(); // Nettoyage des fichiers expirés
            }
        } catch (IOException e) {
            System.err.println("Erreur de démarrage du serveur proxy : " + e.getMessage());
        }
    }

    private static void createDirectoriesIfNotExist(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Répertoire créé : " + dir.getAbsolutePath());
        }
    }

    private static void handleClientRequest(Socket clientSocket, CacheManager cacheManager) throws IOException {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream()
        ) {
            System.out.println("Nouvelle connexion client reçue");

            String requestLine = in.readLine();
            System.out.println("Ligne de requête reçue : " + requestLine);

            if (requestLine == null || !requestLine.startsWith("GET")) {
                System.out.println("Requête invalide : " + requestLine);
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            String fileName = extractFileName(requestLine);
            if (fileName.isEmpty() || fileName.equals("/")) {
                fileName = "";
            }

            System.out.println("Nom de fichier extrait : " + fileName);
            File file = new File(Config.WEB_ROOT + File.separator + fileName);
            if (file.isDirectory()) {
                sendDirectoryListing(file, out);
            } else if (file.exists()) {
                serveFileFromDisk(file, clientSocket, cacheManager);
            } else {
                System.out.println("Fichier non trouvé : " + fileName);
                sendErrorResponse(out, 404, "File Not Found");
            }
        }
    }

    private static String extractFileName(String requestLine) {
        String[] parts = requestLine.split(" ");
        if (parts.length > 1) {
            return parts[1].substring(1);
        }
        return "";
    }

    private static void serveFileFromDisk(File file, Socket clientSocket, CacheManager cacheManager) throws IOException {
        String fileName = file.getName();
        try (OutputStream out = clientSocket.getOutputStream()) {
            if (cacheManager.isCached(fileName)) {
                byte[] cachedContent = cacheManager.getBinaryFromCache(fileName);
                
                if (cachedContent != null) {
                    System.out.println("Le fichier " + fileName + " est servi depuis le cache.");
                    sendBinaryResponse(out, cachedContent, getMimeType(file));
                    return;
                }
            }
    
            System.out.println("Fichier " + fileName + " non trouvé dans le cache. Chargement depuis le disque.");
            byte[] content = Files.readAllBytes(file.toPath());
            cacheManager.saveBinaryToCache(fileName, content); // Sauvegarder le fichier binaire dans le cache
            sendBinaryResponse(out, content, getMimeType(file));
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
            try (OutputStream out = clientSocket.getOutputStream()) {
                sendErrorResponse(out, 500, "Internal Server Error");
            }
        }
    }
    

    private static void sendDirectoryListing(File directory, OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(out, true);
        File[] files = directory.listFiles();
        
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: text/html; charset=UTF-8");
        writer.println();
        writer.println("<html><body>");
        writer.println("<h1>Index of " + directory.getName() + "</h1>");
        writer.println("<ul>");
    
        if (files != null) {
            for (File file : files) {
                String link = file.isDirectory() ? file.getName() + "/" : file.getName();
                writer.println("<li><a href=\"" + link + "\">" + file.getName() + "</a></li>");
            }
        } else {
            writer.println("<li>Le répertoire est vide ou inaccessible.</li>");
        }
    
        writer.println("</ul>");
        writer.println("</body></html>");
    }

    private static void sendBinaryResponse(OutputStream out, byte[] content, String mimeType) throws IOException {
        PrintWriter headerWriter = new PrintWriter(out, true);
        headerWriter.println("HTTP/1.1 200 OK");
        headerWriter.println("Content-Type: " + mimeType);
        headerWriter.println("Content-Length: " + content.length);
        headerWriter.println();
        headerWriter.flush();

        out.write(content);
        out.flush();
    }

    private static void sendErrorResponse(OutputStream out, int statusCode, String message) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + message + "\r\n";
        response += "Content-Type: text/html; charset=UTF-8\r\n";
        response += "\r\n";
        response += "<html><body><h1>" + statusCode + " - " + message + "</h1></body></html>";

        out.write(response.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static String getMimeType(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".php")) return "text/html";
        return "application/octet-stream";
    }    
}
