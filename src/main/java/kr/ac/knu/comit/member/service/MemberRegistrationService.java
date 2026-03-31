package kr.ac.knu.comit.member.service;

import java.time.LocalDateTime;
import kr.ac.knu.comit.global.exception.BusinessException;
import kr.ac.knu.comit.global.exception.MemberErrorCode;
import kr.ac.knu.comit.member.domain.Member;
import kr.ac.knu.comit.member.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberRegistrationService {

    private final MemberRepository memberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Member register(
            String ssoSub,
            String name,
            String phone,
            String nickname,
            String studentNumber,
            String majorTrack
    ) {
        String normalizedNickname = nickname == null ? null : nickname.trim();
        if (normalizedNickname != null && !normalizedNickname.isBlank() && memberRepository.existsByNickname(normalizedNickname)) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        try {
            return memberRepository.saveAndFlush(
                    Member.create(
                            ssoSub,
                            name,
                            phone,
                            nickname,
                            studentNumber,
                            majorTrack,
                            LocalDateTime.now()
                    )
            );
        } catch (DataIntegrityViolationException exception) {
            if (isNicknameCollision(exception)) {
                throw new BusinessException(MemberErrorCode.DUPLICATE_NICKNAME);
            }
            if (isSsoSubCollision(exception)) {
                throw new BusinessException(MemberErrorCode.MEMBER_ALREADY_EXISTS);
            }
            throw exception;
        }
    }

    private boolean isNicknameCollision(DataIntegrityViolationException exception) {
        String message = normalizedConstraintMessage(exception);
        return message.contains("uk_member_nickname") || message.contains("nickname");
    }

    private boolean isSsoSubCollision(DataIntegrityViolationException exception) {
        String message = normalizedConstraintMessage(exception);
        return message.contains("uk_member_sso_sub") || message.contains("sso_sub");
    }

    private String normalizedConstraintMessage(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause() == null
                ? exception.getMessage()
                : exception.getMostSpecificCause().getMessage();
        return message == null ? "" : message.toLowerCase();
    }
}
