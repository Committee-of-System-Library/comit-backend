package kr.ac.knu.comit.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageUploader {

    String upload(MultipartFile file, String folder);
}
