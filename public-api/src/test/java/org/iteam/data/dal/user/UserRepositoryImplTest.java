package org.iteam.data.dal.user;

import org.assertj.core.util.Lists;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.iteam.configuration.ExternalConfigurationProperties;
import org.iteam.data.dal.client.ElasticsearchClientImpl;
import org.iteam.data.model.Nationalities;
import org.iteam.data.model.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

public class UserRepositoryImplTest {

	@InjectMocks
	private UserRepositoryImpl underTest;

	@Mock
	private ElasticsearchClientImpl elasticsearchClient;

	@Mock
	private ExternalConfigurationProperties configuration;

	private static final String USER_AS_JSON = "{\"username\":\"iteam\",\"password\":\"005f6e6f2dda2\",\"name\":\"iteamProject\"}";
	private static final String NATIONALITIES_AS_JSON = "{\"nationalities\":[\"Argentina\", \"Germany\", \"Brazil\"]}";
	private User user;
	private String username;
	private boolean flag;
	private Nationalities nationalities;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void getUserSuccessful() {
		givenAnUsername();
		givenAnElasticsearchResponseOk();
		whenGetUserIsCalled();
		thenUserIsOk();
	}

	@Test
	public void getUserNotSuccessfulResponseNull() {
		givenAnUsername();
		givenAnElasticsearchResponseNull();
		whenGetUserIsCalled();
		thenUserIsNull();
	}

	@Test
	public void getUserNotSuccessfulResponseFailure() {
		givenAnUsername();
		givenAnElasticsearchResponseFailure();
		whenGetUserIsCalled();
		thenUserIsNull();
	}

	@Test
	public void setUserSuccessful() {
		givenANewUser();
		givenAnElasticsearchIndexResponseOk();
		whenSetUserIsCalled();
		thenUserWasInserted();
	}

	@Test
	public void setUserNotSuccessfulResponseNull() {
		givenANewUser();
		givenAnElasticsearchIndexResponseNull();
		whenSetUserIsCalled();
		thenUserWasntInserted();
	}

	@Test
	public void setUserNotSuccessfulFailure() {
		givenANewUser();
		givenAnElasticsearchIndexResponseFailure();
		whenSetUserIsCalled();
		thenUserWasntInserted();
	}

	@Test
	public void checkUserExitenceSuccessful() {
		givenAnUsername();
		givenAnElasticsearchGetResponseOk();
		whenCheckUserExistenceIsCalled();
		thenUserExists();
	}

	@Test
	public void checkUserExistenceNotSuccessfulNull() {
		givenAnUsername();
		givenAnElasticsearchGetResponseNull();
		whenCheckUserExistenceIsCalled();
		thenUserNotExists();
	}

	@Test
	public void checkUserExistenceResponseFailure() {
		givenAnUsername();
		givenAnElasticsearchGetResponseFailure();
		whenCheckUserExistenceIsCalled();
		thenUserNotExists();
	}

	@Test
	public void insertNationalitiesSuccessful() {
		givenNationalities();
		givenAnElasticsearchIndexResponseOk();
		whenInsertNationalitiesIsCalled();
		thenNationalitiesWereCreated();
	}

	@Test
	public void insertNationalitiesNotSuccessfulNull() {
		givenNationalities();
		givenAnElasticsearchIndexResponseNull();
		whenInsertNationalitiesIsCalled();
		thenNationalitiesWerentCreated();
	}

	@Test
	public void insertNationalitiesNotSuccessfulFailure() {
		givenNationalities();
		givenAnElasticsearchIndexResponseFailure();
		whenInsertNationalitiesIsCalled();
		thenNationalitiesWerentCreated();
	}

	@Test
	public void getNationalitiesSuccessful() {
		givenAnElasticsearchGetResponseNatOk();
		whenGetNationalitiesIsCalled();
		thenNationalitiesWere3();
	}

	@Test
	public void getNationalitiesNotSuccessfulNull() {
		givenAnElasticsearchGetResponseNull();
		whenGetNationalitiesIsCalled();
		thenNationalitiesWereNull();
	}

	@Test
	public void getNationalitiesNotSuccessfulFailure() {
		givenAnElasticsearchGetResponseFailure();
		whenGetNationalitiesIsCalled();
		thenNationalitiesWereNull();
	}

	private void thenNationalitiesWereNull() {
		Assert.assertNull(nationalities);
	}

	private void thenNationalitiesWere3() {
		Assert.assertEquals(3, nationalities.getNationalities().size());
		Assert.assertTrue(nationalities.getNationalities().contains("Argentina"));
		Assert.assertTrue(nationalities.getNationalities().contains("Germany"));
		Assert.assertTrue(nationalities.getNationalities().contains("Brazil"));
	}

	private void whenGetNationalitiesIsCalled() {
		nationalities = underTest.getNationalities();
	}

