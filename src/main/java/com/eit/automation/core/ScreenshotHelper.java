package com.eit.automation.core;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScreenshotHelper {

    // Thread-safe map storing local file paths for Post-Suite Batch Upload
    // Key (e.g. Relative Filename "TC_RG_01.mp4" or "step_1.png") -> Local Absolute Path
    private static final Map<String, String> localArtifactsMap = new ConcurrentHashMap<>();

    /**
     * Captures a local screenshot, compresses it as a high-quality JPEG with aspect-ratio preservation,
     * and registers it for post-suite batch upload.
     * ZERO external network overhead during test step execution.
     *
     * @param driver   Active WebDriver instance
     * @param filename Local file path/name destination or identifier
     * @return Local absolute path immediately
     */
    public static String capture(WebDriver driver, String filename) {
        try {
            String originalFilename = filename;

            // Enforce .jpg extension for the actual file saved to disk
            if (filename.toLowerCase().endsWith(".png")) {
                filename = filename.substring(0, filename.length() - 4) + ".jpg";
            } else if (!filename.toLowerCase().endsWith(".jpg") && !filename.toLowerCase().endsWith(".jpeg")) {
                filename = filename + ".jpg";
            }

            File targetFile = new File(filename);
            if (targetFile.getParentFile() != null && !targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }

            File rawScreenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            compressAndSaveAsJpeg(rawScreenshot, targetFile);
            Files.deleteIfExists(rawScreenshot.toPath());

            String localPath = targetFile.getAbsolutePath();
            String cleanJpgKey = targetFile.getName(); // Saved file key (e.g., "STEP_1.jpg")

            // 1. Register clean .jpg key
            localArtifactsMap.put(cleanJpgKey, localPath);

            // 2. ALSO register .png key alias if caller/HTML generator requested .png
            File origFile = new File(originalFilename);
            String origKey = origFile.getName();
            if (origKey.toLowerCase().endsWith(".png")) {
                localArtifactsMap.put(origKey, localPath);
            } else {
                localArtifactsMap.put(cleanJpgKey.replace(".jpg", ".png"), localPath);
            }

            System.out.println("📸 Aspect-ratio preserved JPEG saved (" + (targetFile.length() / 1024) + " KB): " + localPath);

            return localPath;

        } catch (Exception e) {
            System.err.println("⚠️ Failed to capture screenshot: " + e.getMessage());
            return null;
        }
    }

    /**
     * Resizes high-res screenshots while strictly preserving aspect ratio (portrait vs landscape)
     * and applies high-quality rendering hints to avoid blurry text or dirty compression artifacts.
     */
    private static void compressAndSaveAsJpeg(File sourcePng, File targetJpeg) throws IOException {
        BufferedImage originalImage = ImageIO.read(sourcePng);
        if (originalImage == null) {
            throw new IOException("Failed to read captured raw screenshot file.");
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Max dimension cap (1080p long edge keeps UI elements and text razor-sharp)
        int maxDimension = 1080;
        int targetWidth = originalWidth;
        int targetHeight = originalHeight;

        // Calculate aspect-ratio dynamically based on orientation
        if (originalWidth > maxDimension || originalHeight > maxDimension) {
            if (originalWidth > originalHeight) {
                targetWidth = maxDimension;
                targetHeight = (int) ((double) originalHeight / originalWidth * maxDimension);
            } else {
                targetHeight = maxDimension;
                targetWidth = (int) ((double) originalWidth / originalHeight * maxDimension);
            }
        }

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // High Quality Rendering Hints (Fixes blurry/dirty text)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        // Configure JPEG encoder with 80% quality for crisp text rendering
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG ImageWriter found.");
        }
        ImageWriter writer = writers.next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.80f); // 80% quality delivers small file sizes with clean UI text
        }

        try (FileOutputStream fos = new FileOutputStream(targetJpeg);
             ImageOutputStream ios = ImageIO.createImageOutputStream(fos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(resizedImage, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    /**
     * Manually registers any local media file (e.g. Video recordings or custom logs)
     * to the batch upload queue.
     *
     * @param key       Clean identifier/filename (e.g., "TC_RG_08_Panic_Call.mp4")
     * @param localPath Local absolute file path
     */
    public static void registerArtifact(String key, String localPath) {
        if (key != null && localPath != null) {
            // If key is a full path, extract just the filename for clean matching
            File file = new File(key);
            String cleanKey = file.getName();
            localArtifactsMap.put(cleanKey, localPath);
        }
    }

    /**
     * Returns all accumulated local artifacts for Post-Suite Batch Upload.
     *
     * @return Thread-safe Map of Key -> Local File Path
     */
    public static Map<String, String> getLocalArtifactsMap() {
        return localArtifactsMap;
    }

    /**
     * Clears the local artifact registry (use after batch upload completes).
     */
    public static void clearRegistry() {
        localArtifactsMap.clear();
    }
}