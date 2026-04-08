package com.bolas.ecommerce.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageUploadService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/quicktime", "video/webm", "video/x-msvideo"
    );
    private static final long MAX_SIZE = 5 * 1024 * 1024;       // 5 MB images
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100 MB vidéos

    // ─── Magic bytes (signatures réelles des fichiers) ────────────────────────
    // JPEG : FF D8 FF
    private static final byte[] MAGIC_JPEG = {(byte)0xFF, (byte)0xD8, (byte)0xFF};
    // PNG  : 89 50 4E 47 0D 0A 1A 0A
    private static final byte[] MAGIC_PNG  = {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    // WebP : RIFF????WEBP
    private static final byte[] MAGIC_WEBP_RIFF = {0x52, 0x49, 0x46, 0x46}; // "RIFF"
    private static final byte[] MAGIC_WEBP_MARK = {0x57, 0x45, 0x42, 0x50}; // "WEBP" at offset 8

    private final Path uploadDir;
    private final Cloudinary cloudinary;
    private final boolean cloudinaryEnabled;

    public ImageUploadService(
            @Value("${bolas.upload.dir:uploads}") String uploadDirPath,
            @Value("${cloudinary.url:}") String cloudinaryUrl) throws IOException {

        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);

        // Cloudinary activé seulement si l'URL est définie
        if (cloudinaryUrl != null && !cloudinaryUrl.isBlank()) {
            this.cloudinary = new Cloudinary(cloudinaryUrl);
            this.cloudinaryEnabled = true;
        } else {
            this.cloudinary = null;
            this.cloudinaryEnabled = false;
        }
    }

    /**
     * Stocke l'image :
     * - En prod (CLOUDINARY_URL définie) : upload sur Cloudinary → retourne l'URL CDN
     * - En dev : sauvegarde locale → retourne /uploads/xxx.jpg
     */
    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Utilisez JPG, PNG ou WebP.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Fichier trop volumineux (max 5 Mo).");
        }
        // Vérification des magic bytes — empêche les fichiers déguisés
        verifyImageMagicBytes(file, contentType);

        if (cloudinaryEnabled) {
            return uploadToCloudinary(file);
        } else {
            return saveLocally(file, contentType);
        }
    }

    /**
     * Vérifie que les premiers octets du fichier correspondent bien au type déclaré.
     * Empêche un attaquant de renommer un fichier malveillant en .jpg.
     */
    private void verifyImageMagicBytes(MultipartFile file, String contentType) throws IOException {
        byte[] header = new byte[12];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header);
            if (read < 3) {
                throw new IllegalArgumentException("Fichier invalide ou trop court.");
            }
        }
        boolean valid = switch (contentType) {
            case "image/jpeg" -> startsWith(header, MAGIC_JPEG);
            case "image/png"  -> startsWith(header, MAGIC_PNG);
            case "image/webp" -> startsWith(header, MAGIC_WEBP_RIFF)
                    && header.length >= 12
                    && header[8] == MAGIC_WEBP_MARK[0]
                    && header[9] == MAGIC_WEBP_MARK[1]
                    && header[10] == MAGIC_WEBP_MARK[2]
                    && header[11] == MAGIC_WEBP_MARK[3];
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Le contenu du fichier ne correspond pas au type déclaré. Fichier refusé.");
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    /**
     * Stocke une vidéo filmée localement (MP4, MOV, WebM, AVI).
     * - En prod : upload Cloudinary resource_type=video → URL CDN
     * - En dev  : sauvegarde locale → /uploads/xxx.mp4
     */
    public String storeVideo(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_VIDEO_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Type de vidéo non autorisé. Utilisez MP4, MOV, WebM ou AVI.");
        }
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new IllegalArgumentException("Vidéo trop volumineuse (max 100 Mo).");
        }

        if (cloudinaryEnabled) {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "bolas/videos",
                            "resource_type", "video"
                    )
            );
            return (String) result.get("secure_url");
        } else {
            return saveLocally(file, contentType);
        }
    }

    private String uploadToCloudinary(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "bolas",
                        "resource_type", "image"
                )
        );
        return (String) result.get("secure_url");
    }

    private String saveLocally(MultipartFile file, String contentType) throws IOException {
        String ext = getExtension(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + filename;
    }

    private String getExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
            if (Set.of(".jpg", ".jpeg", ".png", ".webp", ".mp4", ".mov", ".webm", ".avi").contains(ext)) {
                return ext;
            }
        }
        return switch (contentType) {
            case "image/png"        -> ".png";
            case "image/webp"       -> ".webp";
            case "video/mp4"        -> ".mp4";
            case "video/quicktime"  -> ".mov";
            case "video/webm"       -> ".webm";
            case "video/x-msvideo" -> ".avi";
            default                 -> ".jpg";
        };
    }
}
