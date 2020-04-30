/*
 * Copyright (C) 2016-2020 the original author or authors. 
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.viglet.shio.graphql;

import com.google.common.base.CaseFormat;
import com.viglet.shio.persistence.model.object.ShObject;
import com.viglet.shio.persistence.model.post.ShPost;
import com.viglet.shio.persistence.model.post.ShPostAttr;
import com.viglet.shio.persistence.model.post.type.ShPostType;
import com.viglet.shio.persistence.model.post.type.ShPostTypeAttr;
import com.viglet.shio.persistence.repository.object.ShObjectRepository;
import com.viglet.shio.persistence.repository.post.ShPostRepository;
import com.viglet.shio.persistence.repository.post.type.ShPostTypeAttrRepository;
import com.viglet.shio.persistence.repository.post.type.ShPostTypeRepository;
import com.viglet.shio.persistence.service.post.ShPostAttrService;
import com.viglet.shio.utils.ShPostUtils;

import graphql.GraphQL;
import graphql.scalars.ExtendedScalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static graphql.Scalars.GraphQLString;
import static graphql.Scalars.GraphQLID;
import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLList.list;
import static graphql.schema.GraphQLCodeRegistry.newCodeRegistry;
import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.schema.GraphqlTypeComparatorRegistry.BY_NAME_REGISTRY;

/**
 * GraphQL Provider.
 *
 * @author Alexandre Oliveira
 * @since 0.3.7
 */
@Component
public class ShGraphQL {

	@Autowired
	private ShPostTypeRepository shPostTypeRepository;

	@Autowired
	private ShObjectRepository shObjectRepository;

	@Autowired
	private ShPostRepository shPostRepository;

	@Autowired
	private ShPostTypeAttrRepository shPostTypeAttrRepository;
	@Autowired
	private ShPostUtils shPostUtils;

	@Autowired
	private ShPostAttrService shPostAttrService;

	private final static String QUERY_TYPE = "Query";

	private final static String SEARCH = "_search";
	private final static String AND = "AND";
	private final static String OR = "OR";
	private final static String NOT = "NOT";
	private final static String ID = "id";
	private final static String TITLE = "title";
	private final static String DESCRIPTION = "description";
	private final static String FURL = "furl";
	private final static String MODIFIER = "modifier";
	private final static String PUBLISHER = "publisher";
	private final static String FOLDER = "folder";
	private final static String CREATED_AT = "createdAt";
	private final static String UPDATED_AT = "updatedAt";
	private final static String PUBLISHED_AT = "publishedAt";

	private final static String CONDITION_EQUAL = "equal";
	private final static String CONDITION_NOT = "not";
	private final static String CONDITION_IN = "in";
	private final static String CONDITION_NOT_IN = "not_in";
	private final static String CONDITION_CONTAINS = "contains";
	private final static String CONDITION_NOT_CONTAINS = "not_contains";
	private final static String CONDITION_STARTS_WITH = "starts_with";
	private final static String CONDITION_NOT_STARTS_WITH = "not_starts_with";
	private final static String CONDITION_ENDS_WITH = "ends_with";
	private final static String CONDITION_NOT_ENDS_WITH = "not_ends_with";

	private final static String CONDITION_LT = "lt";
	private final static String CONDITION_LTE = "lte";
	private final static String CONDITION_GT = "gt";
	private final static String CONDITION_GTE = "gte";

	private final static String STAGE_ARG = "stage";
	private final static String LOCALES_ARG = "locales";
	private final static String WHERE_ARG = "where";

	private final static String WHERE_INPUT = "WhereInput";
	private final static String CONDITION_SEPARATOR = "_";

	private final static String FIELD_TYPE_GRAPHQL_ID = "GraphQLID";
	private final static String FIELD_TYPE_GRAPHQL_STRING = "GraphQLString";
	private final static String FIELD_TYPE_GRAPHQL_DATE_TIME = "DateTime";

