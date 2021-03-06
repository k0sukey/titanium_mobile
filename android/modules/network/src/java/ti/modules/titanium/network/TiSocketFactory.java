/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.network;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.appcelerator.kroll.common.Log;

import android.os.Build;

public class TiSocketFactory extends SSLSocketFactory {
	
	private SSLContext sslContext;
	private String tlsVersion;
	private static final String TAG = "TiSocketFactory";
	private static final boolean JELLYBEAN_OR_GREATER = (Build.VERSION.SDK_INT >= 16);
	private static final String TLS_VERSION_1_2_PROTOCOL = "TLSv1.2";
	private static final String TLS_VERSION_1_1_PROTOCOL = "TLSv1.1";
	private static final String TLS_VERSION_1_0_PROTOCOL = "TLSv1";
	protected String[] enabledProtocols;
	
	public TiSocketFactory(KeyManager[] keyManagers, TrustManager[] trustManagers, int protocol) throws NoSuchAlgorithmException, 
	KeyManagementException, KeyStoreException, UnrecoverableKeyException
	{
		super(null,null,null,null,null,null);
		switch (protocol) {

			case NetworkModule.TLS_VERSION_1_0:
				tlsVersion = TLS_VERSION_1_0_PROTOCOL;
				enabledProtocols = new String[] {TLS_VERSION_1_0_PROTOCOL};
				break;
			case NetworkModule.TLS_VERSION_1_1:
				tlsVersion = TLS_VERSION_1_1_PROTOCOL;
				enabledProtocols = new String[] {TLS_VERSION_1_0_PROTOCOL, TLS_VERSION_1_1_PROTOCOL};
				break;
			case NetworkModule.TLS_VERSION_1_2:
				tlsVersion = TLS_VERSION_1_2_PROTOCOL;
				enabledProtocols = new String[] {TLS_VERSION_1_0_PROTOCOL, TLS_VERSION_1_1_PROTOCOL, TLS_VERSION_1_2_PROTOCOL};
				break;
			default:
				Log.e(TAG, "Incorrect TLS version was set in HTTPClient. Reverting to default TLS version.");
			case NetworkModule.TLS_DEFAULT:	
				if (JELLYBEAN_OR_GREATER) {
					tlsVersion = TLS_VERSION_1_2_PROTOCOL;
					enabledProtocols = new String[] {TLS_VERSION_1_0_PROTOCOL, TLS_VERSION_1_1_PROTOCOL, TLS_VERSION_1_2_PROTOCOL};
				} else {
					tlsVersion = TLS_VERSION_1_0_PROTOCOL;
					enabledProtocols = new String[] {TLS_VERSION_1_0_PROTOCOL};
					Log.i(TAG, tlsVersion + " protocol is being used. It is a less-secure version.");
				}
				break;
		}
				
		sslContext = SSLContext.getInstance(tlsVersion);
		sslContext.init(keyManagers, trustManagers, null);
		
	}
	
	@Override
	public Socket createSocket() throws IOException
	{
		SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket();
		return setSupportedAndEnabledProtocolsInSocket(enabledProtocols, sslSocket);
	}
	
	@Override
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException
	{
		SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		return setSupportedAndEnabledProtocolsInSocket(enabledProtocols, sslSocket);
	}

	@Override
	public boolean isSecure(Socket socket) throws IllegalArgumentException 
	{
		return true;
	}
	
	protected SSLSocket setSupportedAndEnabledProtocolsInSocket(String[] enabledProtocols, SSLSocket sslSocket)
	{
		String[] supportedProtocols = sslSocket.getSupportedProtocols();
		List<String> supportedAndEnabledProtocols = new ArrayList<String>();
		
		for(String enabledProtocol : enabledProtocols){		
			for(String supportedProtocol : supportedProtocols){
				if(enabledProtocol.equals(supportedProtocol)){
					supportedAndEnabledProtocols.add(supportedProtocol);
					break;
				}
			}
		}	
		
		//If there are 0 supported and enabled protocols, it will use the default enabled protocols.
		//Default enabled protocols varies depending on API level.
		if(supportedAndEnabledProtocols.size()>0){
			sslSocket.setEnabledProtocols(supportedAndEnabledProtocols.toArray(new String[supportedAndEnabledProtocols.size()]));
		}

		return sslSocket;
	}
}
