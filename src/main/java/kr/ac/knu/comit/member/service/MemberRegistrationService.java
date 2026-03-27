package kr.ac.knu.comit.member.service;

import kr.ac.knu.comit.global.auth.MemberPrincipal;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberRegistrationService {

    private static final int MAX_NICKNAME_LENGTH = 15;
    private static final String FALLBACK_NICKNAME = "comit-user";

    private final MemberRepository memberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Member register(MemberPrincipal principal) {
        return memberRepository.saveAndFlush(
                Member.create(principal.ssoSub(), resolveAvailableNickname(principal), principal.studentNumber())
        );
    }

    private String resolveAvailableNickname(MemberPrincipal principal) {
        String baseNickname = normalizeBaseNickname(principal.name());
        if (!memberRepository.existsByNickname(baseNickname)) {
            return baseNickname;
        }

        String suffixSeed = buildSuffixSeed(principal);
        for (int suffixLength = 4; suffixLength <= suffixSeed.length(); suffixLength++) {
            String suffix = suffixSeed.substring(suffixSeed.length() - suffixLength);
            String candidate = appendSuffix(baseNickname, suffix);
            if (!memberRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }

        throw new BusinessException(MemberErrorCode.DUPLICATE_NICKNAME);
    }

    private String normalizeBaseNickname(String nickname) {
        String normalized = nickname == null ? "" : nickname.trim();
        if (normalized.isBlank()) {
            normalized = FALLBACK_NICKNAME;
        }
        return truncate(normalized, MAX_NICKNAME_LENGTH);
    }

    private String buildSuffixSeed(MemberPrincipal principal) {
        String studentNumber = sanitizeAlphaNumeric(principal.studentNumber());
        if (!studentNumber.isBlank()) {
            return studentNumber;
        }

        String ssoSub = sanitizeAlphaNumeric(principal.ssoSub());
        if (!ssoSub.isBlank()) {
            return ssoSub.toLowerCase();
        }

        return "user";
    }

    private String appendSuffix(String baseNickname, String suffix) {
        String sanitizedSuffix = sanitizeAlphaNumeric(suffix).toLowerCase();
        if (sanitizedSuffix.isBlank()) {
            sanitizedSuffix = "user";
        }

        int maxSuffixLength = Math.max(1, MAX_NICKNAME_LENGTH - 2);
        if (sanitizedSuffix.length() > maxSuffixLength) {
            sanitizedSuffix = sanitizedSuffix.substring(sanitizedSuffix.length() - maxSuffixLength);
        }

        int baseLimit = Math.max(1, MAX_NICKNAME_LENGTH - sanitizedSuffix.length() - 1);
        return truncate(baseNickname, baseLimit) + "-" + sanitizedSuffix;
    }

    private String sanitizeAlphaNumeric(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (char ch : value.toCharArray()) {
            if (Character.isLetterOrDigit(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
