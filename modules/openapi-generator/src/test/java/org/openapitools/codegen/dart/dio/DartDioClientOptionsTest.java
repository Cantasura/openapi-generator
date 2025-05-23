/*
 * Copyright 2021 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.dart.dio;

import org.openapitools.codegen.AbstractOptionsTest;
import org.openapitools.codegen.CodegenConfig;
import org.openapitools.codegen.languages.DartDioClientCodegen;
import org.openapitools.codegen.options.DartDioClientOptionsProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DartDioClientOptionsTest extends AbstractOptionsTest {
    private DartDioClientCodegen clientCodegen = mock(DartDioClientCodegen.class, mockSettings);

    public DartDioClientOptionsTest() {
        super(new DartDioClientOptionsProvider());
    }

    @Override
    protected CodegenConfig getCodegenConfig() {
        return clientCodegen;
    }

    @SuppressWarnings("unused")
    @Override
    protected void verifyOptions() {
        verify(clientCodegen).setSortParamsByRequiredFlag(Boolean.valueOf(DartDioClientOptionsProvider.SORT_PARAMS_VALUE));
        verify(clientCodegen).setPubLibrary(DartDioClientOptionsProvider.PUB_LIBRARY_VALUE);
        verify(clientCodegen).setPubName(DartDioClientOptionsProvider.PUB_NAME_VALUE);
        verify(clientCodegen).setPubVersion(DartDioClientOptionsProvider.PUB_VERSION_VALUE);
        verify(clientCodegen).setPubDescription(DartDioClientOptionsProvider.PUB_DESCRIPTION_VALUE);
        verify(clientCodegen).setPubAuthor(DartDioClientOptionsProvider.PUB_AUTHOR_VALUE);
        verify(clientCodegen).setPubAuthorEmail(DartDioClientOptionsProvider.PUB_AUTHOR_EMAIL_VALUE);
        verify(clientCodegen).setPubHomepage(DartDioClientOptionsProvider.PUB_HOMEPAGE_VALUE);
        verify(clientCodegen).setPubRepository(DartDioClientOptionsProvider.PUB_REPOSITORY_VALUE);
        verify(clientCodegen).setPubPublishTo(DartDioClientOptionsProvider.PUB_PUBLISH_TO_VALUE);
        verify(clientCodegen).setSourceFolder(DartDioClientOptionsProvider.SOURCE_FOLDER_VALUE);
        verify(clientCodegen).setUseEnumExtension(Boolean.parseBoolean(DartDioClientOptionsProvider.USE_ENUM_EXTENSION));
        verify(clientCodegen).setDateLibrary(DartDioClientCodegen.DATE_LIBRARY_DEFAULT);
        verify(clientCodegen).setLibrary(DartDioClientCodegen.SERIALIZATION_LIBRARY_DEFAULT);
        verify(clientCodegen).setEqualityCheckMethod(DartDioClientCodegen.EQUALITY_CHECK_METHOD_DEFAULT);
        verify(clientCodegen).setUseLowerCamelCase(Boolean.parseBoolean(DartDioClientOptionsProvider.USE_LOWER_CAMEL_CASE));
        verify(clientCodegen).setEnumUnknownDefaultCase(Boolean.parseBoolean(DartDioClientOptionsProvider.ENUM_UNKNOWN_DEFAULT_CASE_VALUE));
    }
}
