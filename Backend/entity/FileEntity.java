package com.FileStorage.FileManagementSystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String fileType;
    
    @Column(nullable = false)
    private Long fileSize;
    
    @Column(nullable = false)
    private String filePath;
    
    @Column(nullable = false)
    private LocalDateTime uploadDate;
    
    // Constructors
    public FileEntity() {}
    
    public FileEntity(String fileName, String fileType, Long fileSize, String filePath) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.uploadDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFileName() { 
        return fileName; 
    }
    public void setFileName(String fileName) { 
        this.fileName = fileName; 
    }
    
    public String getFileType() { 
        return fileType; 
    }
    public void setFileType(String fileType) { 
        this.fileType = fileType; 
    }
    
    public Long getFileSize() { 
        return fileSize; 
    }
    public void setFileSize(Long fileSize) { 
        this.fileSize = fileSize; 
    }
    
    public String getFilePath() { 
        return filePath; 
    }
    public void setFilePath(String filePath) { 
        this.filePath = filePath; 
    }
    
    public LocalDateTime getUploadDate() { 
        return uploadDate; 
    }
    public void setUploadDate(LocalDateTime uploadDate) { 
        this.uploadDate = uploadDate; 
    }
}