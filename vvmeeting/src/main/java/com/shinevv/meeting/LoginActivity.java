package com.shinevv.meeting;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.alibaba.fastjson.JSON;
import com.shinevv.data.Constants;
import com.shinevv.modles.BaseServerRes;
import com.shinevv.modles.LoginParam;
import com.shinevv.modles.VVAuthResponse;
import com.shinevv.vvroom.AccountManager;
import com.shinevv.vvroom.BaseActivity;
import com.shinevv.vvroom.MainActivityMeet;
import com.shinevv.vvroom.Shinevv;
import com.shinevv.vvroom.VVRoomApplication;
import com.shinevv.vvroom.modles.VVUser;

import org.webrtc.Logging;

import java.util.concurrent.TimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @createDate: 2019/10/08 14:48
 * @author: houtong
 * @email: houtyim@gamil.com
 * @description:
 */
public class LoginActivity extends AppCompatActivity {
    public static final String TAG = LoginActivity.class.getSimpleName();
    protected EditText etRoomNumber;
    protected EditText etRoomPassword;
    protected EditText etNickName;
    protected EditText etMediaServerAddress;
    protected RadioGroup rgMode;
    private View vLogin;

    protected String roomId;
    protected String roomPassword;
    protected String displayName;
    protected boolean roleTeacher;
    protected String mediaMode = Shinevv.MEDIA_KINE_NONE;
    private MaterialDialog loadingDialog;

