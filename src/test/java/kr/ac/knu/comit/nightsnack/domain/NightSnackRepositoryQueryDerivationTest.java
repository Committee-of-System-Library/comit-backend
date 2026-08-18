package kr.ac.knu.comit.nightsnack.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * 파생 쿼리 메서드 이름은 애플리케이션 기동 시점에 파싱된다. 이름이 틀리면 서버가 아예 뜨지 않으므로,
 * Spring Data 가 쓰는 {@link PartTree} 로 이름만 미리 파싱해 둔다. DB 없이 검증 가능하다.
 */
@DisplayName("NightSnackRepository 파생 쿼리 이름")
class NightSnackRepositoryQueryDerivationTest {

    private static final String METHOD_NAME =
            "findFirstByNightSnackDateGreaterThanEqualOrderByNightSnackDateAsc";

    @Test
    @DisplayName("nightSnackDate 기준 1건 제한 + 오름차순 정렬로 파싱된다")
    void parsesAsLimitedAscendingQuery() {
        PartTree tree = new PartTree(METHOD_NAME, NightSnack.class);

        assertThat(tree.isLimiting()).isTrue();
        assertThat(tree.getMaxResults()).isEqualTo(1);

        Sort.Order order = tree.getSort().getOrderFor("nightSnackDate");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("조건은 nightSnackDate >= ? 하나뿐이다")
    void hasSingleGreaterThanEqualCondition() {
        PartTree tree = new PartTree(METHOD_NAME, NightSnack.class);

        assertThat(tree.getParts())
                .singleElement()
                .satisfies(part -> {
                    assertThat(part.getProperty().getSegment()).isEqualTo("nightSnackDate");
                    assertThat(part.getType())
                            .isEqualTo(org.springframework.data.repository.query.parser.Part.Type.GREATER_THAN_EQUAL);
                });
    }
}
