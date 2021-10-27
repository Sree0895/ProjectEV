package com.example.evchargeapp;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.Date;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

public class mqttClass implements Parcelable {
    private static MqttAndroidClient mqttAndroidClient;
    private static String TAG = "MqttClass";
    MqttConnectOptions mqttConnectOptions;
    private Handler mHandler;
    private String mClientId;
    private String mHost;
    private String mUserName;
    private String mPassword;
    private static Context mCnxt;

    String caCertFile = "AmazonRootCA1.pem" ;
    String crtFile = "certificate.pem.crt";
    String keyFile = "private.pem.key";
    String password = "" ;
    boolean serverHostnameVerification = true;

    public mqttClass(Context cnxt, String host, String clientId, String UserName, String password, Handler handler) {
        mCnxt = cnxt;
        mHost = host;
        mClientId = clientId;
        mUserName = UserName;
        mPassword = password;
        mHandler = handler;

        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setConnectionTimeout(60);
        mqttConnectOptions.setKeepAliveInterval(60);
        mqttConnectOptions.setAutomaticReconnect(true);

        SSLSocketFactory socketFactory = null;
/*        try {
            socketFactory = getSSLSocketFactory(caCertFile,crtFile,keyFile, password, serverHostnameVerification);
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        }*/

        if(null != socketFactory) {
            mqttConnectOptions.setSocketFactory(socketFactory);
            // mqttConnectOptions.setCleanSession(true);
        }


        mqttAndroidClient = new MqttAndroidClient(mCnxt, host, clientId);
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i(TAG, "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i(TAG, "topic: " + topic + ", msg: " + new String(message.getPayload()));
                Bundle bundle = new Bundle();
                Message msg = mHandler.obtainMessage();
                msg.what = 8;
                bundle.putString("mqttRxData", new String(message.getPayload()) );
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.i(TAG, "msg delivered");
            }
        });
    }

    protected mqttClass(Parcel in) {
        mClientId = in.readString();
        mHost = in.readString();
        mUserName = in.readString();
        mPassword = in.readString();
        caCertFile = in.readString();
        crtFile = in.readString();
        keyFile = in.readString();
        password = in.readString();
        serverHostnameVerification = in.readByte() != 0;
    }

    public static final Creator<mqttClass> CREATOR = new Creator<mqttClass>() {
        @Override
        public mqttClass createFromParcel(Parcel in) {
            return new mqttClass(in);
        }

        @Override
        public mqttClass[] newArray(int size) {
            return new mqttClass[size];
        }
    };

    public static String convertStreamToString(InputStream is)
            throws IOException {

        Writer writer = new StringWriter();
        char[] buffer = new char[2048];

        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } finally {
            is.close();
        }

        String text = writer.toString();
        return text;
    }

    public boolean getMqttStatus()
    {
        return mqttAndroidClient.isConnected();
    }



    public void mqttConnect(String topic)
    {
        /* Mqtt*/
        try {
            mqttAndroidClient.connect(mqttConnectOptions,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "connect succeed");
                    if(!topic.isEmpty())
                        subscribeTopic(topic);

                    Bundle bundle = new Bundle();
                    Message msg = mHandler.obtainMessage();
                    msg.what = 6;
                    bundle.putBoolean("mqttFlag", true);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "connect failed");
                    Bundle bundle = new Bundle();
                    Message msg = mHandler.obtainMessage();
                    msg.what = 6;
                    bundle.putBoolean("mqttFlag", false);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }

    }


    public void mqttDisconnect()
    {
        /* Mqtt*/
        try {
            mqttAndroidClient.disconnect(1000,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "Disconnect succeed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "Disconnect failed");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    public void subscribeTopic(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "subscribed succeed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "subscribed failed");
                    Bundle bundle = new Bundle();
                    Message msg = mHandler.obtainMessage();
                    msg.what = 9;
                    bundle.putBoolean("subscribeFlag", false);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);

                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribeTopic(String topic) {
        try {
            mqttAndroidClient.unsubscribe(topic, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "unsubscribed succeed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "unsubscribed failed");
/*                    Bundle bundle = new Bundle();
                    Message msg = mHandler.obtainMessage();
                    msg.what = 9;
                    bundle.putBoolean("subscribeFlag", false);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);*/

                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publishMessage(String topic, String payload) {
        try {
            if (mqttAndroidClient.isConnected() == false) {
                mqttAndroidClient.connect();
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(1);
            mqttAndroidClient.publish(topic, message,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "publish succeed!"+ payload);

/*                    Bundle bundle = new Bundle();
                    Message msg = mHandler.obtainMessage();
                    msg.what = 7;
                    bundle.putBoolean("publishFlag", true);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);*/
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG, "publish failed!");
                    Bundle bundle = new Bundle();
                    Message msg = mHandler.obtainMessage();
                    msg.what = 7;
                    bundle.putBoolean("publishFlag", false);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    public static SSLSocketFactory getSSLSocketFactory(final String caCrtFile, final String crtFile,
                                                       final String keyFile, final String password, boolean serverHostnameVerification)
            throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException,
            KeyManagementException, UnrecoverableKeyException {
        /**
         * Add BouncyCastle as a Security Provider
         */
        Security.addProvider(new BouncyCastleProvider());
        JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter();

        AssetManager assetManager = mCnxt.getAssets();

        /**
         * Load Certificate Authority (CA) certificate
         */
        InputStream caInputStream  = assetManager.open(caCrtFile);
        Reader      caInputStreamReader = new InputStreamReader(caInputStream);
        X509CertificateHolder caCertHolder = (X509CertificateHolder) readPEMFile(caInputStreamReader);
        X509Certificate caCert = certificateConverter.getCertificate(caCertHolder);

        /**
         * Load client certificate
         */
        InputStream inputStream  = assetManager.open(crtFile);
        Reader inputStreamReader = new InputStreamReader(inputStream);
        X509CertificateHolder certHolder = (X509CertificateHolder) readPEMFile(inputStreamReader);
        X509Certificate cert = certificateConverter.getCertificate(certHolder);

        /**
         * Load client private key
         */
        InputStream keyInputStream  = assetManager.open(keyFile);
        Reader keyInputStreamReader = new InputStreamReader(keyInputStream);
        Object keyObject = readPEMFile(keyInputStreamReader);

        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();

        PrivateKey privateKey = null;

        if (keyObject instanceof PEMEncryptedKeyPair) {
            PEMDecryptorProvider provider = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
            KeyPair keyPair = keyConverter.getKeyPair(((PEMEncryptedKeyPair) keyObject).decryptKeyPair(provider));
            privateKey = keyPair.getPrivate();
        } else if (keyObject instanceof PEMKeyPair) {
            KeyPair keyPair = keyConverter.getKeyPair((PEMKeyPair) keyObject);
            privateKey = keyPair.getPrivate();
        } else if (keyObject instanceof PrivateKeyInfo) {
            privateKey = keyConverter.getPrivateKey((PrivateKeyInfo) keyObject);
        } else {
            throw new IOException(String.format("Unsupported type of keyFile %s", keyFile));
        }

        /**
         * CA certificate is used to authenticate server
         */
        KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        caKeyStore.load(null, null);
        caKeyStore.setCertificateEntry("ca-certificate", caCert);

        /**
         * Client key and certificates are sent to server so it can authenticate the
         * client. (server send CertificateRequest message in TLS handshake step).
         */
        KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        clientKeyStore.load(null, null);
        clientKeyStore.setCertificateEntry("certificate", cert);
        clientKeyStore.setKeyEntry("private-key", privateKey, password.toCharArray(), new Certificate[] { cert });

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, password.toCharArray());

        /**
         * Create SSL socket factory
         */
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(keyManagerFactory.getKeyManagers(),
                serverHostnameVerification ? getTrustManagers(caKeyStore) : getUnsafeTrustManagers(caKeyStore), null);

        /**
         * Return the newly created socket factory object
         */
        return context.getSocketFactory();
    }

    private static Object readPEMFile(Reader inputReader) throws IOException {
        try (PEMParser reader = new PEMParser(inputReader)) {
            return reader.readObject();
        }
    }

    private static TrustManager[] getTrustManagers(KeyStore caKeyStore)
            throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(caKeyStore);
        return trustManagerFactory.getTrustManagers();
    }

    /**
     * This method checks server and client certificates but overrides server hostname verification.
     * @param caKeyStore
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
    ' */
    private static TrustManager[] getUnsafeTrustManagers(KeyStore caKeyStore)
            throws NoSuchAlgorithmException, KeyStoreException {
        X509TrustManager standardTrustManager = (X509TrustManager) getTrustManagers(caKeyStore)[0];
        return new TrustManager[] { new X509ExtendedTrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                standardTrustManager.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                standardTrustManager.checkServerTrusted(chain, authType);
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return standardTrustManager.getAcceptedIssuers();
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
                    throws CertificateException {
                standardTrustManager.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
                    throws CertificateException {
                standardTrustManager.checkServerTrusted(chain, authType);
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                    throws CertificateException {
                standardTrustManager.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
                    throws CertificateException {
                standardTrustManager.checkServerTrusted(chain, authType);
            }
        } };
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mClientId);
        dest.writeString(mHost);
        dest.writeString(mUserName);
        dest.writeString(mPassword);
        dest.writeString(caCertFile);
        dest.writeString(crtFile);
        dest.writeString(keyFile);
        dest.writeString(password);
        dest.writeByte((byte) (serverHostnameVerification ? 1 : 0));
    }
}


