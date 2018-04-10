/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.test.io;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.CsvReader;
import org.apache.flink.test.io.csv.custom.type.NestedCustomJsonType;
import org.apache.flink.test.io.csv.custom.type.complex.GenericsAwareCustomJsonType;
import org.apache.flink.types.IntValue;
import org.apache.flink.types.StringValue;
import org.apache.flink.types.parser.FieldParser;
import org.apache.flink.types.parser.ParserFactory;
import org.junit.Test;

/**
 * A collection of tests for checking different approaches of operating over user-defined Row types,
 * utilizing newly introduced CsvReader methods.
 * his class is aimed to verify use cases of classes with Java Generics.
 */
public class CsvReaderCustomGenericsAwareRowTupleIT extends CsvReaderCustomTypeTest {

	public CsvReaderCustomGenericsAwareRowTupleIT(TestExecutionMode mode) {
		super(mode);
	}


	@Test
	public void testCustomGenericsAwareRowTypeViaRowTypeMethod() throws Exception {
		givenCsvSourceData("1,'column2','{\"f1\":5, \"f2\": {\"f21\":\"nested_simple_f21\"}, \"f3\": {\"f21\":\"nested_generic_f31\"}}'\n");
		givenCsvReaderConfigured();

		whenCustomTypesAreRegisteredAlongWithTheirParsers();
		whenDataSourceCreatedViaRowTypeMethod();
		whenProcessingExecutedToCollectResultTuples();

		thenResultingTupleHasExpectedHierarchyAndFieldValues();
	}

	@Test
	public void testCustomGenericsAwareRowTypeViaPreciseRowTypeMethod() throws Exception {
		givenCsvSourceData("1,'column2','{\"f1\":5, \"f2\": {\"f21\":\"nested_simple_f21\"}, \"f3\": {\"f21\":\"nested_generic_f31\"}}'\n");
		givenCsvReaderConfigured();

		whenCustomTypesAreRegisteredAlongWithTheirParsers();
		whenDataSourceCreatedViaPreciseRowTypeMethod();
		whenProcessingExecutedToCollectResultTuples();

		thenResultingTupleHasExpectedHierarchyAndFieldValues();
	}

	private void givenCsvSourceData(String sourceData) {
		context.sourceData = sourceData;
	}

	private void givenCsvReaderConfigured() throws Exception {
		final String dataPath = createInputData(tempFolder, context.sourceData);
		final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
		CsvReader reader = env.readCsvFile(dataPath);
		reader.fieldDelimiter(",");
		reader.parseQuotedStrings('\'');
		context.reader = reader;
	}

	private void whenCustomTypesAreRegisteredAlongWithTheirParsers() {
		ParserFactory<NestedCustomJsonType> factoryForNestedCustomType = new NestedCustomJsonParserFactory();
		FieldParser.registerCustomParser(NestedCustomJsonType.class, factoryForNestedCustomType);

		TypeHint<GenericsAwareCustomJsonType<NestedCustomJsonType>> typeHint = new TypeHint<GenericsAwareCustomJsonType<NestedCustomJsonType>>() {
		};
		TypeInformation<GenericsAwareCustomJsonType<NestedCustomJsonType>> typeInfo = TypeInformation.of(typeHint);
		Class<GenericsAwareCustomJsonType<NestedCustomJsonType>> type = typeInfo.getTypeClass();

		ParserFactory<GenericsAwareCustomJsonType<NestedCustomJsonType>> factoryForGenericType = new GenericsAwareCustomJsonParserFactory<>(
			new TypeReference<GenericsAwareCustomJsonType<NestedCustomJsonType>>() {
			}
		);
		FieldParser.registerCustomParser(type, factoryForGenericType);
	}

	private void whenDataSourceCreatedViaRowTypeMethod() {
		context.dataSource = context.reader.rowType(IntValue.class, StringValue.class, GenericsAwareCustomJsonType.class);
	}

	private void whenDataSourceCreatedViaPreciseRowTypeMethod() {
		context.dataSource = context.reader.preciseRowType(
			BasicTypeInfo.INT_TYPE_INFO,
			BasicTypeInfo.STRING_TYPE_INFO,
			TypeInformation.of(new TypeHint<GenericsAwareCustomJsonType<NestedCustomJsonType>>() {
			})
		);
	}

	private void whenProcessingExecutedToCollectResultTuples() throws Exception {
		context.result = context.dataSource.collect();
	}

	private void thenResultingTupleHasExpectedHierarchyAndFieldValues() {
		compareResultAsText(
			context.result,
			"1,column2,GenericsAwareCustomJsonType{f1='5', f2=NestedCustomJsonType{f21='nested_simple_f21'}, f3=NestedCustomJsonType{f21='nested_generic_f31'}}"
		);
	}

}
