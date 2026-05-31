package kr.ac.knu.comit.auth.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.ac.knu.comit.auth.dto.RegisterPrefillResponse;
import kr.ac.knu.comit.auth.dto.RegisterProfileImagePresignedRequest;
import kr.ac.knu.comit.auth.dto.RegisterRequest;
import kr.ac.knu.comit.global.docs.annotation.ApiContract;
import kr.ac.knu.comit.global.docs.annotation.ApiDoc;
import kr.ac.knu.comit.global.docs.annotation.ApiError;
import kr.ac.knu.comit.global.docs.annotation.Example;
import kr.ac.knu.comit.global.docs.annotation.FieldDesc;
import kr.ac.knu.comit.global.exception.ApiResponse;
import kr.ac.knu.comit.image.dto.PresignedUploadResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiContract
@RequestMapping("/auth/register")
public interface RegisterControllerApi {

    @ApiDoc(
            summary = "회원가입 prefill 조회",
            description = "SSO 토큰 쿠키를 검증하고 회원가입 폼에 미리 채울 값을 반환합니다.",
            descriptions = {
                    @FieldDesc(name = "name", value = "SSO JWT에서 읽은 실명입니다."),
                    @FieldDesc(name = "studentNumber", value = "SSO JWT에서 읽은 학번입니다. 없으면 null입니다."),
                    @FieldDesc(name = "major", value = "SSO JWT에서 읽은 세부전공입니다. 없으면 null입니다.")
            },
            errors = {
                    @ApiError(code = "UNAUTHORIZED", when = "SSO 토큰 쿠키가 없거나 유효하지 않을 때"),
                    @ApiError(code = "MEMBER_ALREADY_EXISTS", when = "이미 가입된 회원이 다시 prefill을 조회할 때")
            },
            example = @Example(
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "name": "홍길동",
                                "studentNumber": "2023000001",
                                "major": "심화"
                              }
                            }
                            """
            )
    )
    @GetMapping("/prefill")
    ResponseEntity<ApiResponse<RegisterPrefillResponse>> getPrefill(HttpServletRequest request);

    @ApiDoc(
            summary = "회원가입 프로필 이미지 업로드용 presigned URL 발급",
            description = "회원가입 전 단계에서 SSO 토큰 쿠키를 검증한 뒤 프로필 이미지 업로드용 presigned URL을 발급합니다.",
            descriptions = {
                    @FieldDesc(name = "fileName", value = "업로드할 파일 이름입니다."),
                    @FieldDesc(name = "contentType", value = "업로드할 파일의 MIME 타입입니다."),
                    @FieldDesc(name = "presignedUrl", value = "S3 업로드에 사용할 presigned URL입니다."),
                    @FieldDesc(name = "imageUrl", value = "회원가입 요청의 profileImageUrl에 전달할 최종 이미지 URL입니다.")
            },
            errors = {
                    @ApiError(code = "UNAUTHORIZED", when = "SSO 토큰 쿠키가 없거나 유효하지 않을 때"),
                    @ApiError(code = "FORBIDDEN", when = "외부 사용자(EXTERNAL)가 호출할 때"),
                    @ApiError(code = "MEMBER_ALREADY_EXISTS", when = "이미 가입된 회원이 호출할 때"),
                    @ApiError(code = "UNSUPPORTED_FILE_TYPE", when = "지원하지 않는 파일 형식으로 요청할 때"),
                    @ApiError(code = "INVALID_REQUEST", when = "요청 본문이 검증 규칙을 만족하지 않을 때")
            },
            example = @Example(
                    request = """
                            {
                              "fileName": "profile.png",
                              "contentType": "image/png"
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS",
                              "data": {
                                "presignedUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/members/profile.png?signature=...",
                                "imageUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/members/profile.png"
                              }
                            }
                            """
            )
    )
    @PostMapping("/profile-image/presigned")
    ResponseEntity<ApiResponse<PresignedUploadResponse>> createProfileImagePresignedUpload(
            @RequestBody @Valid RegisterProfileImagePresignedRequest request,
            HttpServletRequest httpServletRequest
    );

    @ApiDoc(
            summary = "회원가입 완료",
            description = "SSO 토큰 쿠키와 사용자가 입력한 닉네임, 연락처, 약관 동의 여부로 회원가입을 완료합니다.",
            descriptions = {
                    @FieldDesc(name = "nickname", value = "1자 이상 15자 이하의 닉네임입니다."),
                    @FieldDesc(name = "phone", value = "숫자와 하이픈만 포함한 10~15자 연락처입니다."),
                    @FieldDesc(name = "profileImageUrl", value = "프로필 이미지 URL입니다. 생략하거나 null로 보내면 프로필 이미지 없이 가입됩니다."),
                    @FieldDesc(name = "agreedToTerms", value = "약관 동의 여부입니다. true여야만 회원가입이 완료됩니다.")
            },
            errors = {
                    @ApiError(code = "INVALID_REQUEST", when = "약관 동의가 false이거나 요청 형식이 올바르지 않을 때"),
                    @ApiError(code = "UNAUTHORIZED", when = "SSO 토큰 쿠키가 없거나 유효하지 않을 때"),
                    @ApiError(code = "MEMBER_ALREADY_EXISTS", when = "이미 가입된 ssoSub로 다시 회원가입을 시도할 때"),
                    @ApiError(code = "DUPLICATE_NICKNAME", when = "이미 사용 중인 닉네임으로 회원가입을 시도할 때")
            },
            example = @Example(
                    request = """
                            {
                              "nickname": "길동이",
                              "phone": "010-1234-5678",
                              "profileImageUrl": "https://bucket.s3.ap-northeast-2.amazonaws.com/members/uuid.png",
                              "agreedToTerms": true
                            }
                            """,
                    response = """
                            {
                              "result": "SUCCESS"
                            }
                            """
            )
    )
    @PostMapping
    ResponseEntity<ApiResponse<Void>> register(
            @RequestBody @Valid RegisterRequest request,
            HttpServletRequest httpServletRequest
    );
}