	private GraphQL graphQL;

	public static GraphQLEnumType stageEnum = newEnum().name("Stage").description("Stage system enumeration")
			.value("PUBLISHED", 10, "System Published Stage.").value("DRAFT", 20, "System Draft Stage")
			.comparatorRegistry(BY_NAME_REGISTRY).build();

	public static GraphQLEnumType localeEnum = newEnum().name("Locale").description("Locale system enumeration")
			.value("en", "EN", "System Locale.").comparatorRegistry(BY_NAME_REGISTRY).build();

	private void createInputObjectField(GraphQLInputObjectType.Builder builder, String name, GraphQLInputType type,
			String description) {
		builder.field(newInputObjectField().name(name).description(description).type(type));
	}

	private void createInputObjectField(GraphQLInputObjectType.Builder builder, String name, String type,
			String description) {

		GraphQLScalarType scalarType = null;

		if (type.equals(FIELD_TYPE_GRAPHQL_DATE_TIME)) {
			scalarType = ExtendedScalars.DateTime;
			this.createInputObjectFieldCondition(builder, name, null, scalarType, description);
			this.createInputObjectFieldCondition(builder, name, CONDITION_NOT, scalarType,
					"All values that are not equal to given value.");
			this.createInputObjectFieldCondition(builder, name, CONDITION_IN, scalarType,
					"All values that are not contained in given list.");
			this.createInputObjectFieldCondition(builder, name, CONDITION_LT, scalarType,
					"All values less than the given value.");
			this.createInputObjectFieldCondition(builder, name, CONDITION_LTE, scalarType,
					"All values less than or equal the given value.");
			this.createInputObjectFieldCondition(builder, name, CONDITION_GT, scalarType,
					"All values greater than the given value.");
			this.createInputObjectFieldCondition(builder, name, CONDITION_GTE, scalarType,
					"All values greater than the given value.");
		} else if (type.equals(FIELD_TYPE_GRAPHQL_STRING) || type.equals(FIELD_TYPE_GRAPHQL_ID)) {
			if (type.equals(FIELD_TYPE_GRAPHQL_STRING))
				scalarType = GraphQLString;
			else if (type.equals(FIELD_TYPE_GRAPHQL_ID))
				scalarType = GraphQLID;

			this.createInputObjectFieldCondition(builder, name, null, scalarType, description);
			this.createInputObjectFieldCondition(builder, name, CONDITION_NOT, scalarType,
					"All values that are not equal to given value.");
			this.createInputObjectFieldCondition(builder, name, CONDITION_IN, scalarType,
					"All values that are contained in given list.");
			this.createInputObjectFieldCondition(builder, name, CONDITION_NOT_IN, scalarType,
					"All values that are not contained in given list.");
			this.createInputObjectFieldCondition(builder, name, CONDITION_CONTAINS, scalarType,
					"All values containing the given string.");
			this.createInputObjectFieldCondition(builder, name, CONDITION_NOT_CONTAINS, scalarType,
					"All values not containing the given string.");
			this.createInputObjectFieldCondition(builder, name, CONDITION_STARTS_WITH, scalarType,
					"All values starting with the given string.");
			this.createInputObjectFieldCondition(builder, name, CONDITION_NOT_STARTS_WITH, scalarType,
					"All values not starting with the given string.");
			this.createInputObjectFieldCondition(builder, name, CONDITION_ENDS_WITH, scalarType,
					"All values ending with the given string.");
			this.createInputObjectFieldCondition(builder, name, CONDITION_NOT_ENDS_WITH, scalarType,
					"All values not ending with the given string");
		}
	}

	private void createInputObjectFieldCondition(GraphQLInputObjectType.Builder builder, String name, String condition,
			GraphQLScalarType scalarType, String description) {
		builder.field(newInputObjectField()
				.name(condition == null ? name : String.format("%s%s%s", name, CONDITION_SEPARATOR, condition))
				.description(description).type(scalarType));
	}

