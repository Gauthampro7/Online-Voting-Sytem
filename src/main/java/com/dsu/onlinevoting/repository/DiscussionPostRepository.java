package com.dsu.onlinevoting.repository;

import com.dsu.onlinevoting.model.DiscussionPost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, Long> {
    List<DiscussionPost> findAllByOrderByTimestampDesc();
}
