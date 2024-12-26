import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Paths;
import java.io.InputStream;
import java.util.Scanner;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class MultiThreadedDownloader {

    private static final int NUM_THREADS = 4; // Number of threads

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter the download URL:");
        String fileURL = scanner.nextLine();

        System.out.println("Enter the custom filename:");
        String savePath = scanner.nextLine();

        new MultiThreadedDownloader().downloadFile(fileURL, savePath);
    }

    private void downloadFile(String fileURL, String savePath) {
        try {
            String fileName = Paths.get(new URL(fileURL).getPath()).getFileName().toString();

            // Extract file name from URL if savePath is not provided
            if (savePath.isEmpty()) {
                savePath = fileName;
            } else {
                // Ensure the savePath has the same extension as the fileURL
                String fileExtension = fileURL.substring(fileURL.lastIndexOf('.'));
                if (!savePath.endsWith(fileExtension)) {
                    savePath += fileExtension;
                }
            }

            // Step 1: Get the file size
            HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
            int fileSize = connection.getContentLength();
            connection.disconnect();

            double fileSizeMB = fileSize / (1024.0 * 1024.0);
            System.out.println("File size: " + String.format("%.2f", fileSizeMB) + " MB");

            // Step 2: Divide the file into segments
            int segmentSize = fileSize / NUM_THREADS;
            int remainder = fileSize % NUM_THREADS;

            // Step 3: Create a thread pool and download segments
            ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
            ProgressBarBuilder pbb = new ProgressBarBuilder()
                    .setTaskName("Downloading")
                    .setStyle(ProgressBarStyle.ASCII)
                    .setInitialMax(fileSize);
            ProgressBar pb = pbb.build();

            for (int i = 0; i < NUM_THREADS; i++) {
                int startByte = i * segmentSize;
                int endByte = (i == NUM_THREADS - 1) ? (startByte + segmentSize + remainder - 1) : startByte + segmentSize - 1;

                executor.execute(new FileSegmentDownloader(fileURL, savePath, startByte, endByte, i, pb));
            }

            // Step 4: Shutdown the executor and wait for threads to complete
            executor.shutdown();
            while (!executor.isTerminated()) {
                Thread.sleep(100); // Wait for all threads to finish
            }

            pb.close();
            System.out.println("Download complete!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class FileSegmentDownloader implements Runnable {
        private final String fileURL;
        private final String savePath;
        private final int startByte;
        private final int endByte;
        private final int threadId;
        private final ProgressBar progressBar;

        public FileSegmentDownloader(String fileURL, String savePath, int startByte, int endByte, int threadId, ProgressBar progressBar) {
            this.fileURL = fileURL;
            this.savePath = savePath;
            this.startByte = startByte;
            this.endByte = endByte;
            this.threadId = threadId;
            this.progressBar = progressBar;
        }

        @Override
        public void run() {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
                connection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);

                try (RandomAccessFile raf = new RandomAccessFile(savePath, "rw")) {
                    raf.seek(startByte);

                    byte[] buffer = new byte[8192]; // Increased buffer size
                    int bytesRead;
                    int totalBytesRead = 0;
                    int segmentSize = endByte - startByte + 1;

                    try (InputStream inputStream = connection.getInputStream()) {
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            raf.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            progressBar.stepBy(bytesRead);
                        }
                    }

                    // System.out.println("Thread " + threadId + " completed downloading bytes " + startByte + " to " + endByte);
                }
            } catch (Exception e) {
                System.err.println("Thread " + threadId + " encountered an error: " + e.getMessage());
            }
        }
    }
}
