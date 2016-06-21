package com.raise.bluetoothclient;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fota.bluetooth.protocol.TransferPackageInfo;
import com.fota.bluetooth.service.BluetoothFotaService;
import com.fota.bluetooth.utils.BluetoothUtils;
import com.fota.bluetooth.utils.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * 智能设备端界面
 */
public class ClientActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private TextView mTips;

    private ProgressBar mProgressBar;

    private ListView listView;
    private BTAdapter mAdapter;

    private Set<BluetoothDevice> mdatas;

    private BluetoothFotaService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTips = (TextView) findViewById(R.id.textview_display_info);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        listView = (ListView) findViewById(R.id.listview_bottom);

        mdatas = new HashSet<>();
        mAdapter = new BTAdapter(new ArrayList<>(mdatas), this);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);

        // 初始化蓝牙传输服务类
        mService = new BluetoothFotaService(this, mServiceHandler);
        dialog = new AlertDialog.Builder(this)
                .setTitle("蓝牙连接")
                .setMessage("正在连接到蓝牙设备")
                .create();
        // 蓝牙操作工具类
        BluetoothUtils.init(mBluetoothUtilHandler, 20 * 1000);
        // 设置升级包保存路径
        mService.client_set_file_path(Environment.getExternalStorageDirectory().getAbsolutePath() + "/update.zip");
    }

    public void click_start_discovery(View view) {
        // 查找手机端蓝牙设备
        BluetoothUtils.start_discovery(this);
    }

    private int down_progress;

    private Handler mServiceHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {

                case Constants.MESSAGE_DEVICE_NAME:
//                    Toast.makeText(ClientActivity.this, "连接上蓝牙设备：" + ((BluetoothDevice) msg.obj).getName(), Toast.LENGTH_SHORT).show();
                    printf("连接上蓝牙设备：" + ((BluetoothDevice) msg.obj).getName());
                    dialog.dismiss();
                    // 将穿戴设备端，系统版本号发给手机端
                    mService.client_check_version("v1.1.1");
                    printf("请求更新，当前设备版本：" + "v1.1.1");

                    break;
                case Constants.MESSAGE_READ_STRING:
                    printf("server:" + msg.obj);
                    switch (msg.arg1) {
                        case TransferPackageInfo.PACKAGE_TYPE_SERVER_HAS_NEW_VERSION:
                            Log.d("ClientActivity", "客户端请求服务器发送update文件.");
                            mService.client_transfer_file_start();
                            break;
                        case TransferPackageInfo.PACKAGE_TYPE_SERVER_IS_LATEST_VERSION:
                            Toast.makeText(ClientActivity.this, "已是最新版本.", Toast.LENGTH_SHORT).show();

                            break;
                    }
                    break;
                case Constants.MESSAGE_READ_FILE:
                    // 接收文件进度
                    int progress = msg.arg1;
                    // 接收文件总大小
                    int total_size = msg.arg2;
                    // 保存文件路径
                    String file_path = (String) msg.obj;
                    if (down_progress != progress) {
                        down_progress = progress;
                        printf(String.format("downloading,progress:%s,total_size:%s,file_path:%s", progress, total_size, file_path));
                        //下载进度，发给服务端
                        mService.client_post_progress(String.valueOf(progress));
                        if (down_progress == 100) {
//                            MobAgentPolicy.rebootUpgrade(ClientActivity.this,null);
                        }
                    }
                    break;
                case Constants.MESSAGE_STATE_CHANGE:
                    if (msg.arg1 == BluetoothFotaService.STATE_CONNECTING) {
                        dialog.show();
                    } else {
                        if (dialog.isShowing())
                            dialog.dismiss();
                    }
                    break;
            }
            return true;
        }
    });

    private Handler mBluetoothUtilHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothUtils.WHAT_FOUND_BT_DEVICE:
                    // 找到蓝牙设备
                    mdatas.add((BluetoothDevice) msg.obj);
                    mAdapter.setDatas(new ArrayList<>(mdatas));
                    break;
                case BluetoothUtils.WHAT_START_DISCOVERY:
                    // 开始查找蓝牙设备
                    mProgressBar.setVisibility(View.VISIBLE);
                    mdatas.clear();
                    mAdapter.setDatas(new ArrayList<>(mdatas));
                    break;
                case BluetoothUtils.WHAT_CANCEL_DISCOVERY:
                    // 取消查找蓝牙设备
                    mProgressBar.setVisibility(View.GONE);
                    break;
            }
            return true;
        }
    });

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BluetoothUtils.cancel_discovery(this);
        BluetoothDevice item = mAdapter.getItem(position);
        Log.d("ClientActivity", "connect:" + item.getName());
        // 连接手机端(蓝牙连接)
        mService.connect(item, true);

