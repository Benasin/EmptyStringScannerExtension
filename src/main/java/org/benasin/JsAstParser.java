package org.benasin;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import java.util.ArrayList;
import java.util.Objects;

public class JsAstParser {

    private final ArrayList<EmptyStringResult> results;
    private final MontoyaApi api;
    private final CompilerEnvirons compilerEnvirons;

    public JsAstParser(MontoyaApi api) {
        this.api = api;
        this.results = new ArrayList<>();
        this.compilerEnvirons = new CompilerEnvirons();
        this.compilerEnvirons.setLanguageVersion(180); // Choose an appropriate language version
    }

    public ArrayList<EmptyStringResult> parse(String code, HttpRequestResponse brr) {
        this.results.clear();
        Parser parser = new Parser(compilerEnvirons);
        AstRoot astRoot = parser.parse(code, null, 1);

        // Traverse the AST using a custom visitor
        astRoot.visit(new CustomNodeVisitor(api, results, brr));

        return results;
    }

    static class CustomNodeVisitor implements NodeVisitor {
        private final MontoyaApi api;
        private final HttpRequestResponse brr;

        private final ArrayList<EmptyStringResult> results;

        public CustomNodeVisitor(MontoyaApi api, ArrayList<EmptyStringResult> results, HttpRequestResponse brr) {
            this.api = api;
            this.results = results;
            this.brr = brr;
        }

        @Override
        public boolean visit(AstNode node) {
            // Process the node here

            // Finding empty string declaration, assignment
            if (node instanceof VariableInitializer initializer) {
                AstNode l_node = initializer.getTarget();
                AstNode r_node = initializer.getInitializer();
                if (isEmpty(r_node)) {
                    api.logging().logToOutput("Variable " + l_node.toSource() + " is empty.");
                    results.add(new EmptyStringResult(l_node.toSource(), node.toSource(), "Declaration", brr));
                }
            } else if (node instanceof Assignment assignment) {
                AstNode l_node = assignment.getLeft();
                AstNode r_node = assignment.getRight();
                if (isEmpty(r_node)) {
                    api.logging().logToOutput("Assignment " + l_node.toSource() + " is empty.");
                    if(l_node instanceof PropertyGet) {
                        results.add(new EmptyStringResult(((PropertyGet) l_node).getProperty().toSource(), node.toSource(), "Assignment", brr));
                    } else {
                        results.add(new EmptyStringResult(l_node.toSource(), node.toSource(), "Assignment", brr));
                    }
                }
            }

            // Finding empty string comparisons, string operations
            if (node instanceof InfixExpression infixExpression) {
                int operator = infixExpression.getOperator();
                if (isOperator(operator)) {
                    AstNode l_node = infixExpression.getLeft();
                    AstNode r_node = infixExpression.getRight();
                    if(isEmpty(l_node) || isEmpty(r_node)) {
                        api.logging().logToOutput("The operation " + infixExpression.toSource() + " is empty.");
                        results.add(new EmptyStringResult("", node.toSource(), "Comparison/Operation", brr));
                    }
                }
            }

            // Finding empty string in function params
            if (node instanceof FunctionCall functionCall) {
                for(AstNode argument : functionCall.getArguments()) {
                    if (isEmpty(argument)) {
                        api.logging().logToOutput("Function call " + functionCall.toSource() + " has empty string.");
                        results.add(new EmptyStringResult("", node.toSource(), "Function Call", brr));
                    }
                }
            }
            return true;
        }

        private boolean isEmpty(AstNode node) {
            return (node instanceof StringLiteral && ((StringLiteral) node).getValue().isEmpty()) ||
                    (node instanceof TemplateLiteral && Objects.equals(node.toSource(), "``"));
        }
        private boolean isOperator(int operator) {
            return operator == Token.EQ ||
                    operator == Token.SHEQ ||
                    operator == Token.NE ||
                    operator == Token.SHNE ||
                    operator == Token.LT ||
                    operator == Token.GT ||
                    operator == Token.LE ||
                    operator == Token.GE ||
                    operator == Token.AND ||
                    operator == Token.OR ||
                    operator == Token.NOT ||
                    operator == Token.BITAND ||
                    operator == Token.BITOR ||
                    operator == Token.BITXOR ||
                    operator == Token.ADD ||
                    operator == Token.SUB ||
                    operator == Token.MUL ||
                    operator == Token.DIV ||
                    operator == Token.MOD;
        }
    }
}
