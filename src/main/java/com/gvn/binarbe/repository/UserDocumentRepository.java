package com.gvn.binarbe.repository;

import com.gvn.binarbe.entity.UserDocument;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {
  List<UserDocument> findByUserId(Long userId);
}
