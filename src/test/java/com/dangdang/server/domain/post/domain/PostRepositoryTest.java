package com.dangdang.server.domain.post.domain;

import com.dangdang.server.domain.member.domain.MemberRepository;
import com.dangdang.server.domain.member.domain.entity.Member;
import com.dangdang.server.domain.post.domain.entity.Post;
import com.dangdang.server.domain.town.domain.TownRepository;
import com.dangdang.server.domain.town.domain.entity.Town;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostRepositoryTest {

  @Autowired
  PostRepository postRepository;
  @Autowired
  MemberRepository memberRepository;
  @Autowired
  TownRepository townRepository;

  @BeforeEach
  void savePost() throws Exception {
    for (int i = 1; i <= 4; i++) {
      //given
      Member member = new Member("테스트 멤버" + i, "01012341234", "testImgUrl");
      memberRepository.save(member);
      Town town = new Town("테스트 동" + i, null, null);
      townRepository.save(town);

      Post post = new Post("제목" + i, "내용" + i, Category.가전제품, 20000,
          null, null, null, 0, false,
          member, town);
      // when
      postRepository.save(post);
    }
    //then
  }

  @Test
  public void getAllPosts() throws Exception {
    //given
    List<Post> posts = postRepository.findAll();
    List<Long> adjacency = posts.stream().map(post -> post.getTown().getId())
        .collect(Collectors.toList());
    int pageNum = 0;
    int size = 2;
    // when
    Slice<Post> findPosts = postRepository.findPostsByTownIdFetchJoinSortByCreatedAt(adjacency,
        PageRequest.of(pageNum, size, Sort.by("createdAt").descending()));
    //then
    Assertions.assertThat(findPosts).hasSize(2);
    Assertions.assertThat(findPosts.getContent().get(0).getCreatedAt())
        .isAfter(findPosts.getContent().get(1).getCreatedAt());
  }
}