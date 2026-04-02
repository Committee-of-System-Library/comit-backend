package kr.ac.knu.comit.post.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPost_IdOrderBySortOrderAsc(Long postId);

    List<PostImage> findByPost_IdInOrderBySortOrderAsc(List<Long> postIds);
}
