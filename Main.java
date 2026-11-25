import javazoom.jl.player.Player;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    public static void main(String[] args) {
        String configFile = "config.txt";
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(configFile));
            if (lines.size() < 2) {
                System.err.println("Ошибка: в файле конфигурации должно быть как минимум две строки.");
                return;
            }
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла конфигурации: " + e.getMessage());
            return;
        }

        String[] imgParts = lines.get(0).trim().split("\\s+");
        String[] mp3Parts = lines.get(1).trim().split("\\s+");

        if (imgParts.length < 2 || mp3Parts.length < 2) {
            System.err.println("Ошибка формата: ожидается '<ссылка> <путь>'");
            return;
        }

        String imgUrl = imgParts[0];
        String imgPath = imgParts[1];
        String mp3Url = mp3Parts[0];
        String mp3Path = mp3Parts[1];

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> imgFuture = executor.submit(() -> downloadFile(imgUrl, imgPath, "Изображение"));

            Future<?> mp3Future = executor.submit(() -> downloadFile(mp3Url, mp3Path, "MP3"));

            imgFuture.get();
            mp3Future.get();

            System.out.println("Все файлы загружены, начинается воспроизведение...");
            playMp3(mp3Path);

        } catch (Exception e) {
            System.err.println("Произошла ошибка во время выполнения: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    
    private static void downloadFile(String urlStr, String savePath, String fileType) {
        try {
            URL url = new URL(urlStr);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(savePath)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("[" + fileType + "] Загрузка завершена: " + savePath);
        } catch (Exception e) {
            System.err.println("[" + fileType + "] Ошибка загрузки: " + e.getMessage());
        }
    }


    private static void playMp3(String filePath) {
        new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(filePath);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {

                Player player = new Player(bis);
                player.play();
                System.out.println("[MP3] Воспроизведение завершено.");

            } catch (Exception e) {
                System.err.println("[MP3] Ошибка воспроизведения: " + e.getMessage());
            }
        }).start();
    }
}