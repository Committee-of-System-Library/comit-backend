package kr.ac.knu.comit.auth.dto;

public sealed interface SsoCallbackResult
        permits SsoCallbackSuccess, SsoCallbackRejected, SsoCallbackPendingRegistration {
}
