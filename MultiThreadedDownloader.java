import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Scanner;
import java.nio.file.Paths;
import java.io.InputStream;

public class MultiThreadedDownloader {

    private static final int NUM_THREADS = 4; // Number of threads

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter the download URL: ");
            String fileURL = scanner.nextLine();
            String fileName = Paths.get(new URL(fileURL).getPath()).getFileName().toString();
            System.out.print("Enter the custom filename ("+fileName+"): ");
            String savePath = scanner.nextLine();

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
            for (int i = 0; i < NUM_THREADS; i++) {
                int startByte = i * segmentSize;
                int endByte = (i == NUM_THREADS - 1) ? (startByte + segmentSize + remainder - 1) : startByte + segmentSize - 1;

                executor.execute(new FileSegmentDownloader(fileURL, savePath, startByte, endByte, i));
            }

            // Step 4: Shutdown the executor and wait for threads to complete
            executor.shutdown();
            while (!executor.isTerminated()) {
                Thread.sleep(100); // Wait for all threads to finish
            }

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

        public FileSegmentDownloader(String fileURL, String savePath, int startByte, int endByte, int threadId) {
            this.fileURL = fileURL;
            this.savePath = savePath;
            this.startByte = startByte;
            this.endByte = endByte;
            this.threadId = threadId;
        }

        @Override
        public void run() {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(fileURL).openConnection();
                connection.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);

                try (RandomAccessFile raf = new RandomAccessFile(savePath, "rw")) {
                    raf.seek(startByte);

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    int totalBytesRead = 0;
                    int segmentSize = endByte - startByte + 1;

                    try (InputStream inputStream = connection.getInputStream()) {
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            raf.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            printProgress(totalBytesRead, segmentSize);
                        }
                    }

                    // System.out.println("Thread " + threadId + " completed downloading bytes " + startByte + " to " + endByte);
                }
            } catch (Exception e) {
                System.err.println("Thread " + threadId + " encountered an error: " + e.getMessage());
            }
        }

        private void printProgress(int bytesRead, int totalBytes) {
            int progress = (int) ((bytesRead / (double) totalBytes) * 100);
            double bytesReadMB = bytesRead / (1024.0 * 1024.0);
            double totalBytesMB = totalBytes / (1024.0 * 1024.0);
            StringBuilder progressBar = new StringBuilder("[");
            for (int i = 0; i < 50; i++) {
                if (i < (progress / 2)) {
                    progressBar.append("=");
                } else {
                    progressBar.append(" ");
                }
            }
            progressBar.append("] ").append(String.format("%.2f", bytesReadMB)).append(" MB / ")
                        .append(String.format("%.2f", totalBytesMB)).append(" MB (")
                        .append(progress).append("%)");
            System.out.print("\rThread " + threadId + " " + progressBar.toString());
        }
    }
}
