package com.FileStorage.FileManagementSystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.FileStorage.FileManagementSystem.FileMergeRequest;
import com.FileStorage.FileManagementSystem.entity.FileEntity;
import com.FileStorage.FileManagementSystem.repository.FileRepository;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class FileStorageService {
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;
    
    @Autowired
    private FileRepository fileRepository;
    
    public FileEntity storeFile(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        
        // Save file to disk
        Files.copy(file.getInputStream(), filePath);
        
        // Save file metadata to database
        FileEntity fileEntity = new FileEntity(
            file.getOriginalFilename(),
            file.getContentType(),
            file.getSize(),
            filePath.toString()
        );
        
        return fileRepository.save(fileEntity);
    }
    
    public List<FileEntity> getAllFiles() {
        return fileRepository.findAll();
    }
    
    public Optional<FileEntity> getFile(Long id) {
        return fileRepository.findById(id);
    }
    
    public boolean deleteFile(Long id) {
        Optional<FileEntity> fileEntity = fileRepository.findById(id);
        if (fileEntity.isPresent()) {
            try {
                // Delete file from disk
                Files.deleteIfExists(Paths.get(fileEntity.get().getFilePath()));
                // Delete record from database
                fileRepository.deleteById(id);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }
    public void uploadChunk(MultipartFile chunk, String fileId, Integer chunkIndex) throws IOException {
        Path chunkDir = Paths.get(uploadDir, "chunks", fileId);
        Files.createDirectories(chunkDir);
        
        Path chunkPath = chunkDir.resolve(chunkIndex.toString());
        Files.write(chunkPath, chunk.getBytes());
    }
    
    public FileEntity mergeChunks(FileMergeRequest request) throws IOException {
        Path chunkDir = Paths.get(uploadDir, "chunks", request.getFileId());
        String finalFileName = System.currentTimeMillis() + "_" + request.getFileName();
        Path outputFile = Paths.get(uploadDir, finalFileName);
        
        try (OutputStream outputStream = Files.newOutputStream(outputFile, StandardOpenOption.CREATE)) {
            File[] chunkFiles = chunkDir.toFile().listFiles();
            if (chunkFiles != null) {
                // Sort chunks numerically
                Arrays.sort(chunkFiles, Comparator.comparingInt(f -> Integer.parseInt(f.getName())));
                
                for (File chunkFile : chunkFiles) {
                    Files.copy(chunkFile.toPath(), outputStream);
                }
            }
        }
        
        // Save to database
        FileEntity fileEntity = new FileEntity(
            request.getFileName(),
            request.getFileType(),
            request.getTotalSize(),
            outputFile.toString()
        );
        FileEntity savedEntity = fileRepository.save(fileEntity);
        
        // Cleanup chunks
        deleteDirectory(chunkDir);
        
        return savedEntity;
    }
    
    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
}
