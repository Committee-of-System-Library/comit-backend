package kr.ac.knu.comit.global.auth;

import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.CommonErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link AuthenticatedMember} 파라미터를 현재 {@link MemberPrincipal}로 변환한다.
 *
 * @implNote 현재 구현은 {@link MemberAuthenticationFilter}가 넣어 둔 요청 속성을
 * 읽는다. 실제 SSO starter가 보안 컨텍스트로 클레임을 노출하면
 * 그 방식으로 교체한다.
 */
@Component
public class MemberArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String PRINCIPAL_ATTRIBUTE = "memberPrincipal";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedMember.class)
                && parameter.getParameterType().equals(MemberPrincipal.class);
    }

    @Override
    public MemberPrincipal resolveArgument(MethodParameter parameter,
                                           ModelAndViewContainer mavContainer,
                                           NativeWebRequest webRequest,
                                           WebDataBinderFactory binderFactory) {
        Object principal = webRequest.getAttribute(PRINCIPAL_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);
        if (!(principal instanceof MemberPrincipal memberPrincipal)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return memberPrincipal;
    }
}
