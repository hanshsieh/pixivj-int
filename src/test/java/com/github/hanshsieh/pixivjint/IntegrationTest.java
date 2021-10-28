package com.github.hanshsieh.pixivjint;

import com.github.hanshsieh.pixivj.api.PixivApiClient;
import com.github.hanshsieh.pixivj.model.AddBookmark;
import com.github.hanshsieh.pixivj.model.AddBookmarkResult;
import com.github.hanshsieh.pixivj.model.Comment;
import com.github.hanshsieh.pixivj.model.Comments;
import com.github.hanshsieh.pixivj.model.FilterMode;
import com.github.hanshsieh.pixivj.model.FilterType;
import com.github.hanshsieh.pixivj.model.IllustCommentsFilter;
import com.github.hanshsieh.pixivj.model.IllustDetail;
import com.github.hanshsieh.pixivj.model.Illustration;
import com.github.hanshsieh.pixivj.model.RankedIllusts;
import com.github.hanshsieh.pixivj.model.RankedIllustsFilter;
import com.github.hanshsieh.pixivj.model.RecommendedIllusts;
import com.github.hanshsieh.pixivj.model.RecommendedIllustsFilter;
import com.github.hanshsieh.pixivj.model.Restrict;
import com.github.hanshsieh.pixivj.model.SearchTarget;
import com.github.hanshsieh.pixivj.model.SearchedIllusts;
import com.github.hanshsieh.pixivj.model.SearchedIllustsFilter;
import com.github.hanshsieh.pixivj.oauth.PixivOAuthClient;
import com.github.hanshsieh.pixivj.token.FixedTokenProvider;
import com.github.hanshsieh.pixivj.token.ThreadedTokenRefresher;
import com.github.hanshsieh.pixivj.token.TokenProvider;
import com.github.hanshsieh.pixivjjfx.stage.PixivLoginStage;
import com.github.hanshsieh.pixivjjfx.token.WebViewTokenProvider;
import java.time.LocalDate;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.*;

/**
 * We want to use a same instance for all the test methods, so we use PER_CLASS lifecycle.
 * See https://www.baeldung.com/java-beforeall-afterall-non-static
 */