    String[] permission = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acty_login);
        initView();
    }

    public void initView() {
        etRoomNumber = findViewById(R.id.room_number);
        etRoomPassword = findViewById(R.id.room_password);
        etNickName = findViewById(R.id.nick_name);
        etMediaServerAddress = findViewById(R.id.media_server_address);
        rgMode = findViewById(R.id.media_mode);
        vLogin = findViewById(R.id.login);

        TextView tvVersion = findViewById(R.id.vvroom_version);
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            tvVersion.setText(getString(R.string.app_name) + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        vLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginAction();
            }
        });

        findViewById(R.id.login_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SetServerActivity.class);
                startActivity(intent);
            }
        });
        rgMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.audio_mode) {
                    mediaMode = Shinevv.MEDIA_KINE_AUDIO;
                } else if (checkedId == R.id.video_mode) {
                    mediaMode = Shinevv.MEDIA_KINE_VIDEO;
                } else if (checkedId == R.id.visitor_mode) {
                    mediaMode = Shinevv.MEDIA_KINE_NONE;
                }
            }
        });
    }

    protected void LoginAction() {
        roomId = etRoomNumber.getText().toString().trim();
        if (TextUtils.isEmpty(roomId)) {
            Toast.makeText(this, getString(R.string.input_hint, getString(R.string.room_number)), Toast.LENGTH_LONG).show();
            return;
        }
        roomPassword = etRoomPassword.getText().toString().trim();
        if (TextUtils.isEmpty(roomPassword)) {
            Toast.makeText(this, getString(R.string.input_hint, getString(R.string.room_password)), Toast.LENGTH_LONG).show();
            return;
        }
        displayName = etNickName.getText().toString().trim();
        if (TextUtils.isEmpty(displayName)) {
            Toast.makeText(this, getString(R.string.input_hint, getString(R.string.nick_name)), Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences sp = getSharedPreferences("ip", MODE_PRIVATE);
        String server = sp.getString("server", "");
//        final String ip = sp.getString("ip", "");
//        final String port = sp.getString("port", "");
        if (server.equals("")) {
            Toast.makeText(LoginActivity.this, "请先配置服务地址！", Toast.LENGTH_LONG).show();
            return;
        }

        VVMeetingApplication.upDataAPI(server);
        final String mediaServerAddress = etMediaServerAddress.getText().toString().trim();

        showLoadingDialog(this, R.string.tips, R.string.loading);
        VVRoomApplication.getServerAPI()
                .login(roomId, roomPassword)
                .enqueue(new Callback<BaseServerRes<VVAuthResponse>>() {
                    @Override
                    public void onResponse(Call<BaseServerRes<VVAuthResponse>> call, Response<BaseServerRes<VVAuthResponse>> response) {
                        dismiassLoadingDialog();
                        Log.e(TAG, "onResponse: " + response);
                        if (response != null && response.isSuccessful()) {
                            if (response.body() != null) {
                                if (response.body().isSuccess()) {
                                    VVAuthResponse vvUser = response.body().getData();
                                    if (vvUser.isTeacher()) {
                                        Toast.makeText(LoginActivity.this, R.string.login_teacher_not_support, Toast.LENGTH_LONG).show();
                                    } else if (vvUser.isTutor()) {
                                        Toast.makeText(LoginActivity.this, R.string.login_tutor_not_support, Toast.LENGTH_LONG).show();
                                    } else if (vvUser.getRole().equals("chairman")) {
                                        Toast.makeText(LoginActivity.this, R.string.login_tutor_not_support, Toast.LENGTH_LONG).show();
                                    } else {
                                        // put nick name & room number
                                        vvUser.setDisplayName(displayName);
//                                        vvUser.getWss().setPort(vvUser.getWss().getPort());
//                                        vvUser.getWss().setServer(vvUser.getWss().getServer());
                                        // save
                                        AccountManager.getInstance().setRoomId(roomId);
                                        AccountManager.getInstance().setCurrentUser(response.body().getData());
                                        // jump page
                                        jumpToMainPage();
                                    }
                                } else {
                                    String message = response.body().getMessage();
                                    if (!TextUtils.isEmpty(message)) {
                                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(LoginActivity.this, R.string.request_fail, Toast.LENGTH_LONG).show();
                                    }
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, R.string.request_fail, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, R.string.request_fail, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseServerRes<VVAuthResponse>> call, Throwable t) {
                        Logging.e(TAG, t.getMessage());
                        dismiassLoadingDialog();
                        if (t instanceof TimeoutException) {
                            Toast.makeText(LoginActivity.this, R.string.request_timeout, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, R.string.no_connection, Toast.LENGTH_LONG).show();
                        }
                    }
                });

    }


    public void jumpToMainPage() {
        boolean checkResult = PermissionUtils.checkPermissionsGroup(this, permission);
        if (!checkResult) {
            PermissionUtils.requestPermissions(this, permission,
                    0);
            return;
        }

        Intent intent = new Intent(this, MainActivityMeet.class);
        intent.putExtra(Constants.INTENT_ORIENTATION, getScreenOrientation());
        intent.putExtra(Constants.INTENT_ROOM_NUMBER, roomId);
        intent.putExtra(Constants.INTENT_ROOM_PASSWORD, roomPassword);
        intent.putExtra(Constants.INTENT_NICK_NAME, displayName);
        intent.putExtra(Constants.INTENT_ROLE, roleTeacher ? Constants.INTENT_ROLE_TEACHER : Constants.INTENT_ROLE_STUDENT);
        intent.putExtra(Constants.INTENT_MEDIA_MODE, mediaMode);
        startActivity(intent);
    }

    private int getScreenOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    Logging.e(TAG, "Unknown screen orientation. Defaulting to " +
                            "portrait.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    Logging.e(TAG, "Unknown screen orientation. Defaulting to " +
                            "landscape.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 判断是否所有的权限都已经授予了
        boolean isAllGranted = true;
        for (int grant : grantResults) {
            if (grant != PackageManager.PERMISSION_GRANTED) {
                isAllGranted = false;
                break;
            }
        }

        if (isAllGranted) {

            Intent intent = new Intent(this, MainActivityMeet.class);
            intent.putExtra(Constants.INTENT_ORIENTATION, getScreenOrientation());
            intent.putExtra(Constants.INTENT_ROOM_NUMBER, roomId);
            intent.putExtra(Constants.INTENT_ROOM_PASSWORD, roomPassword);
            intent.putExtra(Constants.INTENT_NICK_NAME, displayName);
            intent.putExtra(Constants.INTENT_ROLE, roleTeacher ? Constants.INTENT_ROLE_TEACHER : Constants.INTENT_ROLE_STUDENT);
            intent.putExtra(Constants.INTENT_MEDIA_MODE, mediaMode);
            startActivity(intent);
        } else {
            // 弹出对话框告诉用户需要权限的原因, 并引导用户去应用权限管理中手动打开权限按钮
            // showPermissionDialog();
            AccountManager.getInstance().setRoomId("");
            AccountManager.getInstance().setCurrentUser(null);
            PermissionUtils.showNoPermissionTip(this, "请赋予相应的权限");
        }

    }

    public void showLoadingDialog(Context context, int title, int content) {
        if (loadingDialog == null) {
            loadingDialog = new MaterialDialog.Builder(context).progress(true, 100, false).build();
        }
        loadingDialog.setTitle(title);
        loadingDialog.setContent(content);
        loadingDialog.show();
    }

    public void dismiassLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}
