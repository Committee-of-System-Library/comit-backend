package kr.ac.knu.comit.global.docs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import kr.ac.knu.comit.global.auth.AuthenticatedMember;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import kr.ac.knu.comit.global.exception.ErrorCode;
import kr.ac.knu.comit.global.exception.ErrorCodeRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ValueConstants;

final class ApiDocIntrospector {

    private static final String DEFAULT_UNAUTHORIZED_WHEN = "인증 정보가 없거나 유효한 회원을 식별할 수 없을 때";
    private static final String DEFAULT_INVALID_REQUEST_WHEN = "요청 파라미터 또는 본문이 검증 규칙을 만족하지 않을 때";

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private static final Set<String> REQUIRED_CONSTRAINTS = Set.of(
            "NotNull",
            "NotBlank",
            "NotEmpty"
    );

    private final String basePackage;

    ApiDocIntrospector(String basePackage) {
        this.basePackage = basePackage;
    }

    List<GeneratedApiDocument> inspect() throws ClassNotFoundException, FileNotFoundException {
        List<Class<?>> contractTypes = scanForTypes(ApiContract.class);
        List<Class<?>> controllerTypes = scanForControllers();

        if (contractTypes.isEmpty()) {
            throw new FileNotFoundException("No @ApiContract interfaces found under package: " + basePackage);
        }

        return contractTypes.stream()
                .sorted(Comparator.comparing(Class::getName))
                .map(contractType -> buildDocument(contractType, controllerTypes))
                .toList();
    }

    private GeneratedApiDocument buildDocument(Class<?> contractType, List<Class<?>> controllerTypes) {
        Class<?> controllerType = findControllerImplementation(contractType, controllerTypes);
        String classPath = extractClassPath(controllerType);

        List<GeneratedEndpoint> endpoints = Arrays.stream(contractType.getDeclaredMethods())
                .sorted(Comparator.comparing(Method::getName))
                .map(method -> buildEndpoint(contractType, method, classPath))
                .toList();

        String sectionPath = deriveSectionPath(contractType);
        String relativeOutputPath = sectionPath.isBlank()
                ? contractType.getSimpleName() + ".html"
                : sectionPath + "/" + contractType.getSimpleName() + ".html";

        return new GeneratedApiDocument(
                contractType.getSimpleName(),
                relativeOutputPath,
                sectionPath,
                endpoints
        );
    }

    private GeneratedEndpoint buildEndpoint(Class<?> contractType, Method method, String classPath) {
        ApiDoc apiDoc = AnnotatedElementUtils.findMergedAnnotation(method, ApiDoc.class);
        if (apiDoc == null) {
            throw new IllegalStateException(contractType.getName() + "#" + method.getName() + " is missing @ApiDoc");
        }

        RequestMappingInfo mappingInfo = extractMappingInfo(method);
        if (mappingInfo == null) {
            throw new IllegalStateException(contractType.getName() + "#" + method.getName() + " is missing mapping annotation");
        }

        Map<String, String> descriptions = Arrays.stream(apiDoc.descriptions())
                .collect(Collectors.toMap(FieldDesc::name, FieldDesc::value, (left, right) -> right, LinkedHashMap::new));

        RequestSpec requestSpec = inspectRequest(method, descriptions);
        Type responseType = unwrapDocumentType(method.getGenericReturnType());
        List<ErrorDoc> errors = resolveErrors(method, apiDoc.errors());

        Example example = apiDoc.example();
        String endpointPath = joinPaths(classPath, mappingInfo.path());
        return new GeneratedEndpoint(
                method.getName(),
                apiDoc.summary(),
                apiDoc.description(),
                mappingInfo.httpMethod(),
                endpointPath,
                requestSpec.pathParameters(),
                requestSpec.queryParameters(),
                extractFields(requestSpec.requestBodyType(), descriptions),
                extractFields(responseType, descriptions),
                errors,
                resolveExample(example.request(), requestSpec.requestBodyType()),
                resolveExample(example.response(), responseType),
                resolveErrorExample(errors, endpointPath)
        );
    }

    private List<Class<?>> scanForTypes(Class<? extends Annotation> annotationType) throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = newScanner();
        scanner.addIncludeFilter(new AnnotationTypeFilter(annotationType));

