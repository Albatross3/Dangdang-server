package com.dangdang.server.controller.post;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dangdang.server.domain.common.StatusType;
import com.dangdang.server.domain.member.domain.MemberRepository;
import com.dangdang.server.domain.member.domain.entity.Member;
import com.dangdang.server.domain.memberTown.domain.MemberTownRepository;
import com.dangdang.server.domain.memberTown.domain.entity.MemberTown;
import com.dangdang.server.domain.post.application.PostService;
import com.dangdang.server.domain.post.domain.Category;
import com.dangdang.server.domain.post.domain.PostSearchRepository;
import com.dangdang.server.domain.post.dto.request.PostSaveRequest;
import com.dangdang.server.domain.post.dto.request.PostSliceRequest;
import com.dangdang.server.domain.post.dto.request.PostUpdateRequest;
import com.dangdang.server.domain.post.dto.request.PostUpdateStatusRequest;
import com.dangdang.server.domain.post.dto.response.PostDetailResponse;
import com.dangdang.server.domain.postImage.dto.PostImageRequest;
import com.dangdang.server.domain.town.domain.TownRepository;
import com.dangdang.server.domain.town.domain.entity.Town;
import com.dangdang.server.domain.town.exception.TownNotFoundException;
import com.dangdang.server.global.exception.ExceptionCode;
import com.dangdang.server.global.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@AutoConfigureRestDocs
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class PostControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  PostService postService;

  @Autowired
  MemberRepository memberRepository;

  @Autowired
  TownRepository townRepository;

  @Autowired
  MemberTownRepository memberTownRepository;

  @Autowired
  PostSearchRepository postSearchRepository;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  JwtTokenProvider jwtTokenProvider;

  String accessToken;
  Member member;
  Town town;
  PostSaveRequest postSaveRequest;
  Long postId;
  MemberTown memberTown;

  @BeforeEach
  void setUp() {
    Member newMember = new Member("01098765467", "yb");
    member = memberRepository.save(newMember);

    town = townRepository.findByName("?????????")
        .orElseThrow(() -> new TownNotFoundException(ExceptionCode.TOWN_NOT_FOUND));

    MemberTown newMemberTown = new MemberTown(this.member, town);
    memberTown = memberTownRepository.save(newMemberTown);

    accessToken = "Bearer " + jwtTokenProvider.createAccessToken(member.getId());
    PostImageRequest postImageRequest = new PostImageRequest(
        List.of("http://s3.amazonaws.com/test1.png", "http://s3.amazonaws.com/test2.png"));
    postSaveRequest = new PostSaveRequest("????????? ??????", "????????? ??????", Category.???????????????, 20000, "????????? ????????????",
        BigDecimal.valueOf(127.0000), BigDecimal.valueOf(36.0000), false, "?????????", postImageRequest);
    PostDetailResponse postDetailResponse = postService.savePost(postSaveRequest, member.getId());
    postId = postDetailResponse.postId();
  }

  @ParameterizedTest
  @ValueSource(strings = {"RESERVED", "SELLING", "COMPLETED"})
  @DisplayName("???????????? ?????????, ?????????, ???????????? ??? 1?????? ????????? post??? ????????? ????????? ??? ??????.")
  void updatePostStatus(String status) throws Exception {

    mockMvc.perform(patch("/posts/" + postId + "/status").contentType(MediaType.APPLICATION_JSON)
            .header("AccessToken", accessToken).content(objectMapper.writeValueAsString(
                new PostUpdateStatusRequest(StatusType.valueOf(status))))).andDo(print())
        .andExpect(status().isOk()).andDo(
            document("post/api/patch/updateStatus", preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("AccessToken").description("Access Token")),
                requestFields(fieldWithPath("status").type(JsonFieldType.STRING)
                    .description("?????? ??????(?????????,?????????,??????)")), responseFields(
                    fieldWithPath("postResponse.id").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                    fieldWithPath("postResponse.title").type(JsonFieldType.STRING)
                        .description("????????? ??????"),
                    fieldWithPath("postResponse.content").type(JsonFieldType.STRING)
                        .description("????????? ??????"),
                    fieldWithPath("postResponse.category").type(JsonFieldType.STRING)
                        .description("????????????"),
                    fieldWithPath("postResponse.price").type(JsonFieldType.NUMBER).description("??????"),
                    fieldWithPath("postResponse.desiredPlaceName").type(JsonFieldType.STRING)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("postResponse.desiredPlaceLongitude").type(JsonFieldType.NUMBER)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("postResponse.desiredPlaceLatitude").type(JsonFieldType.NUMBER)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("postResponse.view").type(JsonFieldType.NUMBER).description("?????????"),
                    fieldWithPath("postResponse.sharing").type(JsonFieldType.BOOLEAN)
                        .description("?????? ??????"),
                    fieldWithPath("postResponse.townName").type(JsonFieldType.STRING)
                        .description("?????? ?????? ??????"),
                    fieldWithPath("postResponse.statusType").type(JsonFieldType.STRING)
                        .description("?????? ??????"),
                    fieldWithPath("postResponse.likeCount").type(JsonFieldType.NUMBER)
                        .description("????????? ??????"),
                    fieldWithPath("memberResponse.id").type(JsonFieldType.NUMBER)
                        .description("????????? ?????????"),
                    fieldWithPath("memberResponse.profileImgUrl").type(JsonFieldType.STRING)
                        .description("????????? ????????? ????????? url").optional(),
                    fieldWithPath("memberResponse.nickName").type(JsonFieldType.STRING)
                        .description("????????? ?????????"),
                    fieldWithPath("imageUrls").type(JsonFieldType.ARRAY).description("????????? ????????? url ?????????")
                        .optional())));
  }

  @Test
  @DisplayName("???????????? ????????? ??? ??????.")
  void savePostTest() throws Exception {
    
    mockMvc.perform(
            post("/posts").header("AccessToken", accessToken).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postSaveRequest)))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated()).andDo(print()).andDo(
            document("post/api/post/save", preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("AccessToken").description("Access Token")),
                requestFields(fieldWithPath("title").type(JsonFieldType.STRING).description("????????? ??????"),
                    fieldWithPath("content").type(JsonFieldType.STRING).description("????????? ??????"),
                    fieldWithPath("category").type(JsonFieldType.STRING).description("????????????"),
                    fieldWithPath("price").type(JsonFieldType.NUMBER).description("??????"),
                    fieldWithPath("desiredPlaceName").type(JsonFieldType.STRING)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("desiredPlaceLongitude").type(JsonFieldType.NUMBER)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("desiredPlaceLatitude").type(JsonFieldType.NUMBER)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("sharing").type(JsonFieldType.BOOLEAN).description("?????? ??????"),
                    fieldWithPath("townName").type(JsonFieldType.STRING).description("?????? ?????? ??????"),
                    fieldWithPath("postImageRequest.urls").type(JsonFieldType.ARRAY)
                        .description("????????? ????????? url ?????????").optional()), responseFields(
                    fieldWithPath("postResponse.id").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                    fieldWithPath("postResponse.title").type(JsonFieldType.STRING)
                        .description("????????? ??????"),
                    fieldWithPath("postResponse.content").type(JsonFieldType.STRING)
                        .description("????????? ??????"),
                    fieldWithPath("postResponse.category").type(JsonFieldType.STRING)
                        .description("????????????"),
                    fieldWithPath("postResponse.price").type(JsonFieldType.NUMBER).description("??????"),
                    fieldWithPath("postResponse.desiredPlaceName").type(JsonFieldType.STRING)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("postResponse.desiredPlaceLongitude").type(JsonFieldType.NUMBER)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("postResponse.desiredPlaceLatitude").type(JsonFieldType.NUMBER)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("postResponse.view").type(JsonFieldType.NUMBER).description("?????????"),
                    fieldWithPath("postResponse.sharing").type(JsonFieldType.BOOLEAN)
                        .description("?????? ??????"),
                    fieldWithPath("postResponse.townName").type(JsonFieldType.STRING)
                        .description("?????? ?????? ??????"),
                    fieldWithPath("postResponse.statusType").type(JsonFieldType.STRING)
                        .description("?????? ??????"),
                    fieldWithPath("postResponse.likeCount").type(JsonFieldType.NUMBER)
                        .description("????????? ??????"),
                    fieldWithPath("memberResponse.id").type(JsonFieldType.NUMBER)
                        .description("????????? ?????????"),
                    fieldWithPath("memberResponse.profileImgUrl").type(JsonFieldType.STRING)
                        .description("????????? ????????? ????????? url").optional(),
                    fieldWithPath("memberResponse.nickName").type(JsonFieldType.STRING)
                        .description("????????? ?????????"),
                    fieldWithPath("imageUrls").type(JsonFieldType.ARRAY).description("????????? ????????? url ?????????")
                        .optional())));
  }

  @Test
  @DisplayName("???????????? ????????? ?????? ??????????????? ????????????????????? ????????? ?????? ???????????? ????????? ??? ??????.")
  public void findAll() throws Exception {
    // given
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("page", "0");
    map.add("size", "10");
    // when
    mockMvc.perform(
            (get("/posts").contentType(MediaType.APPLICATION_JSON).header("AccessToken", accessToken)
                .params(map).characterEncoding(StandardCharsets.UTF_8)))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
        .andDo(print()).andDo(document("PostController/findAll", preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
            requestHeaders(headerWithName("AccessToken").description("Access Token")),
            responseFields(
                fieldWithPath("postSliceResponses[]").type(JsonFieldType.ARRAY)
                    .description("????????? ?????? ?????? ??????").optional(),
                fieldWithPath("postSliceResponses[].id").type(JsonFieldType.NUMBER).description("??? ??????")
                    .optional(), fieldWithPath("postSliceResponses[].title").type(JsonFieldType.STRING)
                    .description("??? ??????").optional(),
                fieldWithPath("postSliceResponses[].townName").type(JsonFieldType.STRING)
                    .description("?????? ????????? ?????? ??????").optional(),
                fieldWithPath("postSliceResponses[].imageUrl").type(JsonFieldType.STRING)
                    .description("??? ?????? ????????? ??????").optional(),
                fieldWithPath("postSliceResponses[].price").type(JsonFieldType.NUMBER)
                    .description("?????? ??????").optional(),
                fieldWithPath("postSliceResponses[].likeCount").type(JsonFieldType.NUMBER)
                    .description("????????? ??????").optional(),
                fieldWithPath("postSliceResponses[].createdAt").type(JsonFieldType.STRING)
                    .description("??? ????????????").optional(),
                fieldWithPath("hasNext").type(JsonFieldType.BOOLEAN).description("?????? ????????? ??? ?????? ??????"))));
  }

  @Test
  @DisplayName("???????????? ???????????? ?????? ????????? ???????????? ????????? ??? ??????.")
  public void search() throws Exception {
    //given
    postService.uploadToES();
    String query = "?????????";

    Thread.sleep(2000);

    // when
    MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
    paramMap.add("query", query);
    paramMap.add("rangeLevel", "4");
    paramMap.add("category", "????????????,???????????????");
    paramMap.add("isTransactionAvailableOnly", "true");
    paramMap.add("minPrice", "1000");
    paramMap.add("page", "0");
    paramMap.add("size", "10");

    mockMvc.perform(get("/posts/search").queryParams(paramMap).header("AccessToken", accessToken))
        .andDo(print()).andExpect(status().isOk()).andDo(print()).andDo(
            document("PostController/search", preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("AccessToken").description("Access Token")),
                responseFields(
                    fieldWithPath("postSliceResponses[]").type(JsonFieldType.ARRAY)
                        .description("????????? ?????? ?????? ??????").optional(),
                    fieldWithPath("postSliceResponses[].id").type(JsonFieldType.NUMBER)
                        .description("??? ??????").optional(),
                    fieldWithPath("postSliceResponses[].title").type(JsonFieldType.STRING)
                        .description("??? ??????").optional(),
                    fieldWithPath("postSliceResponses[].townName").type(JsonFieldType.STRING)
                        .description("?????? ????????? ?????? ??????").optional(),
                    fieldWithPath("postSliceResponses[].imageUrl").type(JsonFieldType.STRING)
                        .description("??? ?????? ????????? ??????").optional(),
                    fieldWithPath("postSliceResponses[].price").type(JsonFieldType.NUMBER)
                        .description("?????? ??????").optional(),
                    fieldWithPath("postSliceResponses[].likeCount").type(JsonFieldType.NUMBER)
                        .description("????????? ??????").optional(),
                    fieldWithPath("postSliceResponses[].createdAt").type(JsonFieldType.STRING)
                        .description("??? ????????????").optional(),
                    fieldWithPath("hasNext").type(JsonFieldType.BOOLEAN)
                        .description("?????? ????????? ??? ?????? ??????"))));

    //then
    postSearchRepository.deleteAll();
  }

  @Test
  @DisplayName("???????????? postId, member, town, view??? ????????? ?????? ?????? ????????? ??? ??????.")
  void updatePostTest() throws Exception {
    String updateTitle = "updateTitle";
    String updateContent = "updateContent";
    Integer updatePrice = 2000;
    Category updateCategory = Category.????????????;
    String updateDesiredPlaceName = "????????? ????????????";
    BigDecimal updateLongitude = BigDecimal.valueOf(127.0000);
    BigDecimal updateLatitude = BigDecimal.valueOf(36.0000);
    String updateUrlOne = "http://s3.amazonaws.com/updateTest1";
    String updateUrlTwo = "http://s3.amazonaws.com/updateTest2";
    PostImageRequest updatePostImageRequest = new PostImageRequest(
        Arrays.asList(updateUrlOne, updateUrlTwo));

    //when
    PostUpdateRequest postUpdateRequest = new PostUpdateRequest(postId, updateTitle, updateContent,
        updateCategory, updatePrice, updateDesiredPlaceName, updateLongitude, updateLatitude, false,
        updatePostImageRequest);

    mockMvc.perform((RestDocumentationRequestBuilders.put("/posts/{id}", postId)
            .header("AccessToken", accessToken).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(postUpdateRequest))))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
        .andDo(MockMvcResultHandlers.print()).andDo(
            document("post/api/put/update", preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("AccessToken").description("Access Token")),
                requestFields(fieldWithPath("id").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                    fieldWithPath("title").type(JsonFieldType.STRING).description("????????? ??????"),
                    fieldWithPath("content").type(JsonFieldType.STRING).description("????????? ??????"),
                    fieldWithPath("category").type(JsonFieldType.STRING).description("????????????"),
                    fieldWithPath("price").type(JsonFieldType.NUMBER).description("??????"),
                    fieldWithPath("desiredPlaceName").type(JsonFieldType.STRING)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("desiredPlaceLongitude").type(JsonFieldType.NUMBER)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("desiredPlaceLatitude").type(JsonFieldType.NUMBER)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("sharing").type(JsonFieldType.BOOLEAN).description("?????? ??????"),
                    fieldWithPath("postImageRequest.urls").type(JsonFieldType.ARRAY)
                        .description("????????? ????????? url ?????????").optional()), responseFields(
                    fieldWithPath("postResponse.id").type(JsonFieldType.NUMBER).description("????????? ?????????"),
                    fieldWithPath("postResponse.title").type(JsonFieldType.STRING)
                        .description("????????? ??????"),
                    fieldWithPath("postResponse.content").type(JsonFieldType.STRING)
                        .description("????????? ??????"),
                    fieldWithPath("postResponse.category").type(JsonFieldType.STRING)
                        .description("????????????"),
                    fieldWithPath("postResponse.price").type(JsonFieldType.NUMBER).description("??????"),
                    fieldWithPath("postResponse.desiredPlaceName").type(JsonFieldType.STRING)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("postResponse.desiredPlaceLongitude").type(JsonFieldType.NUMBER)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("postResponse.desiredPlaceLatitude").type(JsonFieldType.NUMBER)
                        .description("???????????? ?????? ??????").optional(),
                    fieldWithPath("postResponse.view").type(JsonFieldType.NUMBER).description("?????????"),
                    fieldWithPath("postResponse.sharing").type(JsonFieldType.BOOLEAN)
                        .description("?????? ??????"),
                    fieldWithPath("postResponse.townName").type(JsonFieldType.STRING)
                        .description("?????? ?????? ??????"),
                    fieldWithPath("postResponse.statusType").type(JsonFieldType.STRING)
                        .description("?????? ??????"),
                    fieldWithPath("postResponse.likeCount").type(JsonFieldType.NUMBER)
                        .description("????????? ??????"),
                    fieldWithPath("memberResponse.id").type(JsonFieldType.NUMBER)
                        .description("????????? ?????????"),
                    fieldWithPath("memberResponse.profileImgUrl").type(JsonFieldType.STRING)
                        .description("????????? ????????? ????????? url").optional(),
                    fieldWithPath("memberResponse.nickName").type(JsonFieldType.STRING)
                        .description("????????? ?????????"),
                    fieldWithPath("imageUrls").type(JsonFieldType.ARRAY).description("????????? ????????? url ?????????")
                        .optional())));
  }

  @Test
  @DisplayName("????????? ????????? ????????? ???????????? ????????????.")
  void clickLikesTest() throws Exception {

    mockMvc.perform((RestDocumentationRequestBuilders.patch("/posts/{id}/likes", postId)
            .header("AccessToken", accessToken).contentType(MediaType.APPLICATION_JSON)))
        .andExpect(status().isOk()).andDo(MockMvcResultHandlers.print()).andDo(
            document("post/api/patch/updateLikes", preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("AccessToken").description("Access Token"))));
  }

}