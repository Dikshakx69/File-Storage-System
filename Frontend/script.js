const API_BASE_URL = 'http://localhost:8080/api/files';

class FileStorageApp {
    constructor() {
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadFiles();
    }

    setupEventListeners() {
        const uploadArea = document.getElementById('uploadArea');
        const fileInput = document.getElementById('fileInput');

        // Drag and drop events
        uploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadArea.classList.add('drag-over');
        });

        uploadArea.addEventListener('dragleave', () => {
            uploadArea.classList.remove('drag-over');
        });

        uploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadArea.classList.remove('drag-over');
            const files = e.dataTransfer.files;
            this.handleFiles(files);
        });

        // File input change event
        fileInput.addEventListener('change', (e) => {
            this.handleFiles(e.target.files);
        });
    }

    handleFiles(files) {
        for (let i = 0; i < files.length; i++) {
            this.uploadFile(files[i]).then(() => {
                this.loadFiles();
            }).catch((error) => {
                this.showMessage('Error uploading file: ' + error.message, 'error');
            });
        }
    }

    uploadFile(file) {
        // For small files (< 5MB), use normal upload
        if (file.size < 5 * 1024 * 1024) {
            return this.uploadSingleFile(file);
        } else {
            // For large files, use chunked upload
            return this.uploadFileInChunks(file);
        }
    }

    uploadSingleFile(file) {
        return new Promise((resolve, reject) => {
            const formData = new FormData();
            formData.append('file', file);

            fetch(`${API_BASE_URL}/upload`, {
                method: 'POST',
                body: formData
            })
            .then(response => {
                if (response.ok) {
                    this.showMessage('File uploaded successfully!', 'success');
                    resolve();
                } else {
                    throw new Error('Upload failed');
                }
            })
            .catch(error => {
                this.showMessage('Error uploading file: ' + error.message, 'error');
                reject(error);
            });
        });
    }

    uploadFileInChunks(file) {
        return new Promise((resolve, reject) => {
            const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB chunks
            const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
            const fileId = Date.now() + '_' + Math.random().toString(36).substr(2, 9);
            
            this.showUploadProgress(file.name, 0);
            
            let currentChunk = 0;
            
            const uploadNextChunk = () => {
                if (currentChunk >= totalChunks) {
                    // All chunks uploaded, now finalize
                    this.finalizeChunkedUpload(fileId, file.name, file.type, file.size)
                        .then(() => {
                            this.hideUploadProgress();
                            this.showMessage('File uploaded successfully!', 'success');
                            resolve();
                        })
                        .catch(error => {
                            this.hideUploadProgress();
                            this.showMessage('Error finalizing upload: ' + error.message, 'error');
                            reject(error);
                        });
                    return;
                }
                
                const start = currentChunk * CHUNK_SIZE;
                const end = Math.min(start + CHUNK_SIZE, file.size);
                const chunk = file.slice(start, end);
                
                const chunkFormData = new FormData();
                chunkFormData.append('file', chunk, file.name);
                chunkFormData.append('chunkIndex', currentChunk);
                chunkFormData.append('totalChunks', totalChunks);
                chunkFormData.append('fileId', fileId);
                chunkFormData.append('originalName', file.name);
                chunkFormData.append('fileType', file.type);
                
                fetch(`${API_BASE_URL}/upload-chunk`, {
                    method: 'POST',
                    body: chunkFormData
                })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`Failed to upload chunk ${currentChunk + 1}/${totalChunks}`);
                    }
                    
                    // Update progress
                    const progress = ((currentChunk + 1) / totalChunks) * 100;
                    this.showUploadProgress(file.name, progress);
                    
                    currentChunk++;
                    uploadNextChunk();
                })
                .catch(error => {
                    this.hideUploadProgress();
                    this.showMessage('Error uploading chunk: ' + error.message, 'error');
                    reject(error);
                });
            };
            
            // Start uploading chunks
            uploadNextChunk();
        });
    }

    finalizeChunkedUpload(fileId, fileName, fileType, fileSize) {
        return new Promise((resolve, reject) => {
            const requestData = {
                fileId: fileId,
                fileName: fileName,
                fileType: fileType,
                totalSize: fileSize
            };
            
            fetch(`${API_BASE_URL}/finalize-upload`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            })
            .then(response => {
                if (response.ok) {
                    resolve(response.json());
                } else {
                    throw new Error('Failed to finalize upload');
                }
            })
            .catch(error => {
                reject(error);
            });
        });
    }

    loadFiles() {
        const filesGrid = document.getElementById('filesGrid');
        filesGrid.innerHTML = '<div class="loading">Loading files...</div>';

        fetch(API_BASE_URL)
            .then(response => response.json())
            .then(files => {
                if (files.length === 0) {
                    filesGrid.innerHTML = '<div class="loading">No files uploaded yet.</div>';
                    return;
                }

                filesGrid.innerHTML = '';
                files.forEach(file => {
                    const fileCard = this.createFileCard(file);
                    filesGrid.appendChild(fileCard);
                });
            })
            .catch(error => {
                filesGrid.innerHTML = '<div class="error">Error loading files: ' + error.message + '</div>';
            });
    }

    createFileCard(file) {
        const card = document.createElement('div');
        card.className = 'file-card';
        
        const fileIcon = this.getFileIcon(file.fileType);
        const fileSize = this.formatFileSize(file.fileSize);
        const uploadDate = new Date(file.uploadDate).toLocaleDateString();

        card.innerHTML = `
            <div class="file-header">
                <div class="file-icon">${fileIcon}</div>
                <div class="file-info">
                    <h4>${file.fileName}</h4>
                    <div class="file-type">${file.fileType}</div>
                </div>
            </div>
            <div class="file-details">
                <span>Size: ${fileSize}</span>
                <span>Uploaded: ${uploadDate}</span>
            </div>
            <div class="file-actions">
                <button class="btn-download" onclick="app.downloadFile(${file.id}, '${file.fileName.replace(/'/g, "\\'")}')">
                    Download
                </button>
                <button class="btn-delete" onclick="app.deleteFile(${file.id})">
                    Delete
                </button>
            </div>
        `;

        return card;
    }

    getFileIcon(fileType) {
        if (fileType.startsWith('image/')) return '🖼️';
        if (fileType.startsWith('video/')) return '🎥';
        if (fileType.startsWith('audio/')) return '🎵';
        if (fileType.includes('pdf')) return '📕';
        if (fileType.includes('word')) return '📄';
        if (fileType.includes('excel') || fileType.includes('spreadsheet')) return '📊';
        if (fileType.includes('zip') || fileType.includes('compressed')) return '📦';
        return '📁';
    }

    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    downloadFile(fileId, fileName) {
        fetch(`${API_BASE_URL}/${fileId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Download failed');
                }
                return response.blob();
            })
            .then(blob => {
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.style.display = 'none';
                a.href = url;
                a.download = fileName;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
                
                this.showMessage('File download started!', 'success');
            })
            .catch(error => {
                this.showMessage('Error downloading file: ' + error.message, 'error');
            });
    }

    deleteFile(fileId) {
        if (!confirm('Are you sure you want to delete this file?')) {
            return;
        }

        fetch(`${API_BASE_URL}/${fileId}`, {
            method: 'DELETE'
        })
        .then(response => {
            if (response.ok) {
                this.showMessage('File deleted successfully!', 'success');
                this.loadFiles();
            } else {
                throw new Error('Delete failed');
            }
        })
        .catch(error => {
            this.showMessage('Error deleting file: ' + error.message, 'error');
        });
    }

    showUploadProgress(fileName, progress) {
        // Remove existing progress bar
        this.hideUploadProgress();
        
        // Create progress bar
        const progressDiv = document.createElement('div');
        progressDiv.className = 'upload-progress';
        progressDiv.innerHTML = `
            <div class="progress-container">
                <div class="progress-info">
                    <span>Uploading: ${fileName}</span>
                    <span>${Math.round(progress)}%</span>
                </div>
                <div class="progress-bar">
                    <div class="progress-fill" style="width: ${progress}%"></div>
                </div>
            </div>
        `;
        
        document.querySelector('.upload-section').appendChild(progressDiv);
    }

    hideUploadProgress() {
        const existingProgress = document.querySelector('.upload-progress');
        if (existingProgress) {
            existingProgress.remove();
        }
    }

    showMessage(message, type) {
        // Remove existing messages
        const existingMessages = document.querySelectorAll('.message');
        existingMessages.forEach(msg => msg.remove());

        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        messageDiv.textContent = message;

        const container = document.querySelector('.container');
        container.insertBefore(messageDiv, container.firstChild);

        // Auto remove after 5 seconds
        setTimeout(() => {
            messageDiv.remove();
        }, 5000);
    }
}

// Initialize the application
const app = new FileStorageApp();