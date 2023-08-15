package org.benasin;

import burp.api.montoya.http.message.HttpRequestResponse;


public class EmptyStringResult {

    private static int count;
    private final int id;
    private final String hiddenParam;
    private final String expression;
    private final String type;
    private boolean isReflected;
    private HttpRequestResponse brr;

    public EmptyStringResult(String hiddenParam, String expression, String type, HttpRequestResponse brr) {
        count++;
        this.id = count;
        this.hiddenParam = hiddenParam;
        this.expression = expression;
        this.type = type;
        this.brr = brr;
        this.isReflected = false;
    }

    public String getHiddenParam() {return hiddenParam;}
    public String getExpression() {return expression;}
    public String getType() {return type;}
    public HttpRequestResponse getBaseRequestResponse() {return brr;}
    public int getId() {return id;}
    public boolean isReflected() {return isReflected;}
    public void markReflected() {
        isReflected = true;
    }

    public void setHttpRequestResponse(HttpRequestResponse httpRequestResponse) {
        brr = httpRequestResponse;
    }
}
