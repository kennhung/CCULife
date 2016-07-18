package org.zankio.ccudata.base.source.http;

import org.zankio.ccudata.base.model.HttpResponse;
import org.zankio.ccudata.base.model.OkHttpResponse;
import org.zankio.ccudata.base.model.Request;
import org.zankio.ccudata.base.source.FetchParseSource;
import org.zankio.ccudata.base.source.http.annontation.Charset;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import static org.zankio.ccudata.base.utils.AnnotationUtils.getAnnotationValue;

public abstract class HTTPSource<TArgument, TData> extends FetchParseSource<TArgument, TData, HttpResponse> {
    private static final String HTTP_PARAMETERS = "HTTP_PARAMETERS";

    @Override
    protected HttpResponse fetch(Request<TData, TArgument> request, boolean inner) throws Exception {
        initHTTPRequest(request);
        HTTPParameter parameter = httpParameter(request);

        OkHttpClient client = makeClient(parameter);

        okhttp3.Request httpRequest = makeRequest(parameter);
        return new OkHttpResponse(
                client.newCall(httpRequest).execute(),
                getCharset()
        );
    }

    public OkHttpClient makeClient(HTTPParameter parameter) {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .followRedirects(parameter.followRedirect())
                .followSslRedirects(parameter.followRedirect())
                .build();
    }

    private okhttp3.Request makeRequest(HTTPParameter parameter) {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        String urlString = parameter.url();
        if (urlString == null || urlString.isEmpty())
            throw new RuntimeException("HTTP Request Without URL");

        HttpUrl.Builder url = HttpUrl.parse(urlString).newBuilder();
        builder.method(parameter.method().name(), makeRequestBody(parameter));

        //QueryString
        for (Map.Entry<String, List<String>> map: parameter.queryStrings().entrySet()) {
            String key = map.getKey();
            List<String> values = map.getValue();

            for(String value: values)
                url.addQueryParameter(key, value);
        }

        //Header
        for (Map.Entry<String, List<String>> map: parameter.headers().entrySet()) {
            String key = map.getKey();
            List<String> values = map.getValue();

            for(String value: values)
                builder.header(key, value);
        }

        //Cookie
        Map<String, List<String>> cookies = parameter.cookies();
        StringBuilder cookieHeader = new StringBuilder();
        for (Map.Entry<String, List<String>> map: parameter.cookies().entrySet()) {
            String key = map.getKey();
            List<String> values = map.getValue();
            for(String value: values)
                cookieHeader.append(key).append("=").append(value).append(";");
        }

        if (cookieHeader.length() != 0)
            builder.header("Cookie", cookieHeader.toString());

        return builder.url(url.build()).build();
    }

    private okhttp3.RequestBody makeRequestBody(HTTPParameter parameter) {
        if (parameter.method() == HTTPParameter.HTTPMethod.GET) return null;
        Map<String, List<String>> fields = parameter.fields();
        FormBody.Builder formBody = new FormBody.Builder();

        for (Map.Entry<String, List<String>> map: fields.entrySet()) {
            String key = map.getKey();
            List<String> values = map.getValue();

            for(String value: values)
                formBody.add(key, value);
        }

        return formBody.build();

        //File[] files = prop.getFile(request);
        //MultipartBody.Builder multipartBody = new MultipartBody.Builder();
        //multipartBody.addFormDataPart(fields[i], fields[i + 1]);

        /*
        if (files.length > 0) {
            for (int i = 0; i < files.length; i += 2) {
                multipartBody.addFormDataPart(files[i], , files[i + 1]);
            }
        }*/

        //return null;
    }

    public void initHTTPRequest(Request<TData, TArgument> request) {
        request.storage().put(HTTP_PARAMETERS, HTTPAnnotationReader.read(this));

    }

    public String getCharset() {
        return getAnnotationValue(this.getClass(), Charset.class, null);
    }

    public HTTPParameter httpParameter(Request request) {
        return request.storage().get(HTTP_PARAMETERS, HTTPParameter.class);
    }
}
