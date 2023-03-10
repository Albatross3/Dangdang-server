package com.dangdang.server.domain.post.domain;

import com.dangdang.server.domain.post.domain.entity.Post;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

  @Query(value = "select p from Post p join fetch p.town where p.town.id in (:adjacency)")
  Slice<Post> findPostsByTownIdFetchJoinSortByCreatedAt(
      @Param("adjacency") List<Long> adjacency, Pageable pageable);

  @Query(value = "select p from Post p join fetch p.member left join p.likes where p.id = :postId")
  Optional<Post> findPostDetailById(@Param("postId") Long postId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Post p WHERE p.id = :postId")
  Optional<Post> findByIdForUpdate(@Param("postId") Long postId);
}
