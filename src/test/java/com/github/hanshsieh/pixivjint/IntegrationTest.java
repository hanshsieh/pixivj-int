package com.github.hanshsieh.pixivjint;

import com.github.hanshsieh.pixivj.api.PixivApiClient;
import com.github.hanshsieh.pixivj.model.FilterType;
import com.github.hanshsieh.pixivj.model.RecommendedIllusts;
import com.github.hanshsieh.pixivj.model.RecommendedIllustsFilter;
import com.github.hanshsieh.pixivj.oauth.PixivOAuthClient;
import com.github.hanshsieh.pixivj.token.FixedTokenProvider;
import com.github.hanshsieh.pixivj.token.ThreadedTokenRefresher;
import com.github.hanshsieh.pixivj.token.TokenProvider;
import com.github.hanshsieh.pixivjjfx.stage.PixivLoginStage;
import com.github.hanshsieh.pixivjjfx.token.WebViewTokenProvider;
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
  @Test
  @DisplayName("Get recommended illustrations")
  public void recommendedIllusts() throws Exception {
    RecommendedIllustsFilter filter = new RecommendedIllustsFilter();
    filter.setFilter(FilterType.FOR_ANDROID);
    filter.setIncludePrivacyPolicy(true);
    filter.setIncludeRankingIllusts(true);
    RecommendedIllusts illusts = apiClient.getRecommendedIllusts(filter);
    assertNotNull(illusts.getNextUrl());
    assertNotNull(illusts.getPrivacyPolicy());
    assertNotNull(illusts.getPrivacyPolicy().getVersion());
    assertTrue(illusts.getIllusts().size() > 0);
    assertTrue(illusts.getRankingIllusts().size() > 0);
  }
}
