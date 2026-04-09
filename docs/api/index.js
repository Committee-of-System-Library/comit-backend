window.API_DOCS = [
{
  title: "RegisterControllerApi",
  summary: "SSO 토큰 쿠키를 검증하고 회원가입 폼에 미리 채울 값을 반환합니다.",
  href: "./auth/RegisterControllerApi.html",
  endpointCount: 2,
  sectionPath: "auth"
},
{
  title: "SsoAuthControllerApi",
  summary: "auth-server가 전달한 custom JWT와 state를 검증한 뒤, 가입 상태에 따라 stage=success|register|error를 붙여 프론트로 리다이렉트합니다. 로그인 시작 시 저장된 redirectUri cookie가 있으면 그 URL을 우선 사용하고, 콜백 완료 후 redirectUri cookie를 제거합니다.",
  href: "./auth/SsoAuthControllerApi.html",
  endpointCount: 3,
  sectionPath: "auth"
},
{
  title: "AdminCommentControllerApi",
  summary: "관리자가 댓글을 소프트 삭제합니다.",
  href: "./comment/AdminCommentControllerApi.html",
  endpointCount: 3,
  sectionPath: "comment"
},
{
  title: "CommentControllerApi",
  summary: "게시글에 새 댓글 또는 대댓글을 작성합니다. parentCommentId가 없으면 일반 댓글, 있으면 해당 댓글의 대댓글로 생성됩니다.",
  href: "./comment/CommentControllerApi.html",
  endpointCount: 6,
  sectionPath: "comment"
},
{
  title: "ImageControllerApi",
  summary: "클라이언트가 S3에 직접 업로드할 수 있는 presigned URL을 발급합니다. 발급된 presignedUrl로 PUT 요청을 보내 파일을 업로드하고, imageUrl을 게시글/프로필 등록에 사용하세요. URL 유효시간은 10분입니다.",
  href: "./image/ImageControllerApi.html",
  endpointCount: 2,
  sectionPath: "image"
},
{
  title: "AdminMemberControllerApi",
  summary: "관리자가 회원 목록을 조회합니다. status로 필터링할 수 있습니다.",
  href: "./member/AdminMemberControllerApi.html",
  endpointCount: 2,
  sectionPath: "member"
},
{
  title: "MemberControllerApi",
  summary: "현재 로그인한 회원의 프로필을 조회합니다.",
  href: "./member/MemberControllerApi.html",
  endpointCount: 3,
  sectionPath: "member"
},
{
  title: "MyMemberControllerApi",
  summary: "내가 쓴 글/댓글/좋아요 수와 최근 3개 미리보기를 한 번에 반환합니다.",
  href: "./member/MyMemberControllerApi.html",
  endpointCount: 4,
  sectionPath: "member"
},
{
  title: "AdminPostControllerApi",
  summary: "관리자가 NOTICE, EVENT, INFO 게시판에 게시글을 작성합니다. QNA, FREE 게시판에는 작성할 수 없습니다.",
  href: "./post/AdminPostControllerApi.html",
  endpointCount: 4,
  sectionPath: "post"
},
{
  title: "PostControllerApi",
  summary: "새 게시글을 작성합니다. 제목은 최대 30자, 본문은 최대 500자까지 허용합니다.",
  href: "./post/PostControllerApi.html",
  endpointCount: 9,
  sectionPath: "post"
},
{
  title: "AdminReportControllerApi",
  summary: "관리자가 신고 상세 정보를 조회합니다.",
  href: "./report/AdminReportControllerApi.html",
  endpointCount: 3,
  sectionPath: "report"
}
];
