package compiler.semantic;

import compiler.ast.Program;
import compiler.ast.Stmt;
import compiler.ast.Type;
import compiler.parser.Parser;
import compiler.scanner.Lexer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SemanticTest {

    private static Program parse(String source) {
        Parser parser = new Parser(new Lexer(source).scanTokens());
        Program program = parser.parse();
        assertFalse(parser.hasErrors(), "test program has a syntax error: " + parser.getErrors());
        return program;
    }

    private static Analyzer analyze(String source) {
        Analyzer analyzer = new Analyzer();
        analyzer.analyze(parse(source));
        return analyzer;
    }

    /** Just the error messages, in the order reported. */
    private static List<String> messages(String source) {
        List<String> result = new ArrayList<>();
        for (SemanticError e : analyze(source).getErrors()) {
            result.add(e.message());
        }
        return result;
    }

    private static void assertClean(String source) {
        assertEquals(List.of(), messages(source));
    }

    // ---- Stage 2: names and scopes

    @Test
    void aSimpleCorrectProgramHasNoErrors() {
        assertClean("int x = 1; print(x);");
    }

    @Test
    void undeclaredNameInPrint() {
        Analyzer a = analyze("print(y);");
        assertEquals(1, a.getErrors().size());
        assertEquals("Undeclared variable 'y'", a.getErrors().get(0).message());
        assertEquals(1, a.getErrors().get(0).line());
        assertEquals(7, a.getErrors().get(0).column());
    }

    @Test
    void undeclaredNameOnTheLeftOfAnAssignment() {
        Analyzer a = analyze("x = 1;");
        assertEquals(List.of("Undeclared variable 'x'"), messages("x = 1;"));
        assertEquals(1, a.getErrors().get(0).column());
    }

    @Test
    void undeclaredNameInsideAnExpression() {
        assertEquals(List.of("Undeclared variable 'b'"), messages("int a = b + 1;"));
    }

    @Test
    void usingANameBeforeItsDeclaration() {
        assertEquals(List.of("Undeclared variable 'x'"), messages("print(x); int x = 1;"));
    }

    @Test
    void aVariableCannotUseItselfInItsInitializer() {
        assertEquals(List.of("Undeclared variable 'x'"), messages("int x = x;"));
    }

    @Test
    void duplicateDeclarationInTheSameScope() {
        Analyzer a = analyze("int x = 1;\nint x = 2;");
        assertEquals(1, a.getErrors().size());
        assertEquals("'x' is already declared in this scope (first declared at line 1)",
                a.getErrors().get(0).message());
        assertEquals(2, a.getErrors().get(0).line());
    }

    @Test
    void namesAreCaseSensitive() {
        assertClean("int a; int A;");
    }

    @Test
    void innerBlockMayShadowAnOuterName() {
        assertClean("int x = 1; { int x = 2; print(x); } print(x);");
    }

    @Test
    void innerBlockCanReadOuterNames() {
        assertClean("int x = 1; { print(x); }");
    }

    @Test
    void nameIsNotVisibleAfterItsBlock() {
        assertEquals(List.of("Undeclared variable 'y'"), messages("{ int y = 1; } print(y);"));
    }

    @Test
    void duplicateInsideOneInnerBlock() {
        assertEquals(1, messages("{ int x; int x; }").size());
    }

    @Test
    void loopBodyHasItsOwnScope() {
        assertEquals(List.of("Undeclared variable 't'"),
                messages("while (true) { int t = 1; } print(t);"));
    }

    @Test
    void ifAndElseBranchesHaveSeparateScopes() {
        assertClean("if (true) { int a = 1; } else { int a = 2; }");
    }

    @Test
    void everyErrorIsCollected() {
        assertEquals(2, messages("print(a); print(b);").size());
    }

    @Test
    void aBadInitializerStillDeclaresTheName() {
        // only 'y' is wrong; using x afterwards must not add a second error
        assertEquals(List.of("Undeclared variable 'y'"), messages("int x = y; print(x);"));
    }

    // ---- Stage 3: operator types

    @Test
    void arithmeticOnIntsIsFine() {
        assertClean("print(1 + 2 * 3 % 4 - 5 / 1);");
    }

    @Test
    void unaryMinusNeedsAnInt() {
        assertEquals(List.of("Operator '-' needs an int operand, but found bool"), messages("print(-true);"));
    }

    @Test
    void notNeedsABool() {
        assertEquals(List.of("Operator '!' needs a bool operand, but found int"), messages("print(!5);"));
    }

    @Test
    void arithmeticNeedsInts() {
        assertEquals(List.of("Operator '+' needs int operands, but found bool and int"),
                messages("print(true + 1);"));
    }

    @Test
    void orderingComparisonsNeedInts() {
        assertClean("print(1 < 2); print(3 >= 3);");
        assertEquals(List.of("Operator '<' needs int operands, but found bool and bool"),
                messages("print(true < false);"));
    }

    @Test
    void equalityNeedsTwoOperandsOfTheSameType() {
        assertClean("print(1 == 2); print(true != false);");
        assertEquals(List.of("Operator '==' needs two operands of the same type, but found int and bool"),
                messages("print(1 == true);"));
    }

    @Test
    void logicalOperatorsNeedBools() {
        assertClean("print(true && false || true);");
        assertEquals(List.of("Operator '&&' needs bool operands, but found int and bool"),
                messages("print(1 && true);"));
    }

    @Test
    void anOperatorErrorIsReportedAtTheOperator() {
        Analyzer a = analyze("print(1 + true);");
        assertEquals(1, a.getErrors().get(0).line());
        assertEquals(9, a.getErrors().get(0).column());
    }

    @Test
    void theResultOfAComparisonIsABool() {
        assertEquals(List.of("Operator '+' needs int operands, but found bool and int"),
                messages("print((1 < 2) + 1);"));
    }

    @Test
    void aMistakeInsideDoesNotCauseASecondError() {
        assertEquals(1, messages("print((1 + true) * 2);").size());
    }

    @Test
    void anUndeclaredOperandDoesNotCauseAnOperatorError() {
        assertEquals(List.of("Undeclared variable 'x'"), messages("print(!(x + 1));"));
        assertEquals(List.of("Undeclared variable 'y'"), messages("print(y && true);"));
    }

    @Test
    void mixedOperatorsWithVariables() {
        assertClean("int i = 1; bool b = true; print(i + 2 == 3 && !b);");
    }

    // ---- Stage 4: statements and their types

    @Test
    void initializerMustMatchTheDeclaredType() {
        Analyzer a = analyze("int x = true;");
        assertEquals("Cannot initialize 'x' (int) with a bool value", a.getErrors().get(0).message());
        assertEquals(9, a.getErrors().get(0).column());
        assertEquals(List.of("Cannot initialize 'b' (bool) with an int value"), messages("bool b = 1;"));
    }

    @Test
    void matchingInitializersAreFine() {
        assertClean("bool b = 1 < 2; int n = 1 + 1; bool c = b; int m;");
    }

    @Test
    void assignmentMustMatchTheVariableType() {
        assertEquals(List.of("Cannot assign a bool value to 'x' (int)"), messages("int x = 0; x = false;"));
        assertEquals(List.of("Cannot assign an int value to 'b' (bool)").size(), messages("bool b; b = 5;").size());
    }

    @Test
    void ifConditionMustBeBool() {
        Analyzer a = analyze("if (1) { }");
        assertEquals("The condition of 'if' must be bool, but found int", a.getErrors().get(0).message());
        assertEquals(5, a.getErrors().get(0).column());
    }

    @Test
    void anIntVariableIsNotACondition() {
        assertEquals(1, messages("int n = 3; if (n) { }").size());
        assertClean("int n = 3; if (n != 0) { }");
    }

    @Test
    void whileConditionMustBeBool() {
        assertEquals(List.of("The condition of 'while' must be bool, but found int"), messages("while (5) { }"));
    }

    @Test
    void elseIfConditionIsChecked() {
        assertEquals(1, messages("if (true) { } else if (2) { }").size());
    }

    @Test
    void printAcceptsAnyType() {
        assertClean("print(1); print(true);");
    }

    @Test
    void aBadInitializerStillGivesTheNameItsDeclaredType() {
        assertEquals(1, messages("int x = true; x = 1;").size());
    }

    @Test
    void anOperatorMistakeInAnInitializerIsReportedOnce() {
        assertEquals(1, messages("int x = true + 1;").size());
    }

    @Test
    void anUndeclaredInitializerIsReportedOnce() {
        assertEquals(List.of("Undeclared variable 'y'"), messages("int x = y;"));
    }

    // ---- Stage 5: const, break, continue

    // ---- Stage 6: types of expressions, error order, whole programs
}
