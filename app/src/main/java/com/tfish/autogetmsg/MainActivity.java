package com.tfish.autogetmsg;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    RxPermissions rxPermissions = new RxPermissions(this);

    private Disposable disposable;
    EditText editText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.edit_text);

        regRecever();


        disposable = rxPermissions
                .request(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                .subscribe(granted -> {
                    if (granted) {
                        regRecever();
                    } else {
                        Toast.makeText(this, "请先同意权限", Toast.LENGTH_SHORT).show();
                    }
                });


    }

    private void regRecever() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED ");
        intentFilter.setPriority(1000);
        registerReceiver(new SMSReceiver(new SMSReceiver.OnSmsListener() {
            @Override
            public void onReceiver(String string) {
                editText.setText(string);
            }
        }), intentFilter);
    }

    static class SMSReceiver extends BroadcastReceiver {

        interface OnSmsListener {
            void onReceiver(String string);
        }

        OnSmsListener receiver;

        public SMSReceiver(OnSmsListener receiver) {
            this.receiver = receiver;
        }

        private static final String TAG = "SMSReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            //进行获取短信的操作
            getMsg(context, intent);

        }

        private void getMsg(Context context, Intent intent) {
            //pdus短信单位pdu
            //解析短信内容
            Object[] pdus = (Object[]) intent.getExtras().get("pdus");
            assert pdus != null;
            for (Object pdu : pdus) {
                //封装短信参数的对象
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                String number = sms.getOriginatingAddress();
                String body = sms.getMessageBody();
                //写自己的处理逻辑
                //获取短信验证码
                getCode(context, body);
            }
        }

        private void getCode(Context context, String body) {
            Pattern pattern1 = Pattern.compile("(\\d{6})");//提取六位数字
            Matcher matcher1 = pattern1.matcher(body);//进行匹配

            Pattern pattern2 = Pattern.compile("(\\d{4})");//提取四位数字
            Matcher matcher2 = pattern2.matcher(body);//进行匹配

            if (matcher1.find()) {//匹配成功
                String code = matcher1.group(0);
                receiver.onReceiver(code);
                Log.d(TAG, "onReceive: " + code);
            } else if (matcher2.find()) {
                String code = matcher2.group(0);
                Toast.makeText(context, "验证码复制成功", Toast.LENGTH_SHORT).show();
                receiver.onReceiver(code);
                Log.d(TAG, "onReceive: " + code);
            } else {
                Toast.makeText(context, "未检测到验证码", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onReceive: " + "未检测到验证码");
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
