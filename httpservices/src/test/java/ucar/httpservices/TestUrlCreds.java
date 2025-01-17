package ucar.httpservices;

import static com.google.common.truth.Truth.assertThat;
import java.util.Arrays;
import java.util.Collection;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class TestUrlCreds {

  private static final Logger logger = LoggerFactory.getLogger(TestUrlCreds.class);
  private String username;
  private String password;
  private String host;
  private final int actualConnectionsBefore;

  // Each parameter should be placed as an argument here
  // Every time runner triggers, it will pass the arguments
  // from parameters we defined in primeNumbers() method

  public TestUrlCreds(String username, String password, String host) {
    this.username = username;
    this.password = password;
    this.host = host;
    actualConnectionsBefore = HTTPSession.getActualConnections();
  }

  @Parameterized.Parameters
  public static Collection primeNumbers() {
    // {username, password, host}
    return Arrays.asList(new Object[][] {{"user", "someCoolFruitOrSomething", "my.server.edu"},
        {"user%40emailaddress.com", "someCoolerFruitOrSomething", "server.com"},});
  }

  @Test
  public void testUrlCred() throws HTTPException {
    String url = "https://" + username + ":" + password + "@" + host + "/something/garbage.grb?query";
    logger.debug("Testing {}", url);
    try (HTTPMethod method = HTTPFactory.Get(url)) {
      HTTPSession session = method.getSession();
      method.close();
      session.setCredentialsProvider(new BasicCredentialsProvider());
      try (HTTPMethod method2 = HTTPFactory.Get(session, url)) {
        HTTPSession session2 = method2.getSession();
        CredentialsProvider credentialsProvider = session2.sessionprovider;
        assertThat(credentialsProvider).isNotNull();
        AuthScope expectedAuthScope = new AuthScope(host, 80);
        Credentials credentials = credentialsProvider.getCredentials(expectedAuthScope);
        assertThat(credentials).isNotNull();
        assertThat(credentials.getUserPrincipal().getName()).isEqualTo(username);
        assertThat(credentials.getPassword()).isEqualTo(password);
      }
    }
  }

  @Test
  public void testUrlCredDefaultProvider() throws HTTPException {
    String url = "https://" + username + ":" + password + "@" + host + "/something/garbage.grb?query";
    logger.debug("Testing {}", url);
    try (HTTPMethod method = HTTPFactory.Get(url)) {
      // we should have a default BasicCredentialProvider available, even though
      // we didn't call the factory with a specific provider
      HTTPSession session = method.getSession();
      CredentialsProvider credentialsProvider = session.sessionprovider;
      assertThat(credentialsProvider).isNotNull();
      AuthScope expectedAuthScope = new AuthScope(host, 80);
      Credentials credentials = credentialsProvider.getCredentials(expectedAuthScope);
      assertThat(credentials).isNotNull();
      assertThat(credentials.getUserPrincipal().getName()).isEqualTo(username);
      assertThat(credentials.getPassword()).isEqualTo(password);
    }
  }

  @After
  public void checkAllConnectionsClosed() {
    assertThat(HTTPSession.getActualConnections()).isEqualTo(actualConnectionsBefore);
  }
}
