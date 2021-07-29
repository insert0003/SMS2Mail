package me.hatcloud.sms2mail.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.database.Cursor
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import me.hatcloud.sms2mail.Sms2MailApp
import me.hatcloud.sms2mail.core.MailPasswordAuthenticator
import me.hatcloud.sms2mail.core.Sms2MailService
import me.hatcloud.sms2mail.core.SmsObserver
import me.hatcloud.sms2mail.data.MailInfo
import me.hatcloud.sms2mail.data.Sms
import android.net.Uri
import java.util.*

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result

private const val REQUEST_CODE_ASK_PERMISSIONS = 124


fun getAllSmsFromPhone(): List<Sms> {
    val cur = getSmsContentObserverCursor() ?: return ArrayList()
    val smsList = ArrayList<Sms>()
    while (cur.moveToNext()) {
        Sms(cur).let { smsList.add(it) }
    }
    cur.close()
    return smsList
}

fun getSmsContentObserverCursor(): Cursor? {
    return Sms2MailApp.getInstance().contentResolver?.query(SMS_INBOX_URI, SMS_PROJECTION, null, null
            , "date desc")
}
fun getSmsContentObserverCursor(uri: Uri?): Cursor? {
    return uri?.let {
        Sms2MailApp.getInstance().contentResolver?.query(it, SMS_PROJECTION, null, null
                , "date desc")
    }
}

fun registerSmsObserver(smsObserver: SmsObserver) {
    Sms2MailApp.getInstance().contentResolver.registerContentObserver(SMS_INBOX_URI, true, smsObserver);
}

fun unregisterSmsObserver(smsObserver: SmsObserver) {
    Sms2MailApp.getInstance().contentResolver.unregisterContentObserver(smsObserver);
}

fun checkPermission(activity: Activity, permission: String): Boolean =
        checkSelfPermission(activity, permission) == PermissionChecker.PERMISSION_GRANTED

fun requestPermission(activity: Activity, permission: String) {
    requestPermissions(activity, arrayOf(permission),
            REQUEST_CODE_ASK_PERMISSIONS);
}

fun addPermission(activity: Activity, permissionsList: MutableList<String>, permission: String): Boolean {
    if (!checkPermission(activity, permission)) {
        permissionsList.add(permission)
        // Check for Rationale Option
        if (!shouldShowRequestPermissionRationale(activity, permission))
            return false
    }
    return true
}

fun isSms2MailServiceRun(context: Context?): Boolean {
    if (context == null) {
        return false
    }
    var isRunning = false
    val activityManager = context
            .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val serviceList = activityManager
            .getRunningServices(30)

    if (serviceList.size <= 0) {
        return false
    }

    for (i in serviceList.indices) {
        if (serviceList[i].service.className == Sms2MailService::class.java.name) {
            isRunning = true
            break
        }
    }
    return isRunning
}

fun sendMail(mailInfo: MailInfo): Boolean {
    LogUtil.d("send mail: $mailInfo")
    val properties = mailInfo.getProperties() ?: return false

    val apiKey = mailInfo.sendgridApiKey
    println("apiKey: $apiKey")

    // 创建邮件发送者地址
    val from = mailInfo.sendgridAddress
    println("apifrom: $from")

    // 创建邮件的接收者地址，并设置到邮件消息中
    val to = mailInfo.toAddress
    // 邮件主题
    val subject = mailInfo.subject
    val fromName = mailInfo.userName
    val mailContent = mailInfo.content

    // 设置邮件消息的发送者
    val content = """
        {
            "personalizations": [
                {
                    "to": [
                        {
                            "email": "$to"
                        }
                    ]
                }
            ],
            "from": {
                "email": "$from",
                "name": "$fromName"
            },
            "subject": "$subject",
            "content": [
                {
                    "type": "text/plain",
                    "value": "$mailContent"
                },
                {
                    "type": "text/html",
                    "value": "<html><body>$mailContent</body></html>"
                }
            ]
        }
        """

    FuelManager.instance.baseHeaders = mapOf("Content-Type" to "application/json", "Authorization" to "Bearer $apiKey")
    "https://api.sendgrid.com/v3/mail/send".httpPost().body((content)).response { req, res, result ->
        when (result) {
            is Result.Failure -> {
                println("API call failed")
            }
            is Result.Success -> {
                println("API call succeed")
                val data = result.get()
                println(data)
            }
        }
        println(req)
        println(res)
        println(result)
    }

    return true
}