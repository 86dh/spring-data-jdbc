/*
 * Copyright 2017-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jdbc.repository;

import static org.assertj.core.api.Assertions.*;

import junit.framework.AssertionFailedError;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactory;
import org.springframework.data.jdbc.testing.TestConfiguration;
import org.springframework.data.relational.core.mapping.event.BeforeConvertEvent;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Very simple use cases for creation and usage of JdbcRepositories.
 *
 * @author Jens Schauder
 * @author Chirag Tailor
 */
@ContextConfiguration
@Transactional
@ActiveProfiles("hsql")
@ExtendWith(SpringExtension.class)
public class JdbcRepositoryWithCollectionsAndManuallyAssignedIdHsqlIntegrationTests {

	static AtomicLong id = new AtomicLong(0);

	@Configuration
	@Import(TestConfiguration.class)
	static class Config {

		@Autowired JdbcRepositoryFactory factory;

		@Bean
		Class<?> testClass() {
			return JdbcRepositoryWithCollectionsAndManuallyAssignedIdHsqlIntegrationTests.class;
		}

		@Bean
		DummyEntityRepository dummyEntityRepository() {
			return factory.getRepository(DummyEntityRepository.class);
		}

		@Bean
		public ApplicationListener<?> idSetting() {

			return (ApplicationListener<BeforeConvertEvent>) event -> {

				if (event.getEntity() instanceof DummyEntity) {
					setIds((DummyEntity) event.getEntity());
				}
			};
		}

		private void setIds(DummyEntity dummyEntity) {

			if (dummyEntity.getId() == null) {
				dummyEntity.setId(id.incrementAndGet());
			}

		}
	}

	@Autowired NamedParameterJdbcTemplate template;
	@Autowired DummyEntityRepository repository;

	@Test // DATAJDBC-113
	public void saveAndLoadEmptySet() {

		DummyEntity entity = repository.save(createDummyEntity());

		assertThat(entity.id).isNotNull();

		DummyEntity reloaded = repository.findById(entity.id).orElseThrow(AssertionFailedError::new);

		assertThat(reloaded.content) //
				.isNotNull() //
				.isEmpty();
	}

	@Test // DATAJDBC-113
	public void saveAndLoadNonEmptySet() {

		Element element1 = new Element();
		Element element2 = new Element();

		DummyEntity entity = createDummyEntity();
		entity.content.add(element1);
		entity.content.add(element2);

		entity = repository.save(entity);

		assertThat(entity.id).isNotNull();
		assertThat(entity.content).allMatch(element -> element.id != null);

		DummyEntity reloaded = repository.findById(entity.id).orElseThrow(AssertionFailedError::new);

		assertThat(reloaded.content) //
				.isNotNull() //
				.extracting(e -> e.id) //
				.containsExactlyInAnyOrder(element1.id, element2.id);
	}

	@Test // DATAJDBC-113
	public void findAllLoadsCollection() {

		Element element1 = new Element();
		Element element2 = new Element();

		DummyEntity entity = createDummyEntity();
		entity.content.add(element1);
		entity.content.add(element2);

		entity = repository.save(entity);

		assertThat(entity.id).isNotNull();
		assertThat(entity.content).allMatch(element -> element.id != null);

		Iterable<DummyEntity> reloaded = repository.findAll();

		assertThat(reloaded) //
				.extracting(e -> e.id, e -> e.content.size()) //
				.containsExactly(tuple(entity.id, entity.content.size()));
	}

	@Test // DATAJDBC-113
	public void updateSet() {

		Element element1 = createElement("one");
		Element element2 = createElement("two");
		Element element3 = createElement("three");

		DummyEntity entity = createDummyEntity();
		entity.content.add(element1);
		entity.content.add(element2);

		entity = repository.save(entity);

		entity.content.remove(element1);
		element2.content = "two changed";
		entity.content.add(element3);

		entity = repository.save(entity);

		assertThat(entity.id).isNotNull();
		assertThat(entity.content).allMatch(element -> element.id != null);

		DummyEntity reloaded = repository.findById(entity.id).orElseThrow(AssertionFailedError::new);

		// the elements got properly updated and reloaded
		assertThat(reloaded.content) //
				.isNotNull() //
				.extracting(e -> e.id, e -> e.content) //
				.containsExactlyInAnyOrder( //
						tuple(element2.id, "two changed"), //
						tuple(element3.id, "three") //
				);

		Long count = template.queryForObject("select count(1) from Element", new HashMap<>(), Long.class);
		assertThat(count).isEqualTo(2);
	}

	@Test // DATAJDBC-113
	public void deletingWithSet() {

		Element element1 = createElement("one");
		Element element2 = createElement("two");

		DummyEntity entity = createDummyEntity();
		entity.content.add(element1);
		entity.content.add(element2);

		entity = repository.save(entity);

		repository.deleteById(entity.id);

		assertThat(repository.findById(entity.id)).isEmpty();

		Long count = template.queryForObject("select count(1) from Element", new HashMap<>(), Long.class);
		assertThat(count).isEqualTo(0);
	}

	private Element createElement(String content) {

		Element element = new Element();
		element.content = content;
		return element;
	}

	private static DummyEntity createDummyEntity() {

		DummyEntity entity = new DummyEntity();
		entity.setName("Entity Name");
		return entity;
	}

	interface DummyEntityRepository extends CrudRepository<DummyEntity, Long> {}

	@Data
	static class DummyEntity {

		@Id private Long id;
		String name;
		Set<Element> content = new HashSet<>();

	}

	@RequiredArgsConstructor
	static class Element {

		@Id private Long id;
		String content;
	}

}
