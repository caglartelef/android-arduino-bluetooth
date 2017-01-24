package fikirlaboratuvari.com.arduinobluetooth;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class main extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    boolean connectionstate = false;
    byte BtAdapterSayac = 0;
    Button btnOpen, btnFind, btnAyarlar, btnSendData;
    TextView MtxtVwState;
    EditText edTxt;
    String sGelenVeri;
    boolean bisChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnOpen = (Button) findViewById(R.id.btnOpen);/*Bluetooth açıp kapatmak buton tanımlıyoruz*/
        btnFind = (Button) findViewById(R.id.btnFind);/*Bluetooth açıldıktan sonra cihazımıza bağlanmak için buton tanımlıyoruz.*/
        btnAyarlar = (Button) findViewById(R.id.btnSettings);/*Bluetooth ayarları sayfasına gitmek için buton tanımlıyoruz.*/
        btnSendData = (Button) findViewById(R.id.btnSendData);/*Bluetooth ile veri göndermek için buton tanımlıyoruz.*/
        edTxt = (EditText) findViewById(R.id.editText);/*Veri girişi almak için edittext tanımlıyoruz.*/
        MtxtVwState = (TextView) findViewById(R.id.MtxtVwState);


        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!bisChecked) {
                    try {
                        openBT();
                        findBT();
                        openBT();
                        findBT();
                        openBT();
                        findBT();
                        MtxtVwState.setText("Bağlantı Açıldı");
                        bisChecked = true;
                        btnOpen.setText("Kapat");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        closeBT();
                        MtxtVwState.setText("Bağlantı Kapandı");
                        bisChecked = false;
                        btnOpen.setText("Bt Aç");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        btnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    openBT();
                    findBT();
                    openBT();
                    findBT();
                    openBT();
                    findBT();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });


        btnAyarlar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(main.this, btsettings.class));
                finish();
            }
        });

        btnSendData.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("EmptyCatchBlock")
            @Override
            public void onClick(View view) {

                try {
                    sendData(5);
                } catch (Exception ignored) {
                }
            }
        });

    }

    /**********************************************************************************************
     * onCreate End
     *********************************************************************************************/
    void openBT() throws IOException {

        /*Bluetooth u açıyoruz.*/
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard //SerialPortService I
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
            beginListenForData();/*Bluetooth üzerinden gelen verileri yakalamak için bir listener oluşturuyoruz.*/
        } catch (Exception ignored) {
        }

    }

    void findBT() {
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                MtxtVwState.setText("Bluetooth adaptörü bulunamadı");
            }
            if (BtAdapterSayac == 0) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetooth, 0);
                    BtAdapterSayac = 1;
                }
            }
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (("HC-05").equals(device.getName().toString())) {/*Eşleşmiş cihazlarda HC-05 adında cihaz varsa bağlantıyı aktişleştiriyoruz. Burada HC-05 yerine bağlanmasını istediğiniz Bluetooth adını yazabilirsiniz.*/
                        mmDevice = device;
                        MtxtVwState.setText("Bağlantı Bulundu");
                        connectionstate = true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    void closeBT() throws IOException {
        try {
            /*Aktif olan bluetooth bağlantımızı kapatıyoruz.*/
            if (mBluetoothAdapter.isEnabled()) {
                stopWorker = true;
                mBluetoothAdapter.disable();
                mmOutputStream.close();
                mmInputStream.close();
                mmSocket.close();
            } else {
            }
        } catch (Exception ignored) {
        }
    }

    void sendData(int data) throws IOException {
        try {
            if (connectionstate) {
                /*Bluetooth bağlantımız aktifse veri gönderiyoruz.*/
                mmOutputStream.write(String.valueOf(data).getBytes());
            }
        } catch (Exception ignored) {
        }
    }

    void beginListenForData() {
        try {
            final Handler handler = new Handler();
            final byte delimiter = 10; //This is the ASCII code for a newline character

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];
            workerThread = new Thread(new Runnable() {
                public void run() {
                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                        try {
                            int bytesAvailable = mmInputStream.available();
                            if (bytesAvailable > 0) {
                                byte[] packetBytes = new byte[bytesAvailable];
                                mmInputStream.read(packetBytes);
                                for (int i = 0; i < bytesAvailable; i++) {
                                    byte b = packetBytes[i];
                                    if (b == delimiter) {
                                        final byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                        final String data = new String(readBuffer, "US-ASCII");
                                        readBufferPosition = 0;

                                        handler.post(new Runnable() {
                                            public void run() {
                                                sGelenVeri = data.toString();
                                                sGelenVeri = sGelenVeri.substring(0, 3);
                                                MtxtVwState.setText(sGelenVeri);
                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            stopWorker = true;
                        }
                    }
                }
            });
            workerThread.start();
        } catch (Exception ignored) {
        }
    }

}
