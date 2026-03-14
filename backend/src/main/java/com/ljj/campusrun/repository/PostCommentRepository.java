package com.ljj.campusrun.repository;

import com.ljj.campusrun.domain.entity.PostComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    List<PostComment> findByPostIdOrderByCreatedAtAsc(Long postId);
}
