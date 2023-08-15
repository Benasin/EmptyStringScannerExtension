package org.benasin;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

public class ReflectionScanner {
    private final MontoyaApi api;

    public ReflectionScanner(MontoyaApi api) {
        this.api = api;
    }

    public boolean scan(EmptyStringResult esr) {
        String method = esr.getBaseRequestResponse().request().method();
        if (method == "GET") {
            return scanWithParameterType(esr, HttpParameterType.URL);
        } else if (method == "POST") {
            return scanWithParameterType(esr, HttpParameterType.BODY);
        }
        return false;
    }

    private boolean scanWithParameterType(EmptyStringResult esr, HttpParameterType paramType) {
        HttpRequestResponse baseRequestResponse = esr.getBaseRequestResponse();
        HttpParameter hidden_param = HttpParameter.parameter(esr.getHiddenParam(), "reflectedxyz", paramType);
        HttpRequest request = baseRequestResponse.request().withAddedParameters(hidden_param);
        HttpResponse response = api.http().sendRequest(request).response();

        if (response.bodyToString().toLowerCase().contains("reflectedxyz")) {
            esr.setHttpRequestResponse(HttpRequestResponse.httpRequestResponse(request, response));
            return true;
        }

        return false;
    }

    // TODO add cookie and headers
}