        List<Class<?>> discoveredTypes = new ArrayList<>();
        for (var candidate : scanner.findCandidateComponents(basePackage)) {
            discoveredTypes.add(Class.forName(candidate.getBeanClassName()));
        }
        return discoveredTypes;
    }

    private List<Class<?>> scanForControllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = newScanner();
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        List<Class<?>> discoveredTypes = new ArrayList<>();
        for (var candidate : scanner.findCandidateComponents(basePackage)) {
            Class<?> candidateType = Class.forName(candidate.getBeanClassName());
            if (!candidateType.isInterface()) {
                discoveredTypes.add(candidateType);
            }
        }
        return discoveredTypes;
    }

    private ClassPathScanningCandidateComponentProvider newScanner() {
        return new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                AnnotationMetadata metadata = beanDefinition.getMetadata();
                return metadata.isIndependent() && (metadata.isInterface() || metadata.isConcrete());
            }
        };
    }

    private Class<?> findControllerImplementation(Class<?> contractType, List<Class<?>> controllerTypes) {
        List<Class<?>> matches = controllerTypes.stream()
                .filter(contractType::isAssignableFrom)
                .sorted(Comparator.comparing(Class::getName))
                .toList();

        if (matches.isEmpty()) {
            throw new IllegalStateException("No controller implementation found for " + contractType.getName());
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("Multiple controller implementations found for " + contractType.getName());
        }
        return matches.getFirst();
    }

    private String extractClassPath(Class<?> controllerType) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(controllerType, RequestMapping.class);
        return requestMapping == null ? "" : firstPath(requestMapping.path(), requestMapping.value());
    }

    private RequestMappingInfo extractMappingInfo(Method method) {
        GetMapping getMapping = AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class);
        if (getMapping != null) {
            return new RequestMappingInfo("GET", firstPath(getMapping.path(), getMapping.value()));
        }

        PostMapping postMapping = AnnotatedElementUtils.findMergedAnnotation(method, PostMapping.class);
        if (postMapping != null) {
            return new RequestMappingInfo("POST", firstPath(postMapping.path(), postMapping.value()));
        }

        PutMapping putMapping = AnnotatedElementUtils.findMergedAnnotation(method, PutMapping.class);
        if (putMapping != null) {
            return new RequestMappingInfo("PUT", firstPath(putMapping.path(), putMapping.value()));
        }

        PatchMapping patchMapping = AnnotatedElementUtils.findMergedAnnotation(method, PatchMapping.class);
        if (patchMapping != null) {
            return new RequestMappingInfo("PATCH", firstPath(patchMapping.path(), patchMapping.value()));
        }

        DeleteMapping deleteMapping = AnnotatedElementUtils.findMergedAnnotation(method, DeleteMapping.class);
        if (deleteMapping != null) {
            return new RequestMappingInfo("DELETE", firstPath(deleteMapping.path(), deleteMapping.value()));
        }

        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        if (requestMapping == null) {
            return null;
        }

        String methodValue = requestMapping.method().length == 0
                ? "REQUEST"
                : requestMapping.method()[0].name();
        return new RequestMappingInfo(methodValue, firstPath(requestMapping.path(), requestMapping.value()));
    }

    private RequestSpec inspectRequest(Method method, Map<String, String> descriptions) {
        List<FieldDoc> pathParameters = new ArrayList<>();
        List<FieldDoc> queryParameters = new ArrayList<>();
        Type requestBodyType = null;

        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                requestBodyType = unwrapDocumentType(parameter.getParameterizedType());
                continue;
            }

            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            if (pathVariable != null) {
                pathParameters.add(new FieldDoc(
                        resolveParameterName(pathVariable.name(), pathVariable.value(), parameter),
                        describeType(parameter.getParameterizedType()),
                        true,
                        descriptions.getOrDefault(resolveParameterName(pathVariable.name(), pathVariable.value(), parameter), "-")
                ));
                continue;
            }

            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (requestParam != null) {
                String parameterName = resolveParameterName(requestParam.name(), requestParam.value(), parameter);
                queryParameters.add(new FieldDoc(
                        parameterName,
                        describeType(parameter.getParameterizedType()),
                        isRequired(parameter, requestParam),
                        descriptions.getOrDefault(parameterName, "-")
                ));
            }
        }

        return new RequestSpec(
                List.copyOf(pathParameters),
                List.copyOf(queryParameters),
                requestBodyType
        );
    }

    private boolean isRequired(Parameter parameter, RequestParam requestParam) {
        if (!ValueConstants.DEFAULT_NONE.equals(requestParam.defaultValue())) {
            return false;
        }
        if (hasRequiredConstraint(parameter.getAnnotations())) {
            return true;
        }
        return requestParam.required();
    }

    private String resolveParameterName(String name, String value, Parameter parameter) {
        if (StringUtils.hasText(name)) {
            return name;
        }
        if (StringUtils.hasText(value)) {
            return value;
        }
        return parameter.getName();
    }

    private Type findRequestBodyType(Method method) {
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                return unwrapDocumentType(parameter.getParameterizedType());
            }
        }
        return null;
    }

    private Type unwrapDocumentType(Type sourceType) {
        Type current = sourceType;
        while (current != null) {
            Class<?> rawType = rawTypeOf(current);
            if (rawType == ResponseEntity.class || rawType == ApiResponse.class) {
                current = firstTypeArgument(current);
                continue;
            }
            return current;
        }
        return null;
    }

    private Type firstTypeArgument(Type type) {
        if (type instanceof ParameterizedType parameterizedType && parameterizedType.getActualTypeArguments().length > 0) {
            return parameterizedType.getActualTypeArguments()[0];
        }
        return null;
    }

    private List<FieldDoc> extractFields(Type type, Map<String, String> descriptions) {
        if (type == null) {
            return List.of();
        }

        Class<?> rawType = rawTypeOf(type);
        if (rawType == null || isSimpleType(rawType)) {
            return List.of();
        }

        return collectFields(rawType).stream()
                .sorted(Comparator.comparing(Field::getName))
                .map(field -> new FieldDoc(
                        field.getName(),
                        describeType(field.getGenericType()),
                        isRequired(field),
                        descriptions.getOrDefault(field.getName(), "-")
                ))
                .toList();
    }

    private List<Field> collectFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.stream(current.getDeclaredFields())
                    .filter(field -> !field.isSynthetic())
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .toList());
            current = current.getSuperclass();
        }
        return fields;
    }

    private boolean isRequired(Field field) {
        return hasRequiredConstraint(field.getAnnotations());
    }

    private boolean hasRequiredConstraint(Annotation[] annotations) {
        return Arrays.stream(annotations)
                .map(Annotation::annotationType)
                .anyMatch(annotationType ->
                        annotationType.getPackageName().startsWith("jakarta.validation.constraints")
                                && REQUIRED_CONSTRAINTS.contains(annotationType.getSimpleName()));
    }

    private String resolveExample(String rawExample, Type type) {
        if (type == null) {
            return "";
        }
        if (StringUtils.hasText(rawExample)) {
            return prettyJson(rawExample);
        }

        Object generated = generateExampleValue(type, 0, new LinkedHashSet<>());
        if (generated == null) {
            return "";
        }
        return writeJson(generated);
    }

    private List<ErrorDoc> resolveErrors(Method method, ApiError[] declaredErrors) {
        LinkedHashMap<String, ErrorDoc> errors = new LinkedHashMap<>();

        if (hasAuthenticatedMemberParameter(method)) {
            errors.put(
                    CommonErrorCode.UNAUTHORIZED.getCode(),
                    toErrorDoc(CommonErrorCode.UNAUTHORIZED, DEFAULT_UNAUTHORIZED_WHEN)
            );
        }

        if (hasValidationInput(method)) {
            errors.put(
                    CommonErrorCode.INVALID_REQUEST.getCode(),
                    toErrorDoc(CommonErrorCode.INVALID_REQUEST, DEFAULT_INVALID_REQUEST_WHEN)
            );
        }

        for (ApiError declaredError : declaredErrors) {
            ErrorCode code = ErrorCodeRegistry.require(declaredError.code());
            ErrorDoc existing = errors.get(code.getCode());
            String when = StringUtils.hasText(declaredError.when())
                    ? declaredError.when()
                    : existing == null ? "-" : existing.when();
            errors.put(code.getCode(), toErrorDoc(code, when));
        }

        return List.copyOf(errors.values());
    }

    private boolean hasAuthenticatedMemberParameter(Method method) {
        return Arrays.stream(method.getParameters())
                .anyMatch(parameter -> parameter.isAnnotationPresent(AuthenticatedMember.class));
    }

    private boolean hasValidationInput(Method method) {
        return Arrays.stream(method.getParameters())
                .anyMatch(parameter -> Arrays.stream(parameter.getAnnotations())
                        .map(Annotation::annotationType)
                        .anyMatch(annotationType ->
                                annotationType.getSimpleName().equals("Valid")
                                        || annotationType.getSimpleName().equals("Validated")
                                        || annotationType.getPackageName().startsWith("jakarta.validation.constraints")));
    }

    private ErrorDoc toErrorDoc(ErrorCode code, String when) {
        return new ErrorDoc(
                code.getCode(),
                code.getStatus(),
                code.getType(),
                code.getMessage(),
                StringUtils.hasText(when) ? when : "-"
        );
    }

    private String resolveErrorExample(List<ErrorDoc> errors, String endpointPath) {
        if (errors.isEmpty()) {
            return "";
        }

        ErrorDoc primary = errors.getFirst();
        LinkedHashMap<String, Object> example = new LinkedHashMap<>();
        example.put("type", primary.type());
        example.put("title", HttpStatus.valueOf(primary.status()).getReasonPhrase());
        example.put("status", primary.status());
        example.put("detail", primary.detail());
        example.put("instance", endpointPath);
        example.put("errorCode", primary.errorCode());
        if (CommonErrorCode.INVALID_REQUEST.getCode().equals(primary.errorCode())) {
            example.put("invalidFields", List.of(Map.of(
                    "field", "fieldName",
                    "message", "유효하지 않은 입력값입니다."
            )));
        }
        example.put("timestamp", "2026-03-24T03:10:00Z");
        return writeJson(example);
    }

    private String writeJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to render example JSON", exception);
        }
    }

    private Object generateExampleValue(Type type, int depth, Set<String> visitingTypes) {
        if (type == null || depth > 4) {
            return null;
        }

        Class<?> rawType = rawTypeOf(type);
        if (rawType == null) {
            return null;
        }

        if (rawType == String.class) {
            return "string";
        }
        if (rawType == Integer.class || rawType == int.class) {
            return 1;
        }
        if (rawType == Long.class || rawType == long.class) {
            return 1L;
        }
        if (rawType == Double.class || rawType == double.class) {
            return 1.0d;
        }
        if (rawType == Float.class || rawType == float.class) {
            return 1.0f;
        }
        if (rawType == Short.class || rawType == short.class) {
            return (short) 1;
        }
        if (rawType == Byte.class || rawType == byte.class) {
            return (byte) 1;
        }
        if (rawType == BigDecimal.class) {
            return BigDecimal.ONE;
        }
        if (rawType == BigInteger.class) {
            return BigInteger.ONE;
        }
        if (rawType == Boolean.class || rawType == boolean.class) {
            return true;
        }
        if (rawType == LocalDate.class) {
            return "2024-01-01";
        }
        if (rawType == LocalDateTime.class) {
            return "2024-01-01T12:00:00";
        }
        if (rawType == OffsetDateTime.class) {
            return "2024-01-01T12:00:00+09:00";
        }
        if (rawType.isEnum()) {
            Object[] constants = rawType.getEnumConstants();
            return constants.length == 0 ? null : constants[0].toString();
        }
        if (rawType.isArray()) {
            Object element = generateExampleValue(rawType.getComponentType(), depth + 1, visitingTypes);
            return element == null ? List.of() : List.of(element);
        }
        if (Collection.class.isAssignableFrom(rawType)) {
            Type elementType = firstTypeArgument(type);
            Object element = generateExampleValue(elementType, depth + 1, visitingTypes);
            return element == null ? List.of() : List.of(element);
        }
        if (Map.class.isAssignableFrom(rawType)) {
            Type valueType = type instanceof ParameterizedType parameterizedType
                    ? parameterizedType.getActualTypeArguments()[1]
                    : String.class;
            Object value = generateExampleValue(valueType, depth + 1, visitingTypes);
            return value == null ? Map.of() : Map.of("key", value);
        }

        String typeName = rawType.getName();
        if (!visitingTypes.add(typeName)) {
            return null;
        }

        LinkedHashMap<String, Object> example = new LinkedHashMap<>();
        for (Field field : collectFields(rawType).stream().sorted(Comparator.comparing(Field::getName)).toList()) {
            example.put(field.getName(), generateExampleValue(field.getGenericType(), depth + 1, visitingTypes));
        }
        visitingTypes.remove(typeName);
        return example;
    }

    private Class<?> rawTypeOf(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            return rawType instanceof Class<?> clazz ? clazz : null;
        }
        if (type instanceof GenericArrayType genericArrayType) {
            Type componentType = genericArrayType.getGenericComponentType();
            Class<?> rawComponentType = rawTypeOf(componentType);
            return rawComponentType == null ? null : rawComponentType.arrayType();
        }
        return null;
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class == type
                || Character.class == type
                || LocalDate.class == type
                || LocalDateTime.class == type
                || OffsetDateTime.class == type
                || type.isArray() && isSimpleType(type.getComponentType());
    }

    private String describeType(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz.isArray() ? describeType(clazz.getComponentType()) + "[]" : clazz.getSimpleName();
        }
        if (type instanceof ParameterizedType parameterizedType) {
            String rawType = describeType(parameterizedType.getRawType());
            String arguments = Arrays.stream(parameterizedType.getActualTypeArguments())
                    .map(this::describeType)
                    .collect(Collectors.joining(", "));
            return rawType + "<" + arguments + ">";
        }
        if (type instanceof GenericArrayType genericArrayType) {
            return describeType(genericArrayType.getGenericComponentType()) + "[]";
        }
        return type == null ? "-" : type.getTypeName();
    }

    private String prettyJson(String rawExample) {
        try {
            Object parsed = OBJECT_MAPPER.readValue(rawExample, Object.class);
            return writeJson(parsed);
        } catch (JsonProcessingException exception) {
            return rawExample.trim();
        }
    }

    private String deriveSectionPath(Class<?> contractType) {
        String packageName = contractType.getPackageName();
        String relativePackage = packageName.startsWith(basePackage + ".")
                ? packageName.substring(basePackage.length() + 1)
                : packageName;

        List<String> packageSegments = Arrays.stream(relativePackage.split("\\."))
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(ArrayList::new));

        int controllerIndex = packageSegments.indexOf("controller");
        if (controllerIndex >= 0) {
            packageSegments = new ArrayList<>(packageSegments.subList(0, controllerIndex));
        } else if (!packageSegments.isEmpty() && Objects.equals(packageSegments.getLast(), "api")) {
            packageSegments.removeLast();
        }

        return String.join("/", packageSegments);
    }

    private String joinPaths(String left, String right) {
        String normalizedLeft = normalizePath(left);
        String normalizedRight = normalizePath(right);

        if (normalizedLeft.isBlank()) {
            return normalizedRight.isBlank() ? "/" : normalizedRight;
        }
        if (normalizedRight.isBlank()) {
            return normalizedLeft;
        }
        if ("/".equals(normalizedLeft)) {
            return normalizedRight;
        }
        return normalizedLeft + normalizedRight;
    }

    private String normalizePath(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.startsWith("/") ? value : "/" + value;
        return normalized.endsWith("/") && normalized.length() > 1
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }

    private String firstPath(String[] pathValues, String[] valueValues) {
        if (pathValues.length > 0 && StringUtils.hasText(pathValues[0])) {
            return pathValues[0];
        }
        if (valueValues.length > 0 && StringUtils.hasText(valueValues[0])) {
            return valueValues[0];
        }
        return "";
    }

    private record RequestMappingInfo(String httpMethod, String path) {
    }

    private record RequestSpec(
            List<FieldDoc> pathParameters,
            List<FieldDoc> queryParameters,
            Type requestBodyType
    ) {
    }
}
