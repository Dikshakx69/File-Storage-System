package com.FileStorage.FileManagementSystem.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.FileStorage.FileManagementSystem.FileMergeRequest;
import com.FileStorage.FileManagementSystem.entity.FileEntity;
import com.FileStorage.FileManagementSystem.service.FileStorageService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:3000")
public class FileController {
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            FileEntity fileEntity = fileStorageService.storeFile(file);
            return ResponseEntity.ok(fileEntity);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Could not upload file: " + e.getMessage());
        }
    }
    
    @GetMapping
    public ResponseEntity<List<FileEntity>> getAllFiles() {
        List<FileEntity> files = fileStorageService.getAllFiles();
        return ResponseEntity.ok(files);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getFile(@PathVariable Long id) {
        Optional<FileEntity> fileEntity = fileStorageService.getFile(id);
        if (fileEntity.isPresent()) {
            try {
                Path filePath = Paths.get(fileEntity.get().getFilePath());
                Resource resource = new UrlResource(filePath.toUri());
                
                if (resource.exists() && resource.isReadable()) {
                    return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(fileEntity.get().getFileType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + fileEntity.get().getFileName() + "\"")
                        .body(resource);
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("File not found");
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving file: " + e.getMessage());
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("File not found with id: " + id);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        boolean deleted = fileStorageService.deleteFile(id);
        if (deleted) {
            return ResponseEntity.ok("File deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("File not found with id: " + id);
        }
    }
    // Add to your existing FileController
@PostMapping("/upload-chunk")
public ResponseEntity<?> uploadChunk(
        @RequestParam("file") MultipartFile chunk,
        @RequestParam("chunkIndex") Integer chunkIndex,
        @RequestParam("totalChunks") Integer totalChunks,
        @RequestParam("fileId") String fileId,
        @RequestParam("originalName") String originalName,
        @RequestParam("fileType") String fileType) {
    
    try {
        fileStorageService.uploadChunk(chunk, fileId, chunkIndex);
        return ResponseEntity.ok().build();
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Chunk upload failed: " + e.getMessage());
    }
}

@PostMapping("/finalize-upload")
public ResponseEntity<?> finalizeUpload(@RequestBody FileMergeRequest request) {
    try {
        FileEntity fileEntity = fileStorageService.mergeChunks(request);
        return ResponseEntity.ok(fileEntity);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("File merge failed: " + e.getMessage());
    }
}
}