	private GraphQLSchema loadSchema() {
		Builder queryTypeBuilder = newObject().name(QUERY_TYPE);
		graphql.schema.GraphQLCodeRegistry.Builder codeRegistryBuilder = newCodeRegistry();
		for (ShPostType shPostType : shPostTypeRepository.findAll()) {

			String postTypeName = this.getPostTypeName(shPostType);

			Builder builder = newObject().name(postTypeName).description(shPostType.getDescription());

			GraphQLInputObjectType.Builder postTypeWhereInputBuilder = newInputObject()
					.name(postTypeName.concat(WHERE_INPUT)).description("Identifies documents");

			GraphQLObjectType graphQLObjectType = this.postTypeFields(shPostType, builder, postTypeWhereInputBuilder);

			GraphQLInputObjectType postTypeWhereInput = postTypeWhereInputBuilder.comparatorRegistry(BY_NAME_REGISTRY)
					.build();
			this.allPosts(queryTypeBuilder, codeRegistryBuilder, shPostType, graphQLObjectType, postTypeWhereInput);

		}

		GraphQLObjectType queryType = queryTypeBuilder.comparatorRegistry(BY_NAME_REGISTRY).build();

		return GraphQLSchema.newSchema().query(queryType).codeRegistry(codeRegistryBuilder.build()).build();

	}

	private String getPostTypeName(ShPostType shPostType) {
		return this.normalizedName(shPostType.getName());
	}

	private void allPosts(Builder queryTypeBuilder, graphql.schema.GraphQLCodeRegistry.Builder codeRegistryBuilder,
			ShPostType shPostType, GraphQLObjectType graphQLObjectType, GraphQLInputObjectType postTypeWhereInput) {

		String postTypeName = this.getPostTypeName(shPostType);

		queryTypeBuilder.field(newFieldDefinition().name(postTypeName).type(list(graphQLObjectType))
				.argument(newArgument().name(STAGE_ARG)
						.description("A required enumeration indicating the current content Stage (defaults to DRAFT)")
						.type(nonNull(stageEnum)).defaultValue(20))
				.argument(newArgument().name(LOCALES_ARG)
						.description("A required array of one or more locales, defaults to the project's default.")
						.type(nonNull(list(localeEnum))).defaultValue("EN"))
				.argument(newArgument().name(WHERE_ARG)
						.description("An optional object type to filter the content based on a nested set of criteria.")
						.type(postTypeWhereInput)));

		codeRegistryBuilder.dataFetcher(coordinates(QUERY_TYPE, postTypeName), getPostTypeAllDataFetcher(shPostType));
	}

	private GraphQLObjectType postTypeFields(ShPostType shPostType, Builder builder,
			GraphQLInputObjectType.Builder postTypeWhereInputBuilder) {

		builder.field(newFieldDefinition().name(ID).description("Identifier").type(GraphQLID));
		builder.field(newFieldDefinition().name(TITLE).description("Title").type(GraphQLString));
		builder.field(newFieldDefinition().name(DESCRIPTION).description("Description").type(GraphQLString));
		builder.field(newFieldDefinition().name(FURL).description("Friendly URL").type(GraphQLString));
		builder.field(newFieldDefinition().name(MODIFIER).description("Modifier").type(GraphQLString));
		builder.field(newFieldDefinition().name(PUBLISHER).description("Publisher").type(GraphQLString));
		builder.field(newFieldDefinition().name(FOLDER).description("Folder Name").type(GraphQLString));

		for (ShPostTypeAttr shPostTypeAttr : shPostType.getShPostTypeAttrs()) {
			String postTypeAttrName = this.normalizedName(shPostTypeAttr.getName());
			builder.field(newFieldDefinition().name(postTypeAttrName).description(shPostTypeAttr.getDescription())
					.type(GraphQLString));
		}

		this.whereFields(shPostType, postTypeWhereInputBuilder);

		return builder.comparatorRegistry(BY_NAME_REGISTRY).build();
	}

