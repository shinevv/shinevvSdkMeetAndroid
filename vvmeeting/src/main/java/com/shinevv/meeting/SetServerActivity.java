package com.shinevv.meeting;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * @createDate: 2019/10/08 14:47
 * @author: houtong
 * @email: houtyim@gamil.com
 */
public class SetServerActivity extends AppCompatActivity {
    private EditText editTextServer;
    private EditText editTextTIp;
    private EditText editTextTPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_server);
        initView();
        initData();
    }

    private void initData() {
        SharedPreferences sp = getSharedPreferences("ip", MODE_PRIVATE);
        String name = sp.getString("server", "https://meet.shinevv.com:3451/api/");

        if (!name.equals("")) {
            editTextServer.setText(name);
        }

//        if (!ip.equals("")) {
//            editTextTIp.setText(ip);
//        }
//
//        if (!port.equals("")) {
//            editTextTPort.setText(port);
//        }

    }

    private void initView() {
        editTextServer = findViewById(R.id.set_server_server);
        editTextTIp = findViewById(R.id.set_server_ip);
        editTextTPort = findViewById(R.id.set_server_ip_port);
        editTextTIp.setVisibility(View.GONE);
        editTextTPort.setVisibility(View.GONE);
        findViewById(R.id.set_server_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.set_server_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String server = editTextServer.getText().toString();
                //   String ip = editTextTIp.getText().toString();
                // String port = editTextTPort.getText().toString();
                if (server != null && !server.equals("")) {
                    SharedPreferences sp = getSharedPreferences("ip", MODE_PRIVATE);
                    SharedPreferences.Editor edit = sp.edit();
                    edit.putString("server", server);
                    edit.commit();
                    Toast.makeText(SetServerActivity.this, "配置成功！！", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(SetServerActivity.this, "请输入服务器地址！", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
