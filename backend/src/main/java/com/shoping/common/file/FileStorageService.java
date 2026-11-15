package com.shoping.common.file;

import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地图片存储：保存到 ${app.upload.dir}，通过 /uploads/** 对外访问
 */
@Slf4j
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final DateTimeFormatter MONTH_DIR = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String BIZ_PATTERN = "[a-z0-9_-]{1,20}";

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Value("${app.upload.max-size-mb:5}")
    private long maxSizeMb;

    public UploadResult saveImage(MultipartFile file, String bizDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > maxSizeMb * 1024 * 1024) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        String biz = (bizDir != null && bizDir.matches(BIZ_PATTERN)) ? bizDir : "common";
        String month = LocalDate.now().format(MONTH_DIR);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(biz).resolve(month);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName));
        } catch (IOException e) {
            log.error("图片保存失败: dir={}, name={}", dir, fileName, e);
            throw new BusinessException(ErrorCode.FILE_SAVE_FAILED);
        }

        UploadResult result = new UploadResult();
        result.setUrl(trimTrailingSlash(urlPrefix) + "/" + biz + "/" + month + "/" + fileName);
        result.setOriginalName(file.getOriginalFilename());
        result.setSize(file.getSize());
        return result;
    }

    private String extensionOf(String originalName) {
        if (originalName == null) {
            return "";
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String trimTrailingSlash(String value) {
        String result = value == null || value.isBlank() ? "/uploads" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