	private void whereFields(ShPostType shPostType, GraphQLInputObjectType.Builder postTypeWhereInputBuilder) {

		String whereInputName = getPostTypeName(shPostType).concat(WHERE_INPUT);

		GraphQLTypeReference whereInputRef = GraphQLTypeReference.typeRef(whereInputName);

		this.createInputObjectField(postTypeWhereInputBuilder, SEARCH, GraphQLString,
				"Contains search across all appropriate fields.");
		this.createInputObjectField(postTypeWhereInputBuilder, AND, whereInputRef, "Logical AND on all given filters.");
		this.createInputObjectField(postTypeWhereInputBuilder, OR, whereInputRef, "Logical OR on all given filters.");
		this.createInputObjectField(postTypeWhereInputBuilder, NOT, whereInputRef,
				"Logical NOT on all given filters combined by AND.");

		this.createInputObjectField(postTypeWhereInputBuilder, ID, FIELD_TYPE_GRAPHQL_ID, "Identifier");
		this.createInputObjectField(postTypeWhereInputBuilder, TITLE, FIELD_TYPE_GRAPHQL_STRING, "Title");
		this.createInputObjectField(postTypeWhereInputBuilder, DESCRIPTION, FIELD_TYPE_GRAPHQL_STRING, "Description");
		this.createInputObjectField(postTypeWhereInputBuilder, FURL, FIELD_TYPE_GRAPHQL_STRING, "Friendly URL");
		this.createInputObjectField(postTypeWhereInputBuilder, MODIFIER, FIELD_TYPE_GRAPHQL_STRING, "Modifier");
		this.createInputObjectField(postTypeWhereInputBuilder, PUBLISHER, FIELD_TYPE_GRAPHQL_STRING, "Publisher");
		this.createInputObjectField(postTypeWhereInputBuilder, FOLDER, FIELD_TYPE_GRAPHQL_STRING, "Folder Name");
		this.createInputObjectField(postTypeWhereInputBuilder, CREATED_AT, FIELD_TYPE_GRAPHQL_DATE_TIME,
				"Created Date");
		this.createInputObjectField(postTypeWhereInputBuilder, UPDATED_AT, FIELD_TYPE_GRAPHQL_DATE_TIME,
				"Updated Date");
		this.createInputObjectField(postTypeWhereInputBuilder, PUBLISHED_AT, FIELD_TYPE_GRAPHQL_DATE_TIME,
				"Published Date");

		for (ShPostTypeAttr shPostTypeAttr : shPostType.getShPostTypeAttrs()) {

			String postTypeAttrName = this.normalizedName(shPostTypeAttr.getName());

			this.createInputObjectField(postTypeWhereInputBuilder, postTypeAttrName, FIELD_TYPE_GRAPHQL_STRING,
					shPostTypeAttr.getDescription());

		}
	}

