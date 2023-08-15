package org.benasin;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Marker;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.scanner.AuditResult;
import burp.api.montoya.scanner.ConsolidationAction;
import burp.api.montoya.scanner.ScanCheck;
import burp.api.montoya.scanner.audit.insertionpoint.AuditInsertionPoint;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static burp.api.montoya.scanner.AuditResult.auditResult;
import static burp.api.montoya.scanner.ConsolidationAction.KEEP_BOTH;
import static burp.api.montoya.scanner.ConsolidationAction.KEEP_EXISTING;


public class EmptyStringScanner implements ScanCheck
{
    private final MyTableModel tableModel;
    private final MontoyaApi api;
    private final JsAstParser parser;
    private final ReflectionScanner reflectionScanner;
    private boolean logInScope;

    private static ArrayList<String> hiddenParamList;

    public EmptyStringScanner(MontoyaApi api, MyTableModel tableModel)
    {
        this.api = api;
        this.parser = new JsAstParser(api);
        this.tableModel = tableModel;
        this.reflectionScanner = new ReflectionScanner(api);
        hiddenParamList = new ArrayList<>();
        logInScope = false;
    }

    public ArrayList<String> getHiddenParamList() {return hiddenParamList;}

    private void addHiddenParam(String hiddenParam) {
        if(!hiddenParamList.contains(hiddenParam)) {
            hiddenParamList.add(hiddenParam);
        }
    }
    public void toggleLogInScope() {
        logInScope = !logInScope;
    }

    @Override
    public AuditResult activeAudit(HttpRequestResponse baseRequestResponse, AuditInsertionPoint auditInsertionPoint) {
        return null;
    }

    @Override
    public AuditResult passiveAudit(HttpRequestResponse baseRequestResponse) {
        if (logInScope) {
            if (!api.scope().isInScope(baseRequestResponse.url())) {
                return auditResult(Collections.emptyList());
            }
        }
        List<AuditIssue> issueList = new ArrayList<>();
        HttpResponse response = baseRequestResponse.response();

        AuditIssueSeverity severity = AuditIssueSeverity.INFORMATION;
        String detail;

        String mimetype = response.statedMimeType().name();
        if(mimetype == "HTML") {
            api.logging().logToOutput("\nFound HTML Response on: " + baseRequestResponse.url());
            String scriptContents = parseScriptContents(response.bodyToString());
            ArrayList<EmptyStringResult> results = parser.parse(scriptContents, baseRequestResponse);
            for(EmptyStringResult result : results) {
                tableModel.add(result);
                detail = "Empty string in " + result.getType() + " detected.";
                detail += "<br>Expression: " + result.getExpression();

                List<Marker> responseHighlights = getResponseHighlights(result.getBaseRequestResponse(), result.getExpression());

                if(!result.getHiddenParam().isEmpty()){
                    addHiddenParam(result.getHiddenParam());
                    detail += "<br>Potential hidden parameter: " + result.getHiddenParam();
                    if(reflectionScanner.scan(result)) {
                        severity = AuditIssueSeverity.HIGH;
                        detail +=  "<br>Found this hidden parameter reflected: " + result.getHiddenParam();
                        result.markReflected();

                        responseHighlights = getResponseHighlights(result.getBaseRequestResponse(), result.getExpression());
                        List<Marker> reflectionHighlights = getResponseHighlights(result.getBaseRequestResponse(), "reflectedxyz");
                        responseHighlights.addAll(reflectionHighlights);
                        tableModel.fireTableDataChanged();
                    }
                }
                AuditIssue issue = AuditIssue.auditIssue(
                        "Empty String Found",
                        detail,
                        "",
                        result.getBaseRequestResponse().url(),
                        severity,
                        AuditIssueConfidence.FIRM, "",
                        "",
                        AuditIssueSeverity.HIGH,
                        result.getBaseRequestResponse().withResponseMarkers(responseHighlights));
                issueList.add(issue);
            }
        }
        return auditResult(issueList.isEmpty() ? Collections.emptyList() : issueList);
    }

    private static List<Marker> getResponseHighlights(HttpRequestResponse requestResponse, String match)
    {
        List<Marker> highlights = new LinkedList<>();
        String response = requestResponse.response().toString();


        String[] parts = match.split(" ");
        String regex = Pattern.quote(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            regex += ".*";
            regex += Pattern.quote(parts[i]);
        }

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(response);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            Marker marker = Marker.marker(start, end);
            highlights.add(marker);
        }

        return highlights;
    }

    public static String parseScriptContents(String html) {
        StringBuilder scriptContents = new StringBuilder();

        // Match <script>...</script> tags and extract content
        Pattern pattern = Pattern.compile("<script[^>]*>(.*?)</script>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            String scriptContent = matcher.group(1).trim();
            scriptContents.append(scriptContent);
        }

        return scriptContents.toString();
    }

    @Override
    public ConsolidationAction consolidateIssues(AuditIssue newIssue, AuditIssue existingIssue)
    {
        return existingIssue.name().equals(newIssue.name()) &&
               existingIssue.detail().equals(newIssue.name()) ? KEEP_EXISTING : KEEP_BOTH;
    }

}