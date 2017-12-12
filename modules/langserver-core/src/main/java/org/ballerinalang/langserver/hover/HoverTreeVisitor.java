/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.langserver.hover;

import org.ballerinalang.langserver.completions.util.positioning.resolvers.*;
import org.ballerinalang.langserver.hover.model.HoverResolvedNode;
import org.ballerinalang.langserver.hover.util.HoverUtil;
import org.ballerinalang.model.tree.Node;
import org.ballerinalang.model.tree.TopLevelNode;
import org.ballerinalang.model.tree.statements.StatementNode;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolEnter;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolResolver;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolEnv;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.tree.*;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangAnnotAttachmentAttribute;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangAnnotAttachmentAttributeValue;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangArrayLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangBinaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangConnectorInit;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangFieldBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLambdaFunction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangStringTemplateLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTernaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeCastExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeConversionExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeofExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangUnaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLAttribute;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLAttributeAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLCommentLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLElementLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLProcInsLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLQName;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLQuotedString;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLTextLiteral;
import org.wso2.ballerinalang.compiler.tree.statements.BLangAbort;
import org.wso2.ballerinalang.compiler.tree.statements.BLangAssignment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBind;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBreak;
import org.wso2.ballerinalang.compiler.tree.statements.BLangCatch;
import org.wso2.ballerinalang.compiler.tree.statements.BLangComment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangExpressionStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangForkJoin;
import org.wso2.ballerinalang.compiler.tree.statements.BLangIf;
import org.wso2.ballerinalang.compiler.tree.statements.BLangNext;
import org.wso2.ballerinalang.compiler.tree.statements.BLangRetry;
import org.wso2.ballerinalang.compiler.tree.statements.BLangReturn;
import org.wso2.ballerinalang.compiler.tree.statements.BLangThrow;
import org.wso2.ballerinalang.compiler.tree.statements.BLangTransaction;
import org.wso2.ballerinalang.compiler.tree.statements.BLangTryCatchFinally;
import org.wso2.ballerinalang.compiler.tree.statements.BLangVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangWhile;
import org.wso2.ballerinalang.compiler.tree.statements.BLangWorkerReceive;
import org.wso2.ballerinalang.compiler.tree.statements.BLangWorkerSend;
import org.wso2.ballerinalang.compiler.tree.statements.BLangXMLNSStatement;
import org.wso2.ballerinalang.compiler.tree.types.BLangArrayType;
import org.wso2.ballerinalang.compiler.tree.types.BLangBuiltInRefTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangConstrainedType;
import org.wso2.ballerinalang.compiler.tree.types.BLangEndpointTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangFunctionTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangUserDefinedType;
import org.wso2.ballerinalang.compiler.tree.types.BLangValueType;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HoverTreeVisitor extends BLangNodeVisitor {

    private ArrayList<HoverResolvedNode> positionFilteredNodes;
    private String fileName;
    private Position position;
    private boolean terminateVisitor = false;
    private SymbolEnv symbolEnv;
    private SymbolEnter symbolEnter;
    private Class cursorPositionResolver;
    private Map<Class, CursorPositionResolver> cursorPositionResolvers;
    private Object previousNode;

    public HoverTreeVisitor(CompilerContext context, String fileName, TextDocumentPositionParams position, ArrayList<HoverResolvedNode> nodes) {
        this.position = position.getPosition();
        this.fileName = fileName;
        this.positionFilteredNodes = nodes;
        cursorPositionResolvers = new HashMap<>();
        symbolEnter = SymbolEnter.getInstance(context);
        this.position.setLine(this.position.getLine() + 1);
    }

    // Visitor methods

    public void visit(BLangPackage pkgNode) {
        SymbolEnv pkgEnv = symbolEnter.packageEnvs.get(pkgNode.symbol);

        // Then visit each top-level element sorted using the compilation unit
        List<TopLevelNode> topLevelNodes = pkgNode.topLevelNodes.stream().filter(node ->
                node.getPosition().getSource().getCompilationUnitName().equals(this.fileName)
        ).collect(Collectors.toList());

        if (topLevelNodes.isEmpty()) {
            terminateVisitor = true;
            acceptNode(null, null);
        } else {
            cursorPositionResolver = PackageNodeScopeResolver.class;
            topLevelNodes.forEach(topLevelNode -> acceptNode((BLangNode) topLevelNode, pkgEnv));
        }
    }

    public void visit(BLangImportPackage importPkgNode) {
        BPackageSymbol pkgSymbol = importPkgNode.symbol;
        SymbolEnv pkgEnv = symbolEnter.packageEnvs.get(pkgSymbol);
        acceptNode(pkgEnv.node, pkgEnv);
    }

    public void visit(BLangXMLNS xmlnsNode) {
        throw new AssertionError();
    }

    public void visit(BLangFunction funcNode) {
        // Check for native functions
        BSymbol funcSymbol = funcNode.symbol;
        if (Symbols.isNative(funcSymbol)) {
            return;
        }
        SymbolEnv funcEnv = SymbolEnv.createFunctionEnv(funcNode, funcSymbol.scope, symbolEnv);
        previousNode = funcNode;

        // Cursor position is calculated against the Block statement scope resolver
        cursorPositionResolver = BlockStatementScopeResolver.class;
        this.acceptNode(funcNode.body, funcEnv);

        // Process workers
        if (terminateVisitor && !funcNode.workers.isEmpty()) {
            terminateVisitor = false;
        }
        funcNode.workers.forEach(e -> this.symbolEnter.defineNode(e, funcEnv));
        funcNode.workers.forEach(e -> this.acceptNode(e, funcEnv));
    }

    public void visit(BLangStruct structNode) {
        int a = 0;
    }

    @Override
    public void visit(BLangAnnotation annotationNode) {
        int a = 0;
    }

    @Override
    public void visit(BLangVariable varNode) {
        int a = 0;
    }

    @Override
    public void visit(BLangLiteral litNode) {
        int a = 0;
    }

    @Override
    public void visit(BLangSimpleVarRef varRefExpr) {
        int a = 0;
    }

    // Statements

    @Override
    public void visit(BLangBlockStmt blockNode) {
        SymbolEnv blockEnv = SymbolEnv.createBlockEnv(blockNode, symbolEnv);
        // Cursor position is calculated against the Block statement scope resolver
        this.cursorPositionResolver = BlockStatementScopeResolver.class;
        if (blockNode.stmts.isEmpty()) {

        } else {
            blockNode.stmts.forEach(stmt -> this.acceptNode(stmt, blockEnv));
        }
    }

    @Override
    public void visit(BLangVariableDef varDefNode) {
        int b = 0;
    }

    @Override
    public void visit(BLangAssignment assignNode) {
        if(assignNode.expr != null) {
            this.acceptNode(assignNode.expr,null);
        }
    }

    @Override
    public void visit(BLangExpressionStmt exprStmtNode) {
        if (exprStmtNode.getPosition().sLine <= this.position.getLine()
                && exprStmtNode.getPosition().eLine >= this.position.getLine()) {
            this.acceptNode(exprStmtNode.expr, null);
        }
    }

    @Override
    public void visit(BLangIf ifNode) {
        int b = 0;
    }

    public void visit(BLangInvocation invocationExpr) {
        BInvokableSymbol symbol = (BInvokableSymbol) invocationExpr.symbol;
        if (HoverUtil.isMatchingPosition(invocationExpr.getPosition(), this.position)) {
            HoverResolvedNode hoverResolvedNode = new HoverResolvedNode();
            hoverResolvedNode.setPackageID(invocationExpr.symbol.pkgID);
            hoverResolvedNode.setKind(invocationExpr.symbol.kind);
            hoverResolvedNode.setName(invocationExpr.name);
            hoverResolvedNode.setPreviousObject(this.previousNode);
            positionFilteredNodes.add(hoverResolvedNode);
        }
    }


    public void visit(BLangWhile whileNode) {

    }

    public void visit(BLangTransformer transformerNode) {

    }

    public void visit(BLangConnector connectorNode) {

    }

    public void visit(BLangAction actionNode) {

    }

    public void visit(BLangService serviceNode) {

    }

    public void visit(BLangResource resourceNode) {

    }

    @Override
    public void visit(BLangTryCatchFinally tryCatchFinally) {

    }

    @Override
    public void visit(BLangCatch bLangCatch) {

    }

    @Override
    public void visit(BLangTransaction transactionNode) {

    }

    @Override
    public void visit(BLangAbort abortNode) {

    }

    @Override
    public void visit(BLangRetry retryNode) {

    }

    private BLangVariableDef createVarDef(BLangVariable var) {
        BLangVariableDef varDefNode = new BLangVariableDef();
        varDefNode.var = var;
        varDefNode.pos = var.pos;
        return varDefNode;
    }

    private BLangBlockStmt generateCodeBlock(StatementNode... statements) {
        BLangBlockStmt block = new BLangBlockStmt();
        for (StatementNode stmt : statements) {
            block.addStatement(stmt);
        }
        return block;
    }

    @Override
    public void visit(BLangForkJoin forkJoin) {

    }

    @Override
    public void visit(BLangWorker workerNode) {

    }

    @Override
    public void visit(BLangWorkerSend workerSendNode) {

    }

    @Override
    public void visit(BLangWorkerReceive workerReceiveNode) {

    }

    @Override
    public void visit(BLangReturn returnNode) {

    }

    public void visit(BLangNext nextNode) {

    }

    @Override
    public void visit(BLangComment comment) {

    }

    private void acceptNode(BLangNode node, SymbolEnv env) {

        if (this.terminateVisitor) {
            return;
        }

        SymbolEnv prevEnv = this.symbolEnv;
        this.symbolEnv = env;
        node.accept(this);
        this.symbolEnv = prevEnv;
    }
}
