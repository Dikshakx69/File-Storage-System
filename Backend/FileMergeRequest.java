package com.FileStorage.FileManagementSystem;

public class FileMergeRequest {
      private String fileId;
        private String fileName;
        private String fileType;
        private Long totalSize;
    
        // Constructors, Getters and Setters
        public FileMergeRequest() {}
        
        public FileMergeRequest(String fileId, String fileName, String fileType, Long totalSize) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.fileType = fileType;
            this.totalSize = totalSize;
        }
        
        public String getFileId() { 
          return fileId; 
        }
        public void setFileId(String fileId) { 
          this.fileId = fileId; 
        }
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
        public Long getTotalSize() { 
          return totalSize; 
        }
        public void setTotalSize(Long totalSize) { 
          this.totalSize = totalSize; 
        }
}
