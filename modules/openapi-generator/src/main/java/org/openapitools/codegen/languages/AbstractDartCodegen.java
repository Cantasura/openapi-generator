package org.openapitools.codegen.languages;

import com.google.common.collect.Sets;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.meta.features.*;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

import static org.openapitools.codegen.utils.CamelizeOption.LOWERCASE_FIRST_LETTER;
import static org.openapitools.codegen.utils.StringUtils.*;

public abstract class AbstractDartCodegen extends DefaultCodegen {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractDartCodegen.class);

    protected static final List<String> DEFAULT_SUPPORTED_CONTENT_TYPES = Arrays.asList(
            "application/json", "application/x-www-form-urlencoded", "multipart/form-data");

    public static final String PUB_LIBRARY = "pubLibrary";
    public static final String PUB_NAME = "pubName";
    public static final String PUB_VERSION = "pubVersion";
    public static final String PUB_DESCRIPTION = "pubDescription";
    public static final String PUB_AUTHOR = "pubAuthor";
    public static final String PUB_AUTHOR_EMAIL = "pubAuthorEmail";
    public static final String PUB_HOMEPAGE = "pubHomepage";
    public static final String PUB_REPOSITORY = "pubRepository";
    public static final String PUB_PUBLISH_TO = "pubPublishTo";
    public static final String USE_ENUM_EXTENSION = "useEnumExtension";
    public static final String USE_LOWER_CAMEL_CASE = "useLowerCamelCase";

    @Setter protected String pubLibrary = "openapi.api";
    @Setter protected String pubName = "openapi";
    @Setter protected String pubVersion = "1.0.0";
    @Setter protected String pubDescription = "OpenAPI API client";
    @Setter protected String pubAuthor = "Author";
    @Setter protected String pubAuthorEmail = "author@homepage";
    @Setter protected String pubHomepage = "homepage";
    @Setter protected String pubRepository = null;
    @Setter protected String pubPublishTo = null;
    @Setter protected boolean useEnumExtension = false;
    @Setter protected boolean useLowerCamelCase = false;
    @Setter protected String sourceFolder = "src";
    protected String libPath = "lib" + File.separator;
    protected String apiDocPath = "doc/";
    protected String modelDocPath = "doc/";
    protected String apiTestPath = "test" + File.separator;
    protected String modelTestPath = "test" + File.separator;

    protected Map<String, String> imports = new HashMap<>();

    public AbstractDartCodegen() {
        super();

        modifyFeatureSet(features -> features
                .includeDocumentationFeatures(DocumentationFeature.Readme)
                .securityFeatures(EnumSet.of(
                        SecurityFeature.OAuth2_Implicit,
                        SecurityFeature.BasicAuth,
                        SecurityFeature.BearerToken,
                        SecurityFeature.ApiKey
                ))
                .excludeGlobalFeatures(
                        GlobalFeature.XMLStructureDefinitions,
                        GlobalFeature.Callbacks,
                        GlobalFeature.LinkObjects,
                        GlobalFeature.ParameterStyling
                )
                .excludeSchemaSupportFeatures(
                        SchemaSupportFeature.Polymorphism,
                        SchemaSupportFeature.Union,
                        SchemaSupportFeature.Composite
                )
                .includeParameterFeatures(
                        ParameterFeature.Cookie
                )
                .includeClientModificationFeatures(
                        ClientModificationFeature.BasePath
                )
                .excludeWireFormatFeatures(
                        WireFormatFeature.XML
                )
        );

        outputFolder = "generated-code/dart";
        modelTemplateFiles.put("model.mustache", ".dart");
        apiTemplateFiles.put("api.mustache", ".dart");
        embeddedTemplateDir = templateDir = "dart2";
        apiPackage = "api";
        modelPackage = "model";
        modelDocTemplateFiles.put("object_doc.mustache", ".md");
        apiDocTemplateFiles.put("api_doc.mustache", ".md");

        modelTestTemplateFiles.put("model_test.mustache", ".dart");
        apiTestTemplateFiles.put("api_test.mustache", ".dart");

        final List<String> reservedWordsList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(DartClientCodegen.class.getResourceAsStream("/dart/dart-keywords.txt"),
                        StandardCharsets.UTF_8))) {
            while (reader.ready()) {
                reservedWordsList.add(reader.readLine());
            }
        } catch (Exception e) {
            LOGGER.error("Error reading dart keywords. Exception: {}", e.getMessage());
        }
        setReservedWordsLowerCase(reservedWordsList);

        // These types return isPrimitive=true in templates
        languageSpecificPrimitives = Sets.newHashSet(
                "String",
                "bool",
                "int",
                "num",
                "double"
        );

        typeMapping = new HashMap<>();
        typeMapping.put("Array", "List");
        typeMapping.put("array", "List");
        typeMapping.put("map", "Map");
        typeMapping.put("List", "List");
        typeMapping.put("set", "Set");
        typeMapping.put("boolean", "bool");
        typeMapping.put("string", "String");
        typeMapping.put("char", "String");
        typeMapping.put("int", "int");
        typeMapping.put("long", "int");
        typeMapping.put("short", "int");
        typeMapping.put("number", "num");
        typeMapping.put("float", "double");
        typeMapping.put("double", "double");
        typeMapping.put("decimal", "double");
        typeMapping.put("integer", "int");
        typeMapping.put("Date", "DateTime");
        typeMapping.put("date", "DateTime");
        typeMapping.put("DateTime", "DateTime");
        typeMapping.put("file", "MultipartFile");
        typeMapping.put("binary", "MultipartFile");
        typeMapping.put("UUID", "String");
        typeMapping.put("URI", "String");
        typeMapping.put("ByteArray", "String");
        typeMapping.put("object", "Object");
        typeMapping.put("AnyType", "Object");

        // Data types of the above values which are automatically imported
        defaultIncludes = Sets.newHashSet(
                "String",
                "bool",
                "int",
                "num",
                "double",
                "List",
                "Set",
                "Map",
                "DateTime",
                "Object"
        );

        imports.put("String", "dart:core");
        imports.put("bool", "dart:core");
        imports.put("int", "dart:core");
        imports.put("num", "dart:core");
        imports.put("double", "dart:core");
        imports.put("List", "dart:core");
        imports.put("Set", "dart:core");
        imports.put("Map", "dart:core");
        imports.put("DateTime", "dart:core");
        imports.put("Object", "dart:core");
        imports.put("MultipartFile", "package:http/http.dart");

        addOption(PUB_LIBRARY, "Library name in generated code", pubLibrary);
        addOption(PUB_NAME, "Name in generated pubspec", pubName);
        addOption(PUB_VERSION, "Version in generated pubspec", pubVersion);
        addOption(PUB_DESCRIPTION, "Description in generated pubspec", pubDescription);
        addOption(PUB_AUTHOR, "Author name in generated pubspec", pubAuthor);
        addOption(PUB_AUTHOR_EMAIL, "Email address of the author in generated pubspec", pubAuthorEmail);
        addOption(PUB_HOMEPAGE, "Homepage in generated pubspec", pubHomepage);
        addOption(PUB_REPOSITORY, "Repository in generated pubspec", pubRepository);
        addOption(PUB_PUBLISH_TO, "Publish_to in generated pubspec", pubPublishTo);
        addOption(USE_ENUM_EXTENSION, "Allow the 'x-enum-values' extension for enums", String.valueOf(useEnumExtension));
        addOption(USE_LOWER_CAMEL_CASE, "Use lower camel case", String.valueOf(useLowerCamelCase));
        addOption(CodegenConstants.SOURCE_FOLDER, CodegenConstants.SOURCE_FOLDER_DESC, sourceFolder);
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return "dart";
    }

    @Override
    public String getHelp() {
        return "Generates a Dart client library.";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        // Fix a couple Java notation properties
        modelPackage = modelPackage.replace('.', '/');
        apiPackage = apiPackage.replace('.', '/');
        // And overwrite them in the additional properties
        additionalProperties.put(CodegenConstants.MODEL_PACKAGE, modelPackage);
        additionalProperties.put(CodegenConstants.API_PACKAGE, apiPackage);

        if (StringUtils.isEmpty(System.getenv("DART_POST_PROCESS_FILE"))) {
            LOGGER.info("Environment variable DART_POST_PROCESS_FILE not defined so the Dart code may not be properly formatted. To define it, try `export DART_POST_PROCESS_FILE=\"/usr/local/bin/dartfmt -w\"` (Linux/Mac)");
            LOGGER.info("NOTE: To enable file post-processing, 'enablePostProcessFile' must be set to `true` (--enable-post-process-file for CLI).");
        } else if (!this.isEnablePostProcessFile()) {
            LOGGER.info("Warning: Environment variable 'DART_POST_PROCESS_FILE' is set but file post-processing is not enabled. To enable file post-processing, 'enablePostProcessFile' must be set to `true` (--enable-post-process-file for CLI).");
        }

        if (additionalProperties.containsKey(PUB_NAME)) {
            this.setPubName((String) additionalProperties.get(PUB_NAME));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_NAME, pubName);
        }

        if (additionalProperties.containsKey(PUB_LIBRARY)) {
            this.setPubLibrary((String) additionalProperties.get(PUB_LIBRARY));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_LIBRARY, pubLibrary);
        }

        if (additionalProperties.containsKey(PUB_VERSION)) {
            this.setPubVersion((String) additionalProperties.get(PUB_VERSION));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_VERSION, pubVersion);
        }

        if (additionalProperties.containsKey(PUB_DESCRIPTION)) {
            this.setPubDescription((String) additionalProperties.get(PUB_DESCRIPTION));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_DESCRIPTION, pubDescription);
        }

        if (additionalProperties.containsKey(PUB_AUTHOR)) {
            this.setPubAuthor((String) additionalProperties.get(PUB_AUTHOR));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_AUTHOR, pubAuthor);
        }

        if (additionalProperties.containsKey(PUB_AUTHOR_EMAIL)) {
            this.setPubAuthorEmail((String) additionalProperties.get(PUB_AUTHOR_EMAIL));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_AUTHOR_EMAIL, pubAuthorEmail);
        }

        if (additionalProperties.containsKey(PUB_HOMEPAGE)) {
            this.setPubHomepage((String) additionalProperties.get(PUB_HOMEPAGE));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_HOMEPAGE, pubHomepage);
        }

        if (additionalProperties.containsKey(PUB_REPOSITORY)) {
            this.setPubRepository((String) additionalProperties.get(PUB_REPOSITORY));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_REPOSITORY, pubRepository);
        }

        if (additionalProperties.containsKey(PUB_PUBLISH_TO)) {
            this.setPubPublishTo((String) additionalProperties.get(PUB_PUBLISH_TO));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_PUBLISH_TO, pubPublishTo);
        }

        if (additionalProperties.containsKey(USE_ENUM_EXTENSION)) {
            this.setUseEnumExtension(convertPropertyToBooleanAndWriteBack(USE_ENUM_EXTENSION));
        } else {
            // Not set, use to be passed to template.
            additionalProperties.put(USE_ENUM_EXTENSION, useEnumExtension);
        }

        if (additionalProperties.containsKey(USE_LOWER_CAMEL_CASE)) {
            this.setUseLowerCamelCase(convertPropertyToBooleanAndWriteBack(USE_LOWER_CAMEL_CASE));
        } else {
            additionalProperties.put(USE_LOWER_CAMEL_CASE, useLowerCamelCase);
        }

        if (additionalProperties.containsKey(CodegenConstants.SOURCE_FOLDER)) {
            String srcFolder = (String) additionalProperties.get(CodegenConstants.SOURCE_FOLDER);
            this.setSourceFolder(srcFolder.replace('/', File.separatorChar));
        }
        additionalProperties.put(CodegenConstants.SOURCE_FOLDER, sourceFolder);

        // make api and model doc path available in mustache template
        additionalProperties.put("apiDocPath", apiDocPath);
        additionalProperties.put("modelDocPath", modelDocPath);

        // check to not overwrite a custom templateDir
        if (templateDir == null) {
            embeddedTemplateDir = templateDir = "dart2";
        }
    }

    @Override
    protected boolean needToImport(String type) {
        // Import everything, unless it is from dart:core.
        return StringUtils.isNotBlank(type) && (!imports.containsKey(type) || !imports.get(type).equals("dart:core"));
    }

    @Override
    protected boolean isReservedWord(String word) {
        // consider everything as reserved that is
        // * a keyword
        // * a word that has been mapped in the reservedWordsMappings
        // * a default included type or a type include through some library
        return super.isReservedWord(word) || reservedWordsMappings().containsKey(word) || defaultIncludes().contains(word);
    }

    @Override
    public String escapeReservedWord(String name) {
        if (reservedWordsMappings().containsKey(name)) {
            return reservedWordsMappings().get(name);
        }
        return name + "_";
    }

    @Override
    public String apiFileFolder() {
        return (outputFolder + File.separator + libPath + sourceFolder + File.separator + apiPackage()).replace('/', File.separatorChar);
    }

    @Override
    public String modelFileFolder() {
        return (outputFolder + File.separator + libPath + sourceFolder + File.separator + modelPackage()).replace('/', File.separatorChar);
    }

    @Override
    public String apiTestFileFolder() {
        return outputFolder + File.separator + apiTestPath;
    }

    @Override
    public String modelTestFileFolder() {
        return outputFolder + File.separator + modelTestPath;
    }

    @Override
    public String apiDocFileFolder() {
        return outputFolder + File.separator + apiDocPath;
    }

    @Override
    public String modelDocFileFolder() {
        return outputFolder + File.separator + modelDocPath;
    }

    @Override
    public String toVarName(String name) {
        if (nameMapping.containsKey(name)) {
            return nameMapping.get(name);
        }

        // replace - with _ e.g. created-at => created_at
        name = name.replace("-", "_");

        // always need to replace leading underscores first
        if (name.equals("_")) {
            return "underscore";
        }
        name = name.replaceAll("^_", "");

        // if it's all upper case and more than two letters
        if (name.matches("^[A-Z_]*$")) {
            return name;
        }

        // replace all characters that have a mapping but ignore underscores
        // append an underscore to each replacement so that it can be camelized
        if (name.chars().anyMatch(character -> specialCharReplacements.containsKey(String.valueOf((char) character)))) {
            name = escape(name, specialCharReplacements, Collections.singletonList("_"), "_");
        }
        // remove the rest
        name = sanitizeName(name);

        // camelize (lower first character) the variable name
        // pet_id => petI
        name = camelize(name, LOWERCASE_FIRST_LETTER);

        if (name.matches("^\\d.*")) {
            name = "n" + name;
        }

        if (isReservedWord(name)) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    @Override
    public String toParamName(String name) {
        if (parameterNameMapping.containsKey(name)) {
            return parameterNameMapping.get(name);
        }

        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String toModelName(final String name) {
        if (modelNameMapping.containsKey(name)) {
            return modelNameMapping.get(name);
        }

        String sanitizedName = sanitizeName(name);

        if (!StringUtils.isEmpty(modelNamePrefix)) {
            // add '_' so that model name can be camelized correctly
            sanitizedName = modelNamePrefix + "_" + sanitizedName;
        }

        if (!StringUtils.isEmpty(modelNameSuffix)) {
            // add '_' so that model name can be camelized correctly
            sanitizedName = sanitizedName + "_" + modelNameSuffix;
        }

        // camelize the model name
        // phone_number => PhoneNumber
        final String camelizedName = camelize(sanitizedName);

        // Check if there is a mapping that can be used
        if (typeMapping().containsKey(camelizedName)) {
            String typeName = typeMapping().get(camelizedName);
            if (imports.containsKey(typeName)) {
                // Anything with an import mapping is likely
                // generator specific and can not be used as model name.
                final String modelName = "Model" + camelizedName;
                LOGGER.warn("{} (existing type) cannot be used as model name. Renamed to {}", camelizedName, modelName);
                return modelName;
            }
            return typeName;
        }

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(camelizedName)) {
            final String modelName = "Model" + camelizedName;
            LOGGER.warn("{} (reserved word) cannot be used as model name. Renamed to {}", camelizedName, modelName);
            return modelName;
        }

        // model name starts with number
        if (camelizedName.matches("^\\d.*")) {
            final String modelName = "Model" + camelizedName; // e.g. 200Response => Model200Response (after camelize)
            LOGGER.warn("{} (model name starts with number) cannot be used as model name. Renamed to {}", name, modelName);
            return modelName;
        }

        return camelizedName;
    }

    @Override
    public String toModelFilename(String name) {
        return underscore(toModelName(name));
    }

    @Override
    public String toModelDocFilename(String name) {
        return toModelName(name);
    }

    @Override
    public String toApiFilename(String name) {
        return underscore(toApiName(name));
    }

    @Override
    public String toApiTestFilename(String name) {
        return toApiFilename(name) + "_test";
    }

    @Override
    public String toModelTestFilename(String name) {
        return toModelFilename(name) + "_test";
    }

    @Override
    public String toDefaultValue(Schema schema) {
        if (ModelUtils.isMapSchema(schema) || ModelUtils.isSet(schema)) {
            return "const {}";
        }
        if (ModelUtils.isArraySchema(schema)) {
            return "const []";
        }

        if (schema.getDefault() != null) {
            if (ModelUtils.isDateSchema(schema) || ModelUtils.isDateTimeSchema(schema)) {
                // this is currently not supported and would create compile errors
                return null;
            }
            if (ModelUtils.isStringSchema(schema)) {
                return "'" + schema.getDefault().toString().replace("'", "\\'") + "'";
            }
            return schema.getDefault().toString();
        }
        return null;
    }

    @Override
    public String getTypeDeclaration(Schema p) {
        Schema<?> schema = unaliasSchema(p);
        Schema<?> target = ModelUtils.isGenerateAliasAsModel() ? p : schema;
        if (ModelUtils.isArraySchema(target)) {
            Schema<?> items = ModelUtils.getSchemaItems(schema);
            return getSchemaType(target) + "<" + getTypeDeclaration(items) + ">";
        }
        if (ModelUtils.isMapSchema(target)) {
            // Note: ModelUtils.isMapSchema(p) returns true when p is a composed schema that also defines
            // additionalproperties: true
            Schema<?> inner = ModelUtils.getAdditionalProperties(target);
            if (inner == null) {
                LOGGER.error("`{}` (map property) does not have a proper inner type defined. Default to type:string", p.getName());
                inner = new StringSchema().description("TODO default missing map inner type to string");
                p.setAdditionalProperties(inner);
            }
            return getSchemaType(target) + "<String, " + getTypeDeclaration(inner) + ">";
        }
        return super.getTypeDeclaration(p);
    }

    @Override
    public String getSchemaType(Schema p) {
        String openAPIType = super.getSchemaType(p);
        if (openAPIType == null) {
            LOGGER.error("No Type defined for Schema {}", p);
        }
        if (typeMapping().containsKey(openAPIType)) {
            return typeMapping().get(openAPIType);
        }
        return toModelName(openAPIType);
    }

    @Override
    public ModelsMap postProcessModels(ModelsMap objs) {
        return postProcessModelsEnum(objs);
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);
        if (!model.isEnum && property.isEnum && property.getComposedSchemas() == null) {
            // These are inner enums, enums which do not exist as models, just as properties.
            // They are handled via the enum_inline template and are generated in the
            // same file as the containing class. To prevent name clashes the inline enum classes
            // are prefix with the classname of the containing class in the template.
            // Here the datatypeWithEnum template variable gets updated to match that scheme.
            // Also taking into account potential collection types e.g. List<JustSymbolEnum> -> List<EnumArraysJustSymbolEnum>
            final String enumName = model.classname + property.enumName;
            if (property.items != null) {
                // inner items e.g. enums in collections, only works for one level
                // but same is the case for DefaultCodegen
                property.setDatatypeWithEnum(property.datatypeWithEnum.replace(property.items.datatypeWithEnum, enumName));
                property.items.setDatatypeWithEnum(enumName);
                property.items.setEnumName(enumName);
            } else {
                // plain enum property
                property.setDatatypeWithEnum(property.datatypeWithEnum.replace(property.enumName, enumName));
            }
            property.setEnumName(enumName);
        }
    }

    @Override
    public CodegenProperty fromProperty(String name, Schema p, boolean required) {
        final CodegenProperty property = super.fromProperty(name, p, required);

        // Handle composed properties and it's NOT allOf with a single ref only
        if (ModelUtils.isComposedSchema(p) && !(ModelUtils.isAllOf(p) && p.getAllOf().size() == 1)) {
            ComposedSchema composed = (ComposedSchema) p;

            // Count the occurrences of allOf/anyOf/oneOf with exactly one child element
            long count = Stream.of(composed.getAllOf(), composed.getAnyOf(), composed.getOneOf())
                    .filter(list -> list != null && list.size() == 1).count();

            if (count == 1) {
                // Continue only if there is one element that matches
                // and basically treat it as simple property.
                Stream.of(composed.getAllOf(), composed.getAnyOf(), composed.getOneOf())
                        .filter(list -> list != null && list.size() == 1)
                        .findFirst()
                        .map(list -> list.get(0).get$ref())
                        .map(ModelUtils::getSimpleRef)
                        .map(ref -> ModelUtils.getSchemas(this.openAPI).get(ref))
                        .ifPresent(schema -> {
                            property.isEnum = schema.getEnum() != null;
                            property.isModel = true;
                        });

            }
        }
        return property;
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        final CodegenOperation op = super.fromOperation(path, httpMethod, operation, servers);
        for (CodegenResponse r : op.responses) {
            // By default, only set types are automatically added to operation imports, not sure why.
            // Add all container type imports here, by default 'dart:core' imports are skipped
            // but other sub-classes may require specific container type imports.
            if (r.containerType != null && typeMapping().containsKey(r.containerType)) {
                final String value = typeMapping().get(r.containerType);
                if (needToImport(value)) {
                    op.imports.add(value);
                }
            }
        }
        for (CodegenParameter p : op.allParams) {
            if (p.isContainer) {
                final String type = p.isArray ? "array" : "map";
                if (typeMapping().containsKey(type)) {
                    final String value = typeMapping().get(type);
                    // Also add container imports for parameters.
                    if (needToImport(value)) {
                        op.imports.add(value);
                    }
                }
            }
        }
        return op;
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        super.postProcessOperationsWithModels(objs, allModels);
        OperationMap operations = objs.getOperations();
        if (operations != null) {
            List<CodegenOperation> ops = operations.getOperation();
            for (CodegenOperation op : ops) {
                if (op.hasConsumes) {
                    if (!op.formParams.isEmpty() || op.isMultipart) {
                        // DefaultCodegen only sets this if the first consumes mediaType
                        // is application/x-www-form-urlencoded or multipart.
                        // Can just use the original
                        op.prioritizedContentTypes = op.consumes;
                    } else {
                        // Prioritize content types by moving application/json to the front
                        // similar to JavaCodegen
                        op.prioritizedContentTypes = prioritizeContentTypes(op.consumes);
                        String mediaType = op.prioritizedContentTypes.get(0).get("mediaType");
                        if (!DEFAULT_SUPPORTED_CONTENT_TYPES.contains(mediaType)) {
                            LOGGER.warn("The media-type '{}' for operation '{}' is not support in the Dart generators by default.", mediaType, op.path);
                        }
                    }
                }
            }
        }
        return objs;
    }

    private List<Map<String, String>> prioritizeContentTypes(List<Map<String, String>> consumes) {
        if (consumes.size() <= 1) {
            // no need to change any order
            return consumes;
        }

        List<Map<String, String>> prioritizedContentTypes = new ArrayList<>(consumes.size());

        List<Map<String, String>> jsonVendorMimeTypes = new ArrayList<>(consumes.size());
        List<Map<String, String>> jsonMimeTypes = new ArrayList<>(consumes.size());

        for (Map<String, String> consume : consumes) {
            String mediaType = consume.get("mediaType");
            if (isJsonVendorMimeType(mediaType)) {
                jsonVendorMimeTypes.add(consume);
            } else if (isJsonMimeType(mediaType)) {
                jsonMimeTypes.add(consume);
            } else {
                prioritizedContentTypes.add(consume);
            }
        }

        prioritizedContentTypes.addAll(0, jsonMimeTypes);
        prioritizedContentTypes.addAll(0, jsonVendorMimeTypes);
        return prioritizedContentTypes;
    }

    @Override
    protected void updateEnumVarsWithExtensions(List<Map<String, Object>> enumVars, Map<String, Object> vendorExtensions, String dataType) {
        if (vendorExtensions != null && useEnumExtension && vendorExtensions.containsKey("x-enum-values")) {
            // Use the x-enum-values extension for this enum
            // Existing enumVars added by the default handling need to be removed first
            enumVars.clear();

            Object extension = vendorExtensions.get("x-enum-values");
            List<Map<String, Object>> values = (List<Map<String, Object>>) extension;
            for (Map<String, Object> value : values) {
                Map<String, Object> enumVar = new HashMap<>();
                enumVar.put("name", toEnumVarName((String) value.get("identifier"), dataType));
                enumVar.put("value", toEnumValue(value.get("numericValue").toString(), dataType));
                enumVar.put("isString", isDataTypeString(dataType));
                if (value.containsKey("description")) {
                    enumVar.put("description", value.get("description").toString());
                }
                enumVars.add(enumVar);
            }
        } else {
            super.updateEnumVarsWithExtensions(enumVars, vendorExtensions, dataType);
        }
    }

    @Override
    public String toEnumVarName(String value, String datatype) {
        if (enumNameMapping.containsKey(value)) {
            return enumNameMapping.get(value);
        }

        if (value.isEmpty()) {
            return "empty";
        }

        if (useLowerCamelCase && value.matches("^[A-Z_]*$")) {
            value = value.toLowerCase();
        }

        if (("number".equalsIgnoreCase(datatype) ||
                "double".equalsIgnoreCase(datatype) ||
                "int".equalsIgnoreCase(datatype)) &&
                value.matches("^-?\\d.*")) {
            // Only rename numeric values when the datatype is numeric
            // AND the name is not changed by enum extensions (matches a numeric value).
            boolean isNegative = value.startsWith("-");
            return toVarName("number" + (isNegative ? "_negative" : "") + value);
        }
        return toVarName(value);
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if ("number".equalsIgnoreCase(datatype) ||
                "int".equalsIgnoreCase(datatype)) {
            return value;
        } else {
            return "'" + escapeTextInSingleQuotes(value) + "'";
        }
    }

    @Override
    public String toOperationId(String operationId) {
        operationId = super.toOperationId(operationId);

        operationId = camelize(sanitizeName(operationId), LOWERCASE_FIRST_LETTER);

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            String newOperationId = camelize("call_" + operationId, LOWERCASE_FIRST_LETTER);
            LOGGER.warn("{} (reserved word) cannot be used as method name. Renamed to {}", operationId, newOperationId);
            return newOperationId;
        }

        // operationId starts with a number
        if (operationId.matches("^\\d.*")) {
            String newOperationId = camelize("call_" + operationId, LOWERCASE_FIRST_LETTER);
            LOGGER.warn("{} (starting with a number) cannot be used as method name. Renamed to {}", operationId, newOperationId);
            operationId = newOperationId;
        }

        return operationId;
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    @Override
    public void postProcessFile(File file, String fileType) {
        super.postProcessFile(file, fileType);
        if (file == null) {
            return;
        }

        String dartPostProcessFile = System.getenv("DART_POST_PROCESS_FILE");
        if (StringUtils.isEmpty(dartPostProcessFile)) {
            return; // skip if DART_POST_PROCESS_FILE env variable is not defined
        }

        // process all files with dart extension
        if ("dart".equals(FilenameUtils.getExtension(file.toString()))) {
            // currently supported is "dartfmt -w" and "dart format"
            this.executePostProcessor(new String[]{dartPostProcessFile, file.toString()});
        }
    }

    @Override
    public GeneratorLanguage generatorLanguage() {
        return GeneratorLanguage.DART;
    }
}