	private String normalizedName(String object) {
		String objectName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL,
				object.toLowerCase().replaceAll("-", "_"));
		return objectName;
	}

	private DataFetcher<List<Map<String, String>>> getPostTypeAllDataFetcher(ShPostType shPostType) {
		return dataFetchingEnvironment -> {
			List<Map<String, String>> posts = new ArrayList<>();

			Map<String, Object> whereMap = dataFetchingEnvironment.getArgument(WHERE_ARG);

			if (whereMap != null) {
				for (Entry<String, Object> whereArgItem : whereMap.entrySet()) {
					String arg = whereArgItem.getKey();
					if (arg.equals(SEARCH)) {

					} else if (arg.equals(SEARCH)) {

					} else if (arg.equals(AND)) {

					} else if (arg.equals(OR)) {

					} else if (arg.equals(NOT)) {

					} else if (arg.equals(ID)) {
						String objectId = whereMap.get(ID).toString();
						ShObject shObject = shObjectRepository.findById(objectId).orElse(null);
						Map<String, String> postAttrs = shPostUtils.postAttrGraphQL((ShPost) shObject);
						posts.add(postAttrs);
					} else if (arg.equals(TITLE)) {
						List<ShPost> shPosts = shPostRepository.findByTitle(whereMap.get(TITLE).toString());
						for (ShPost shPost : shPosts) {
							Map<String, String> postAttrs = shPostUtils.postAttrGraphQL(shPost);
							posts.add(postAttrs);
						}
					} else if (arg.equals(DESCRIPTION)) {
						List<ShPost> shPosts = shPostRepository.findBySummary(whereMap.get(DESCRIPTION).toString());
						for (ShPost shPost : shPosts) {
							Map<String, String> postAttrs = shPostUtils.postAttrGraphQL(shPost);
							posts.add(postAttrs);
						}
					} else if (arg.equals(FURL)) {
						List<ShPost> shPosts = shPostRepository.findByFurl(whereMap.get(FURL).toString());
						for (ShPost shPost : shPosts) {
							Map<String, String> postAttrs = shPostUtils.postAttrGraphQL(shPost);
							posts.add(postAttrs);
						}
					} else if (arg.equals(MODIFIER)) {
						List<ShPost> shPosts = shPostRepository.findByModifier(whereMap.get(MODIFIER).toString());
						for (ShPost shPost : shPosts) {
							Map<String, String> postAttrs = shPostUtils.postAttrGraphQL(shPost);
							posts.add(postAttrs);
						}
					} else if (arg.equals(PUBLISHER)) {
						List<ShPost> shPosts = shPostRepository.findByPublisher(whereMap.get(PUBLISHER).toString());
						for (ShPost shPost : shPosts) {
							Map<String, String> postAttrs = shPostUtils.postAttrGraphQL(shPost);
							posts.add(postAttrs);
						}
					} else if (arg.equals(FOLDER)) {
						List<ShPost> shPosts = shPostRepository.findByShFolder_Name(whereMap.get(FOLDER).toString());
						for (ShPost shPost : shPosts) {
							Map<String, String> postAttrs = shPostUtils.postAttrGraphQL(shPost);
							posts.add(postAttrs);
						}
					} else {
						String field = arg;
						String action = CONDITION_EQUAL;
						if (arg.contains(CONDITION_SEPARATOR)) {
							field = arg.split(CONDITION_SEPARATOR)[0];
							action = arg.replaceFirst(field.concat(CONDITION_SEPARATOR), "");
						}
						this.fieldWhereCondition(shPostType, posts, whereArgItem, field, action);
					}
				}
			} else {
				List<ShPost> shPosts = shPostRepository.findByShPostType(shPostType);
				for (ShPost shPost : shPosts) {
					Map<String, String> postAttrs = shPostUtils.postAttrGraphQL(shPost);
					posts.add(postAttrs);
				}
			}
			return posts;
		};
	}

	private void fieldWhereCondition(ShPostType shPostType, List<Map<String, String>> posts,
			Entry<String, Object> whereArgItem, String field, String action) {
		ShPostTypeAttr shPostTypeAttr = shPostTypeAttrRepository.findByShPostTypeAndName(shPostType,
				field.toUpperCase());
		List<ShPostAttr> shPostAttrs = shPostAttrService.findByShPostTypeAttrAndValueAndCondition(shPostTypeAttr,
				whereArgItem.getValue().toString(), action);
		for (ShPostAttr shPostAttr : shPostAttrs) {
			Map<String, String> postAttrsDefault = shPostUtils.postAttrGraphQL(shPostAttr.getShPost());
			posts.add(postAttrsDefault);
		}
	}

	@PostConstruct
	public void init() throws IOException {
		GraphQLSchema graphQLSchema = this.loadSchema();
		this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
	}

	@Bean
	public GraphQL graphQL() {
		return graphQL;
	}

}