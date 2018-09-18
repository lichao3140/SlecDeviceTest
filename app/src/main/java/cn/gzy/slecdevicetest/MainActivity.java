package cn.gzy.slecdevicetest;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.wits.serialport.SerialPort;
import com.wits.serialport.SerialPortManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

public class MainActivity extends AppCompatActivity {

    private SerialPortManager mSerialPortManager;
    private InputStream mInputStream4;
    private OutputStream mOutputStream4;
    private Handler handler;
    //    private InputStream mInputStream;
    //private OutputStream mOutputStream;
    private String icCard = "";
    private TextView resultTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();

        mSerialPortManager = new SerialPortManager();
        findviews();
        new SLecDeviceThread().start();
    }

    private void findviews() {

        resultTv = (TextView) findViewById(R.id.resultTv);
    }

    /**
     * @param view
     */
    public void doClick(View view) {
        switch (view.getId()) {
            case R.id.openRelay:
                if (mOutputStream4 == null) {
                    Toast.makeText(this, "请先打开串口", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    byte[] bytes1 = SlecProtocol.hexStringToBytes(new String[]{
                                    "55555555",  //用户id,8个字符，缺少的前面补0
                                    "12345678",//用户卡号,8个字符，缺少的的前面补0
                                    "0001"}//开门间隔,4个字符，缺少的的前面补0
                            , true);
                    byte[] bytes = SlecProtocol.commandAndDataToAscii(
                            ((byte) 0x01),
                            bytes1
                    );

                    mOutputStream4.write(bytes);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.openRelay2:
                if (mOutputStream4 == null) {
                    Toast.makeText(this, "请先打开串口", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    long cardNum = 1431655765L;
                    String cardStr = Long.toHexString(cardNum);
                    for (int i = 0; i < 8 - cardStr.length(); i++) {
                        cardStr = "0" + cardStr;
                    }
                    byte[] bytes1 = SlecProtocol.hexStringToBytes(new String[]{
                                    "55555555",  //用户id,8个字符，缺少的前面补0
                                    cardStr,//用户卡号,8个字符，缺少的前面补0
                                    "0001",//开锁时长,4个字符，缺少的前面补0
                                    "01",//00是用户id，01是用户卡号
                                    "01",//00是韦根26，01是韦根34
                                    "00"}//00是顺序，01是倒序
                            , true);
                    byte[] bytes = SlecProtocol.commandAndDataToAscii(
                            ((byte) 0x06),
                            bytes1
                    );

                    Log.e("gzy", "sendToSerial: " + SlecProtocol.bytesToHexString(bytes));


                    mOutputStream4.write(bytes);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            /**
             * 以下接口用于修复开关时间不准确问题
             * 可以自行控制开关
             *
             * openRelay4：发送最长时间的开关信号（相当于是开信号），
             * 如果需要发送韦根数据，这个数据里面 的用户id和用户卡号不能是0
             *
             *
             *
             *
             * closeRelay4:发送一个0时间的开关信号（相当于关闭继电器）。
             * 发送的数据里面的用户id和用户卡号必须全部为0（否则会发送重复数据）
             *
             *
             */


            case R.id.openRelay4:
                if (mOutputStream4 == null) {
                    Toast.makeText(this, "请先打开串口", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    byte[] bytes1 = SlecProtocol.hexStringToBytes(new String[]{
                                    "55555555",  //用户id,8个字符，缺少的前面补0
                                    "12345678",//用户卡号,8个字符，缺少的的前面补0
                                    "FFFF"}//开门间隔,4个字符，缺少的的前面补0
                            , true);
                    byte[] bytes = SlecProtocol.commandAndDataToAscii(
                            ((byte) 0x01),
                            bytes1
                    );

                    mOutputStream4.write(bytes);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.closeRelay4:
                if (mOutputStream4 == null) {
                    Toast.makeText(this, "请先打开串口", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    byte[] bytes1 = SlecProtocol.hexStringToBytes(new String[]{
                                    "00000000",  //用户id,8个字符，缺少的前面补0
                                    "00000000",//用户卡号,8个字符，缺少的的前面补0
                                    "0000"}//开门间隔,4个字符，缺少的的前面补0
                            , true);
                    byte[] bytes = SlecProtocol.commandAndDataToAscii(
                            ((byte) 0x01),
                            bytes1
                    );

                    mOutputStream4.write(bytes);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            default:
                break;

        }
    }



    private class SLecDeviceThread extends Thread {
        @Override
        public void run() {
            try {
                //串口4，继电器控制
                SerialPort serialPort4 = mSerialPortManager.getSerialPort4();
                mInputStream4 = serialPort4.getInputStream();
                mOutputStream4 = serialPort4.getOutputStream();


                sleep(500);
/*
                //串口1，读卡器
                SerialPort serialPort = mSerialPortManager.getSerialPort();
                mInputStream = serialPort.getInputStream();
                //mOutputStream = serialPort.getOutputStream();*/

                while (true) {
                    try {
                        sleep(50);
                        byte[] buffer = new byte[64];
                        if (mInputStream4 == null) {
                            continue;
                        }
                        int size = mInputStream4.read(buffer);

                        if (size < 1) {
                            continue;
                        }

                        int len = icCard.length();
                        Log.e("gzy", "run: " + size + "--" + icCard);
                        if (len == 0) {
                            //第一条数据
                            icCard = SlecProtocol.bytesToHexString2(buffer, size);
                        } else {
                            //之前已经有数据
                            icCard = icCard + SlecProtocol.bytesToHexString2(buffer, size);
                        }
                        handler.removeCallbacks(cancelCardRunnable);
                        //200ms没有新的数据就发送
                        handler.postDelayed(cancelCardRunnable, 200);

                    } catch (SecurityException e) {
                        Log.e("SerialPort", "-----------------SecurityException");
                    } catch (IOException e) {
                        Log.e("SerialPort", "-----------------IOException" + e.toString());
                    } catch (InvalidParameterException e) {
                        Log.e("SerialPort", "-----------------InvalidParameterException");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 退出读卡状态
     */
    private Runnable cancelCardRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                //处理icCard
                Log.e("gzy", "接收到的串口数据为: " + icCard);

                final byte[] bytes = SlecProtocol.asciiToHex(SlecProtocol.hexToByteArray(icCard));
                if (bytes.length > 5) {
                    Log.e("gzy", "接收转换: " + SlecProtocol.bytesToHexString2(bytes, bytes.length) +
                            "--命令：" + bytes[3] +
                            "--数据长度：" + bytes[5] +
                            "--数据：" + (bytes[5] == 0 ? "没有数据" : bytes[6])
                    );

                    switch (bytes[3]) {
                        case 1:
                            if (bytes[6] == 0) {
                                Log.e("gzy", "run: 发送开门指令成功");
                            } else {
                                Log.e("gzy", "run: 发送开门指令失败");
                            }
                            break;
                        case 2:
                            //刷卡
//                        6-9是用户id，10-13是卡号
                            if (bytes.length > 13) {
                                final byte[] card = new byte[4];
                                for (int i = 0; i < card.length; i++) {
//                                card[i]=bytes[13-i];
                                    card[i] = bytes[10 + i];
                                }
//                                try {
//                                    byte[] bytes2 = SlecProtocol.hexStringToBytes(new String[]{
//                                                    "55555555",  //用户id,8个字符，缺少的前面补0
//                                                    SlecProtocol.bytesToHexString2(card, card.length),//用户卡号,8个字符，缺少的前面补0
//                                                    "0001",//开锁时长,4个字符，缺少的前面补0
//                                                    "00",//00是用户id，01是用户卡号
//                                                    "01",//00是韦根26，01是韦根34
//                                                    "00"}//00是顺序，01是倒序
//                                            , true);
//                                    byte[] bytes3 = SlecProtocol.commandAndDataToAscii(
//                                            ((byte) 0x06),
//                                            bytes2
//                                    );
//
//                                    mOutputStream4.write(bytes3);
//
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        resultTv.setText("卡号为: " + SlecProtocol.bytesToHexString2(card, card.length));
                                    }
                                });
                                handler.removeCallbacks(cleanResultRunnable);
                                handler.postDelayed(cleanResultRunnable, 2000);
                            }

                            break;
                        default:
                    }
                }


                //重置
                icCard = "";

            } catch (Exception e2) {
                e2.printStackTrace();
                //重置
                icCard = "";
            }

            //重置
            icCard = "";
        }
    };


    private Runnable cleanResultRunnable = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    resultTv.setText("");
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSerialPortManager.closeSerialPort4();
    }
}