//        GATT_connecting(item);
    }
//
//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
//    private void GATT_connecting(BluetoothDevice device) {
//        BluetoothGatt bluetoothGatt = device.connectGatt(this, false, new BluetoothGattCallback() {
//            @Override
//            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//                super.onConnectionStateChange(gatt, status, newState);
//                Log.d("ClientActivity", "onConnectionStateChange() status = " + status + "  newState = " + newState);
//                if (newState == BluetoothProfile.STATE_CONNECTED) {
//                    Log.d("ClientActivity", "onConnectionStateChange() GATT 连接成功.");
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(ClientActivity.this, "GATT连接成功.", Toast.LENGTH_SHORT).show();
//
//                        }
//                    });
////                    gatt.discoverServices();
//
//                } else {
//                    Log.d("ClientActivity", "onConnectionStateChange() 连接失败.");
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(ClientActivity.this, "GATT连接失败.", Toast.LENGTH_SHORT).show();
//
//                        }
//                    });
//                }
//            }
//
//            @Override
//            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//                super.onServicesDiscovered(gatt, status);
//                Log.d("ClientActivity", "onServicesDiscovered() status = " + status);
//                if (gatt.getServices().size()>0)
//                    Log.d("ClientActivity" ,"onServicesDiscovered() services = "+gatt.getServices().get(0).toString());
//            }
//
//            @Override
//            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//                super.onCharacteristicRead(gatt, characteristic, status);
//                Log.d("ClientActivity" ,"onCharacteristicRead() ");
//            }
//
//            @Override
//            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//                super.onCharacteristicWrite(gatt, characteristic, status);
//                Log.d("ClientActivity" ,"onCharacteristicWrite() ");
//
//
//            }
//
//            @Override
//            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//                super.onCharacteristicChanged(gatt, characteristic);
//
//                Log.d("ClientActivity" ,"onCharacteristicChanged() ");
//            }
//
//            @Override
//            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
//                super.onDescriptorRead(gatt, descriptor, status);
//                Log.d("ClientActivity" ,"onDescriptorRead() ");
//            }
//
//            @Override
//            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
//                super.onDescriptorWrite(gatt, descriptor, status);
//                Log.d("ClientActivity" ,"onDescriptorWrite() ");
//            }
//
//            @Override
//            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
//                super.onReliableWriteCompleted(gatt, status);
//                Log.d("ClientActivity" ,"onReliableWriteCompleted() ");
//            }
//
//            @Override
//            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
//                super.onReadRemoteRssi(gatt, rssi, status);
//                Log.d("ClientActivity", "onReadRemoteRssi() rssi = " + rssi);
//            }
//
//            @Override
//            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
//                super.onMtuChanged(gatt, mtu, status);
//                Log.d("ClientActivity" ,"onMtuChanged() ");
//            }
//        });
//
//        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
//        defaultAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
//            @Override
//            public void onServiceConnected(int profile, BluetoothProfile proxy) {
//                Log.d("ClientActivity", "onServiceConnected() " + profile);
//                if (profile == BluetoothProfile.A2DP)
//                    m_bluetoothA2dp = (BluetoothA2dp) proxy;
//            }
//
//            @Override
//            public void onServiceDisconnected(int profile) {
//
//            }
//        }, BluetoothProfile.A2DP);
//
//    }
//
//    BluetoothA2dp m_bluetoothA2dp;
    AlertDialog dialog;

    /**
     * 显示提示消息
     *
     * @param tips
     */
    private void printf(String tips) {
        mTips.setText(mTips.getText() + "\n" + tips);
    }


    public void click_cancel_discovery(View view) {
        BluetoothUtils.cancel_discovery(this);
    }

    public void click_test(View view) {

    }
}