@DisplayName("Integration test")
@TestInstance(Lifecycle.PER_CLASS)
public class IntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);
  private PixivOAuthClient authClient = null;
  private TokenProvider tokenProvider = null;
  private PixivApiClient apiClient = null;
  @BeforeAll
  public void setup() throws Exception {
    TestApplication.launchIfNeeded();
    String accessToken = System.getenv("ACCESS_TOKEN");
    if (accessToken != null) {
      logger.info("Using access token from environment variable");
      tokenProvider = new FixedTokenProvider(accessToken);
    } else {
      logger.info("Getting access token with web login");
      PixivLoginStage loginStage;
      authClient = new PixivOAuthClient.Builder().build();
      ThreadedTokenRefresher tokenRefresher = new ThreadedTokenRefresher.Builder()
          .setAuthClient(authClient)
          .build();
      loginStage = new PixivLoginStage.Builder().buildInFxThread();
      tokenProvider = new WebViewTokenProvider.Builder()
          .setAuthClient(authClient)
          .setTokenRefresher(tokenRefresher)
          .setLoginStage(loginStage)
          .build();
      accessToken = tokenProvider.getAccessToken();
    }
    if (Boolean.parseBoolean(System.getenv("SHOW_ACCESS_TOKEN"))) {
      logger.info("Access token: {}", accessToken);
    }
    apiClient = new PixivApiClient.Builder()
        .setTokenProvider(tokenProvider)
        .build();
  }
  @AfterAll
  public void tearDown() throws Exception {
    if (tokenProvider != null) {
      tokenProvider.close();
    }
    if (authClient != null) {
      authClient.close();
    }
    if (apiClient != null) {
      apiClient.close();
    }
  }

  private void validateIllustrations(@NonNull List<Illustration> illusts) {
    for (Illustration illust : illusts) {
      assertNotNull(illust.getId());
      assertNotNull(illust.getTitle());
      assertNotNull(illust.getCaption());
      assertNotNull(illust.getCreateDate());
    }
  }

  private void validateComment(Comment comment) {
    assertNotNull(comment.getComment());
    assertNotNull(comment.getDate());
    assertNotNull(comment.getHasReplies());
    assertNotNull(comment.getId());
    assertNotNull(comment.getUser());
    assertNotNull(comment.getUser().getAccount());
    assertNotNull(comment.getUser().getId());
    assertNotNull(comment.getUser().getName());
    assertNotNull(comment.getUser().getProfileImageUrls());
  }

  @Test
  @DisplayName("Get ranked illustrations")
  public void testGetRankedIllusts() throws Exception {
    RankedIllustsFilter filter = new RankedIllustsFilter();
    filter.setFilter(FilterType.FOR_ANDROID);
    filter.setMode(FilterMode.DAY_MALE);
    // Today's ranking might not be ready
    filter.setDate(LocalDate.now().minusDays(3));
    filter.setOffset(1);
    for (int i = 0; i < 2; ++i) {
      RankedIllusts illusts = apiClient.getRankedIllusts(filter);
      assertNotNull(illusts.getNextUrl());
      assertNotNull(illusts.getIllusts());
      validateIllustrations(illusts.getIllusts());
      String nextUrl = illusts.getNextUrl();
      if (nextUrl == null) {
        break;
      }
      filter = RankedIllustsFilter.fromUrl(nextUrl);
    }
  }

  @Test
  @DisplayName("Get recommended illustrations")
  public void testGetRecommendedIllusts() throws Exception {
    RecommendedIllustsFilter filter = new RecommendedIllustsFilter();
    filter.setFilter(FilterType.FOR_ANDROID);
    filter.setIncludePrivacyPolicy(true);
    filter.setIncludeRankingIllusts(true);
    for (int i = 0; i < 2; ++i) {
      RecommendedIllusts illusts = apiClient.getRecommendedIllusts(filter);
      assertNotNull(illusts.getNextUrl());
      assertNotNull(illusts.getPrivacyPolicy());
      if (i == 0) {
        assertNotNull(illusts.getPrivacyPolicy().getVersion());
        assertTrue(illusts.getRankingIllusts().size() > 0);
      } else {
        assertNull(illusts.getPrivacyPolicy().getVersion());
        assertTrue(illusts.getRankingIllusts().isEmpty());
      }
      assertTrue(illusts.getIllusts().size() > 0);
      validateIllustrations(illusts.getIllusts());
      validateIllustrations(illusts.getRankingIllusts());
      String nextUrl = illusts.getNextUrl();
      if (nextUrl == null) {
        break;
      }
      filter = RecommendedIllustsFilter.fromUrl(nextUrl);
    }
  }

  @Test
  @DisplayName("Search illustrations")
  public void testSearchIllusts() throws Exception {
    SearchedIllustsFilter filter = new SearchedIllustsFilter();
    filter.setFilter(FilterType.FOR_ANDROID);
    filter.setWord("swimsuit girl");
    filter.setIncludeTranslatedTagResults(true);
    filter.setMergePlainKeywordResults(true);
    filter.setOffset(5);
    filter.setSearchTarget(SearchTarget.PARTIAL_MATCH_FOR_TAGS);
    for (int i = 0; i < 2; ++i) {
      SearchedIllusts illusts = apiClient.searchIllusts(filter);
      assertNotNull(illusts.getNextUrl());
      assertNotNull(illusts.getSearchSpanLimit());
      validateIllustrations(illusts.getIllusts());
      String nextUrl = illusts.getNextUrl();
      if (nextUrl == null) {
        break;
      }
      filter = SearchedIllustsFilter.fromUrl(nextUrl);
    }
  }

  @Test
  @DisplayName("Get illustration details")
  public void testGetIllustDetail() throws Exception {
    RecommendedIllustsFilter filter = new RecommendedIllustsFilter();
    RecommendedIllusts illusts = apiClient.getRecommendedIllusts(filter);
    assertFalse(illusts.getIllusts().isEmpty());
    Illustration illust = illusts.getIllusts().get(0);
    long illustId = illust.getId();
    IllustDetail detail = apiClient.getIllustDetail(illustId);
    Illustration illust2 = detail.getIllust();
    assertEquals(illust2, illust);
  }

  @Test
  @DisplayName("Get illustration comments")
  public void testIllustComments() throws Exception {
    IllustCommentsFilter filter = new IllustCommentsFilter();
    filter.setIllustId(79898361L);
    for (int i = 0; i < 2; ++i) {
      Comments comments = apiClient.getIllustComments(filter);
      for (Comment comment : comments.getComments()) {
        validateComment(comment);
      }
      String nextUrl = comments.getNextUrl();
      if (nextUrl == null) {
        break;
      }
      filter = IllustCommentsFilter.fromUrl(nextUrl);
    }
  }

  @Test
  @DisplayName("Add bookmark")
  public void testAddBookmark() throws Exception {
    AddBookmark bookmark = new AddBookmark();
    bookmark.setIllustId(93727205L);
    bookmark.setRestrict(Restrict.PUBLIC);
    AddBookmarkResult result = apiClient.addBookmark(bookmark);
    assertNotNull(result);
  }
}
