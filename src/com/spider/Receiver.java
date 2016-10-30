package com.spider;

/**
 * @author Administrator
 *
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class Receiver {

	/**
	 * Download resource by given url and name the resource as given file name.
	 * 
	 * @param url
	 * @param resourceFile
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static void downResource(String url, File resourceFile)
			throws ClientProtocolException, IOException {
		resourceFile.getParentFile().mkdirs();
		if (url.startsWith("https://")) {
			downHttpsRequestByGet(url, new HashMap(), resourceFile);
		} else {
			HttpClient httpclient = new DefaultHttpClient();
			// ����Get����ʵ��
			HttpGet httpgets = new HttpGet(url);
			HttpResponse response = httpclient.execute(httpgets);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream instreams = entity.getContent();
				// String str = convertStreamToString(instreams);
				convertStreamToFile(instreams, resourceFile);
				// FileUtils.writeStringToFile(resourceFile, str);
				httpgets.abort();
			}
		}
	}

	public static void main(String[] args) {
		Map param = new HashMap();
		downHttpsRequestByGet(
				"https://s.geilicdn.com/official/201610/css/pc/index.92729443.css",
				param, new File("d:\\1.css"));
	}

	public static final void downHttpsRequestByGet(String url,
			Map<String, String> params, File resourceFile) {
		String responseContent = null;
		HttpClient httpClient = new DefaultHttpClient();
		// 创建TrustManager
		X509TrustManager xtm = new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};
		// 这个好像是HOST验证
		X509HostnameVerifier hostnameVerifier = new X509HostnameVerifier() {
			public boolean verify(String arg0, SSLSession arg1) {
				return true;
			}

			public void verify(String arg0, SSLSocket arg1) throws IOException {
			}

			public void verify(String arg0, String[] arg1, String[] arg2)
					throws SSLException {
			}

			public void verify(String arg0, X509Certificate arg1)
					throws SSLException {
			}
		};
		try {
			// TLS1.0与SSL3.0基本上没有太大的差别，可粗略理解为TLS是SSL的继承者，但它们使用的是相同的SSLContext
			SSLContext ctx = SSLContext.getInstance("TLS");
			// 使用TrustManager来初始化该上下文，TrustManager只是被SSL的Socket所使用
			ctx.init(null, new TrustManager[] { xtm }, null);
			// 创建SSLSocketFactory
			SSLSocketFactory socketFactory = new SSLSocketFactory(ctx);
			socketFactory.setHostnameVerifier(hostnameVerifier);
			// 通过SchemeRegistry将SSLSocketFactory注册到我们的HttpClient上
			httpClient.getConnectionManager().getSchemeRegistry()
					.register(new Scheme("https", socketFactory, 443));
			HttpGet httpGet = new HttpGet(url);
			List<NameValuePair> formParams = new ArrayList<NameValuePair>(); // 构建POST请求的表单参数
			for (Map.Entry<String, String> entry : params.entrySet()) {
				formParams.add(new BasicNameValuePair(entry.getKey(), entry
						.getValue()));
			}
		//	httpGet.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
			HttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity(); // 获取响应实体
			if (entity != null) {
				InputStream instreams = entity.getContent();
				convertStreamToFile(instreams, resourceFile);
			}
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// 关闭连接,释放资源
			httpClient.getConnectionManager().shutdown();
		}
	}

	public static void convertStreamToFile(InputStream is, File outFile)
			throws IOException {
		byte[] b = new byte[10240000];
		FileOutputStream outs = new FileOutputStream(outFile);
		try {
			int size = 0;
			while ((size = is.read(b)) != -1) {
				outs.write(b, 0, size);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (outs != null)
				outs.close();
		}

	}

	private static String convertStreamToString(InputStream is) {

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));

		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public static void writeFile(File f, String fileContent) {
		try {
			if (!f.exists()) {
				f.getParentFile().mkdirs();
				f.createNewFile();
			}
			OutputStreamWriter write = new OutputStreamWriter(
					new FileOutputStream(f), "UTF-8");
			BufferedWriter writer = new BufferedWriter(write);
			writer.write(fileContent);
			writer.close();
		} catch (Exception e) {
			System.out.println("д�ļ����ݲ�������");
			e.printStackTrace();
		}
	}

}
