package kr.ac.knu.comit.global.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ApiDocIntrospector")
class ApiDocIntrospectorTest {

    @Test
    @DisplayName("자기참조 컬렉션과 맵 예시 생성 시 null 재귀값으로 실패하지 않는다")
    void generatesExamplesForRecursiveCollectionsWithoutNullPointerException() throws Exception {
        ApiDocIntrospector introspector = new ApiDocIntrospector("kr.ac.knu.comit");
        Method method = ApiDocIntrospector.class.getDeclaredMethod(
                "generateExampleValue",
                Type.class,
                int.class,
                Set.class
        );
        method.setAccessible(true);

        AtomicReference<Object> result = new AtomicReference<>();
        assertThatCode(() -> result.set(method.invoke(introspector, RecursiveExample.class, 0, new HashSet<>())))
                .doesNotThrowAnyException();

        assertThat(result.get()).isInstanceOf(Map.class);
        Map<?, ?> example = (Map<?, ?>) result.get();
        assertThat(example.get("children")).isEqualTo(List.of());
        assertThat(example.get("related")).isEqualTo(Map.of());
    }

    private static final class RecursiveExample {
        private List<RecursiveExample> children;
        private Map<String, RecursiveExample> related;
    }
}