	private void givenAnElasticsearchGetResponseNatOk() {
		GetResponse response = Mockito.mock(GetResponse.class);

		Mockito.when(response.getSourceAsString()).thenReturn(NATIONALITIES_AS_JSON);

		Mockito.when(response.isExists()).thenReturn(true);

		Mockito.when(elasticsearchClient.getDocument(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(response);

		ReflectionTestUtils.setField(underTest, "elasticsearchClient", elasticsearchClient);
	}

	private void thenNationalitiesWerentCreated() {
		Assert.assertFalse(flag);
	}

	private void thenNationalitiesWereCreated() {
		Assert.assertTrue(flag);
	}

	private void whenInsertNationalitiesIsCalled() {
		flag = underTest.insertNationalities(nationalities);
	}

	private void givenNationalities() {
		nationalities = new Nationalities();
		nationalities.setNationalities(Lists.newArrayList("Argentina", "Germany", "Brazil"));
	}

	private void givenAnElasticsearchGetResponseFailure() {
		givenAnElasticsearchGetResponse(false);
	}

	private void thenUserNotExists() {
		Assert.assertFalse(flag);
	}

	private void givenAnElasticsearchGetResponseNull() {
		Mockito.when(elasticsearchClient.getDocument(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(null);

		ReflectionTestUtils.setField(underTest, "elasticsearchClient", elasticsearchClient);
	}

	private void thenUserExists() {
		Assert.assertTrue(flag);
	}

	private void whenCheckUserExistenceIsCalled() {
		flag = underTest.checkUserExistance(username);
	}

	private void givenAnElasticsearchGetResponseOk() {
		givenAnElasticsearchGetResponse(true);
	}

	private void givenAnElasticsearchIndexResponseFailure() {
		givenAnElasticsearchIndexResponse(false);
	}

	private void thenUserWasntInserted() {
		Assert.assertFalse(flag);
	}

	private void givenAnElasticsearchIndexResponseNull() {
		Mockito.when(elasticsearchClient.insertData(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyString())).thenReturn(null);

		ReflectionTestUtils.setField(underTest, "elasticsearchClient", elasticsearchClient);
	}

	private void thenUserWasInserted() {
		Assert.assertTrue(flag);
	}

	private void whenSetUserIsCalled() {
		flag = underTest.setUser(user);
	}

	private void givenAnElasticsearchIndexResponseOk() {
		givenAnElasticsearchIndexResponse(true);
	}

	private void givenANewUser() {
		user = new User();
		user.setUsername("iteam");
		user.setPassword("admin");
	}

	private void givenAnElasticsearchResponseFailure() {
		SearchResponse response = Mockito.mock(SearchResponse.class);
		SearchHits searchHits = Mockito.mock(SearchHits.class);

		Mockito.when(response.getHits()).thenReturn(searchHits);
		Mockito.when(searchHits.getTotalHits()).thenReturn(0l);

		Mockito.when(elasticsearchClient.search(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject()))
				.thenReturn(response);

		ReflectionTestUtils.setField(underTest, "elasticsearchClient", elasticsearchClient);
	}

	private void thenUserIsNull() {
		Assert.assertNull(user);
	}

	private void givenAnElasticsearchResponseNull() {
		Mockito.when(elasticsearchClient.search(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject()))
				.thenReturn(null);

		ReflectionTestUtils.setField(underTest, "elasticsearchClient", elasticsearchClient);
	}

	private void givenAnUsername() {
		username = "iteam";
	}

	private void thenUserIsOk() {
		Assert.assertEquals(username, user.getUsername());
		Assert.assertEquals("iteamProject", user.getName());
	}

	private void whenGetUserIsCalled() {
		user = underTest.getUser(username);
	}

	private void givenAnElasticsearchResponseOk() {
		SearchResponse response = Mockito.mock(SearchResponse.class);
		SearchHits searchHits = Mockito.mock(SearchHits.class);
		SearchHit hit = Mockito.mock(SearchHit.class);

		Mockito.when(response.getHits()).thenReturn(searchHits);
		Mockito.when(searchHits.getTotalHits()).thenReturn(1l);
		Mockito.when(searchHits.getAt(0)).thenReturn(hit);
		Mockito.when(hit.getSourceAsString()).thenReturn(USER_AS_JSON);

		Mockito.when(elasticsearchClient.search(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject()))
				.thenReturn(response);

		ReflectionTestUtils.setField(underTest, "elasticsearchClient", elasticsearchClient);
	}

	private void givenAnElasticsearchIndexResponse(boolean flag) {
		IndexResponse response = Mockito.mock(IndexResponse.class);

		Mockito.when(response.isCreated()).thenReturn(flag);

		Mockito.when(elasticsearchClient.insertData(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyString())).thenReturn(response);

		ReflectionTestUtils.setField(underTest, "elasticsearchClient", elasticsearchClient);
	}

	private void givenAnElasticsearchGetResponse(boolean flag) {
		GetResponse response = Mockito.mock(GetResponse.class);

		Mockito.when(response.isExists()).thenReturn(flag);

		Mockito.when(elasticsearchClient.getDocument(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(response);

		ReflectionTestUtils.setField(underTest, "elasticsearchClient", elasticsearchClient);
	}

}
