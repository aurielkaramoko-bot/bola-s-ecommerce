package com.bolas.ecommerce.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB

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

        if (cloudinaryEnabled) {
            return uploadToCloudinary(file);
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
            if (Set.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
                return ext;
            }
        }
        return switch (contentType) {
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default           -> ".jpg";
        };
    }
}
