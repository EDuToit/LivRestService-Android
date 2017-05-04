package tech.livx.restservice.requests;

import android.content.Context;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Rest Request
 * Bugs: none known
 *
 * @author Mitch, LivX : livx.tech
 * @version 1.0
 */
public abstract class RestRequest {
    protected static final String METHOD_POST = "POST";
    protected static final String METHOD_GET = "GET";

    protected OkHttpClient okHttpClient;
    private Handler handler;

    public RestRequest() {
        okHttpClient = new OkHttpClient();
    }

    public void execute(final Context context, final long requestId, final RequestCallback callback) {
        this.handler = new Handler(context.getMainLooper());

        Request request = buildRequest(context);

        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (e != null) {
                    e.printStackTrace();
                }
                onError(context);
                if (callback != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailed(requestId, RestRequest.this);
                        }
                    });
                }
            }

            @Override
            public void onResponse(Call call, final Response response) {

                try {
                    onComplete(context, response.body().byteStream());

                    if (callback != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onComplete(requestId, response.code(), RestRequest.this);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    if (callback != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFailed(requestId, RestRequest.this);
                            }
                        });
                    }
                }
            }
        });
    }

    private Request buildRequest(Context context) {
        Request.Builder builder = new Request.Builder();
        builder.method(getMethod(context), (getBody(context)!= null) ? RequestBody.create(getMediaType(context),getBody(context)) : null);
        builder.url(getURL(context));
        if (getHeaders(context) != null) {
            for (String key : getHeaders(context).keySet()) {
                builder.addHeader(key, getHeaders(context).get(key));
            }
        }

        builder.addHeader("X-Requested-With", "XMLHttpRequest");

        return builder.build();
    }

    protected static String getStringFromInputStream(InputStream stream) throws IOException
    {
        int n = 0;
        char[] buffer = new char[1024 * 4];
        InputStreamReader reader = new InputStreamReader(stream, "UTF8");
        StringWriter writer = new StringWriter();
        while (-1 != (n = reader.read(buffer))) writer.write(buffer, 0, n);
        return writer.toString();
    }

    protected abstract MediaType getMediaType(Context context);

    protected abstract String getURL(Context context);

    protected abstract Map<String, String> getHeaders(Context context);

    protected abstract byte[] getBody(Context context);

    protected abstract void onComplete(Context context, InputStream response) throws IOException;

    protected abstract void onError(Context context);

    protected abstract String getMethod(Context context);

    public abstract boolean isAuthenticated();


}
