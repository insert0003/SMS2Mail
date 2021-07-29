package me.hatcloud.sms2mail.data

import me.hatcloud.sms2mail.utils.Coder
import me.hatcloud.sms2mail.utils.ConfigurationUtil
import java.security.spec.InvalidKeySpecException

data class Configuration(var email: String?,
                         var smtpHost: String?,
                         var smtpPort: String?,
                         var securityType: SecurityType,
                         var emailToForward: String?,
                         var sendgridAddress: String?,
                         var sendgridApiKey: String?,
                         var encryptedPassword: String? = "") {
    var password
        get() = Coder.decodeAES(encryptedPassword.toString(), ConfigurationUtil.passwordAesKey) ?: ""
        set(unencryptedPassword) {
            encryptedPassword = try {
                Coder.encodeAES(unencryptedPassword
                        , ConfigurationUtil.passwordAesKey) ?: ""
            } catch (e: InvalidKeySpecException) {
                ConfigurationUtil.clearPasswordKey()
                ""
            }
        }
}

enum class SecurityType(val value: Int) {
    NONE(0), SSL(1), TLS(2)
}

fun Int.toSecurityType(): SecurityType {
    return SecurityType.values().find { it.value == this } ?: SecurityType.NONE
}