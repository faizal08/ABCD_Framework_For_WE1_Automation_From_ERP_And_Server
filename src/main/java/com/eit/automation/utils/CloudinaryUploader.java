package com.eit.automation.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.eit.automation.core.Main;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CloudinaryUploader {

    private static Cloudinary cloudinary;

    // Thread pool dedicated strictly to background screenshot uploads
    private static final ExecutorService uploadExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true); // Ensures threads don't block JVM termination
        return thread;
    });

    // Background scheduler for automatic cleanup task
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    // Static initializer block: Loads credentials dynamically and starts auto-cleanup
    static {
        initializeCloudinary();
        startAutoCleanupScheduler(2); // Runs every 2 days
    }

    /**
     * Initializes Cloudinary by reading credentials from Main.config.
     * Falls back to root config file or environment variables if loaded independently.
     */
    private static synchronized void initializeCloudinary() {
        Properties props = Main.config;

        // Fallback: If Main.config is null or empty, load directly from root folder
        if (props == null || props.isEmpty()) {
            props = new Properties();
            String env = System.getProperty("env");
            String configFileName = (env != null && !env.trim().isEmpty()) ? env.trim() + ".properties" : "config.properties";
            File configFile = new File(configFileName);

            if (!configFile.exists()) {
                configFile = new File("config.properties");
            }

            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                } catch (Exception e) {
                    System.err.println("[CLOUDINARY] ❌ Error loading fallback properties file: " + e.getMessage());
                }
            }
        }

        // Fetch properties with System environment fallback
        String cloudName = props.getProperty("cloudinary.cloud.name", System.getenv("CLOUDINARY_CLOUD_NAME"));
        String apiKey = props.getProperty("cloudinary.api.key", System.getenv("CLOUDINARY_API_KEY"));
        String apiSecret = props.getProperty("cloudinary.api.secret", System.getenv("CLOUDINARY_API_SECRET"));

        if (cloudName == null || apiKey == null || apiSecret == null) {
            System.err.println("[CLOUDINARY] ⚠️ Warning: Missing Cloudinary credentials in configuration!");
        } else {
            System.out.println("[CLOUDINARY] 🔧 Initialized Cloudinary instance for cloud: " + cloudName);
        }

        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    /**
     * BATCH PARALLEL UPLOAD (Post-Suite Upload Pattern)
     * Uploads all accumulated local files in parallel after test suite completion.
     * Uses an internal cache to safely handle file path aliases (.png vs .jpg) without file deletion errors.
     *
     * @param localFilesMap Map of Key (e.g., Step ID / Media Key) -> Local File Path
     * @param poolSize      Number of parallel upload threads (e.g., 10)
     * @return Thread-safe Map of Key -> Cloudinary Secure URL
     */
    public static Map<String, String> uploadBatchInParallel(Map<String, String> localFilesMap, int poolSize) {
        Map<String, String> uploadedUrls = new ConcurrentHashMap<>();
        Map<String, String> pathUrlCache = new ConcurrentHashMap<>(); // Caches Local File Path -> Cloudinary URL

        if (localFilesMap == null || localFilesMap.isEmpty()) {
            System.out.println("[CLOUDINARY] No artifacts to upload in batch.");
            return uploadedUrls;
        }

        System.out.println("[CLOUDINARY] 🚀 Starting batch upload of " + localFilesMap.size()
                + " artifacts using " + poolSize + " parallel threads...");

        ExecutorService batchExecutor = Executors.newFixedThreadPool(poolSize);

        for (Map.Entry<String, String> entry : localFilesMap.entrySet()) {
            String key = entry.getKey();
            String localFilePath = entry.getValue();

            batchExecutor.submit(() -> {
                if (localFilePath == null || localFilePath.trim().isEmpty()) {
                    return;
                }

                // 1. CHECK CACHE FIRST: If this file path was already uploaded under a different key alias
                if (pathUrlCache.containsKey(localFilePath)) {
                    String cachedUrl = pathUrlCache.get(localFilePath);
                    uploadedUrls.put(key, cachedUrl);
                    System.out.println("[CLOUDINARY] Linked alias key [" + key + "] to cached URL -> " + cachedUrl);
                    return;
                }

                File file = new File(localFilePath);

                // 2. RETRY WAIT: Wait up to 5 seconds if file/video is still being written to disk
                int retryCount = 0;
                while (!file.exists() && retryCount < 10) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {}
                    retryCount++;
                    file = new File(localFilePath); // re-check
                }

                // Double check cache after wait loop in case a concurrent thread uploaded it
                if (pathUrlCache.containsKey(localFilePath)) {
                    String cachedUrl = pathUrlCache.get(localFilePath);
                    uploadedUrls.put(key, cachedUrl);
                    return;
                }

                if (!file.exists() || file.length() == 0) {
                    System.err.println("[CLOUDINARY] File not found or empty (0 bytes) for key '" + key + "': " + localFilePath);
                    return;
                }

                try {
                    synchronized (localFilePath.intern()) { // Ensure only 1 upload attempt per unique file path
                        if (pathUrlCache.containsKey(localFilePath)) {
                            uploadedUrls.put(key, pathUrlCache.get(localFilePath));
                            return;
                        }

                        String lowerPath = localFilePath.toLowerCase().trim();
                        boolean isVideo = lowerPath.endsWith(".mp4") || lowerPath.endsWith(".webm") || lowerPath.endsWith(".avi") || lowerPath.endsWith(".mov");

                        Map uploadResult;
                        if (isVideo) {
                            uploadResult = cloudinary.uploader().uploadLarge(file, ObjectUtils.asMap(
                                    "folder", "automation-reports",
                                    "resource_type", "video",
                                    "chunk_size", 6000000 // 6MB chunk size
                            ));
                        } else {
                            uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                                    "folder", "automation-reports",
                                    "resource_type", "image"
                            ));
                        }

                        String secureUrl = (String) uploadResult.get("secure_url");
                        if (secureUrl != null) {
                            pathUrlCache.put(localFilePath, secureUrl);
                            uploadedUrls.put(key, secureUrl);
                            System.out.println("[CLOUDINARY] Uploaded [" + key + "] -> " + secureUrl);

                            // Delete local copy after successful cloud upload
                            if (file.delete()) {
                                System.out.println("[CLOUDINARY] Deleted local temp file: " + localFilePath);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[CLOUDINARY] Failed batch upload for [" + key + "]: " + e.getMessage());
                }
            });
        }

        // Graceful shutdown and wait for all uploads to complete
        batchExecutor.shutdown();
        try {
            boolean finished = batchExecutor.awaitTermination(15, TimeUnit.MINUTES);
            if (finished) {
                System.out.println("[CLOUDINARY] ✅ Batch upload complete! " + uploadedUrls.size() + "/" + localFilesMap.size() + " artifact keys mapped.");
            } else {
                System.err.println("[CLOUDINARY] ⚠️ Batch upload timed out before completing all files.");
            }
        } catch (InterruptedException e) {
            System.err.println("[CLOUDINARY] Batch upload thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        return uploadedUrls;
    }

    /**
     * Synchronous Upload
     */
    public static String uploadArtifact(String localFilePath) {
        try {
            File file = new File(localFilePath);
            if (!file.exists() || file.length() == 0) {
                System.err.println("[CLOUDINARY] Local file not found or empty: " + localFilePath);
                return null;
            }

            String lowerPath = localFilePath.toLowerCase().trim();
            boolean isVideo = lowerPath.endsWith(".mp4") || lowerPath.endsWith(".webm") || lowerPath.endsWith(".avi") || lowerPath.endsWith(".mov");

            Map uploadResult;
            if (isVideo) {
                uploadResult = cloudinary.uploader().uploadLarge(file, ObjectUtils.asMap(
                        "folder", "automation-reports",
                        "resource_type", "video",
                        "chunk_size", 6000000
                ));
            } else {
                uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                        "folder", "automation-reports",
                        "resource_type", "image"
                ));
            }

            String secureUrl = (String) uploadResult.get("secure_url");
            System.out.println("[CLOUDINARY] Upload successful: " + secureUrl);

            if (file.delete()) {
                System.out.println("[CLOUDINARY] Deleted local file: " + localFilePath);
            }

            return secureUrl;

        } catch (Exception e) {
            System.err.println("[CLOUDINARY] Upload failed: " + e.getMessage());
            return null;
        }
    }

    public static void uploadArtifactAsync(String localFilePath, Consumer<String> callback) {
        CompletableFuture.runAsync(() -> {
            String url = uploadArtifact(localFilePath);
            if (callback != null) {
                callback.accept(url);
            }
        }, uploadExecutor);
    }

    public static void uploadArtifactAsync(String localFilePath) {
        uploadArtifactAsync(localFilePath, null);
    }

    public static void deleteOldArtifacts(int daysOld) {
        try {
            System.out.println("[CLOUDINARY] Checking for artifacts older than " + daysOld + " days...");

            String[] resourceTypes = {"image", "video"};

            for (String resType : resourceTypes) {
                // Build expression explicitly including resource_type
                String expression = String.format("folder:automation-reports AND resource_type:%s AND created_at < %dd", resType, daysOld);

                Map searchResult = cloudinary.search()
                        .expression(expression)
                        .maxResults(500)
                        .execute();

                List<Map> resources = (List<Map>) searchResult.get("resources");

                if (resources != null && !resources.isEmpty()) {
                    List<String> publicIds = new ArrayList<>();
                    for (Map res : resources) {
                        publicIds.add((String) res.get("public_id"));
                    }

                    // Delete resources in batches of max 100 to stay within Cloudinary API limits
                    deleteOldArtifactsInBatches(publicIds, resType);
                } else {
                    System.out.println("[CLOUDINARY] No old " + resType + " artifacts found older than " + daysOld + " days.");
                }
            }

        } catch (Exception e) {
            System.err.println("[CLOUDINARY] Auto-delete error: " + e.getMessage());
        }
    }

    /**
     * Batch deletion helper method to prevent "Too many public_ids in request" errors
     */
    private static void deleteOldArtifactsInBatches(List<String> publicIdsToDelete, String resourceType) {
        if (publicIdsToDelete == null || publicIdsToDelete.isEmpty()) {
            return;
        }

        int batchSize = 100; // Cloudinary's strict maximum limit per request
        int totalItems = publicIdsToDelete.size();

        System.out.println("[CLOUDINARY] Deleting " + totalItems + " old " + resourceType + " public IDs in batches of " + batchSize + "...");

        for (int i = 0; i < totalItems; i += batchSize) {
            List<String> batch = publicIdsToDelete.subList(i, Math.min(i + batchSize, totalItems));

            try {
                Map response = cloudinary.api().deleteResources(batch, ObjectUtils.asMap("resource_type", resourceType));
                System.out.println("[CLOUDINARY] Successfully deleted " + resourceType + " batch (" + (i + 1) + " to " + Math.min(i + batchSize, totalItems) + ")");
            } catch (Exception e) {
                System.err.println("[CLOUDINARY] Error deleting " + resourceType + " batch starting at index " + i + ": " + e.getMessage());
            }
        }
    }

    private static void startAutoCleanupScheduler(int intervalInDays) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                deleteOldArtifacts(intervalInDays);
            } catch (Exception e) {
                System.err.println("[CLOUDINARY] Scheduled cleanup error: " + e.getMessage());
            }
        }, 0, intervalInDays, TimeUnit.DAYS);
    }

    public static void shutdownExecutors() {
        try {
            uploadExecutor.shutdown();
            uploadExecutor.awaitTermination(10, TimeUnit.SECONDS);
            scheduler.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}