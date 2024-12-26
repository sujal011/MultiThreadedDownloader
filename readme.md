# MultiThreaded Downloader

This project demonstrates a multi-threaded file downloader in Java. The downloader splits the file into segments and downloads them concurrently using multiple threads, significantly reducing the download time.

## How to Use

1. **Compile the Program:**
    ```sh
    javac MultiThreadedDownloader.java
    ```

2. **Run the Program:**
    ```sh
    java MultiThreadedDownloader
    ```

3. **Input the Download URL and Custom Filename:**
    - Enter the URL of the file you want to download.
    - Enter the custom filename (with extension) where you want to save the downloaded file.

## Performance Comparison

### Example File

- **File URL:** [Cricket Bowling 150fps 1200.avi](https://www.quintic.com/software/sample_videos/Cricket%20Bowling%20150fps%201200.avi)

### Download Time

- **Normal Browser:** 4 minutes and 4 seconds
- **MultiThreaded Downloader (4 threads):** 1 minute and 48 seconds

![Performance Comparison](https://github.com/user-attachments/assets/91ecc578-bb74-4a51-afe6-2d112fd3214f)

