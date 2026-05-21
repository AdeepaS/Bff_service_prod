package com.example.bff.error;

/**
 * Central registry of error codes for the BFF service.
 * Naming convention: <SERVICE>_<PROFILE>_<DOMAIN>_<SCENARIO>
 */
public enum BffErrorCode {

    // Generic fallbacks
    BFF_P1_UNCLASSIFIED_FAILURE,
    BFF_P2_UNCLASSIFIED_FAILURE,

    // Token / routing / gateway errors
    BFF_P1_TOKEN_INVALID,
    BFF_P1_TOKEN_EXPIRED,
    BFF_P1_ACCESS_DENIED,
    BFF_P2_BACKEND_CLIENT_ERROR,
    BFF_P2_BACKEND_SERVER_ERROR;

    public String code() {
        return name();
    }
}
