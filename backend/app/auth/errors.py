class AuthError(Exception):
    """Base authentication error."""


class InvalidTokenError(AuthError):
    """Token is malformed, has a bad signature, or is otherwise unverifiable."""


class ExpiredTokenError(AuthError):
    """Token's `exp` claim is in the past."""


class FirebaseVerificationError(AuthError):
    """firebase-admin failed to verify the ID token."""
