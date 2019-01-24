/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.relational.core.mapping;

import static org.assertj.core.api.Assertions.*;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.PropertyHandler;

/**
 * Unit tests for the {@link BasicRelationalPersistentProperty}.
 *
 * @author Jens Schauder
 * @author Oliver Gierke
 * @author Florian Lüdiger
 * @author Bastian Wilhelm
 */
public class BasicRelationalPersistentPropertyUnitTests {

	RelationalMappingContext context = new RelationalMappingContext();
	RelationalPersistentEntity<?> entity = context.getRequiredPersistentEntity(DummyEntity.class);

	@Test // DATAJDBC-104
	public void enumGetsStoredAsString() {

		RelationalPersistentEntity<?> persistentEntity = context.getRequiredPersistentEntity(DummyEntity.class);

		entity.doWithProperties((PropertyHandler<RelationalPersistentProperty>) p -> {
			switch (p.getName()) {
				case "someEnum":
					assertThat(p.getColumnType()).isEqualTo(String.class);
					break;
				case "localDateTime":
					assertThat(p.getColumnType()).isEqualTo(Date.class);
					break;
				case "zonedDateTime":
					assertThat(p.getColumnType()).isEqualTo(String.class);
					break;
				default:
			}
		});
	}

	@Test // DATAJDBC-104, DATAJDBC-1384
	public void testTargetTypesForPropertyType() {

		SoftAssertions softly = new SoftAssertions();

		checkTargetType(softly, entity, "someEnum", String.class);
		checkTargetType(softly, entity, "localDateTime", Date.class);
		checkTargetType(softly, entity, "zonedDateTime", String.class);
		checkTargetType(softly, entity, "uuid", UUID.class);

		softly.assertAll();
	}

	@Test // DATAJDBC-106
	public void detectsAnnotatedColumnName() {

		assertThat(entity.getRequiredPersistentProperty("name").getColumnName()).isEqualTo("dummy_name");
		assertThat(entity.getRequiredPersistentProperty("localDateTime").getColumnName())
				.isEqualTo("dummy_last_updated_at");
	}

	@Test // DATAJDBC-218
	public void detectsAnnotatedColumnAndKeyName() {

		RelationalPersistentProperty listProperty = entity.getRequiredPersistentProperty("someList");

		assertThat(listProperty.getReverseColumnName()).isEqualTo("dummy_column_name");
		assertThat(listProperty.getKeyColumn()).isEqualTo("dummy_key_column_name");
	}

	@Test // DATAJDBC-111
	public void detectsEmbeddedEntity() {

		final RelationalPersistentEntity<?> requiredPersistentEntity = context
				.getRequiredPersistentEntity(DummyEntity.class);

		SoftAssertions softly = new SoftAssertions();

		BiConsumer<String, String> checkEmbedded = (name, prefix) -> {

			RelationalPersistentProperty property = requiredPersistentEntity.getRequiredPersistentProperty(name);

			softly.assertThat(property.isEmbedded()) //
					.describedAs(name + " is embedded") //
					.isEqualTo(prefix != null);

			softly.assertThat(property.getEmbeddedPrefix()) //
					.describedAs(name + " prefix") //
					.isEqualTo(prefix);
		};

		checkEmbedded.accept("someList", null);
		checkEmbedded.accept("id", null);
		checkEmbedded.accept("embeddableEntity", "");
		checkEmbedded.accept("prefixedEmbeddableEntity", "prefix");

		softly.assertAll();
	}

	@Test // DATAJDBC-259
	public void classificationOfCollectionLikeProperties() {

		RelationalPersistentProperty listOfString = entity.getRequiredPersistentProperty("listOfString");
		RelationalPersistentProperty arrayOfString = entity.getRequiredPersistentProperty("arrayOfString");
		RelationalPersistentProperty listOfEntity = entity.getRequiredPersistentProperty("listOfEntity");
		RelationalPersistentProperty arrayOfEntity = entity.getRequiredPersistentProperty("arrayOfEntity");

		SoftAssertions softly = new SoftAssertions();

		softly.assertThat(listOfString.isCollectionOfSimpleTypeLike())
				.describedAs("listOfString is a Collection of a simple type.").isEqualTo(true);
		softly.assertThat(arrayOfString.isCollectionOfSimpleTypeLike())
				.describedAs("arrayOfString is a Collection of a simple type.").isTrue();
		softly.assertThat(listOfEntity.isCollectionOfSimpleTypeLike())
				.describedAs("listOfEntity  is a Collection of a simple type.").isFalse();
		softly.assertThat(arrayOfEntity.isCollectionOfSimpleTypeLike())
				.describedAs("arrayOfEntity is a Collection of a simple type.").isFalse();

		BiConsumer<RelationalPersistentProperty, String> checkEitherOr = (p, s) -> softly
				.assertThat(p.isCollectionOfSimpleTypeLike()).describedAs(s + " contains either simple types or entities")
				.isNotEqualTo(p.isCollectionOfEntitiesLike());

		checkEitherOr.accept(listOfString,"listOfString");
		checkEitherOr.accept(arrayOfString,"arrayOfString");
		checkEitherOr.accept(listOfEntity,"listOfEntity");
		checkEitherOr.accept(arrayOfEntity,"arrayOfEntity");

		softly.assertThat(arrayOfString.getColumnType()).isEqualTo(String[].class);
		softly.assertThat(listOfString.getColumnType()).isEqualTo(String[].class);

		softly.assertAll();
	}

	private void checkTargetType(SoftAssertions softly, RelationalPersistentEntity<?> persistentEntity,
			String propertyName, Class<?> expected) {

		RelationalPersistentProperty property = persistentEntity.getRequiredPersistentProperty(propertyName);

		softly.assertThat(property.getColumnType()).describedAs(propertyName).isEqualTo(expected);
	}

	@Data
	@SuppressWarnings("unused")
	private static class DummyEntity {

		@Id private final Long id;
		private final SomeEnum someEnum;
		private final LocalDateTime localDateTime;
		private final ZonedDateTime zonedDateTime;
		private final UUID uuid;

		// DATAJDBC-259
		private final List<String> listOfString;
		private final String[] arrayOfString;
		private final List<OtherEntity> listOfEntity;
		private final OtherEntity[] arrayOfEntity;

		@Column(value = "dummy_column_name", keyColumn = "dummy_key_column_name") private List<Integer> someList;

		// DATACMNS-106
		private @Column("dummy_name") String name;

		// DATAJDBC-111
		private @Embedded EmbeddableEntity embeddableEntity;

		// DATAJDBC-111
		private @Embedded("prefix") EmbeddableEntity prefixedEmbeddableEntity;

		@Column("dummy_last_updated_at")
		public LocalDateTime getLocalDateTime() {
			return localDateTime;
		}

		public void setListSetter(Integer integer) {

		}

		public List<Date> getListGetter() {
			return null;
		}
	}

	@SuppressWarnings("unused")
	private enum SomeEnum {
		ALPHA
	}

	// DATAJDBC-111
	@Data
	private static class EmbeddableEntity {
		private final String embeddedTest;
	}

	@SuppressWarnings("unused")
	private static class OtherEntity {}
}
