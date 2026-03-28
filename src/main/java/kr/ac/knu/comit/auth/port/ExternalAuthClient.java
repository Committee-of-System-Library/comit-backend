package kr.ac.knu.comit.auth.port;

public interface ExternalAuthClient {

    String buildLoginRedirectUrl(String state);

    ExternalIdentity verify(String token);
}
