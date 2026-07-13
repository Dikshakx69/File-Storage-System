package com.FileStorage.FileManagementSystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.FileStorage.FileManagementSystem.entity.FileEntity;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
}
