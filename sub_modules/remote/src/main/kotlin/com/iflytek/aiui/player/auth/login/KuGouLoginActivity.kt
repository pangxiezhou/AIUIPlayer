package com.iflytek.aiui.player.auth.login

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import com.iflytek.aiui.player.auth.PlayerRemote
import com.iflytek.aiui.player.common.rpc.method.TokenReq
import com.iflytek.aiui.player.init.ThirdPartyPlayers

import com.iflytek.aiui.player_auth.R
import com.kugou.kgmusicsdk.IKGMusicHttpResp
import com.kugou.kgmusicsdk.KGMusicSDK

import kotlinx.android.synthetic.main.activity_kugou_login.*
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.*
import kotlin.concurrent.schedule

class KuGouLoginActivity : AppCompatActivity() {
    private var tokenReq: TokenReq? = null
    private var mLastVerifyKey: String? = null

    private val mTimer = Timer()
    private var mLastCountDownTask: TimerTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenReq = intent.getParcelableExtra("request")

        ThirdPartyPlayers.initKuGouMusic()

        setContentView(R.layout.activity_kugou_login)
        sms_code.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        kugou_login.setOnClickListener { attemptLogin() }
        graphic_code_img.setOnClickListener { refreshGraphicCode() }
        req_sms_btn.setOnClickListener { requestSMSCode() }
        register_link.setOnClickListener {
            val link = Uri.parse("http://www.kugou.com/")
            startActivity(Intent(Intent.ACTION_VIEW, link))
        }

