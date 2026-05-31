package kr.ac.knu.comit.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 현재 인증된 회원의 {@link MemberPrincipal}이 주입된다.
 *
 * <pre>
 * {@code
 * public ResponseEntity<PostDetailResponse> getPost(
 *     @AuthenticatedMember MemberPrincipal principal,
 *     @PathVariable Long postId) { ... }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticatedMember {
}