        refreshGraphicCode()
    }

    private fun attemptLogin() {
        clearError()

        // Store values at the time of the login attempt.
        val phoneNum = phone.text.toString()
        val graphicCode = graphic_code.text.toString()
        val smsCode = sms_code.text.toString()

        var cancel = false
        var focusView: View? = null


        // 检测断行验证码有效性
        if (!isSMSCodeValid(smsCode)) {
            sms_code.error = getString(R.string.error_invalid_sms_code)
            focusView = sms_code
            cancel = true
        }

        // 检测图形验证码有效性
        if (!isGraphicCodeValid(graphicCode)) {
            graphic_code.error = getString(R.string.error_invalid_graphic_code)
            focusView = graphic_code
            cancel = true
        }

        // 检测手机号码有效性
        if (!isMobileNO(phoneNum)) {
            phone.error = getString(R.string.error_invalid_phone)
            focusView = phone
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            showProgress(true)
            login(phoneNum, smsCode)
        }
    }


    private fun login(phone: String, smsCode: String) {
        KGMusicSDK.sharedInstance().loginWithMobile(this, phone, smsCode, object : IKGMusicHttpResp {
            override fun onSuccess(jsonObject: JSONObject) {
                val message = if (jsonObject.optInt("status") == 1) {
                    val data = jsonObject.optJSONObject("data")
                    val token = data.optString("token")
                    val userId = data.optInt("userid")
                    if(tokenReq != null) {
                        PlayerRemote.rpcProxy?.response(tokenReq!!, "$userId#$token")
                    }
                    this@KuGouLoginActivity.finish()
                    "登录成功"
                } else {
                    val errorCode = jsonObject.optInt("error_code")
                    refreshGraphicCode()
                    when (errorCode) {
                        30709 -> {
                            onGraphicCodeError()
                            "请输入图形验证码后重试"
                        }
                        20021 -> {
                            runOnUiThread {
                                sms_code.error = getString(R.string.error_invalid_sms_code)
                                sms_code.requestFocus()
                            }
                            "短信验证码输入错误，请确认"
                        }
                        20020 -> {
                            "该手机号未注册，请前往酷狗官网进行注册"
                        }
                        else -> {
                            "登录失败: $errorCode"
                        }
                    }
                }

                runOnUiThread {
                    showProgress(false)
                    showToast(message)
                    refreshGraphicCode()
                }
            }

            override fun onFail(s: String) {
                runOnUiThread {
                    showProgress(false)
                    showToast("登录失败 $s")
                }
            }
        })
    }


    private fun refreshGraphicCode() {
        KGMusicSDK.sharedInstance().requestImgCodeEx(object : IKGMusicHttpResp {
            override fun onSuccess(jsonObject: JSONObject) {
                if(jsonObject.optInt("status") == 1) {
                    val dataObject = jsonObject.optJSONObject("data")
                    mLastVerifyKey = dataObject.optString("verifykey")
                    val verifyCode = dataObject.optString("verifycode")
                    val data = Base64.decode(verifyCode, android.util.Base64.DEFAULT)
                    val inputStream = ByteArrayInputStream(data)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    runOnUiThread {
                        graphic_code_img.setImageBitmap(bitmap)
                    }
                } else {
                    val errorCode = jsonObject.optInt("error_code")
                    showToast("获取图形验证码失败 $errorCode")
                }
            }

            override fun onFail(s: String) {
                showToast("获取图形验证码失败 $s")
            }
        })
    }

    private fun requestSMSCode() {
        clearError()

        val phoneNum = phone.text.toString()
        val graphicCode = graphic_code.text.toString()

        var cancel = false
        var focusView: View? = null

        // 检测图形验证码有效性
        if (!isGraphicCodeValid(graphicCode)) {
            graphic_code.error = getString(R.string.error_invalid_graphic_code)
            focusView = graphic_code
            cancel = true
        }

        // 检测手机号码有效性
        if (!isMobileNO(phoneNum)) {
            phone.error = getString(R.string.error_invalid_phone)
            focusView = phone
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
            return
        }

        KGMusicSDK.sharedInstance().requestVerifyCode(phoneNum, graphicCode, mLastVerifyKey, object : IKGMusicHttpResp {
            override fun onSuccess(jsonObject: JSONObject) {
                val message = if (jsonObject.optInt("status") == 1) {
                    runOnUiThread {
                        sms_countdown.visibility = View.VISIBLE
                        req_sms_btn.visibility = View.GONE
                    }

                    var countDown = 30
                    mLastCountDownTask = mTimer.schedule(0, 1000) {
                        runOnUiThread {
                            countDown--
                            if(countDown == 0) {
                                sms_countdown.visibility = View.GONE
                                req_sms_btn.visibility = View.VISIBLE
                                req_sms_btn.text = "重新发送"
                                cancel()
                            } else {
                                sms_countdown.text = "${countDown}秒"
                            }
                        }
                    }

                    "短信验证码发送成功"
                } else {
                    val errorCode = jsonObject.optInt("error_code")
                    refreshGraphicCode()
                    when (errorCode) {
                        30709 -> {
                            onGraphicCodeError()
                            "请输入图形验证码后重试"
                        }

                        20020 -> {
                            onGraphicCodeError()
                            "图形验证码失效"
                        }

                        20021 -> {
                            onGraphicCodeError()
                            "图形验证码错误"
                        }

                        else -> {
                            "短信验证码获取失败 错误：$errorCode"
                        }
                    }
                }

                showToast(message)
            }

            override fun onFail(s: String) {
                refreshGraphicCode()
                showToast("短信验证码获取失败 $s")
            }
        })
    }

    fun onGraphicCodeError() {
        runOnUiThread {
            graphic_code.error = getString(R.string.error_invalid_graphic_code)
            graphic_code.requestFocus()
        }
    }

    private fun clearError() {
        runOnUiThread {
            // Reset errors.
            phone.error = null
            graphic_code.error = null
            sms_code.error = null
        }
    }

    private fun isSMSCodeValid(smsCode: String): Boolean {
        return Regex("""\d{6}""").matches(smsCode)
    }

    private fun isGraphicCodeValid(graphicCode: String): Boolean {
        return Regex("""[0-9a-zA-Z]{4}""").matches(graphicCode)
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            login_form.visibility = if (show) View.GONE else View.VISIBLE
            login_form.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 0 else 1).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_form.visibility = if (show) View.GONE else View.VISIBLE
                        }
                    })

            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_progress.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 1 else 0).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_progress.visibility = if (show) View.VISIBLE else View.GONE
                        }
                    })
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_form.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@KuGouLoginActivity, message, Toast.LENGTH_SHORT).show()
        }
    }


        private fun isMobileNO(phoneNumber: String): Boolean {
            /**
             * 判断字符串是否符合手机号码格式
             * 移动号段: 134,135,136,137,138,139,147,150,151,152,157,158,159,170,178,182,183,184,187,188
             * 联通号段: 130,131,132,145,155,156,170,171,175,176,185,186
             * 电信号段: 133,149,153,170,173,177,180,181,189
             */
            // "[1]"代表下一位为数字可以是几，"[0-9]"代表可以为0-9中的一个，"[5,7,9]"表示可以是5,7,9中的任意一位,[^4]表示除4以外的任何一个,\\d{9}"代表后面是可以是0～9的数字，有9位。
            val telRegex = Regex("^((13[0-9])|(14[5,7,9])|(15[^4])|(18[0-9])|(17[0,1,3,5,6,7,8]))\\d{8}$")
            return if (TextUtils.isEmpty(phoneNumber))
                false
            else
                phoneNumber.matches(telRegex)
        }
}
